package tempeststudios.inventorysort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import tempeststudios.inventorysort.core.InventorySortCore;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyDataMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MIGRATION_ID = "legacy-inventorysort-game-directory-v1";
    private static final int REGISTRY_VERSION = 1;
    private static final Pattern REPORT_FILE_PATTERN =
            Pattern.compile("^report_(.+)_([0-9]{8}_[0-9]{6})\\.(json|txt)$");

    private static boolean attempted;

    private LegacyDataMigration() {
    }

    public static synchronized void migrateLegacyData(Minecraft client) {
        if (attempted) {
            return;
        }
        attempted = true;

        Path legacyRoot = TempestStudiosData.legacyRoot(client);
        if (!Files.isDirectory(legacyRoot)) {
            return;
        }

        MigrationRegistry registry = loadRegistry();
        boolean changed = false;

        changed |= migrateFile(client, registry,
                "invcore.server_world_profiles",
                legacyRoot.resolve("server_world_profiles.json"),
                TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.CORE).resolve("server_world_profiles.json"),
                LegacyDataMigration::copyRaw);
        changed |= migrateFile(client, registry,
                "invsort.sort_rules",
                legacyRoot.resolve("sort_rules.json"),
                TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.SORT).resolve("sort_rules.json"),
                LegacyDataMigration::copySortRules);
        changed |= migrateItemLocations(client, registry, legacyRoot);
        changed |= migrateCatalog(client, registry, legacyRoot.resolve("catalog"));

        if (changed) {
            saveRegistry(registry);
        }
    }

    private static boolean migrateItemLocations(Minecraft client, MigrationRegistry registry, Path legacyRoot) {
        if (!Files.isDirectory(legacyRoot)) {
            return false;
        }

        boolean changed = false;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(legacyRoot, "item_locations_*.json")) {
            for (Path source : files) {
                String fileName = source.getFileName().toString();
                String namespaceSafe = stripPrefixSuffix(fileName, "item_locations_", ".json");
                if (namespaceSafe == null) {
                    continue;
                }

                String sourceNamespace;
                try {
                    sourceNamespace = inferSearchNamespace(source, namespaceSafe);
                } catch (Exception e) {
                    changed |= recordSkipped(client, registry, "invsearch.item_locations", source,
                            TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.SEARCH),
                            "skipped_invalid_json");
                    continue;
                }
                if (sourceNamespace == null) {
                    changed |= recordSkipped(client, registry, "invsearch.item_locations", source,
                            TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.SEARCH),
                            "skipped_ambiguous_namespace");
                    continue;
                }

                String targetNamespace = TempestStudiosData.mapLegacyNamespace(client, sourceNamespace);
                Path target = TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.SEARCH)
                        .resolve("item_locations_" + TempestStudiosData.fileNameSafe(targetNamespace) + ".json");
                String oldNamespace = TempestStudiosData.sanitize(sourceNamespace);
                changed |= migrateFile(client, registry,
                        "invsearch.item_locations",
                        source,
                        target,
                        (ignoredClient, sourceFile, targetFile) ->
                                copySearchLocations(sourceFile, targetFile, oldNamespace, targetNamespace));
            }
        } catch (IOException e) {
            InventorySortCore.LOGGER.warn("Failed to scan legacy item location data", e);
        }
        return changed;
    }

    private static boolean migrateCatalog(Minecraft client, MigrationRegistry registry, Path legacyCatalogDir) {
        if (!Files.isDirectory(legacyCatalogDir)) {
            return false;
        }

        boolean changed = false;
        Path targetDir = TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.CATALOGUE).resolve("catalog");
        try (DirectoryStream<Path> files = Files.newDirectoryStream(legacyCatalogDir, "catalog_*.json")) {
            for (Path source : files) {
                String namespaceSafe = stripPrefixSuffix(source.getFileName().toString(), "catalog_", ".json");
                if (namespaceSafe == null) {
                    continue;
                }
                String targetNamespace = TempestStudiosData.mapLegacyNamespace(client, namespaceFromFileSafe(namespaceSafe));
                Path target = targetDir.resolve("catalog_" + TempestStudiosData.fileNameSafe(targetNamespace) + ".json");
                changed |= migrateFile(client, registry, "invcatalogue.catalog", source, target,
                        LegacyDataMigration::copyRaw);
            }
        } catch (IOException e) {
            InventorySortCore.LOGGER.warn("Failed to scan legacy catalogue data", e);
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(legacyCatalogDir, "report_*.*")) {
            for (Path source : files) {
                String fileName = source.getFileName().toString();
                if (!fileName.endsWith(".json") && !fileName.endsWith(".txt")) {
                    continue;
                }
                ReportMigration report = reportMigration(client, source);
                if (report == null) {
                    Path target = targetDir.resolve(fileName);
                    changed |= migrateFile(client, registry, "invcatalogue.report", source, target,
                            LegacyDataMigration::copyRaw);
                    continue;
                }
                changed |= migrateFile(client, registry, "invcatalogue.report", source, report.target,
                        (ignoredClient, sourceFile, targetFile) -> copyReport(sourceFile, targetFile, report));
            }
        } catch (IOException e) {
            InventorySortCore.LOGGER.warn("Failed to scan legacy catalogue reports", e);
        }

        return changed;
    }

    private static boolean migrateFile(Minecraft client,
                                       MigrationRegistry registry,
                                       String dataSet,
                                       Path source,
                                       Path target,
                                       LegacyFileCopy copier) {
        if (!Files.isRegularFile(source)) {
            return false;
        }

        String key = migrationKey(dataSet, source);
        if (registry.records.containsKey(key)) {
            return false;
        }

        String result;
        try {
            if (Files.exists(target)) {
                result = "skipped_target_exists";
            } else {
                result = copier.copy(client, source, target);
            }
        } catch (Exception e) {
            InventorySortCore.LOGGER.warn("Failed to migrate legacy {} from {}", dataSet, source, e);
            return false;
        }

        registry.records.put(key, MigrationRecord.create(client, dataSet, source, target, result));
        InventorySortCore.LOGGER.info("Legacy {} migration {}: {} -> {}", dataSet, result, source, target);
        return true;
    }

    private static boolean recordSkipped(Minecraft client,
                                         MigrationRegistry registry,
                                         String dataSet,
                                         Path source,
                                         Path target,
                                         String result) {
        String key = migrationKey(dataSet, source);
        if (registry.records.containsKey(key)) {
            return false;
        }
        registry.records.put(key, MigrationRecord.create(client, dataSet, source, target, result));
        InventorySortCore.LOGGER.warn("Legacy {} migration {}: {}", dataSet, result, source);
        return true;
    }

    private static String copyRaw(Minecraft client, Path source, Path target) throws IOException {
        copyRawAtomically(source, target);
        return "copied";
    }

    private static String copySortRules(Minecraft client, Path source, Path target) throws IOException {
        JsonObject root = readJsonObject(source);
        JsonElement overridesElement = root.get("containerOverrides");
        if (overridesElement != null && overridesElement.isJsonObject()) {
            JsonObject rewritten = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : overridesElement.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                int separator = key.indexOf('|');
                if (separator > 0) {
                    String namespace = key.substring(0, separator);
                    key = TempestStudiosData.mapLegacyNamespace(client, namespace) + key.substring(separator);
                }
                if (!rewritten.has(key)) {
                    rewritten.add(key, entry.getValue().deepCopy());
                }
            }
            root.add("containerOverrides", rewritten);
        }
        writeJsonAtomically(target, root);
        return "copied_namespace_scoped";
    }

    private static String copySearchLocations(Path source,
                                              Path target,
                                              String oldNamespace,
                                              String targetNamespace) throws IOException {
        JsonObject root = readJsonObject(source);
        Set<String> namespaces = namespacesInSearchRoot(root);
        boolean acceptNamespaceLessEntries = namespaces.isEmpty();
        JsonObject rewritten = new JsonObject();
        int keptEntries = 0;

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            JsonArray locations = new JsonArray();
            for (JsonElement locationElement : entry.getValue().getAsJsonArray()) {
                if (!locationElement.isJsonObject()) {
                    continue;
                }
                JsonObject location = locationElement.getAsJsonObject();
                String namespace = stringValue(location.get("namespace"));
                boolean matches = acceptNamespaceLessEntries
                        || oldNamespace.equals(TempestStudiosData.sanitize(namespace));
                if (!matches) {
                    continue;
                }
                JsonObject copy = location.deepCopy();
                copy.addProperty("namespace", targetNamespace);
                locations.add(copy);
                keptEntries++;
            }
            if (locations.size() > 0) {
                rewritten.add(entry.getKey(), locations);
            }
        }

        if (keptEntries == 0) {
            return "skipped_no_matching_entries";
        }
        writeJsonAtomically(target, rewritten);
        return oldNamespace.equals(targetNamespace) ? "copied" : "copied_namespace_scoped";
    }

    private static String copyReport(Path source, Path target, ReportMigration report) throws IOException {
        if ("json".equals(report.extension)) {
            JsonObject root = readJsonObject(source);
            root.addProperty("id", report.id);
            root.addProperty("namespace", report.namespace);
            root.addProperty("reportFileName", report.id + ".txt");
            writeJsonAtomically(target, root);
            return "copied_namespace_scoped";
        }

        List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("World: ")) {
                lines.set(i, "World: " + report.namespace);
            }
        }
        writeTextAtomically(target, String.join(System.lineSeparator(), lines) + System.lineSeparator());
        return "copied_namespace_scoped";
    }

    private static String inferSearchNamespace(Path source, String namespaceSafe) throws IOException {
        JsonObject root = readJsonObject(source);
        Set<String> namespaces = namespacesInSearchRoot(root);
        if (namespaces.size() > 1) {
            return null;
        }
        if (namespaces.size() == 1) {
            return namespaces.iterator().next();
        }
        return namespaceFromFileSafe(namespaceSafe);
    }

    private static Set<String> namespacesInSearchRoot(JsonObject root) {
        Set<String> namespaces = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement locationElement : entry.getValue().getAsJsonArray()) {
                if (!locationElement.isJsonObject()) {
                    continue;
                }
                String namespace = stringValue(locationElement.getAsJsonObject().get("namespace"));
                if (namespace != null && !namespace.isBlank()) {
                    namespaces.add(TempestStudiosData.sanitize(namespace));
                }
            }
        }
        return namespaces;
    }

    private static ReportMigration reportMigration(Minecraft client, Path source) throws IOException {
        String fileName = source.getFileName().toString();
        Matcher matcher = REPORT_FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }

        String extension = matcher.group(3);
        String sourceNamespace = reportNamespace(source, extension);
        if (sourceNamespace == null) {
            sourceNamespace = namespaceFromFileSafe(matcher.group(1));
        }
        String targetNamespace = TempestStudiosData.mapLegacyNamespace(client, sourceNamespace);
        String id = "report_" + TempestStudiosData.fileNameSafe(targetNamespace) + "_" + matcher.group(2);
        Path target = TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.CATALOGUE)
                .resolve("catalog")
                .resolve(id + "." + extension);
        return new ReportMigration(target, id, targetNamespace, extension);
    }

    private static String reportNamespace(Path source, String extension) throws IOException {
        if ("json".equals(extension)) {
            JsonObject root = readJsonObject(source);
            return stringValue(root.get("namespace"));
        }

        for (String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {
            if (line.startsWith("World: ")) {
                return line.substring("World: ".length()).trim();
            }
        }
        return null;
    }

    private static String namespaceFromFileSafe(String namespaceSafe) {
        String safe = TempestStudiosData.sanitize(namespaceSafe).replace(':', '_');
        if (safe.startsWith("singleplayer_")) {
            String suffix = safe.substring("singleplayer_".length());
            if (suffix.startsWith("instance_")) {
                int separator = suffix.indexOf('_', "instance_".length());
                if (separator > 0 && separator < suffix.length() - 1) {
                    return "singleplayer:" + suffix.substring(0, separator) + ":" + suffix.substring(separator + 1);
                }
            }
            return "singleplayer:" + suffix;
        }
        if (safe.startsWith("server_")) {
            String suffix = safe.substring("server_".length());
            int worldSeparator = suffix.lastIndexOf("_world_");
            if (worldSeparator > 0 && worldSeparator < suffix.length() - "_world_".length()) {
                return "server:" + suffix.substring(0, worldSeparator)
                        + ":world:" + suffix.substring(worldSeparator + "_world_".length());
            }
            return "server:" + suffix;
        }
        return safe;
    }

    private static String stripPrefixSuffix(String value, String prefix, String suffix) {
        if (!value.startsWith(prefix) || !value.endsWith(suffix)) {
            return null;
        }
        return value.substring(prefix.length(), value.length() - suffix.length());
    }

    private static String stringValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject readJsonObject(Path source) throws IOException {
        JsonElement element;
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            element = GSON.fromJson(reader, JsonElement.class);
        }
        if (element == null || !element.isJsonObject()) {
            throw new IOException("Expected JSON object in " + source);
        }
        return element.getAsJsonObject();
    }

    private static void copyRawAtomically(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.copy(source, tempFile, StandardCopyOption.REPLACE_EXISTING);
            moveIntoPlace(tempFile, target);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
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

    private static void writeTextAtomically(Path target, String data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tempFile, data, StandardCharsets.UTF_8);
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

    private static String migrationKey(String dataSet, Path source) {
        Path normalized;
        try {
            normalized = source.toRealPath();
        } catch (IOException e) {
            normalized = source.toAbsolutePath().normalize();
        }
        return MIGRATION_ID + "|" + dataSet + "|" + normalized;
    }

    private static Path registryFile() {
        return TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.CORE)
                .resolve("migration_registry.json");
    }

    private static MigrationRegistry loadRegistry() {
        Path file = registryFile();
        if (!Files.exists(file)) {
            return new MigrationRegistry();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            MigrationRegistry registry = GSON.fromJson(reader, MigrationRegistry.class);
            if (registry != null) {
                registry.normalize();
                return registry;
            }
        } catch (Exception e) {
            InventorySortCore.LOGGER.warn("Failed to read migration registry {}; starting fresh", file, e);
        }
        return new MigrationRegistry();
    }

    private static void saveRegistry(MigrationRegistry registry) {
        try {
            registry.normalize();
            writeJsonAtomically(registryFile(), registry);
        } catch (IOException e) {
            InventorySortCore.LOGGER.warn("Failed to write migration registry", e);
        }
    }

    private static String versionOf(String modId) {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("not_present");
    }

    private interface LegacyFileCopy {
        String copy(Minecraft client, Path source, Path target) throws IOException;
    }

    private static final class ReportMigration {
        final Path target;
        final String id;
        final String namespace;
        final String extension;

        ReportMigration(Path target, String id, String namespace, String extension) {
            this.target = target;
            this.id = id;
            this.namespace = namespace;
            this.extension = extension;
        }
    }

    private static final class MigrationRegistry {
        int version = REGISTRY_VERSION;
        Map<String, MigrationRecord> records = new LinkedHashMap<>();

        void normalize() {
            version = REGISTRY_VERSION;
            if (records == null) {
                records = new LinkedHashMap<>();
            }
        }
    }

    private static final class MigrationRecord {
        String migrationId;
        String dataSet;
        String source;
        String target;
        String result;
        long migratedAt;
        String migratedAtIso;
        String gameDirectory;
        Map<String, String> modVersions;

        static MigrationRecord create(Minecraft client, String dataSet, Path source, Path target, String result) {
            MigrationRecord record = new MigrationRecord();
            record.migrationId = MIGRATION_ID;
            record.dataSet = dataSet;
            record.source = source.toAbsolutePath().normalize().toString();
            record.target = target.toAbsolutePath().normalize().toString();
            record.result = result;
            record.migratedAt = System.currentTimeMillis();
            record.migratedAtIso = Instant.ofEpochMilli(record.migratedAt).toString();
            record.gameDirectory = TempestStudiosData.gameDirectory(client).toAbsolutePath().normalize().toString();
            record.modVersions = new LinkedHashMap<>();
            record.modVersions.put("inventorysort_core", versionOf("inventorysort_core"));
            record.modVersions.put("inventorysort", versionOf("inventorysort"));
            record.modVersions.put("inventorysearch", versionOf("inventorysearch"));
            record.modVersions.put("inventorycatalogue", versionOf("inventorycatalogue"));
            return record;
        }
    }
}
