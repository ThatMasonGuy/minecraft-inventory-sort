package tempeststudios.inventorysort;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tempeststudios.inventorysort.compat.render.ChestHighlightRenderCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public final class ChestHighlightRenderer {
    private static final long HIGHLIGHT_DURATION_MILLIS = 60_000L;
    private static List<BlockPos> highlighted = Collections.emptyList();
    private static ResourceKey<Level> highlightedDimension;
    private static Level highlightedLevel;
    private static long highlightExpiresAt;
    private static boolean initialized;

    private ChestHighlightRenderer() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ChestHighlightRenderCompat.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> validate(client));
    }

    public static void setHighlighted(BlockPos pos) {
        if (pos == null) {
            clear();
            return;
        }

        setHighlightedPositions(Collections.singletonList(pos));
    }

    public static void setHighlightedPositions(Collection<BlockPos> positions) {
        Minecraft client = Minecraft.getInstance();
        if (positions == null || client == null || client.level == null) {
            clear();
            return;
        }

        LinkedHashSet<BlockPos> uniquePositions = new LinkedHashSet<>();
        for (BlockPos pos : positions) {
            if (pos != null) {
                uniquePositions.add(pos.immutable());
            }
        }
        if (uniquePositions.isEmpty()) {
            clear();
            return;
        }

        highlighted = Collections.unmodifiableList(new ArrayList<>(uniquePositions));
        highlightedDimension = client.level.dimension();
        highlightedLevel = client.level;
        highlightExpiresAt = System.currentTimeMillis() + HIGHLIGHT_DURATION_MILLIS;
    }

    public static void setHighlightedLocations(Collection<LocationEntry> locations) {
        Minecraft client = Minecraft.getInstance();
        if (locations == null || client == null || client.level == null) {
            clear();
            return;
        }

        String dimensionKey = tempeststudios.inventorysort.compat.core.MinecraftApiCompat.dimensionId(client.level.dimension());
        List<BlockPos> positions = new ArrayList<>();
        for (LocationEntry location : locations) {
            if (location != null && location.getType() == LocationEntry.LocationType.CONTAINER
                    && location.getPos() != null && (location.getDimensionKey() == null
                    || location.getDimensionKey().equals(dimensionKey))) {
                positions.add(location.getPos());
            }
        }
        setHighlightedPositions(positions);
    }

    public static boolean setHighlighted(LocationEntry selected, Collection<LocationEntry> locations, boolean selectedOnly) {
        if (selected == null || selected.getPos() == null) {
            return false;
        }
        if (selectedOnly) {
            setHighlighted(selected.getPos());
        } else {
            setHighlightedLocations(locations);
        }
        return true;
    }

    public static void clear() {
        highlighted = Collections.emptyList();
        highlightedDimension = null;
        highlightedLevel = null;
        highlightExpiresAt = 0L;
    }

    public static BlockPos getHighlighted() {
        return highlighted.isEmpty() ? null : highlighted.get(0);
    }

    public static List<BlockPos> getHighlightedPositions() {
        return highlighted;
    }

    private static void validate(Minecraft client) {
        if (highlighted.isEmpty()) {
            return;
        }
        if (System.currentTimeMillis() >= highlightExpiresAt
                || client == null || client.player == null || client.level == null || client.level != highlightedLevel
                || !client.level.dimension().equals(highlightedDimension)) {
            clear();
            return;
        }

        List<BlockPos> validPositions = null;
        for (BlockPos pos : highlighted) {
            if (client.level.hasChunkAt(pos) && client.level.getBlockEntity(pos) == null) {
                if (validPositions == null) {
                    validPositions = new ArrayList<>(highlighted);
                }
                validPositions.remove(pos);
            }
        }
        if (validPositions != null) {
            if (validPositions.isEmpty()) {
                clear();
                return;
            }
            highlighted = Collections.unmodifiableList(validPositions);
        }

        var screen = tempeststudios.inventorysort.compat.core.MinecraftApiCompat.getScreen(client);
        if (screen instanceof AbstractContainerScreen<?> containerScreen
                && containerScreen instanceof InventorySortContainerContext context) {
            ContainerIdentity identity = context.inventorysort$getContainerIdentity();
            if (identity != null && highlighted.contains(identity.getPrimaryPos())) {
                List<BlockPos> remaining = new ArrayList<>(highlighted);
                remaining.remove(identity.getPrimaryPos());
                if (remaining.isEmpty()) {
                    clear();
                } else {
                    highlighted = Collections.unmodifiableList(remaining);
                }
            }
        }
    }
}
