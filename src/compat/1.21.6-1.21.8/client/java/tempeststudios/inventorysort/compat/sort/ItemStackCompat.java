package tempeststudios.inventorysort.compat.sort;

import net.minecraft.world.item.ItemStack;

public final class ItemStackCompat {
    private ItemStackCompat() {
    }

    public static Object identityData(ItemStack stack) {
        return stack.getComponents();
    }

    public static int identityHash(ItemStack stack) {
        return stack.getComponents().hashCode();
    }

    public static boolean sameItemAndComponents(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }
}
