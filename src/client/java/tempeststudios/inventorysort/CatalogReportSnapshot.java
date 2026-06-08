package tempeststudios.inventorysort;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CatalogReportSnapshot {
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy h:mma", Locale.ROOT);

    private String id;
    private String namespace;
    private long generatedAt;
    private long durationSeconds;
    private int locationCount;
    private int uniqueItems;
    private int totalItems;
    private String reportFileName;
    private Map<String, Integer> itemCounts;

    public CatalogReportSnapshot() {
        this.itemCounts = new LinkedHashMap<>();
    }

    private CatalogReportSnapshot(String id,
                                  String namespace,
                                  long generatedAt,
                                  long durationSeconds,
                                  int locationCount,
                                  int totalItems,
                                  String reportFileName,
                                  Map<String, Integer> itemCounts) {
        this.id = id;
        this.namespace = namespace;
        this.generatedAt = generatedAt;
        this.durationSeconds = durationSeconds;
        this.locationCount = locationCount;
        this.totalItems = totalItems;
        this.reportFileName = reportFileName;
        this.itemCounts = new LinkedHashMap<>(itemCounts);
        this.uniqueItems = this.itemCounts.size();
    }

    public static CatalogReportSnapshot create(String id,
                                               String namespace,
                                               long generatedAt,
                                               long durationSeconds,
                                               int locationCount,
                                               int totalItems,
                                               String reportFileName,
                                               Map<String, Integer> itemCounts) {
        return new CatalogReportSnapshot(id, namespace, generatedAt, durationSeconds,
                locationCount, totalItems, reportFileName, itemCounts);
    }

    public boolean isUsable() {
        return id != null && !id.isBlank()
                && namespace != null && !namespace.isBlank()
                && itemCounts != null
                && !itemCounts.isEmpty();
    }

    public void normalize(String fallbackId, String fallbackReportFileName) {
        if (id == null || id.isBlank()) {
            id = fallbackId;
        }
        if (namespace == null || namespace.isBlank()) {
            namespace = "unknown";
        }
        if (generatedAt <= 0L) {
            generatedAt = System.currentTimeMillis();
        }
        if (itemCounts == null) {
            itemCounts = new LinkedHashMap<>();
        }
        if (uniqueItems <= 0) {
            uniqueItems = itemCounts.size();
        }
        if (totalItems <= 0) {
            int total = 0;
            for (int count : itemCounts.values()) {
                total += count;
            }
            totalItems = total;
        }
        if (reportFileName == null || reportFileName.isBlank()) {
            reportFileName = fallbackReportFileName;
        }
    }

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public int getLocationCount() {
        return locationCount;
    }

    public int getUniqueItems() {
        return uniqueItems;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public Map<String, Integer> getItemCounts() {
        return Collections.unmodifiableMap(itemCounts);
    }

    public String displayTime() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(generatedAt),
                ZoneId.systemDefault());
        return dateTime.format(DISPLAY_TIME).toLowerCase(Locale.ROOT);
    }

    public List<ItemCount> sortedItems() {
        List<ItemCount> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                items.add(new ItemCount(entry.getKey(), entry.getValue()));
            }
        }
        items.sort((left, right) -> {
            int byCount = Integer.compare(right.count(), left.count());
            return byCount != 0 ? byCount : left.itemId().compareTo(right.itemId());
        });
        return items;
    }

    public static final class ItemCount {
        private final String itemId;
        private final int count;

        ItemCount(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }

        public String itemId() {
            return itemId;
        }

        public int count() {
            return count;
        }
    }
}
