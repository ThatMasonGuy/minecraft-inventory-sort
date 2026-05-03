package tempeststudios.inventorysort;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class LocationEntry {
    private final LocationType type;
    private final String namespace;
    private final String locationIdentity;
    private final String positionLabel;
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
        this(null, null, null, pos, dimension, containerType, count, timestamp);
    }

    public LocationEntry(ContainerIdentity identity, int count, long timestamp) {
        this(identity.getNamespace(),
                identity.getIdentityKey(),
                identity.getPositionLabel(),
                identity.getPrimaryPos(),
                identity.getDimension(),
                identity.getContainerType(),
                count,
                timestamp);
    }

    public LocationEntry(String namespace, String locationIdentity, String positionLabel,
                         BlockPos pos, ResourceKey<Level> dimension, String containerType,
                         int count, long timestamp) {
        this.type = LocationType.CONTAINER;
        this.namespace = namespace;
        this.locationIdentity = locationIdentity;
        this.positionLabel = positionLabel;
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
        this(null, LocationType.INVENTORY, count, timestamp);
    }

    public LocationEntry(String namespace, LocationType inventoryType, int count, long timestamp) {
        this.type = LocationType.INVENTORY;
        this.namespace = namespace;
        this.locationIdentity = "inventory";
        this.positionLabel = null;
        this.pos = null;
        this.dimensionKey = null;
        this.shulkerIdentifier = null;
        this.containerType = "Player Inventory";
        this.count = count;
        this.lastSeen = timestamp;
    }

    // Constructor for shulker box
    public LocationEntry(String shulkerIdentifier, int count, long timestamp) {
        this(null, shulkerIdentifier, count, timestamp);
    }

    public LocationEntry(String namespace, String shulkerIdentifier, int count, long timestamp) {
        this.type = LocationType.SHULKER_BOX;
        this.namespace = namespace;
        this.locationIdentity = shulkerIdentifier;
        this.positionLabel = null;
        this.pos = null;
        this.dimensionKey = null;
        this.shulkerIdentifier = shulkerIdentifier;
        this.containerType = "Shulker Box";
        this.count = count;
        this.lastSeen = timestamp;
    }

    public boolean isSameLocation(LocationEntry other) {
        if (this.type != other.type) return false;
        if (!sameNullable(this.namespace, other.namespace)) return false;

        switch (type) {
            case CONTAINER:
                if (this.locationIdentity != null && other.locationIdentity != null) {
                    return this.locationIdentity.equals(other.locationIdentity);
                }
                return this.pos.equals(other.pos) &&
                        this.dimensionKey.equals(other.dimensionKey);
            case INVENTORY:
                return true; // All inventory locations are "the same"
            case SHULKER_BOX:
                if (this.locationIdentity != null && other.locationIdentity != null) {
                    return this.locationIdentity.equals(other.locationIdentity);
                }
                return this.shulkerIdentifier.equals(other.shulkerIdentifier);
            default:
                return false;
        }
    }

    public boolean isInNamespace(String currentNamespace) {
        return namespace != null && namespace.equals(currentNamespace);
    }

    private static boolean sameNullable(String a, String b) {
        return a == null || b == null || a.equals(b);
    }

    public void updateTimestamp(long timestamp) {
        this.lastSeen = timestamp;
    }

    // Getters
    public LocationType getType() { return type; }
    public String getNamespace() { return namespace; }
    public String getLocationIdentity() { return locationIdentity; }
    public String getPositionLabel() { return positionLabel; }
    public BlockPos getPos() { return pos; }
    public String getDimensionKey() { return dimensionKey; }
    public String getShulkerIdentifier() { return shulkerIdentifier; }
    public String getContainerType() { return containerType; }
    public int getCount() { return count; }
    public long getLastSeen() { return lastSeen; }

    public String getDisplayName() {
        switch (type) {
            case CONTAINER:
                String location = positionLabel != null ? positionLabel : String.format("%d, %d, %d",
                        pos.getX(), pos.getY(), pos.getZ());
                return String.format("%s at %s (%s)",
                        containerType,
                        location,
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
