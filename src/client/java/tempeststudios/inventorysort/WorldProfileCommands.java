package tempeststudios.inventorysort;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class WorldProfileCommands {

    private WorldProfileCommands() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("world")
                .then(ClientCommandManager.literal("list")
                        .executes(WorldProfileCommands::listWorldProfiles)
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
                        .executes(WorldProfileCommands::currentWorldProfile)
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
        manager.confirmActiveProfile(serverKey);
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
}
