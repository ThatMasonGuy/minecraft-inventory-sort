package tempeststudios.inventorysort.compat.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

public final class MinecraftApiCompat {
    private MinecraftApiCompat() {
    }

    public static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.identifier().toString();
    }

    public static BlockPos connectedChestPos(BlockPos pos, BlockState state) {
        return ChestBlock.getConnectedBlockPos(pos, state);
    }

    public static long windowHandle(Minecraft client) {
        return client.getWindow().handle();
    }

    public static void pushHudPose(GuiGraphics graphics) {
        graphics.pose().pushMatrix();
    }

    public static void scaleHudPose(GuiGraphics graphics, float scale) {
        graphics.pose().scale(scale, scale);
    }

    public static void popHudPose(GuiGraphics graphics) {
        graphics.pose().popMatrix();
    }

    public static Path singleplayerServerDirectory(Minecraft client) {
        return client.getSingleplayerServer().getServerDirectory();
    }
}
