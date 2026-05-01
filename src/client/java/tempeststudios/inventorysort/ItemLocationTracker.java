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

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemLocationTracker {
    private static final int MAX_LOCATIONS_PER_ITEM = 5;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map: Item ID -> List of LocationEntry (max 5, sorted by most recent)
    private final Map<String, LinkedList<LocationEntry>> trackedLocations;
    private final Path saveFile;

    private static ItemLocationTracker instance;

    private ItemLocationTracker() {
        this.trackedLocations = new ConcurrentHashMap<>();

        // Save file in .minecraft/inventorysort/item_locations.json
        Minecraft mc = Minecraft.getInstance();
        Path gameDir = mc.gameDirectory.toPath();
        Path modDir = gameDir.resolve("inventorysort");

        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            InventorySortClient.LOGGER.error("Failed to create mod directory", e);
        }

        this.saveFile = modDir.resolve("item_locations.json");
        load();
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
        if (stack.isEmpty()) return;

        String itemId = getItemId(stack.getItem());
        LocationEntry newEntry = new LocationEntry(pos, dimension, containerType, stack.getCount(), System.currentTimeMillis());

        addOrUpdateLocation(itemId, newEntry);
    }

    /**
     * Track an item in player inventory
     */
    public void trackItemInInventory(ItemStack stack) {
        if (stack.isEmpty()) return;

        String itemId = getItemId(stack.getItem());
        LocationEntry newEntry = new LocationEntry(stack.getCount(), System.currentTimeMillis());

        addOrUpdateLocation(itemId, newEntry);
    }

    /**
     * Track an item in a shulker box
     */
    public void trackItemInShulker(ItemStack stack, String shulkerIdentifier) {
        if (stack.isEmpty()) return;

        String itemId = getItemId(stack.getItem());
        LocationEntry newEntry = new LocationEntry(shulkerIdentifier, stack.getCount(), System.currentTimeMillis());

        addOrUpdateLocation(itemId, newEntry);
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
            InventorySortClient.LOGGER.info("Updated existing location for {} with new containerType and timestamp", itemId);
        } else {
            // Add new location at front
            locations.addFirst(newEntry);

            // Keep only the 5 most recent locations
            while (locations.size() > MAX_LOCATIONS_PER_ITEM) {
                locations.removeLast();
            }
        }
    }

    /**
     * Get all tracked locations for an item
     */
    public List<LocationEntry> getLocations(Item item) {
        String itemId = getItemId(item);
        LinkedList<LocationEntry> locations = trackedLocations.get(itemId);

        if (locations == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(locations);
    }

    /**
     * Get all tracked locations for an item stack
     */
    public List<LocationEntry> getLocations(ItemStack stack) {
        if (stack.isEmpty()) return Collections.emptyList();
        return getLocations(stack.getItem());
    }

    /**
     * Clear all tracking data
     */
    public void clear() {
        trackedLocations.clear();
        save();
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
        try (Writer writer = new FileWriter(saveFile.toFile())) {
            // Convert to serializable format
            Map<String, List<SerializableLocationEntry>> serializable = new HashMap<>();

            for (Map.Entry<String, LinkedList<LocationEntry>> entry : trackedLocations.entrySet()) {
                List<SerializableLocationEntry> serializableList = new ArrayList<>();
                for (LocationEntry loc : entry.getValue()) {
                    serializableList.add(SerializableLocationEntry.fromLocationEntry(loc));
                }
                serializable.put(entry.getKey(), serializableList);
            }

            GSON.toJson(serializable, writer);
            InventorySortClient.LOGGER.info("Saved item location tracking data");
        } catch (IOException e) {
            InventorySortClient.LOGGER.error("Failed to save item location data", e);
        }
    }

    /**
     * Load tracking data from disk
     */
    private void load() {
        if (!Files.exists(saveFile)) {
            InventorySortClient.LOGGER.info("No saved item location data found");
            return;
        }

        try (Reader reader = new FileReader(saveFile.toFile())) {
            Type type = new TypeToken<Map<String, List<SerializableLocationEntry>>>(){}.getType();
            Map<String, List<SerializableLocationEntry>> serializable = GSON.fromJson(reader, type);

            if (serializable != null) {
                for (Map.Entry<String, List<SerializableLocationEntry>> entry : serializable.entrySet()) {
                    LinkedList<LocationEntry> locations = new LinkedList<>();
                    for (SerializableLocationEntry ser : entry.getValue()) {
                        locations.add(ser.toLocationEntry());
                    }
                    trackedLocations.put(entry.getKey(), locations);
                }

                InventorySortClient.LOGGER.info("Loaded tracking data for {} items", trackedLocations.size());
            }
        } catch (IOException e) {
            InventorySortClient.LOGGER.error("Failed to load item location data", e);
        }
    }

    /**
     * Helper class for JSON serialization
     */
    private static class SerializableLocationEntry {
        String type;
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
                    // Parse dimension string back to ResourceKey
                    ResourceKey<Level> dimKey = parseDimensionKey(dimension);
                    return new LocationEntry(pos.toBlockPos(), dimKey, containerType, count, lastSeen);
                case INVENTORY:
                    return new LocationEntry(count, lastSeen);
                case SHULKER_BOX:
                    return new LocationEntry(shulkerIdentifier, count, lastSeen);
                default:
                    throw new IllegalStateException("Unknown location type: " + type);
            }
        }

        private ResourceKey<Level> parseDimensionKey(String dimensionStr) {
            // Recreate the ResourceKey from the string
            // The string is in format "minecraft:overworld", "minecraft:the_nether", etc.
            if (dimensionStr.equals("minecraft:overworld")) {
                return Level.OVERWORLD;
            } else if (dimensionStr.equals("minecraft:the_nether")) {
                return Level.NETHER;
            } else if (dimensionStr.equals("minecraft:the_end")) {
                return Level.END;
            }
            // Default to overworld if unknown
            return Level.OVERWORLD;
        }
    }
}