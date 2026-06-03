package tempeststudios.inventorysort.compat.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.nio.file.Path;

public final class MinecraftApiCompat {
    private MinecraftApiCompat() {
    }

    public static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    public static BlockPos connectedChestPos(BlockPos pos, BlockState state) {
        return pos.relative(ChestBlock.getConnectedDirection(state));
    }

    public static long windowHandle(Minecraft client) {
        return client.getWindow().getWindow();
    }

    public static void pushHudPose(GuiGraphics graphics) {
        graphics.pose().pushPose();
    }

    public static void scaleHudPose(GuiGraphics graphics, float scale) {
        graphics.pose().scale(scale, scale, 1.0F);
    }

    public static void popHudPose(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    public static Path singleplayerServerDirectory(Minecraft client) {
        File serverDirectory = client.getSingleplayerServer().getServerDirectory();
        return serverDirectory != null ? serverDirectory.toPath() : null;
    }

    public static boolean isGuiHidden(Minecraft client) {
        return client.options.hideGui;
    }

    public static boolean isScreenOpen(Minecraft client) {
        return client.screen != null;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    public static boolean isSingleplayer(Minecraft client) {
        return client.isSingleplayer();
    }

    public static void sendSystemMessage(Minecraft client, Component message) {
        if (client.player != null) {
            client.player.displayClientMessage(message, false);
        }
    }

    public static void sendOverlayMessage(Minecraft client, Component message) {
        if (client.player != null) {
            client.player.displayClientMessage(message, true);
        }
    }
}
