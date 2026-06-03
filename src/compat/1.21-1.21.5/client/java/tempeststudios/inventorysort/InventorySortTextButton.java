package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class InventorySortTextButton extends Button {
    public InventorySortTextButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        InventorySortTextButtonRenderer.render(InventorySortDrawContexts.wrap(guiGraphics), getX(), getY(), getWidth(), getHeight(), getMessage(), active, isHoveredOrFocused());
    }
}
