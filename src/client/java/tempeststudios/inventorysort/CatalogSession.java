package tempeststudios.inventorysort;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.world.item.ItemStack;

/**
 * Drives a cataloguing session.
 *
 * <p>A session is a recording window over the persistent per-namespace {@link CatalogStore}. While
 * active, opening containers, shulkers and the player inventory records (or replaces) their
 * snapshots in the store, keyed by the canonical {@link ContainerIdentity} identity. Because the
 * store is persistent and namespace-scoped, the catalogue accumulates across sessions and survives
 * restarts, letting you tally everything you own in a world over an entire playthrough.
 */
public final class CatalogSession {
    /** Max item rows printed to chat before linking to the full report file. */
    private static final int CHAT_ITEM_LIMIT = 60;

    private static CatalogSession activeSession;

    private final boolean includeInventory;
    private final String namespace;
    private final long startTime;

    public enum RecordResult {
        ADDED,      // newly catalogued location
        UPDATED,    // existing location's snapshot refreshed
        SKIPPED     // tracking not allowed, or namespace changed mid-session
    }

    private CatalogSession(boolean includeInventory, String namespace) {
        this.includeInventory = includeInventory;
        this.namespace = namespace;
        this.startTime = System.currentTimeMillis();
    }

    public static boolean isActive() {
        return activeSession != null;
    }

    public static CatalogSession getActive() {
        return activeSession;
    }

    public static CatalogSession start(boolean includeInventory) {
        if (activeSession != null) {
            throw new IllegalStateException("A catalog session is already active");
        }
        CatalogStore store = CatalogStore.getInstance();
        store.reloadForCurrentNamespace();
        activeSession = new CatalogSession(includeInventory, store.currentNamespace());
        return activeSession;
    }

    public static List<Component> stop() {
        if (activeSession == null) {
            throw new IllegalStateException("No active catalog session");
        }
        List<Component> report = activeSession.buildReport(true);
        activeSession = null;
        return report;
    }

    public boolean shouldIncludeInventory() {
        return includeInventory;
    }

    public String getNamespace() {
        return namespace;
    }

    // --- Recording ----------------------------------------------------------

    public RecordResult recordContainer(ContainerIdentity identity, Collection<ItemStack> items) {
        if (identity == null || !canRecord()) {
            return RecordResult.SKIPPED;
        }
        boolean added = CatalogStore.getInstance().record(
                identity.getIdentityKey(),
                identity.getContainerType(),
                identity.getPositionLabel(),
                identity.getDimensionKey(),
                items);
        return added ? RecordResult.ADDED : RecordResult.UPDATED;
    }

    public RecordResult recordShulker(String shulkerIdentifier, Collection<ItemStack> items) {
        if (shulkerIdentifier == null || !canRecord()) {
            return RecordResult.SKIPPED;
        }
        boolean added = CatalogStore.getInstance().record(
                "shulker:" + shulkerIdentifier,
                "Shulker Box",
                "Portable shulker",
                null,
                items);
        return added ? RecordResult.ADDED : RecordResult.UPDATED;
    }

    public RecordResult recordInventory(Collection<ItemStack> items) {
        if (!canRecord()) {
            return RecordResult.SKIPPED;
        }
        boolean added = CatalogStore.getInstance().record(
                CatalogStore.INVENTORY_KEY,
                "Player Inventory",
                "Player Inventory",
                null,
                items);
        return added ? RecordResult.ADDED : RecordResult.UPDATED;
    }

    /**
     * Only record when the world is confirmed for tracking and we are still in the namespace the
     * session was started in (guards against the active world profile changing mid-session).
     */
    private boolean canRecord() {
        Minecraft client = Minecraft.getInstance();
        if (!ServerWorldProfileManager.getInstance().trackingAllowed(client)) {
            return false;
        }
        return namespace.equals(TrackingNamespace.current(client));
    }

    // --- Status / reporting -------------------------------------------------

    public int getLocationCount() {
        return CatalogStore.getInstance().locationCount();
    }

    public int getUniqueItems() {
        return CatalogStore.getInstance().aggregateTotals().size();
    }

    public int getTotalItems() {
        return CatalogStore.getInstance().aggregateTotals().values().stream()
                .mapToInt(Integer::intValue).sum();
    }

