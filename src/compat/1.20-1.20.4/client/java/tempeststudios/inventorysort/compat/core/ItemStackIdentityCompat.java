package tempeststudios.inventorysort.compat.core;

import net.minecraft.world.item.ItemStack;

public final class ItemStackIdentityCompat {
    private ItemStackIdentityCompat() {
    }

    public static boolean hasVariantData(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getTag() != null && !stack.getTag().isEmpty();
    }

    public static String identityPayload(ItemStack stack) {
        return hasVariantData(stack) ? stack.getTag().toString() : "";
    }
}
