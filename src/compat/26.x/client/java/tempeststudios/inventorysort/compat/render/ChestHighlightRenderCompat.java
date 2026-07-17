package tempeststudios.inventorysort.compat.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import tempeststudios.inventorysort.ChestHighlightRenderer;

public final class ChestHighlightRenderCompat {
    private static final int STROKE_COLOR = 0xFFFFE014;
    private static final GizmoStyle[] STYLES = createStyles();
    private static boolean initialized;

    private ChestHighlightRenderCompat() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        LevelRenderEvents.BEFORE_GIZMOS.register(ChestHighlightRenderCompat::render);
    }

    private static void render(LevelRenderContext context) {
        BlockPos pos = ChestHighlightRenderer.getHighlighted();
        Minecraft client = Minecraft.getInstance();
        if (pos == null || client.level == null || !client.level.hasChunkAt(pos)) {
            return;
        }

        int styleIndex = (int) ((System.nanoTime() / 125_000_000L) & 15L);
        try (Gizmos.TemporaryCollection ignored = context.levelRenderer().collectPerFrameRenderThreadGizmos()) {
            Gizmos.cuboid(pos, STYLES[styleIndex]).setAlwaysOnTop();
        }
    }

    private static GizmoStyle[] createStyles() {
        GizmoStyle[] styles = new GizmoStyle[16];
        for (int i = 0; i < styles.length; i++) {
            float pulse = 0.5F + 0.5F * (float) Math.sin((Math.PI * 2.0D * i) / styles.length);
            int alpha = (int) ((0.16F + 0.10F * pulse) * 255.0F);
            styles[i] = GizmoStyle.strokeAndFill(STROKE_COLOR, 3.0F, (alpha << 24) | (STROKE_COLOR & 0x00FFFFFF));
        }
        return styles;
    }
}
