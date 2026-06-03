package tempeststudios.inventorysort.compat.sort;

import net.minecraft.world.item.ItemStack;

public final class ItemStackCompat {
    private ItemStackCompat() {
    }

    public static Object identityData(ItemStack stack) {
        return stack.getTag();
    }

    public static int identityHash(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().hashCode() : 0;
    }

    public static boolean sameItemAndComponents(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }
}
