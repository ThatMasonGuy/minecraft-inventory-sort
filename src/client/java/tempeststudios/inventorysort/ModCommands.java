package tempeststudios.inventorysort;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Registers client-side commands for the inventory sort mod
 */
public class ModCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
                ClientCommandManager.literal("inventorysort")
                        .then(ClientCommandManager.literal("catalog")
                                .then(ClientCommandManager.literal("start")
                                        .executes(context -> startCatalog(context, false))
                                        .then(ClientCommandManager.literal("includeInventory")
                                                .executes(context -> startCatalog(context, true))
                                        )
                                )
                                .then(ClientCommandManager.literal("stop")
                                        .executes(ModCommands::stopCatalog)
                                )
                                .then(ClientCommandManager.literal("status")
                                        .executes(ModCommands::catalogStatus)
                                )
                        )
                        .then(ClientCommandManager.literal("world")
                                .then(ClientCommandManager.literal("list")
                                        .executes(ModCommands::listWorldProfiles)
                                )
                                .then(ClientCommandManager.literal("use")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                                .executes(context -> useWorldProfile(context, StringArgumentType.getString(context, "name")))
                                        )
                                )
                                .then(ClientCommandManager.literal("default")
                                        .executes(context -> useWorldProfile(context, "default"))
                                )
                                .then(ClientCommandManager.literal("current")
                                        .executes(ModCommands::currentWorldProfile)
                                )
                        )
        );
    }

    private static int listWorldProfiles(CommandContext<FabricClientCommandSource> context) {
        String serverKey = currentServerKeyOrError(context);
        if (serverKey == null) {
            return 0;
        }

        ServerWorldProfileManager manager = ServerWorldProfileManager.getInstance();
        String active = manager.getActiveProfile(serverKey);
        context.getSource().sendFeedback(Component.literal("Tracked worlds for " + serverKey + ":").withStyle(ChatFormatting.AQUA));
        for (String profile : manager.getProfiles(serverKey)) {
            ChatFormatting style = profile.equals(active) ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            String prefix = profile.equals(active) ? "* " : "  ";
            context.getSource().sendFeedback(Component.literal(prefix + manager.displayName(profile)).withStyle(style));
        }
        return 1;
    }

    private static int currentWorldProfile(CommandContext<FabricClientCommandSource> context) {
        String serverKey = currentServerKeyOrError(context);
        if (serverKey == null) {
            return 0;
        }

        String active = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);
        context.getSource().sendFeedback(Component.literal("Current tracked world: " + active).withStyle(ChatFormatting.GREEN));
        context.getSource().sendFeedback(Component.literal("Namespace: " + TrackingNamespace.current(Minecraft.getInstance())).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int useWorldProfile(CommandContext<FabricClientCommandSource> context, String profileName) {
        String serverKey = currentServerKeyOrError(context);
        if (serverKey == null) {
            return 0;
        }

        ServerWorldProfileManager manager = ServerWorldProfileManager.getInstance();
        manager.setActiveProfile(serverKey, profileName);
        String active = manager.getActiveProfile(serverKey);
        context.getSource().sendFeedback(Component.literal("Tracking server world: " + active).withStyle(ChatFormatting.GREEN));
        context.getSource().sendFeedback(Component.literal("Namespace: " + TrackingNamespace.current(Minecraft.getInstance())).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static String currentServerKeyOrError(CommandContext<FabricClientCommandSource> context) {
        String serverKey = TrackingNamespace.currentServerKey(Minecraft.getInstance());
        if (serverKey == null) {
            context.getSource().sendError(Component.literal("Server world profiles are only available on multiplayer servers."));
            return null;
        }
        return serverKey;
    }

    private static int startCatalog(CommandContext<FabricClientCommandSource> context, boolean includeInventory) {
        if (CatalogSession.isActive()) {
            context.getSource().sendError(Component.literal("A catalog session is already active! Use /inventorysort catalog stop to end it."));
            return 0;
        }

        CatalogSession.start(includeInventory);

        context.getSource().sendFeedback(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GREEN));
        context.getSource().sendFeedback(Component.literal("📦 Catalog session started!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        context.getSource().sendFeedback(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GREEN));
        context.getSource().sendFeedback(Component.empty());
        context.getSource().sendFeedback(Component.literal("Open containers to catalog their contents.").withStyle(ChatFormatting.GRAY));
        context.getSource().sendFeedback(Component.literal(String.format("Include inventory: %s", includeInventory ? "Yes" : "No")).withStyle(ChatFormatting.GRAY));
        context.getSource().sendFeedback(Component.empty());
        context.getSource().sendFeedback(Component.literal("Use /inventorysort catalog stop when done.").withStyle(ChatFormatting.YELLOW));

        InventorySortClient.LOGGER.info("Catalog session started (includeInventory: {})", includeInventory);
        return 1;
    }

    private static int stopCatalog(CommandContext<FabricClientCommandSource> context) {
        if (!CatalogSession.isActive()) {
            context.getSource().sendError(Component.literal("No active catalog session! Use /inventorysort catalog start to begin."));
            return 0;
        }

        List<Component> report = CatalogSession.stop();

        // Send the report to chat
        for (Component line : report) {
            context.getSource().sendFeedback(line);
        }

        InventorySortClient.LOGGER.info("Catalog session stopped and report generated");
        return 1;
    }

    private static int catalogStatus(CommandContext<FabricClientCommandSource> context) {
        if (!CatalogSession.isActive()) {
            context.getSource().sendFeedback(Component.literal("No active catalog session.").withStyle(ChatFormatting.GRAY));
            return 0;
        }

        CatalogSession session = CatalogSession.getActive();

        context.getSource().sendFeedback(Component.literal("=".repeat(40)).withStyle(ChatFormatting.AQUA));
        context.getSource().sendFeedback(Component.literal("📊 Catalog Session Status").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        context.getSource().sendFeedback(Component.literal("=".repeat(40)).withStyle(ChatFormatting.AQUA));
        context.getSource().sendFeedback(Component.empty());
        context.getSource().sendFeedback(Component.literal(String.format("Containers tracked: %d", session.getContainersTracked())).withStyle(ChatFormatting.WHITE));
        context.getSource().sendFeedback(Component.literal(String.format("Unique items: %d", session.getUniqueItems())).withStyle(ChatFormatting.WHITE));
        context.getSource().sendFeedback(Component.literal(String.format("Total items: %,d", session.getTotalItems())).withStyle(ChatFormatting.WHITE));
        context.getSource().sendFeedback(Component.empty());
        context.getSource().sendFeedback(Component.literal("Use /inventorysort catalog stop to finish.").withStyle(ChatFormatting.GRAY));

        return 1;
    }
}
