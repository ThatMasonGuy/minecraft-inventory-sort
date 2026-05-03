package tempeststudios.inventorysort;

import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ContainerIdentity {
    private final String namespace;
    private final ResourceKey<Level> dimension;
    private final String dimensionKey;
    private final String identityKey;
    private final String positionLabel;
    private final BlockPos primaryPos;
    private final String containerType;

    private ContainerIdentity(String namespace,
                              ResourceKey<Level> dimension,
                              String identityKey,
                              String positionLabel,
                              BlockPos primaryPos,
                              String containerType) {
        this.namespace = namespace;
        this.dimension = dimension;
        this.dimensionKey = dimensionKey(dimension);
        this.identityKey = identityKey;
        this.positionLabel = positionLabel;
        this.primaryPos = primaryPos;
        this.containerType = containerType;
    }

    public static ContainerIdentity fromLookedAt(Minecraft client, BlockPos pos) {
        if (client == null || client.level == null || pos == null) {
            return null;
        }

        Level level = client.level;
        BlockState state = level.getBlockState(pos);
        MenuProvider menuProvider = state.getMenuProvider(level, pos);
        if (menuProvider == null) {
            return null;
        }

        String namespace = TrackingNamespace.current(client);
        ResourceKey<Level> dimension = level.dimension();
        String dimensionKey = dimensionKey(dimension);
        String containerType = extractContainerType(state);

        List<BlockPos> positions = new ArrayList<>();
        positions.add(pos);

        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
            ChestType chestType = state.getValue(ChestBlock.TYPE);
            if (chestType != ChestType.SINGLE) {
                BlockPos connected = ChestBlock.getConnectedBlockPos(pos, state);
                BlockState connectedState = level.getBlockState(connected);
                if (connectedState.getBlock() == state.getBlock()
                        && connectedState.hasProperty(ChestBlock.TYPE)
                        && connectedState.getValue(ChestBlock.TYPE) == chestType.getOpposite()) {
                    positions.add(connected);
                    containerType = containerType.toLowerCase(Locale.ROOT).contains("trapped")
                            ? "Double Trapped Chest"
                            : "Double Chest";
                }
            }
        }

        positions.sort(Comparator
                .comparingInt((BlockPos p) -> p.getX())
                .thenComparingInt(p -> p.getY())
                .thenComparingInt(p -> p.getZ()));

        StringBuilder key = new StringBuilder("fixed:");
        key.append(dimensionKey).append(":");
        for (int i = 0; i < positions.size(); i++) {
            BlockPos p = positions.get(i);
            if (i > 0) {
                key.append("+");
            }
            key.append(posKey(p));
        }

        return new ContainerIdentity(namespace, dimension, key.toString(), compactPositionLabel(positions), positions.get(0), containerType);
    }

    private static String compactPositionLabel(List<BlockPos> positions) {
        if (positions.size() == 2) {
            BlockPos a = positions.get(0);
            BlockPos b = positions.get(1);
            if (a.getY() == b.getY() && a.getZ() == b.getZ()) {
                return String.format("(%d - %d), %d, %d", a.getX(), b.getX(), a.getY(), a.getZ());
            }
            if (a.getX() == b.getX() && a.getY() == b.getY()) {
                return String.format("%d, %d, (%d - %d)", a.getX(), a.getY(), a.getZ(), b.getZ());
            }
        }

        StringBuilder label = new StringBuilder();
        for (int i = 0; i < positions.size(); i++) {
            BlockPos p = positions.get(i);
            if (i > 0) {
                label.append(" + ");
            }
            label.append(p.getX()).append(", ").append(p.getY()).append(", ").append(p.getZ());
        }
        return label.toString();
    }

    private static String extractContainerType(BlockState state) {
        String blockName = state.getBlock().getName().getString();
        if (blockName.contains("Block{")) {
            int start = blockName.indexOf("Block{") + 6;
            int end = blockName.indexOf("}", start);
            if (end > start) {
                blockName = blockName.substring(start, end);
            }
        }
        if (blockName.contains(":")) {
            blockName = blockName.substring(blockName.lastIndexOf(':') + 1);
        }
        blockName = blockName.replace('_', ' ').trim();
        if (blockName.isEmpty()) {
            return "Container";
        }
        return blockName.substring(0, 1).toUpperCase() + blockName.substring(1);
    }

    private static String posKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String dimensionKey(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return "unknown";
        }
        return dimension.identifier().toString();
    }

    public String getNamespace() {
        return namespace;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public String getDimensionKey() {
        return dimensionKey;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public String getPositionLabel() {
        return positionLabel;
    }

    public BlockPos getPrimaryPos() {
        return primaryPos;
    }

    public String getContainerType() {
        return containerType;
    }

    public int getExpectedSlotCount() {
        String type = containerType.toLowerCase(Locale.ROOT);
        if (type.contains("double")) {
            return 54;
        }
        if (type.contains("shulker") || type.contains("barrel") || type.contains("chest")) {
            return 27;
        }
        return -1; // Unknown or variable size
    }

}
