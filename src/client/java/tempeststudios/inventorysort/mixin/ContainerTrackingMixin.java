package tempeststudios.inventorysort.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.CatalogSession;
import tempeststudios.inventorysort.ItemLocationTracker;
import tempeststudios.inventorysort.InventorySortClient;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerTrackingMixin {

    @Shadow public AbstractContainerMenu menu;

    private boolean hasTrackedThisContainer = false;

    @Inject(method = "init", at = @At("TAIL"))
    private void onContainerInit(CallbackInfo ci) {
        // Reset flag for new container
        hasTrackedThisContainer = false;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        String screenClassName = screen.getClass().getSimpleName();
        String menuClassName = menu.getClass().getSimpleName();

        InventorySortClient.LOGGER.info("=== Container Screen Opened ===");
        InventorySortClient.LOGGER.info("  Screen Class: {}", screenClassName);
        InventorySortClient.LOGGER.info("  Menu Class: {}", menuClassName);
        InventorySortClient.LOGGER.info("  Total Slots: {}", menu.slots.size());
        InventorySortClient.LOGGER.info("===============================");

        // Delay tracking by 2 ticks to ensure container contents are fully loaded from server
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().execute(() -> {
                if (!hasTrackedThisContainer) {
                    trackContainerItems();
                    hasTrackedThisContainer = true;
                }
            });
        });
    }

    private void trackContainerItems() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        ItemLocationTracker tracker = ItemLocationTracker.getInstance();
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        // Determine if this is player inventory by checking the screen class name
        String screenClassName = screen.getClass().getSimpleName();
        boolean isPlayerInventory = screenClassName.equals("InventoryScreen") ||
                screenClassName.equals("CreativeModeInventoryScreen");

        InventorySortClient.LOGGER.info("Tracking screen: {} (isPlayerInventory: {})", screenClassName, isPlayerInventory);

        if (isPlayerInventory) {
            // Track player inventory items
            trackPlayerInventory(tracker, client);

            // If catalog session is active and includes inventory, track for catalog
            if (CatalogSession.isActive() && CatalogSession.getActive().shouldIncludeInventory()) {
                trackInventoryForCatalog(client);
            }
        } else {
            // Track container items
            trackContainer(tracker, client, screenClassName);

            // If catalog session is active, track for catalog
            if (CatalogSession.isActive()) {
                trackContainerForCatalog(client, screenClassName);
            }
        }
    }

    private void trackPlayerInventory(ItemLocationTracker tracker, Minecraft client) {
        // Get player position for tracking
        BlockPos playerPos = null;
        ResourceKey<Level> dimension = null;

        if (client.player != null && client.level != null) {
            playerPos = client.player.blockPosition();
            dimension = client.level.dimension();
        }

        // Only track main inventory and hotbar (slots 0-35 in player inventory)
        int itemsTracked = 0;
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && slot.getContainerSlot() < 36) {
                if (playerPos != null && dimension != null) {
                    // Track with player position
                    tracker.trackItem(stack, playerPos, dimension, "Player Inventory");
                } else {
                    // Fallback to position-less tracking
                    tracker.trackItemInInventory(stack);
                }
                itemsTracked++;
            }
        }

        if (playerPos != null) {
            InventorySortClient.LOGGER.info("Tracked {} items from Player Inventory at {}", itemsTracked, playerPos);
        } else {
            InventorySortClient.LOGGER.info("Tracked {} items from Player Inventory", itemsTracked);
        }
    }

    private void trackContainer(ItemLocationTracker tracker, Minecraft client, String screenClassName) {
        // Get the position of the container the player opened
        BlockPos containerPos = tempeststudios.inventorysort.ContainerPositionCapture.getLastLookedAtBlock();
        ResourceKey<Level> dimension = client.level.dimension();

        // Determine container type with better extraction
        String containerType = "Container"; // Default fallback
        if (containerPos != null) {
            try {
                // Get block at position
                var blockState = client.level.getBlockState(containerPos);
                var block = blockState.getBlock();

                // Try to get descriptive name first
                String blockName = block.getName().getString();

                // Clean up the name - remove "Block{" and "}" if present
                if (blockName.contains("Block{")) {
                    blockName = blockName.substring(blockName.indexOf("Block{") + 6, blockName.indexOf("}"));
                }

                // Get the last part after namespace (minecraft:chest -> chest)
                if (blockName.contains(":")) {
                    blockName = blockName.substring(blockName.lastIndexOf(":") + 1);
                }

                // Capitalize first letter
                if (!blockName.isEmpty()) {
                    containerType = blockName.substring(0, 1).toUpperCase() + blockName.substring(1);
                }

                InventorySortClient.LOGGER.info("Extracted container type: '{}' from block: {}", containerType, block.getClass().getSimpleName());
            } catch (Exception e) {
                InventorySortClient.LOGGER.warn("Failed to extract container type, using default", e);
            }
        }

        // Determine if we're looking at a shulker box or container at a fixed position
        String shulkerIdentifier = null;
        boolean isShulker = false;

        if (containerPos == null) {
            // No position means it's likely a shulker box or other portable container
            shulkerIdentifier = generateShulkerIdentifier(menu);
            isShulker = true;
            containerType = "Shulker Box";

            InventorySortClient.LOGGER.info("Container opened without position - treating as shulker");
        } else {
            // Check if this position has a shulker box block
            if (client.level.getBlockState(containerPos).getBlock() instanceof ShulkerBoxBlock) {
                shulkerIdentifier = generateShulkerIdentifier(menu, containerPos);
                isShulker = true;
                containerType = "Shulker Box";

                InventorySortClient.LOGGER.info("Shulker box detected at position {}", containerPos);
            } else {
                InventorySortClient.LOGGER.info("Container detected at position {} (type: {})", containerPos, containerType);
            }
        }

        int containerSlots = menu.slots.size() - 36; // Subtract player inventory
        int itemsTracked = 0;

        InventorySortClient.LOGGER.info("Scanning {} container slots for items", containerSlots);

        for (int i = 0; i < containerSlots; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();

            if (!stack.isEmpty()) {
                String itemName = stack.getItem().toString();
                InventorySortClient.LOGGER.info("Found item in slot {}: {} x{}", i, itemName, stack.getCount());

                if (isShulker && shulkerIdentifier != null) {
                    tracker.trackItemInShulker(stack, shulkerIdentifier);
                    itemsTracked++;
                } else if (containerPos != null) {
                    tracker.trackItem(stack, containerPos, dimension, containerType);
                    itemsTracked++;
                }
            }
        }

        if (itemsTracked == 0) {
            InventorySortClient.LOGGER.info("No items found in {} (container is empty)", containerType);
        } else {
            InventorySortClient.LOGGER.info("Tracked {} items from {} container", itemsTracked, containerType);
        }

        if (isShulker) {
            InventorySortClient.LOGGER.info("Completed shulker box tracking (ID: {})", shulkerIdentifier);
        }
    }

    /**
     * Generate a semi-stable identifier for shulkers based on position and NBT
     */
    private String generateShulkerIdentifier(AbstractContainerMenu menu, BlockPos pos) {
        // Use position as part of identifier since shulker is at a known location
        return "shulker_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_" + generateContainerHash(menu);
    }

    /**
     * Generate identifier for portable shulkers (in inventory or being carried)
     */
    private String generateShulkerIdentifier(AbstractContainerMenu menu) {
        // Use a hash of the container structure and some items
        return "shulker_portable_" + generateContainerHash(menu);
    }

    /**
     * Generate a hash based on container contents to identify it
     */
    private String generateContainerHash(AbstractContainerMenu menu) {
        int containerSlots = menu.slots.size() - 36;

        // Hash first few items to create a semi-stable ID
        int itemHash = 0;
        for (int i = 0; i < Math.min(5, containerSlots); i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                itemHash = itemHash * 31 + stack.getItem().hashCode();
                itemHash = itemHash * 31 + stack.getCount();
            }
        }

        return Integer.toHexString(Math.abs(itemHash));
    }

    /**
     * Track a container for the active catalog session
     */
    private void trackContainerForCatalog(Minecraft client, String screenClassName) {
        BlockPos containerPos = tempeststudios.inventorysort.ContainerPositionCapture.getLastLookedAtBlock();
        ResourceKey<Level> dimension = client.level.dimension();

        if (containerPos == null) {
            InventorySortClient.LOGGER.warn("Cannot track container for catalog - no position available");
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("⚠ Container position unknown - not counted").withStyle(net.minecraft.ChatFormatting.RED), false);
            return;
        }

        // Determine container type
        String containerType = "Container";
        try {
            var blockState = client.level.getBlockState(containerPos);
            var block = blockState.getBlock();
            String blockName = block.getName().getString();

            if (blockName.contains("Block{")) {
                blockName = blockName.substring(blockName.indexOf("Block{") + 6, blockName.indexOf("}"));
            }
            if (blockName.contains(":")) {
                blockName = blockName.substring(blockName.lastIndexOf(":") + 1);
            }
            if (!blockName.isEmpty()) {
                containerType = blockName.substring(0, 1).toUpperCase() + blockName.substring(1);
            }
        } catch (Exception e) {
            InventorySortClient.LOGGER.warn("Failed to extract container type", e);
        }

        // Generate fingerprint
        String fingerprint = CatalogSession.generateFingerprint(containerPos, dimension, containerType);

        // Collect items
        int containerSlots = menu.slots.size() - 36;
        java.util.List<ItemStack> items = new java.util.ArrayList<>();

        for (int i = 0; i < containerSlots; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }

        // Track in catalog session
        boolean newContainer = CatalogSession.getActive().trackContainer(fingerprint, items);

        if (newContainer) {
            InventorySortClient.LOGGER.info("Cataloged new container at {} with {} items", containerPos, items.size());
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(String.format("✓ Cataloged %s (%d items)", containerType, items.size())).withStyle(net.minecraft.ChatFormatting.GREEN), false);
        } else {
            InventorySortClient.LOGGER.info("Skipped already-cataloged container at {}", containerPos);
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("⊘ Container already counted").withStyle(net.minecraft.ChatFormatting.YELLOW), false);
        }
    }

    /**
     * Track player inventory for the active catalog session
     */
    private void trackInventoryForCatalog(Minecraft client) {
        BlockPos playerPos = client.player.blockPosition();
        ResourceKey<Level> dimension = client.level.dimension();

        // Use a special fingerprint for player inventory
        String fingerprint = CatalogSession.generateFingerprint(playerPos, dimension, "Player Inventory");

        // Collect inventory items (slots 0-35)
        java.util.List<ItemStack> items = new java.util.ArrayList<>();

        for (Slot slot : menu.slots) {
            if (slot.getContainerSlot() < 36) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    items.add(stack.copy());
                }
            }
        }

        // Track in catalog session
        boolean newInventory = CatalogSession.getActive().trackContainer(fingerprint, items);

        if (newInventory) {
            InventorySortClient.LOGGER.info("Cataloged player inventory with {} items", items.size());
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(String.format("✓ Cataloged inventory (%d items)", items.size())).withStyle(net.minecraft.ChatFormatting.GREEN), false);
        } else {
            InventorySortClient.LOGGER.info("Skipped already-cataloged inventory");
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("⊘ Inventory already counted").withStyle(net.minecraft.ChatFormatting.YELLOW), false);
        }
    }
}