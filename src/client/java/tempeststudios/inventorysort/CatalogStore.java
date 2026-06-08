package tempeststudios.inventorysort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent, namespace-scoped store of catalogued storage locations.
 *
 * <p>Each location is keyed by its canonical {@link ContainerIdentity} identity key (the same
 * identity scheme the inventory-search tracker uses), so reopening a container <em>replaces</em>
 * rather than double-counts its snapshot, and different chests are reliably differentiated.
 *
 * <p>Kept deliberately independent of {@link ItemLocationTracker} so the catalogue feature can be
 * split into its own mod later. It only reads the shared {@link ContainerIdentity} /
 * {@link TrackingNamespace} primitives, never the tracker's stored data.
 */
public final class CatalogStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Stable identity key used for the player's own inventory within a namespace. */
    public static final String INVENTORY_KEY = "player_inventory";

    private static CatalogStore instance;

    private final Path catalogDir;
    /** identityKey -> snapshot, for the currently loaded namespace. Ordered by first-seen. */
    private final Map<String, LocationSnapshot> snapshots = new LinkedHashMap<>();
    private String activeNamespace;
    private Path saveFile;

    private CatalogStore() {
        Minecraft mc = Minecraft.getInstance();
        Path gameDir = mc.gameDirectory.toPath();
        this.catalogDir = gameDir.resolve("inventorysort").resolve("catalog");

        try {
            Files.createDirectories(catalogDir);
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to create catalog directory", e);
        }

        ensureNamespaceLoaded(TrackingNamespace.current(mc));
    }

    public static CatalogStore getInstance() {
        if (instance == null) {
            instance = new CatalogStore();
        }
        return instance;
    }

    public String currentNamespace() {
        return ensureNamespaceLoaded(TrackingNamespace.current(Minecraft.getInstance()));
    }

    public Path catalogDirectory() {
        return catalogDir;
    }

    /**
     * Record (or replace) the snapshot for a location, keyed by its identity.
     *
     * @return {@code true} if this identity was newly added, {@code false} if an existing
     *         snapshot was replaced.
     */
    public boolean record(String identityKey,
                          String containerType,
                          String positionLabel,
                          String dimensionKey,
                          Collection<ItemStack> items) {
        ensureCurrentNamespace();

        boolean isNew = !snapshots.containsKey(identityKey);
        LocationSnapshot snapshot = new LocationSnapshot(identityKey, containerType, positionLabel,
                dimensionKey, System.currentTimeMillis(), aggregate(items));
        snapshots.put(identityKey, snapshot);
        save();
        return isNew;
    }

    /** Remove all catalogue data for the current namespace. */
    public void clear() {
        ensureCurrentNamespace();
        snapshots.clear();
        try {
            if (saveFile != null) {
                Files.deleteIfExists(saveFile);
                Files.deleteIfExists(backupFile(saveFile));
            }
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to delete catalog file for {}", activeNamespace, e);
        }
    }

    public Collection<LocationSnapshot> snapshots() {
        ensureCurrentNamespace();
        return snapshots.values();
    }

    public int locationCount() {
        ensureCurrentNamespace();
        return snapshots.size();
    }

    /** Aggregate item totals across every location in the current namespace. */
    public Map<String, Integer> aggregateTotals() {
        ensureCurrentNamespace();
        Map<String, Integer> totals = new HashMap<>();
        for (LocationSnapshot snapshot : snapshots.values()) {
            for (Map.Entry<String, Integer> entry : snapshot.getCounts().entrySet()) {
                totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        return totals;
    }

    private Map<String, Integer> aggregate(Collection<ItemStack> items) {
        Map<String, Integer> counts = new HashMap<>();
        if (items == null) {
            return counts;
        }
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            counts.merge(itemId, stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private void ensureCurrentNamespace() {
        ensureNamespaceLoaded(TrackingNamespace.current(Minecraft.getInstance()));
    }

    /** Reload the store so it reflects the active world/server profile. */
    public void reloadForCurrentNamespace() {
        ensureNamespaceLoaded(TrackingNamespace.current(Minecraft.getInstance()));
    }

    private String ensureNamespaceLoaded(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            namespace = "unknown";
        }
        if (namespace.equals(activeNamespace)) {
            return activeNamespace;
        }

        if (activeNamespace != null) {
            save();
        }

        activeNamespace = namespace;
        saveFile = catalogDir.resolve("catalog_" + TrackingNamespace.fileNameSafe(namespace) + ".json");
        snapshots.clear();
        load();
        return activeNamespace;
    }

    public void save() {
        if (saveFile == null) {
            return;
        }
        try {
            writeJsonAtomically(saveFile, snapshots);
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("Saved catalog data for {}", activeNamespace);
        } catch (Exception e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to save catalog data", e);
        }
    }

    private void load() {
        if (saveFile == null || !Files.exists(saveFile)) {
            return;
        }
        try {
            loadFromFile(saveFile);
        } catch (Exception e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to load catalog data for {}", activeNamespace, e);
            Path backup = backupFile(saveFile);
            if (Files.exists(backup)) {
                try {
                    loadFromFile(backup);
                    tempeststudios.inventorysort.core.InventorySortCore.LOGGER.warn(
                            "Restored catalog data for {} from backup {}", activeNamespace, backup.getFileName());
                    return;
                } catch (Exception backupError) {
                    tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error(
                            "Failed to load catalog backup for {}", activeNamespace, backupError);
                }
            }
            snapshots.clear();
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.warn(
                    "Starting with empty catalog data for {}", activeNamespace);
        }
    }

    private void loadFromFile(Path file) throws IOException {
        Type type = new TypeToken<LinkedHashMap<String, LocationSnapshot>>() {}.getType();
        Map<String, LocationSnapshot> loaded;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            loaded = GSON.fromJson(reader, type);
        }

        Map<String, LocationSnapshot> sanitized = new LinkedHashMap<>();
        if (loaded != null) {
            for (Map.Entry<String, LocationSnapshot> entry : loaded.entrySet()) {
                LocationSnapshot snapshot = entry.getValue();
                if (snapshot != null && snapshot.getCounts() != null) {
                    sanitized.put(entry.getKey(), snapshot);
                }
            }
        }

        snapshots.clear();
        snapshots.putAll(sanitized);
        tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info("Loaded catalog with {} locations for {}",
                snapshots.size(), activeNamespace);
    }

    private void writeJsonAtomically(Path target, Object data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            if (Files.exists(target)) {
                Files.copy(target, backupFile(target), StandardCopyOption.REPLACE_EXISTING);
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

    private static Path backupFile(Path target) {
        return target.resolveSibling(target.getFileName().toString() + ".bak");
    }

    /**
     * A single catalogued storage location and its item counts. Serialized directly via Gson.
     */
    public static final class LocationSnapshot {
        private String identityKey;
        private String containerType;
        private String positionLabel;
        private String dimensionKey;
        private long lastSeen;
        private Map<String, Integer> counts;

        LocationSnapshot(String identityKey,
                         String containerType,
                         String positionLabel,
                         String dimensionKey,
                         long lastSeen,
                         Map<String, Integer> counts) {
            this.identityKey = identityKey;
            this.containerType = containerType;
            this.positionLabel = positionLabel;
            this.dimensionKey = dimensionKey;
            this.lastSeen = lastSeen;
            this.counts = counts;
        }

        public String getIdentityKey() {
            return identityKey;
        }

        public String getContainerType() {
            return containerType != null ? containerType : "Container";
        }

        public String getPositionLabel() {
            return positionLabel;
        }

        public String getDimensionKey() {
            return dimensionKey;
        }

        public long getLastSeen() {
            return lastSeen;
        }

        public Map<String, Integer> getCounts() {
            return counts;
        }

        public int getTotalItems() {
            int total = 0;
            for (int count : counts.values()) {
                total += count;
            }
            return total;
        }
    }
}
