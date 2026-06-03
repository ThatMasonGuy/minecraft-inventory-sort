package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class InventorySortTextButtonRenderer {
    private InventorySortTextButtonRenderer() {
    }

    static void render(GuiGraphics guiGraphics, int x, int y, int width, int height, Component message, boolean active, boolean hovered) {
        InventorySortUIUtils.drawBeveledPanel(guiGraphics, x, y, width, height, hovered);

        int textColor = active ? (hovered ? 0xFF000000 : 0xFF1C1C1C) : 0xFF777777;

        Font font = Minecraft.getInstance().font;
        int textX = x + (width - font.width(message)) / 2;
        int textY = y + (height - 8) / 2;

        guiGraphics.drawString(font, message, textX, textY, textColor, false);
    }
}
