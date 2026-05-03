package tempeststudios.inventorysort.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;
import tempeststudios.inventorysort.CatalogSession;
import tempeststudios.inventorysort.ContainerIdentity;
import tempeststudios.inventorysort.ItemLocationTracker;
import tempeststudios.inventorysort.InventorySortClient;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerTrackingMixin {

    @Shadow public AbstractContainerMenu menu;

    @Unique private ContainerIdentity inventorySort$capturedIdentity;
    @Unique private String inventorySort$capturedContainerType;
    @Unique private boolean inventorySort$isShulker;
    @Unique private String inventorySort$shulkerIdentifier;
    @Unique private boolean inventorySort$isPlayerInventory;
    @Unique private String inventorySort$screenClassName;

    @Inject(method = "init", at = @At("TAIL"))
    private void onContainerInit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        inventorySort$screenClassName = screen.getClass().getSimpleName();
        String menuClassName = menu.getClass().getSimpleName();

        InventorySortClient.LOGGER.info("=== Container Screen Opened ===");
        InventorySortClient.LOGGER.info("  Screen Class: {}", inventorySort$screenClassName);
        InventorySortClient.LOGGER.info("  Menu Class: {}", menuClassName);
        InventorySortClient.LOGGER.info("  Total Slots: {}", menu.slots.size());
        InventorySortClient.LOGGER.info("===============================");

        inventorySort$isPlayerInventory = inventorySort$screenClassName.equals("InventoryScreen") ||
                inventorySort$screenClassName.equals("CreativeModeInventoryScreen");

        if (!inventorySort$isPlayerInventory) {
            BlockPos containerPos = tempeststudios.inventorysort.ContainerPositionCapture.getLastLookedAtBlock();
            
            if (containerPos != null) {
                inventorySort$capturedIdentity = ContainerIdentity.fromLookedAt(client, containerPos);
                
                // If we captured an identity, consume the interaction so other screens (like crafting tables)
                // don't accidentally reuse it.
                if (inventorySort$capturedIdentity != null) {
                    tempeststudios.inventorysort.ContainerPositionCapture.clear();
                }
            }
            
            inventorySort$capturedContainerType = "Container";
            if (inventorySort$capturedIdentity != null) {
                inventorySort$capturedContainerType = inventorySort$capturedIdentity.getContainerType();
            } else if (containerPos != null) {
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
                        inventorySort$capturedContainerType = blockName.substring(0, 1).toUpperCase() + blockName.substring(1);
                    }
                } catch (Exception e) {}
            }

            inventorySort$shulkerIdentifier = null;
            inventorySort$isShulker = false;

            if (inventorySort$capturedIdentity == null && containerPos == null) {
                inventorySort$shulkerIdentifier = generateShulkerIdentifier(menu);
                inventorySort$isShulker = true;
                inventorySort$capturedContainerType = "Shulker Box";
            } else if (inventorySort$capturedIdentity != null) {
                if (client.level.getBlockState(containerPos).getBlock() instanceof ShulkerBoxBlock) {
                    inventorySort$shulkerIdentifier = generateShulkerIdentifier(menu, containerPos);
                    inventorySort$isShulker = true;
                    inventorySort$capturedContainerType = "Shulker Box";
                }
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onContainerClosed(CallbackInfo ci) {
        trackContainerItems();
    }

    private void trackContainerItems() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        ItemLocationTracker tracker = ItemLocationTracker.getInstance();

        InventorySortClient.LOGGER.info("Tracking screen on close: {} (isPlayerInventory: {})", inventorySort$screenClassName, inventorySort$isPlayerInventory);

        if (inventorySort$isPlayerInventory) {
            // Player inventory is tracked continuously by InventoryHistorySampler, no need to duplicate
            if (CatalogSession.isActive() && CatalogSession.getActive().shouldIncludeInventory()) {
                trackInventoryForCatalog(client);
            }
        } else {
            trackContainer(tracker, client, inventorySort$screenClassName);

            if (CatalogSession.isActive()) {
                trackContainerForCatalog(client, inventorySort$screenClassName);
            }
        }
    }

    private void trackContainer(ItemLocationTracker tracker, Minecraft client, String screenClassName) {
        int containerSlots = menu.slots.size() - 36; // Subtract player inventory
        int itemsTracked = 0;
        List<ItemStack> fixedContainerItems = new ArrayList<>();

        InventorySortClient.LOGGER.info("Scanning {} container slots for items on close", containerSlots);

        // Validation: If we have a captured identity, ensure the slot count matches the container type.
        // This prevents "ghost" tracking where a different screen (like a crafting table) uses the
        // last-interacted-block identity and wipes its data.
        if (inventorySort$capturedIdentity != null) {
            int expected = inventorySort$capturedIdentity.getExpectedSlotCount();
            if (expected != -1 && expected != containerSlots) {
                InventorySortClient.LOGGER.warn("Rejecting tracking for {} - slot count mismatch (got {}, expected {})",
                        inventorySort$capturedIdentity.getPositionLabel(), containerSlots, expected);
                return;
            }
        }

        for (int i = 0; i < containerSlots; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();

            if (!stack.isEmpty()) {
                if (inventorySort$isShulker && inventorySort$shulkerIdentifier != null) {
                    tracker.trackItemInShulker(stack, inventorySort$shulkerIdentifier);
                    itemsTracked++;
                } else if (inventorySort$capturedIdentity != null) {
                    fixedContainerItems.add(stack.copy());
                    itemsTracked++;
                }
            }
        }

        if (inventorySort$capturedIdentity != null && !inventorySort$isShulker) {
            tracker.replaceContainerSnapshot(inventorySort$capturedIdentity, fixedContainerItems);
        }

        if (itemsTracked == 0) {
            InventorySortClient.LOGGER.info("No items found in {} (container is empty)", inventorySort$capturedContainerType);
        } else {
            InventorySortClient.LOGGER.info("Tracked {} items from {} container", itemsTracked, inventorySort$capturedContainerType);
        }

        if (inventorySort$isShulker) {
            InventorySortClient.LOGGER.info("Completed shulker box tracking (ID: {})", inventorySort$shulkerIdentifier);
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
        if (inventorySort$capturedIdentity == null) {
            InventorySortClient.LOGGER.warn("Cannot track container for catalog - no identity captured");
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("⚠ Container position unknown - not counted").withStyle(net.minecraft.ChatFormatting.RED), false);
            return;
        }

        String containerType = inventorySort$capturedContainerType;

        // Generate fingerprint
        String fingerprint = CatalogSession.generateFingerprint(inventorySort$capturedIdentity);

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
            InventorySortClient.LOGGER.info("Cataloged new container at {} with {} items", inventorySort$capturedIdentity.getPositionLabel(), items.size());
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(String.format("✓ Cataloged %s (%d items)", containerType, items.size())).withStyle(net.minecraft.ChatFormatting.GREEN), false);
        } else {
            InventorySortClient.LOGGER.info("Skipped already-cataloged container at {}", inventorySort$capturedIdentity.getPositionLabel());
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