    /**
     * Build the chat report. When {@code writeFile} is true a full plain-text report is also saved
     * to disk and its path is appended to the chat output.
     */
    public List<Component> buildReport(boolean writeFile) {
        List<Component> report = new ArrayList<>();
        long durationSeconds = (System.currentTimeMillis() - startTime) / 1000;

        CatalogStore store = CatalogStore.getInstance();
        Map<String, Integer> totals = store.aggregateTotals();
        int locationCount = store.locationCount();

        List<Map.Entry<String, Integer>> sortedItems = totals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toList());
        int totalItems = totals.values().stream().mapToInt(Integer::intValue).sum();

        report.add(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GOLD));
        report.add(Component.literal("📊 Catalog Report").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        report.add(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GOLD));
        report.add(Component.empty());
        report.add(Component.literal("World: " + namespace).withStyle(ChatFormatting.GRAY));
        report.add(Component.literal(String.format("Session duration: %d seconds", durationSeconds)).withStyle(ChatFormatting.GRAY));
        report.add(Component.literal(String.format("Locations catalogued: %d", locationCount)).withStyle(ChatFormatting.GRAY));
        report.add(Component.literal(String.format("Unique items: %d", totals.size())).withStyle(ChatFormatting.GRAY));
        report.add(Component.empty());

        if (sortedItems.isEmpty()) {
            report.add(Component.literal("Nothing catalogued yet - open some containers!").withStyle(ChatFormatting.RED));
        } else {
            report.add(Component.literal("Items:").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            report.add(Component.empty());

            int shown = Math.min(sortedItems.size(), CHAT_ITEM_LIMIT);
            for (int i = 0; i < shown; i++) {
                Map.Entry<String, Integer> entry = sortedItems.get(i);
                MutableComponent line = Component.literal("  • " + formatItemName(entry.getKey()) + ": ")
                        .withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(String.format("%,d", entry.getValue()))
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                report.add(line);
            }
            if (sortedItems.size() > shown) {
                report.add(Component.literal(String.format("  … and %d more (see full report file)",
                        sortedItems.size() - shown)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }

            report.add(Component.empty());
            report.add(Component.literal(String.format("Total items: %,d", totalItems))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }

        if (writeFile && !sortedItems.isEmpty()) {
            Path file = writeReportFile(sortedItems, totalItems, locationCount, durationSeconds);
            if (file != null) {
                report.add(Component.empty());
                report.add(Component.literal("Full report saved to: " + file)
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }

        report.add(Component.empty());
        report.add(Component.literal("=".repeat(50)).withStyle(ChatFormatting.GOLD));
        return report;
    }

    private Path writeReportFile(List<Map.Entry<String, Integer>> sortedItems,
                                 int totalItems,
                                 int locationCount,
                                 long durationSeconds) {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportId = "report_" + TrackingNamespace.fileNameSafe(namespace) + "_" + stamp;
        Path file = CatalogStore.getInstance().catalogDirectory().resolve(reportId + ".txt");
        try (Writer writer = Files.newBufferedWriter(file)) {
            writer.write("Inventory Catalogue Report\n");
            writer.write("World: " + namespace + "\n");
            writer.write("Generated: " + LocalDateTime.now() + "\n");
            writer.write("Session duration (s): " + durationSeconds + "\n");
            writer.write("Locations catalogued: " + locationCount + "\n");
            writer.write("Unique items: " + sortedItems.size() + "\n");
            writer.write("Total items: " + totalItems + "\n");
            writer.write("\n");
            for (Map.Entry<String, Integer> entry : sortedItems) {
                writer.write(String.format("%,d\t%s\t(%s)%n",
                        entry.getValue(), formatItemName(entry.getKey()), entry.getKey()));
            }
            CatalogReportHistory.save(CatalogReportSnapshot.create(reportId, namespace,
                    System.currentTimeMillis(), durationSeconds, locationCount, totalItems,
                    file.getFileName().toString(), itemCountMap(sortedItems)));
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to write catalog report file", e);
            return null;
        }
        return file;
    }

    private Map<String, Integer> itemCountMap(List<Map.Entry<String, Integer>> sortedItems) {
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sortedItems) {
            itemCounts.put(entry.getKey(), entry.getValue());
        }
        return itemCounts;
    }

    /** minecraft:iron_ingot -> Iron Ingot */
    private static String formatItemName(String itemId) {
        String name = itemId.contains(":") ? itemId.substring(itemId.lastIndexOf(':') + 1) : itemId;
        return Arrays.stream(name.split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
