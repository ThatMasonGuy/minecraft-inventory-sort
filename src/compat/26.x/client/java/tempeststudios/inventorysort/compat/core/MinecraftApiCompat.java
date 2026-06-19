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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        Object gui = fieldValue(client, "gui");
        Object hud = gui != null ? fieldValue(gui, "hud") : null;
        Object hidden = hud != null ? invoke(hud, "isHidden") : null;
        if (hidden instanceof Boolean value) {
            return value;
        }
        Object options = fieldValue(client, "options");
        Object hideGui = options != null ? fieldValue(options, "hideGui") : null;
        return hideGui instanceof Boolean value && value;
    }

    public static boolean isScreenOpen(Minecraft client) {
        Object gui = fieldValue(client, "gui");
        Object screen = gui != null ? invoke(gui, "screen") : null;
        if (screen != null) {
            return true;
        }
        return fieldValue(client, "screen") != null;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        Object gui = fieldValue(client, "gui");
        if (gui != null && invoke(gui, "setScreen", Screen.class, screen)) {
            return;
        }
        if (invoke(client, "setScreen", Screen.class, screen)) {
            return;
        }
        throw new IllegalStateException("Unable to set Minecraft screen on this 26.x runtime");
    }

    public static boolean isSingleplayer(Minecraft client) {
        Object hasServer = invoke(client, "hasSingleplayerServer");
        if (hasServer instanceof Boolean value) {
            return value;
        }
        Object singleplayer = invoke(client, "isSingleplayer");
        return singleplayer instanceof Boolean value && value;
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

    private static Object fieldValue(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getField(name);
            return field.get(target);
        } catch (NoSuchFieldException ignored) {
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException | IllegalAccessException ignoredAgain) {
                return null;
            }
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (NoSuchMethodException ignored) {
            try {
                Method method = target.getClass().getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignoredAgain) {
                return null;
            }
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static boolean invoke(Object target, String name, Class<?> argumentType, Object argument) {
        if (target == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod(name, argumentType);
            method.invoke(target, argument);
            return true;
        } catch (NoSuchMethodException ignored) {
            try {
                Method method = target.getClass().getDeclaredMethod(name, argumentType);
                method.setAccessible(true);
                method.invoke(target, argument);
                return true;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignoredAgain) {
                return false;
            }
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }
}
