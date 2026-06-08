package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

final class InventorySortTextButtonRenderer {
    private InventorySortTextButtonRenderer() {
    }

    static void render(InventorySortDrawContext guiGraphics, int x, int y, int width, int height, Component message, boolean active, boolean hovered) {
        int textColor = InvUi.button(guiGraphics, x, y, width, height, hovered, active, false, InvUi.ACCENT_SORT);

        Font font = Minecraft.getInstance().font;
        int textX = x + (width - font.width(message)) / 2;
        int textY = y + (height - 8) / 2;

        guiGraphics.drawString(font, message, textX, textY, textColor, false);
    }
}
