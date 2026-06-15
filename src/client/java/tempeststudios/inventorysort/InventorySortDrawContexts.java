package tempeststudios.inventorysort;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

public final class InventorySortDrawContexts {
    private InventorySortDrawContexts() {
    }

    public static InventorySortDrawContext wrap(GuiGraphics graphics) {
        return new GuiGraphicsDrawContext(graphics);
    }

    private static final class GuiGraphicsDrawContext implements InventorySortDrawContext {
        private final GuiGraphics graphics;

        private GuiGraphicsDrawContext(GuiGraphics graphics) {
            this.graphics = graphics;
        }

        @Override
        public void fill(int left, int top, int right, int bottom, int color) {
            graphics.fill(left, top, right, bottom, color);
        }

        @Override
        public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
            graphics.drawString(font, text, x, y, color, shadow);
        }

        @Override
        public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
            graphics.drawString(font, text, x, y, color, shadow);
        }

        @Override
        public void pushPose() {
            MinecraftApiCompat.pushHudPose(graphics);
        }

        @Override
        public void scale(float scale) {
            MinecraftApiCompat.scaleHudPose(graphics, scale);
        }

        @Override
        public void popPose() {
            MinecraftApiCompat.popHudPose(graphics);
        }
    }
}
