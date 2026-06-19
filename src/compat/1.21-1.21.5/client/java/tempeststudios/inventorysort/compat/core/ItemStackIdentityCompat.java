package tempeststudios.inventorysort.compat.core;

import net.minecraft.world.item.ItemStack;

public final class ItemStackIdentityCompat {
    private ItemStackIdentityCompat() {
    }

    public static boolean hasVariantData(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && !ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()));
    }

    public static String identityPayload(ItemStack stack) {
        return hasVariantData(stack) ? stack.getComponents().toString() : "";
    }
}
