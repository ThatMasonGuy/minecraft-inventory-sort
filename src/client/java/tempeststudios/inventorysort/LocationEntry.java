package tempeststudios.inventorysort;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class LocationEntry {
    private final LocationType type;
    private final BlockPos pos;
    private final String dimensionKey;
    private final String shulkerIdentifier; // For tracking shulker boxes uniquely
    private final String containerType; // Type of container (chest, barrel, etc.)
    private final int count; // Number of items at this location
    private long lastSeen;

    public enum LocationType {
        CONTAINER,      // Chest, barrel, etc. at a fixed location
        INVENTORY,      // Player inventory
        SHULKER_BOX     // Shulker box (can move around)
    }

    // Constructor for container at position
    public LocationEntry(BlockPos pos, ResourceKey<Level> dimension, String containerType, int count, long timestamp) {
        this.type = LocationType.CONTAINER;
        this.pos = pos;
        // Get the dimension identifier string
        if (dimension == Level.OVERWORLD) {
            this.dimensionKey = "minecraft:overworld";
        } else if (dimension == Level.NETHER) {
            this.dimensionKey = "minecraft:the_nether";
        } else if (dimension == Level.END) {
            this.dimensionKey = "minecraft:the_end";
        } else {
            // Fallback - try to get string representation
            this.dimensionKey = dimension.toString();
        }
        this.shulkerIdentifier = null;
        this.containerType = containerType != null ? containerType : "Container";
        this.count = count;
        this.lastSeen = timestamp;
    }

    // Constructor for player inventory
    public LocationEntry(int count, long timestamp) {
        this.type = LocationType.INVENTORY;
        this.pos = null;
        this.dimensionKey = null;
        this.shulkerIdentifier = null;
        this.containerType = "Player Inventory";
        this.count = count;
        this.lastSeen = timestamp;
    }

    // Constructor for shulker box
    public LocationEntry(String shulkerIdentifier, int count, long timestamp) {
        this.type = LocationType.SHULKER_BOX;
        this.pos = null;
        this.dimensionKey = null;
        this.shulkerIdentifier = shulkerIdentifier;
        this.containerType = "Shulker Box";
        this.count = count;
        this.lastSeen = timestamp;
    }

    public boolean isSameLocation(LocationEntry other) {
        if (this.type != other.type) return false;

        switch (type) {
            case CONTAINER:
                return this.pos.equals(other.pos) &&
                        this.dimensionKey.equals(other.dimensionKey);
            case INVENTORY:
                return true; // All inventory locations are "the same"
            case SHULKER_BOX:
                return this.shulkerIdentifier.equals(other.shulkerIdentifier);
            default:
                return false;
        }
    }

    public void updateTimestamp(long timestamp) {
        this.lastSeen = timestamp;
    }

    // Getters
    public LocationType getType() { return type; }
    public BlockPos getPos() { return pos; }
    public String getDimensionKey() { return dimensionKey; }
    public String getShulkerIdentifier() { return shulkerIdentifier; }
    public String getContainerType() { return containerType; }
    public int getCount() { return count; }
    public long getLastSeen() { return lastSeen; }

    public String getDisplayName() {
        switch (type) {
            case CONTAINER:
                return String.format("%s at %d, %d, %d (%s)",
                        containerType,
                        pos.getX(), pos.getY(), pos.getZ(),
                        dimensionKey.substring(dimensionKey.lastIndexOf(':') + 1));
            case INVENTORY:
                return "Player Inventory";
            case SHULKER_BOX:
                return "Shulker Box #" + shulkerIdentifier.substring(0, Math.min(8, shulkerIdentifier.length()));
            default:
                return "Unknown";
        }
    }
}