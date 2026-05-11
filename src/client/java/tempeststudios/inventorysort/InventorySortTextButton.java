package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class InventorySortTextButton extends Button {

    public InventorySortTextButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        boolean hovered = isHoveredOrFocused();

        InventorySortUIUtils.drawBeveledPanel(guiGraphics, x, y, width, height, hovered);

        int textColor = active ? (hovered ? 0xFF000000 : 0xFF1C1C1C) : 0xFF777777;
        
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        int textX = x + (width - font.width(getMessage())) / 2;
        int textY = y + (height - 8) / 2;
        
        guiGraphics.drawString(font, getMessage(), textX, textY, textColor, false);
    }
}
