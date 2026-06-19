package tempeststudios.inventorysort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemLocationTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map: item stack key -> current known locations, sorted by most recent.
    private final Map<String, LinkedList<LocationEntry>> trackedLocations;
    private final Path modDir;
    private Path saveFile;
    private String activeNamespace;

    private static ItemLocationTracker instance;

    private ItemLocationTracker() {
        this.trackedLocations = new ConcurrentHashMap<>();

        Minecraft mc = Minecraft.getInstance();
        LegacyDataMigration.migrateLegacyData(mc);
        this.modDir = TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.SEARCH);

        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to create InvSearch data directory", e);
        }

        this.activeNamespace = null;
        ensureNamespaceLoaded(TrackingNamespace.current(mc));
    }

    public static ItemLocationTracker getInstance() {
        if (instance == null) {
            instance = new ItemLocationTracker();
        }
        return instance;
    }

    /**
     * Track an item at a container location
     */
    public void trackItem(ItemStack stack, BlockPos pos, ResourceKey<Level> dimension, String containerType) {
        if (!ServerWorldProfileManager.getInstance().trackingAllowed(Minecraft.getInstance())) return;
        if (stack.isEmpty()) return;
        String namespace = ensureCurrentNamespace();

        String itemId = ItemStackIdentity.key(stack);
        LocationEntry newEntry = new LocationEntry(namespace, null, null,
                pos, dimension, containerType, stack.getCount(), System.currentTimeMillis());

        addOrUpdateLocation(itemId, newEntry);
    }

    /**
     * Track an item at a canonical fixed container location.
     */
    public void trackItem(ItemStack stack, ContainerIdentity identity) {
        if (!ServerWorldProfileManager.getInstance().trackingAllowed(Minecraft.getInstance())) return;
        if (stack.isEmpty() || identity == null) return;
        ensureNamespaceLoaded(identity.getNamespace());

        String itemId = ItemStackIdentity.key(stack);
        LocationEntry newEntry = new LocationEntry(identity, stack.getCount(), System.currentTimeMillis());

        addOrUpdateLocation(itemId, newEntry);
    }


    /**
     * Track an item in a shulker box
     */
    public void trackItemInShulker(ItemStack stack, String shulkerIdentifier) {
        if (!ServerWorldProfileManager.getInstance().trackingAllowed(Minecraft.getInstance())) return;
        if (stack.isEmpty()) return;
        String namespace = ensureCurrentNamespace();

        String itemId = ItemStackIdentity.key(stack);
        LocationEntry newEntry = new LocationEntry(namespace, shulkerIdentifier, stack.getCount(), System.currentTimeMillis());

        addOrUpdateLocation(itemId, newEntry);
    }

    public void replaceContainerSnapshot(ContainerIdentity identity, Collection<ItemStack> stacks) {
        if (!ServerWorldProfileManager.getInstance().trackingAllowed(Minecraft.getInstance())) return;
        if (identity == null) return;
        String namespace = ensureNamespaceLoaded(identity.getNamespace());
        Map<String, Integer> aggregated = aggregateByStackKey(stacks);

        removeLocations(location -> location.getType() == LocationEntry.LocationType.CONTAINER
                && location.isInNamespace(namespace)
                && isSameFixedContainer(location, identity));

        if (isPlacedShulker(identity) && !aggregated.isEmpty()) {
            removeMatchingPlacedShulkerSnapshots(namespace, identity, aggregated);
        }

        long timestamp = System.currentTimeMillis();

        for (Map.Entry<String, Integer> entry : aggregated.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            addOrUpdateLocation(entry.getKey(), new LocationEntry(identity, entry.getValue(), timestamp));
        }

        save();
    }

    public void replaceInventorySnapshot(Collection<ItemStack> stacks) {
        if (!ServerWorldProfileManager.getInstance().trackingAllowed(Minecraft.getInstance())) return;
        String namespace = ensureCurrentNamespace();

        removeLocations(location -> location.getType() == LocationEntry.LocationType.INVENTORY
                && location.isInNamespace(namespace));

        long timestamp = System.currentTimeMillis();
        for (Map.Entry<String, Integer> entry : aggregateByStackKey(stacks).entrySet()) {
            if (entry.getValue() <= 0) continue;
            addOrUpdateLocation(entry.getKey(), new LocationEntry(namespace,
                    LocationEntry.LocationType.INVENTORY, entry.getValue(), timestamp));
        }

        save();
    }

    private boolean isSameFixedContainer(LocationEntry location, ContainerIdentity identity) {
        if (location.getLocationIdentity() != null) {
            return location.getLocationIdentity().equals(identity.getIdentityKey());
        }
        return location.getPos() != null
                && location.getPos().equals(identity.getPrimaryPos())
                && Objects.equals(location.getDimensionKey(), identity.getDimensionKey());
    }

    private boolean isPlacedShulker(ContainerIdentity identity) {
        return identity.getContainerType().toLowerCase(Locale.ROOT).contains("shulker");
    }

    private void removeMatchingPlacedShulkerSnapshots(String namespace,
                                                      ContainerIdentity currentIdentity,
                                                      Map<String, Integer> currentSnapshot) {
        Set<String> matchingIdentities = findPlacedShulkerIdentitiesWithSnapshot(namespace, currentIdentity, currentSnapshot);
        if (matchingIdentities.isEmpty()) {
            return;
        }

        removeLocations(location -> location.getType() == LocationEntry.LocationType.CONTAINER
                && location.isInNamespace(namespace)
                && matchingIdentities.contains(location.getLocationIdentity()));
    }

    private Set<String> findPlacedShulkerIdentitiesWithSnapshot(String namespace,
                                                                ContainerIdentity currentIdentity,
                                                                Map<String, Integer> currentSnapshot) {
        Map<String, Map<String, Integer>> snapshotsByIdentity = new HashMap<>();

        for (Map.Entry<String, LinkedList<LocationEntry>> itemEntry : trackedLocations.entrySet()) {
            String itemId = itemEntry.getKey();
            for (LocationEntry location : itemEntry.getValue()) {
                if (location.getType() != LocationEntry.LocationType.CONTAINER
                        || !location.isInNamespace(namespace)
                        || location.getLocationIdentity() == null
                        || location.getLocationIdentity().equals(currentIdentity.getIdentityKey())
                        || !location.getContainerType().toLowerCase(Locale.ROOT).contains("shulker")) {
                    continue;
                }

                snapshotsByIdentity
                        .computeIfAbsent(location.getLocationIdentity(), ignored -> new HashMap<>())
                        .put(itemId, location.getCount());
            }
        }

        Set<String> matchingIdentities = new HashSet<>();
        for (Map.Entry<String, Map<String, Integer>> entry : snapshotsByIdentity.entrySet()) {
            if (entry.getValue().equals(currentSnapshot)) {
                matchingIdentities.add(entry.getKey());
            }
        }
        return matchingIdentities;
    }

    private void removeLocations(java.util.function.Predicate<LocationEntry> predicate) {
        Iterator<Map.Entry<String, LinkedList<LocationEntry>>> iterator = trackedLocations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LinkedList<LocationEntry>> entry = iterator.next();
            entry.getValue().removeIf(predicate);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    private Map<String, Integer> aggregateByStackKey(Collection<ItemStack> stacks) {
        Map<String, Integer> totals = new HashMap<>();
        if (stacks == null) {
            return totals;
        }

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;

            String itemId = ItemStackIdentity.key(stack);
            totals.put(itemId, totals.getOrDefault(itemId, 0) + stack.getCount());
        }

        return totals;
    }

    /**
     * Core logic: add new location or update existing one
     */
    private void addOrUpdateLocation(String itemId, LocationEntry newEntry) {
        trackedLocations.putIfAbsent(itemId, new LinkedList<>());
        LinkedList<LocationEntry> locations = trackedLocations.get(itemId);

        // Check if this location already exists
        LocationEntry existing = null;
        for (LocationEntry entry : locations) {
            if (entry.isSameLocation(newEntry)) {
                existing = entry;
                break;
            }
        }

        if (existing != null) {
            // Replace with new entry (updates containerType, timestamp, etc.)
            locations.remove(existing);
            locations.addFirst(newEntry);
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("Updated existing location for {} with new containerType and timestamp", itemId);
        } else {
            // Add new location at front
            locations.addFirst(newEntry);

        }
    }

    /**
     * Get all tracked locations for an item
     */
    public List<LocationEntry> getLocations(Item item) {
        String namespace = ensureCurrentNamespace();
        String itemId = getItemId(item);
        return locationsForKey(namespace, itemId);
    }

    public List<LocationEntry> getLocationsByKey(String itemKey) {
        String namespace = ensureCurrentNamespace();
        return locationsForKey(namespace, itemKey);
    }

    public Set<String> getTrackedItemKeys() {
        String namespace = ensureCurrentNamespace();
        Set<String> keys = new LinkedHashSet<>();
        for (Map.Entry<String, LinkedList<LocationEntry>> entry : trackedLocations.entrySet()) {
            for (LocationEntry location : entry.getValue()) {
                if (location.isInNamespace(namespace)) {
                    keys.add(entry.getKey());
                    break;
                }
            }
        }
        return keys;
    }

    private List<LocationEntry> locationsForKey(String namespace, String itemId) {
        LinkedList<LocationEntry> locations = trackedLocations.get(itemId);

        if (locations == null) {
            return Collections.emptyList();
        }

        List<LocationEntry> filtered = new ArrayList<>();
        for (LocationEntry location : locations) {
            if (location.isInNamespace(namespace)) {
                filtered.add(location);
            }
        }
        return filtered;
    }

    /**
     * Get all tracked locations for an item stack
     */
    public List<LocationEntry> getLocations(ItemStack stack) {
        if (stack.isEmpty()) return Collections.emptyList();
        return getLocationsByKey(ItemStackIdentity.key(stack));
    }

    /**
     * Clear all tracking data
     */
    public void clear() {
        trackedLocations.clear();
        if (saveNow()) {
            deleteBackupAfterClear();
        }
    }

    /**
     * Get item ID string
     */
    private String getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /**
     * Save tracking data to disk
     */
    public void save() {
        saveNow();
    }

    private boolean saveNow() {
        if (saveFile == null) return false;
        try {
            writeJsonAtomically(saveFile, toSerializableLocations());
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("Saved item location tracking data for {}", activeNamespace);
            return true;
        } catch (Exception e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to save item location data", e);
            return false;
        }
    }

    private Map<String, List<SerializableLocationEntry>> toSerializableLocations() {
        Map<String, List<SerializableLocationEntry>> serializable = new HashMap<>();

        for (Map.Entry<String, LinkedList<LocationEntry>> entry : trackedLocations.entrySet()) {
            List<SerializableLocationEntry> serializableList = new ArrayList<>();
            for (LocationEntry loc : entry.getValue()) {
                serializableList.add(SerializableLocationEntry.fromLocationEntry(loc));
            }
            serializable.put(entry.getKey(), serializableList);
        }

        return serializable;
    }

    private void deleteBackupAfterClear() {
        if (saveFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(backupFile(saveFile));
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error(
                    "Failed to delete item location backup for {}", activeNamespace, e);
        }
    }

    private String ensureCurrentNamespace() {
        return ensureNamespaceLoaded(TrackingNamespace.current(Minecraft.getInstance()));
    }

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
        saveFile = modDir.resolve("item_locations_" + TrackingNamespace.fileNameSafe(namespace) + ".json");
        trackedLocations.clear();
        load();
        return activeNamespace;
    }

    /**
     * Load tracking data from disk
     */
    private void load() {
        if (saveFile == null) return;
        if (!Files.exists(saveFile)) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info("No saved item location data found for {}", activeNamespace);
            return;
        }

        try {
            loadFromFile(saveFile);
        } catch (Exception e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to load item location data for {}", activeNamespace, e);
            Path backup = backupFile(saveFile);
            if (Files.exists(backup)) {
                try {
                    loadFromFile(backup);
                    tempeststudios.inventorysort.core.InventorySortCore.LOGGER.warn(
                            "Restored item location data for {} from backup {}", activeNamespace, backup.getFileName());
                    return;
                } catch (Exception backupError) {
                    tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error(
                            "Failed to load item location backup for {}", activeNamespace, backupError);
                }
            }
            trackedLocations.clear();
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.warn(
                    "Starting with empty item location data for {}", activeNamespace);
        }
    }

    private void loadFromFile(Path file) throws IOException {
        Type type = new TypeToken<Map<String, List<SerializableLocationEntry>>>(){}.getType();
        Map<String, List<SerializableLocationEntry>> serializable;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            serializable = GSON.fromJson(reader, type);
        }

        Map<String, LinkedList<LocationEntry>> loaded = new HashMap<>();
        if (serializable != null) {
            for (Map.Entry<String, List<SerializableLocationEntry>> entry : serializable.entrySet()) {
                LinkedList<LocationEntry> locations = new LinkedList<>();
                if (entry.getValue() != null) {
                    for (SerializableLocationEntry ser : entry.getValue()) {
                        if (ser != null) {
                            locations.add(ser.toLocationEntry());
                        }
                    }
                }
                loaded.put(entry.getKey(), locations);
            }
        }

        trackedLocations.clear();
        trackedLocations.putAll(loaded);
        tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info(
                "Loaded tracking data for {} items in {}", trackedLocations.size(), activeNamespace);
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
     * Helper class for JSON serialization
     */
    private static class SerializableLocationEntry {
        String type;
        String namespace;
        String locationIdentity;
        String positionLabel;
        BlockPosData pos;
        String dimension;
        String shulkerIdentifier;
        String containerType;
        int count;
        long lastSeen;

        static class BlockPosData {
            int x, y, z;

            BlockPosData(BlockPos pos) {
                this.x = pos.getX();
                this.y = pos.getY();
                this.z = pos.getZ();
            }

            BlockPos toBlockPos() {
                return new BlockPos(x, y, z);
            }
        }

        static SerializableLocationEntry fromLocationEntry(LocationEntry entry) {
            SerializableLocationEntry ser = new SerializableLocationEntry();
            ser.type = entry.getType().name();
            ser.namespace = entry.getNamespace();
            ser.locationIdentity = entry.getLocationIdentity();
            ser.positionLabel = entry.getPositionLabel();
            ser.pos = entry.getPos() != null ? new BlockPosData(entry.getPos()) : null;
            ser.dimension = entry.getDimensionKey();
            ser.shulkerIdentifier = entry.getShulkerIdentifier();
            ser.containerType = entry.getContainerType();
            ser.count = entry.getCount();
            ser.lastSeen = entry.getLastSeen();
            return ser;
        }

        LocationEntry toLocationEntry() {
            LocationEntry.LocationType locType = LocationEntry.LocationType.valueOf(type);

            switch (locType) {
                case CONTAINER:
                    // Keep the stored dimension id string as-is so modded/custom dimensions
                    // survive the round-trip instead of collapsing to overworld.
                    return new LocationEntry(namespace, locationIdentity, positionLabel,
                            pos.toBlockPos(), dimension, containerType, count, lastSeen);
                case INVENTORY:
                    return new LocationEntry(namespace, LocationEntry.LocationType.INVENTORY, count, lastSeen);
                case SHULKER_BOX:
                    return new LocationEntry(namespace, shulkerIdentifier, count, lastSeen);
                default:
                    throw new IllegalStateException("Unknown location type: " + type);
            }
        }
    }
}
