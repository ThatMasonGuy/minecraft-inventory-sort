package tempeststudios.inventorysort.compat.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import tempeststudios.inventorysort.ChestHighlightRenderer;

public final class ChestHighlightRenderCompat {
    private static final float RED = 1.0F;
    private static final float GREEN = 0.88F;
    private static final float BLUE = 0.08F;
    private static boolean initialized;

    private ChestHighlightRenderCompat() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        WorldRenderEvents.AFTER_ENTITIES.register(ChestHighlightRenderCompat::render);
    }

    private static void render(WorldRenderContext context) {
        BlockPos pos = ChestHighlightRenderer.getHighlighted();
        Minecraft client = Minecraft.getInstance();
        if (pos == null || client.level == null || !client.level.hasChunkAt(pos)) {
            return;
        }

        PoseStack matrices = context.matrixStack();
        if (matrices == null || context.consumers() == null || context.camera() == null) {
            return;
        }

        Vec3 camera = context.camera().getPosition();
        double x = pos.getX() - camera.x;
        double y = pos.getY() - camera.y;
        double z = pos.getZ() - camera.z;
        float alpha = 0.65F + 0.20F * (float) Math.sin((System.nanoTime() / 1_000_000_000.0D) * 3.0D);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2.0F);
        LevelRenderer.renderLineBox(matrices, context.consumers().getBuffer(RenderType.lines()),
                x - 0.002D, y - 0.002D, z - 0.002D,
                x + 1.002D, y + 1.002D, z + 1.002D,
                RED, GREEN, BLUE, alpha);
        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
