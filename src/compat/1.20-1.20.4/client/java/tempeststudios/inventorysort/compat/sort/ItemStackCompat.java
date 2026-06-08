package tempeststudios.inventorysort.compat.sort;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BundleItem;

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

    public static boolean isBundle(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BundleItem;
    }
}
