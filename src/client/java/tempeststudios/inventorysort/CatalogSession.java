package tempeststudios.inventorysort;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages a cataloging session where the player opens chests to count total items
 */
public class CatalogSession {
    private static CatalogSession activeSession = null;

    private final boolean includeInventory;
    private final Map<String, Integer> itemCounts; // Item ID -> total count
    private final Set<String> trackedContainers; // Fingerprints of already-tracked containers
    private final long startTime;
    private int containersTracked;

    private CatalogSession(boolean includeInventory) {
        this.includeInventory = includeInventory;
        this.itemCounts = new HashMap<>();
        this.trackedContainers = new HashSet<>();
        this.startTime = System.currentTimeMillis();
        this.containersTracked = 0;
    }

    public static boolean isActive() {
        return activeSession != null;
    }

    public static CatalogSession start(boolean includeInventory) {
        if (activeSession != null) {
            throw new IllegalStateException("A catalog session is already active");
        }
        activeSession = new CatalogSession(includeInventory);
        return activeSession;
    }

    public static CatalogSession getActive() {
        return activeSession;
    }

    public static List<Component> stop() {
        if (activeSession == null) {
            throw new IllegalStateException("No active catalog session");
        }

        List<Component> report = activeSession.generateReport();
        activeSession = null;
        return report;
    }

    /**
     * Generate a unique fingerprint for a container
     */
    public static String generateFingerprint(BlockPos pos, ResourceKey<Level> dimension, String containerType) {
        if (pos != null && dimension != null) {
            // For containers with a fixed position
            // Get dimension key as string
            String dimensionKey;
            if (dimension == Level.OVERWORLD) {
                dimensionKey = "overworld";
            } else if (dimension == Level.NETHER) {
                dimensionKey = "nether";
            } else if (dimension == Level.END) {
                dimensionKey = "end";
            } else {
                dimensionKey = dimension.toString();
            }

            return String.format("%s_%d_%d_%d_%s",
                    dimensionKey,
                    pos.getX(), pos.getY(), pos.getZ(),
                    containerType);
        }
        // Fallback for portable containers (shouldn't really happen in catalog mode)
        return "unknown_" + System.currentTimeMillis();
    }

    public static String generateFingerprint(ContainerIdentity identity) {
        if (identity == null) {
            return "unknown_" + System.currentTimeMillis();
        }
        return identity.getNamespace() + ":" + identity.getIdentityKey() + ":" + identity.getContainerType();
    }

    public static String generatePlayerInventoryFingerprint() {
        return "player_inventory";
    }

    /**
     * Check if a container has already been tracked in this session
     */
    public boolean hasTracked(String fingerprint) {
        return trackedContainers.contains(fingerprint);
    }

    /**
     * Track items from a container
     * Returns true if container was newly tracked, false if already counted
     */
    public boolean trackContainer(String fingerprint, List<ItemStack> items) {
        if (hasTracked(fingerprint)) {
            return false; // Already counted this container
        }

        trackedContainers.add(fingerprint);
        containersTracked++;

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                itemCounts.merge(itemId, stack.getCount(), Integer::sum);
            }
        }

        return true;
    }

    public boolean shouldIncludeInventory() {
        return includeInventory;
    }

    /**
     * Generate a formatted report of all tracked items
     */
    private List<Component> generateReport() {
        List<Component> report = new ArrayList<>();
        long duration = (System.currentTimeMillis() - startTime) / 1000; // seconds

        // Header
        report.add(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GOLD));
        report.add(Component.literal("📊 Catalog Session Report").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        report.add(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GOLD));
        report.add(Component.empty());

        // Summary
        report.add(Component.literal(String.format("Duration: %d seconds", duration)).withStyle(ChatFormatting.GRAY));
        report.add(Component.literal(String.format("Containers scanned: %d", containersTracked)).withStyle(ChatFormatting.GRAY));
        report.add(Component.literal(String.format("Unique items: %d", itemCounts.size())).withStyle(ChatFormatting.GRAY));
        report.add(Component.literal(String.format("Include inventory: %s", includeInventory ? "Yes" : "No")).withStyle(ChatFormatting.GRAY));
        report.add(Component.empty());

        if (itemCounts.isEmpty()) {
            report.add(Component.literal("No items found!").withStyle(ChatFormatting.RED));
        } else {
            // Sort items by count (descending)
            List<Map.Entry<String, Integer>> sortedItems = itemCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());

            report.add(Component.literal("Items cataloged:").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            report.add(Component.empty());

            // Display items in a formatted list
            for (Map.Entry<String, Integer> entry : sortedItems) {
                String itemName = formatItemName(entry.getKey());
                int count = entry.getValue();

                MutableComponent line = Component.literal(String.format("  • %s: ", itemName))
                        .withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(String.format("%,d", count))
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                report.add(line);
            }

            // Calculate total items
            int totalItems = itemCounts.values().stream().mapToInt(Integer::intValue).sum();
            report.add(Component.empty());
            report.add(Component.literal(String.format("Total items: %,d", totalItems))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }

        report.add(Component.empty());
        report.add(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GOLD));

        return report;
    }

    /**
     * Format item ID into a readable name
     * minecraft:diamond -> Diamond
     * minecraft:iron_ingot -> Iron Ingot
     */
    private String formatItemName(String itemId) {
        // Remove namespace (minecraft:diamond -> diamond)
        String name = itemId.contains(":") ? itemId.substring(itemId.lastIndexOf(':') + 1) : itemId;

        // Replace underscores with spaces and capitalize
        name = Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));

        return name;
    }

    public int getContainersTracked() {
        return containersTracked;
    }

    public int getUniqueItems() {
        return itemCounts.size();
    }

    public int getTotalItems() {
        return itemCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
