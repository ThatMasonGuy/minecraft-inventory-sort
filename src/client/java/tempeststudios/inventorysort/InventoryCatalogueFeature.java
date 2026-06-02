package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import tempeststudios.inventorysort.core.InventorySortEvents;

public final class InventoryCatalogueFeature {
    private static boolean initialized = false;

    private InventoryCatalogueFeature() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        InventorySortEvents.NAMESPACE_CHANGED.register(context -> CatalogStore.getInstance().reloadForCurrentNamespace());
        InventorySortEvents.CONTAINER_SNAPSHOT.register(InventoryCatalogueFeature::recordContainerSnapshot);
        InventorySortEvents.INVENTORY_SNAPSHOT.register(InventoryCatalogueFeature::recordInventorySnapshot);
    }

    private static void recordContainerSnapshot(InventorySortEvents.ContainerSnapshotContext context) {
        if (!CatalogSession.isActive()) {
            return;
        }

        CatalogSession session = CatalogSession.getActive();
        CatalogSession.RecordResult result;
        String label;

        if (context.portableShulker() && context.portableShulkerId() != null) {
            result = session.recordShulker(context.portableShulkerId(), context.items());
            label = "Shulker Box";
        } else if (context.identity() != null) {
            result = session.recordContainer(context.identity(), context.items());
            label = context.containerType();
        } else {
            InventorySortClient.LOGGER.warn("Cannot catalog container - no identity captured");
            displayMessage(context.client(), "Container position unknown - not counted", net.minecraft.ChatFormatting.RED);
            return;
        }

        reportCatalogResult(context.client(), result, label, context.items().size());
    }

    private static void recordInventorySnapshot(InventorySortEvents.InventorySnapshotContext context) {
        if (!CatalogSession.isActive() || !CatalogSession.getActive().shouldIncludeInventory()) {
            return;
        }

        CatalogSession.RecordResult result = CatalogSession.getActive().recordInventory(context.items());
        reportCatalogResult(context.client(), result, "inventory", context.items().size());
    }

    private static void reportCatalogResult(Minecraft client,
                                            CatalogSession.RecordResult result,
                                            String label,
                                            int itemCount) {
        switch (result) {
            case ADDED:
                InventorySortClient.LOGGER.info("Cataloged {} ({} stacks)", label, itemCount);
                displayMessage(client, String.format("Cataloged %s (%d stacks)", label, itemCount), net.minecraft.ChatFormatting.GREEN);
                break;
            case UPDATED:
                InventorySortClient.LOGGER.debug("Refreshed cataloged {} ({} stacks)", label, itemCount);
                displayMessage(client, String.format("Updated %s (%d stacks)", label, itemCount), net.minecraft.ChatFormatting.YELLOW);
                break;
            case SKIPPED:
            default:
                InventorySortClient.LOGGER.debug("Skipped cataloging {} (tracking not allowed or world changed)", label);
                break;
        }
    }

    private static void displayMessage(Minecraft client, String message, net.minecraft.ChatFormatting style) {
        if (client != null && client.player != null) {
            client.player.displayClientMessage(Component.literal(message).withStyle(style), false);
        }
    }
}
