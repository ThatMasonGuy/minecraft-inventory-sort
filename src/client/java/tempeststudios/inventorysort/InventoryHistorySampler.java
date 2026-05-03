package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class InventoryHistorySampler {
    private static String lastSignature = "";

    private InventoryHistorySampler() {
    }

    public static void sample(Minecraft client) {
        if (client == null || client.player == null) {
            lastSignature = "";
            return;
        }
        if (!ServerWorldProfileManager.getInstance().trackingAllowed(client)) {
            lastSignature = "";
            return;
        }

        Map<String, ItemStack> totals = collectInventoryTotals(client.player.getInventory(), client.player.containerMenu.getCarried());
        String signature = buildSignature(totals);
        if (signature.equals(lastSignature)) {
            return;
        }

        lastSignature = signature;
        ItemLocationTracker.getInstance().replaceInventorySnapshot(totals.values());
    }

    public static void reset() {
        lastSignature = "";
    }

    private static Map<String, ItemStack> collectInventoryTotals(Inventory inventory, ItemStack carried) {
        Map<String, ItemStack> totals = new HashMap<>();
        int slots = Math.min(36, inventory.getContainerSize());
        for (int slot = 0; slot < slots; slot++) {
            addStack(totals, inventory.getItem(slot));
        }
        addStack(totals, carried);
        return totals;
    }

    private static void addStack(Map<String, ItemStack> totals, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ItemStack existing = totals.get(key);
        if (existing == null) {
            ItemStack copy = stack.copy();
            totals.put(key, copy);
        } else {
            existing.setCount(existing.getCount() + stack.getCount());
        }
    }

    private static String buildSignature(Map<String, ItemStack> totals) {
        Map<String, Integer> ordered = new TreeMap<>();
        for (Map.Entry<String, ItemStack> entry : totals.entrySet()) {
            ordered.put(entry.getKey(), entry.getValue().getCount());
        }
        return ordered.toString();
    }
}
