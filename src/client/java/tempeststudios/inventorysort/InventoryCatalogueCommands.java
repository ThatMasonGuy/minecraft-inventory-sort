package tempeststudios.inventorysort;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class InventoryCatalogueCommands {

    private InventoryCatalogueCommands() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("inventorycatalogue")
                .then(ClientCommandManager.literal("start")
                        .executes(context -> startCatalog(context, false))
                        .then(ClientCommandManager.literal("includeInventory")
                                .executes(context -> startCatalog(context, true))
                        )
                )
                .then(ClientCommandManager.literal("stop")
                        .executes(InventoryCatalogueCommands::stopCatalog)
                )
                .then(ClientCommandManager.literal("status")
                        .executes(InventoryCatalogueCommands::catalogStatus)
                )
                .then(ClientCommandManager.literal("report")
                        .executes(InventoryCatalogueCommands::catalogReport)
                )
                .then(ClientCommandManager.literal("clear")
                        .executes(InventoryCatalogueCommands::clearCatalog)
                );
    }

    private static int startCatalog(CommandContext<FabricClientCommandSource> context, boolean includeInventory) {
        if (CatalogSession.isActive()) {
            context.getSource().sendError(Component.literal("A catalog session is already active! Use /inventorycatalogue stop to end it."));
            return 0;
        }

        CatalogSession.start(includeInventory);

        context.getSource().sendFeedback(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GREEN));
        context.getSource().sendFeedback(Component.literal("Catalog session started!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        context.getSource().sendFeedback(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GREEN));
        context.getSource().sendFeedback(Component.empty());
        context.getSource().sendFeedback(Component.literal("World: " + CatalogSession.getActive().getNamespace()).withStyle(ChatFormatting.GRAY));
        context.getSource().sendFeedback(Component.literal("Open containers to catalog their contents.").withStyle(ChatFormatting.GRAY));
        context.getSource().sendFeedback(Component.literal(String.format("Include inventory: %s", includeInventory ? "Yes" : "No")).withStyle(ChatFormatting.GRAY));
        context.getSource().sendFeedback(Component.literal("Existing catalogue for this world is kept - use /inventorycatalogue clear to reset.").withStyle(ChatFormatting.GRAY));
        context.getSource().sendFeedback(Component.empty());
        context.getSource().sendFeedback(Component.literal("Use /inventorycatalogue stop when done.").withStyle(ChatFormatting.YELLOW));

        tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info("Catalog session started (includeInventory: {})", includeInventory);
        return 1;
    }

    private static int stopCatalog(CommandContext<FabricClientCommandSource> context) {
        if (!CatalogSession.isActive()) {
            context.getSource().sendError(Component.literal("No active catalog session! Use /inventorycatalogue start to begin."));
            return 0;
        }

        List<Component> report = CatalogSession.stop();

        for (Component line : report) {
            context.getSource().sendFeedback(line);
        }

        tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info("Catalog session stopped and report generated");
        return 1;
    }

    private static int catalogStatus(CommandContext<FabricClientCommandSource> context) {
        if (!CatalogSession.isActive()) {
            context.getSource().sendFeedback(Component.literal("No active catalog session.").withStyle(ChatFormatting.GRAY));
            return 0;
        }

        CatalogSession session = CatalogSession.getActive();

        context.getSource().sendFeedback(Component.literal("=".repeat(40)).withStyle(ChatFormatting.AQUA));
        context.getSource().sendFeedback(Component.literal("Catalog Session Status").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        context.getSource().sendFeedback(Component.literal("=".repeat(40)).withStyle(ChatFormatting.AQUA));
        context.getSource().sendFeedback(Component.empty());
        context.getSource().sendFeedback(Component.literal("World: " + session.getNamespace()).withStyle(ChatFormatting.GRAY));
        context.getSource().sendFeedback(Component.literal(String.format("Locations catalogued: %d", session.getLocationCount())).withStyle(ChatFormatting.WHITE));
        context.getSource().sendFeedback(Component.literal(String.format("Unique items: %d", session.getUniqueItems())).withStyle(ChatFormatting.WHITE));
        context.getSource().sendFeedback(Component.literal(String.format("Total items: %,d", session.getTotalItems())).withStyle(ChatFormatting.WHITE));
        context.getSource().sendFeedback(Component.empty());
        context.getSource().sendFeedback(Component.literal("Use /inventorycatalogue stop to finish.").withStyle(ChatFormatting.GRAY));

        return 1;
    }

    private static int catalogReport(CommandContext<FabricClientCommandSource> context) {
        if (!CatalogSession.isActive()) {
            context.getSource().sendError(Component.literal("No active catalog session! Use /inventorycatalogue start to begin."));
            return 0;
        }

        for (Component line : CatalogSession.getActive().buildReport(false)) {
            context.getSource().sendFeedback(line);
        }
        return 1;
    }

    private static int clearCatalog(CommandContext<FabricClientCommandSource> context) {
        if (CatalogSession.isActive()) {
            context.getSource().sendError(Component.literal("Stop the active session before clearing (use /inventorycatalogue stop)."));
            return 0;
        }

        CatalogStore.getInstance().reloadForCurrentNamespace();
        int cleared = CatalogStore.getInstance().locationCount();
        CatalogStore.getInstance().clear();
        context.getSource().sendFeedback(Component.literal(String.format("Cleared catalogue for this world (%d locations removed).", cleared)).withStyle(ChatFormatting.GREEN));
        return 1;
    }
}
