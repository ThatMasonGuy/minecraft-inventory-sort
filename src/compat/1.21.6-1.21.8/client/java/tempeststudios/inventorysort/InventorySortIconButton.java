package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class InventorySortIconButton extends Button {
    public static final int SORT = InventorySortIconButtonRenderer.SORT;
    public static final int MATCHING = InventorySortIconButtonRenderer.MATCHING;
    public static final int ALL = InventorySortIconButtonRenderer.ALL;
    public static final int SEARCH = InventorySortIconButtonRenderer.SEARCH;

    private final int icon;
    private final OnPress secondaryOnPress;

    public InventorySortIconButton(int x, int y, int icon, Component tooltip, OnPress onPress) {
        this(x, y, icon, tooltip, onPress, null);
    }

    public InventorySortIconButton(int x, int y, int icon, Component tooltip, OnPress onPress, OnPress secondaryOnPress) {
        super(x, y, InventorySortIconButtonRenderer.SIZE, InventorySortIconButtonRenderer.SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.secondaryOnPress = secondaryOnPress;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (secondaryOnPress != null && button == 1 && active && visible && isMouseOver(mouseX, mouseY)) {
            secondaryOnPress.onPress(this);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        InventorySortIconButtonRenderer.render(InventorySortDrawContexts.wrap(guiGraphics), getX(), getY(), icon, isHoveredOrFocused());
    }
}
