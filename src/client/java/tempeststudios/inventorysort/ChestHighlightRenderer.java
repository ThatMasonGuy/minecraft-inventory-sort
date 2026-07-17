package tempeststudios.inventorysort;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tempeststudios.inventorysort.compat.render.ChestHighlightRenderCompat;

public final class ChestHighlightRenderer {
    private static BlockPos highlighted;
    private static ResourceKey<Level> highlightedDimension;
    private static Level highlightedLevel;
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
        Minecraft client = Minecraft.getInstance();
        if (pos == null || client == null || client.level == null) {
            clear();
            return;
        }

        highlighted = pos.immutable();
        highlightedDimension = client.level.dimension();
        highlightedLevel = client.level;
    }

    public static void clear() {
        highlighted = null;
        highlightedDimension = null;
        highlightedLevel = null;
    }

    public static BlockPos getHighlighted() {
        return highlighted;
    }

    private static void validate(Minecraft client) {
        BlockPos pos = highlighted;
        if (pos == null) {
            return;
        }
        if (client == null || client.player == null || client.level == null || client.level != highlightedLevel
                || !client.level.dimension().equals(highlightedDimension)) {
            clear();
            return;
        }
        if (client.level.hasChunkAt(pos) && client.level.getBlockEntity(pos) == null) {
            clear();
            return;
        }
        var screen = tempeststudios.inventorysort.compat.core.MinecraftApiCompat.getScreen(client);
        if (screen instanceof AbstractContainerScreen<?> containerScreen
                && containerScreen instanceof InventorySortContainerContext context) {
            ContainerIdentity identity = context.inventorysort$getContainerIdentity();
            if (identity != null && pos.equals(identity.getPrimaryPos())) {
                clear();
            }
        }
    }
}
