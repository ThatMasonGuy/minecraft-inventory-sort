package tempeststudios.inventorysort;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PlayerInventorySnapshot {
    private PlayerInventorySnapshot() {
    }

    public static List<ItemStack> collect(Inventory inventory, ItemStack carried) {
        List<ItemStack> items = new ArrayList<>();
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack != null && !stack.isEmpty()) {
                    items.add(stack.copy());
                }
            }
        }
        if (carried != null && !carried.isEmpty()) {
            items.add(carried.copy());
        }
        return items;
    }
}
