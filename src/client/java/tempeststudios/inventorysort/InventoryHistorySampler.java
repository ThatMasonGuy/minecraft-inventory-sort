package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class InventoryHistorySampler {
    private static final long SAVE_DEBOUNCE_MILLIS = 1_000L;
    private static final long MAX_SAVE_DELAY_MILLIS = 5_000L;

    private static String lastSavedSignature = "";
    private static String pendingSignature = "";
    private static List<ItemStack> pendingStacks = Collections.emptyList();
    private static long pendingStartedAt = 0L;
    private static long pendingChangedAt = 0L;

    private InventoryHistorySampler() {
    }

    public static void sample(Minecraft client) {
        if (client == null || client.player == null) {
            reset();
            return;
        }
        if (!ServerWorldProfileManager.getInstance().trackingAllowed(client)) {
            reset();
            return;
        }

        Map<String, ItemStack> totals = collectInventoryTotals(client.player.getInventory(), client.player.containerMenu.getCarried());
        String signature = buildSignature(totals);
        if (signature.equals(lastSavedSignature)) {
            discardPending();
            return;
        }

        long now = System.currentTimeMillis();
        if (!signature.equals(pendingSignature)) {
            if (pendingSignature.isEmpty()) {
                pendingStartedAt = now;
            }
            pendingSignature = signature;
            pendingStacks = copyStacks(totals.values());
            pendingChangedAt = now;
        }

        if (now - pendingChangedAt >= SAVE_DEBOUNCE_MILLIS
                || now - pendingStartedAt >= MAX_SAVE_DELAY_MILLIS) {
            flush();
        }
    }

    public static void flush() {
        if (pendingSignature.isEmpty()) {
            return;
        }

        if (!ServerWorldProfileManager.getInstance().trackingAllowed(Minecraft.getInstance())) {
            discardPending();
            return;
        }

        ItemLocationTracker.getInstance().replaceInventorySnapshot(pendingStacks);
        lastSavedSignature = pendingSignature;
        discardPending();
    }

    public static void reset() {
        lastSavedSignature = "";
        discardPending();
    }

    private static void discardPending() {
        pendingSignature = "";
        pendingStacks = Collections.emptyList();
        pendingStartedAt = 0L;
        pendingChangedAt = 0L;
    }

    private static List<ItemStack> copyStacks(Collection<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                copies.add(stack.copy());
            }
        }
        return copies;
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
