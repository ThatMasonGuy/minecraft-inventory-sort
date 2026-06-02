package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import tempeststudios.inventorysort.core.InventorySortEvents;

public final class InventorySearchFeature {
    private static boolean initialized = false;

    private InventorySearchFeature() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            ItemLocationTracker.getInstance();
            InventorySortClient.LOGGER.info("Item location tracking enabled");
        } catch (Exception e) {
            InventorySortClient.LOGGER.error("Failed to initialize item location tracker", e);
        }

        InventorySortEvents.NAMESPACE_CHANGED.register(context -> {
            ItemLocationTracker.getInstance().reloadForCurrentNamespace();
            InventoryHistorySampler.reset();
        });
        InventorySortEvents.CONTAINER_SNAPSHOT.register(InventorySearchFeature::trackContainerSnapshot);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                InventorySortClient.LOGGER.info("Saving item location data on shutdown");
                ItemLocationTracker.getInstance().save();
            } catch (Exception e) {
                InventorySortClient.LOGGER.error("Failed to save item location data", e);
            }
        }, "ItemLocationTracker-Shutdown"));
    }

    public static void sampleInventory(Minecraft client) {
        InventoryHistorySampler.sample(client);
    }

    private static void trackContainerSnapshot(InventorySortEvents.ContainerSnapshotContext context) {
        ItemLocationTracker tracker = ItemLocationTracker.getInstance();
        int itemsTracked = 0;

        if (context.portableShulker() && context.portableShulkerId() != null) {
            for (ItemStack stack : context.items()) {
                if (!stack.isEmpty()) {
                    tracker.trackItemInShulker(stack, context.portableShulkerId());
                    itemsTracked++;
                }
            }
            if (itemsTracked > 0) {
                tracker.save();
            }
            InventorySortClient.LOGGER.debug("Completed shulker box tracking (ID: {})", context.portableShulkerId());
            return;
        }

        if (context.identity() == null) {
            InventorySortClient.LOGGER.debug("Skipping search snapshot without an identity: {}", context.screenClassName());
            return;
        }

        tracker.replaceContainerSnapshot(context.identity(), context.items());
        itemsTracked = context.items().size();

        if (itemsTracked == 0) {
            InventorySortClient.LOGGER.debug("No items found in {} (container is empty)", context.containerType());
        } else {
            InventorySortClient.LOGGER.debug("Tracked {} items from {} container", itemsTracked, context.containerType());
        }
    }
}
