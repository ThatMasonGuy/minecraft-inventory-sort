package tempeststudios.inventorysort.compat.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    public static void pushHudPose(GuiGraphicsExtractor graphics) {
        graphics.pose().pushMatrix();
    }

    public static void scaleHudPose(GuiGraphicsExtractor graphics, float scale) {
        graphics.pose().scale(scale, scale);
    }

    public static void popHudPose(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    public static Path singleplayerServerDirectory(Minecraft client) {
        return client.getSingleplayerServer().getServerDirectory();
    }

    public static boolean isGuiHidden(Minecraft client) {
        return client.gui.hud.isHidden();
    }

    public static boolean isScreenOpen(Minecraft client) {
        return client.gui.screen() != null;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }

    public static boolean isSingleplayer(Minecraft client) {
        return client.hasSingleplayerServer();
    }

    public static void sendSystemMessage(Minecraft client, Component message) {
        if (client.player != null) {
            client.player.sendSystemMessage(message);
        }
    }

    public static void sendOverlayMessage(Minecraft client, Component message) {
        if (client.player != null) {
            client.player.sendOverlayMessage(message);
        }
    }
}
