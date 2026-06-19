package tempeststudios.inventorysort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CatalogReportHistory {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private CatalogReportHistory() {
    }

    public static void save(CatalogReportSnapshot snapshot) {
        if (snapshot == null || !snapshot.isUsable()) {
            return;
        }

        Path target = CatalogStore.getInstance().catalogDirectory().resolve(snapshot.getId() + ".json");
        try {
            writeJsonAtomically(target, snapshot);
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to save catalog report snapshot", e);
        }
    }

    public static List<CatalogReportSnapshot> loadAll() {
        Path dir = CatalogStore.getInstance().catalogDirectory();
        Map<String, CatalogReportSnapshot> reportsById = new HashMap<>();

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            try (var files = Files.list(dir)) {
                files
                        .filter(path -> path.getFileName().toString().startsWith("report_"))
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(path -> loadJson(path, reportsById));
            }
            try (var files = Files.list(dir)) {
                files
                        .filter(path -> path.getFileName().toString().startsWith("report_"))
                        .filter(path -> path.getFileName().toString().endsWith(".txt"))
                        .forEach(path -> loadLegacyText(path, reportsById));
            }
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to load catalog report history", e);
        }

        List<CatalogReportSnapshot> reports = new ArrayList<>(reportsById.values());
        reports.sort((left, right) -> Long.compare(right.getGeneratedAt(), left.getGeneratedAt()));
        return reports;
    }

    private static void loadJson(Path path, Map<String, CatalogReportSnapshot> reportsById) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CatalogReportSnapshot snapshot = GSON.fromJson(reader, CatalogReportSnapshot.class);
            if (snapshot != null) {
                String id = stripExtension(path.getFileName().toString());
                snapshot.normalize(id, id + ".txt");
                if (snapshot.isUsable()) {
                    reportsById.put(snapshot.getId(), snapshot);
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error(
                    "Failed to read catalog report snapshot {}", path.getFileName(), e);
        }
    }

    private static void loadLegacyText(Path path, Map<String, CatalogReportSnapshot> reportsById) {
        String id = stripExtension(path.getFileName().toString());
        if (reportsById.containsKey(id)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String namespace = "unknown";
            long generatedAt = Files.getLastModifiedTime(path).toMillis();
            long durationSeconds = 0L;
            int locationCount = 0;
            int totalItems = 0;
            Map<String, Integer> itemCounts = new LinkedHashMap<>();

            for (String line : lines) {
                if (line.startsWith("World: ")) {
                    namespace = line.substring("World: ".length()).trim();
                } else if (line.startsWith("Generated: ")) {
                    generatedAt = parseGeneratedAt(line.substring("Generated: ".length()).trim(), generatedAt);
                } else if (line.startsWith("Session duration (s): ")) {
                    durationSeconds = parseLong(line.substring("Session duration (s): ".length()).trim(), 0L);
                } else if (line.startsWith("Locations catalogued: ")) {
                    locationCount = parseInt(line.substring("Locations catalogued: ".length()).trim(), 0);
                } else if (line.startsWith("Total items: ")) {
                    totalItems = parseInt(line.substring("Total items: ".length()).trim(), 0);
                } else if (line.contains("\t") && line.endsWith(")")) {
                    parseItemLine(line, itemCounts);
                }
            }

            CatalogReportSnapshot snapshot = CatalogReportSnapshot.create(id, namespace, generatedAt,
                    durationSeconds, locationCount, totalItems, path.getFileName().toString(),
                    itemCounts, itemInfoFor(itemCounts));
            snapshot.normalize(id, path.getFileName().toString());
            if (snapshot.isUsable()) {
                reportsById.put(snapshot.getId(), snapshot);
            }
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error(
                    "Failed to read legacy catalog report {}", path.getFileName(), e);
        }
    }

    private static Map<String, ItemStackIdentity.Info> itemInfoFor(Map<String, Integer> itemCounts) {
        Map<String, ItemStackIdentity.Info> itemInfo = new LinkedHashMap<>();
        for (String itemKey : itemCounts.keySet()) {
            itemInfo.put(itemKey, ItemStackIdentity.legacyInfo(itemKey));
        }
        return itemInfo;
    }

    private static void parseItemLine(String line, Map<String, Integer> itemCounts) {
        String[] parts = line.split("\t");
        if (parts.length < 3) {
            return;
        }
        int count = parseInt(parts[0].replace(",", "").trim(), 0);
        String rawId = parts[2].trim();
        if (rawId.startsWith("(") && rawId.endsWith(")")) {
            String itemId = rawId.substring(1, rawId.length() - 1);
            if (!itemId.isBlank() && count > 0) {
                itemCounts.put(itemId, count);
            }
        }
    }

    private static long parseGeneratedAt(String raw, long fallback) {
        try {
            return LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long parseLong(String raw, long fallback) {
        try {
            return Long.parseLong(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static void writeJsonAtomically(Path target, Object data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            moveIntoPlace(tempFile, target);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    private static void moveIntoPlace(Path tempFile, Path target) throws IOException {
        try {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
