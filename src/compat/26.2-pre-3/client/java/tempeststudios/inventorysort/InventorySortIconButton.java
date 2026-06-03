package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class InventorySortIconButton extends Button {
    public static final int SORT = InventorySortIconButtonRenderer.SORT;
    public static final int MATCHING = InventorySortIconButtonRenderer.MATCHING;
    public static final int ALL = InventorySortIconButtonRenderer.ALL;
    public static final int SEARCH = InventorySortIconButtonRenderer.SEARCH;

    private final int icon;

    public InventorySortIconButton(int x, int y, int icon, Component tooltip, OnPress onPress) {
        super(x, y, InventorySortIconButtonRenderer.SIZE, InventorySortIconButtonRenderer.SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        InventorySortIconButtonRenderer.render(InventorySortDrawContexts.wrap(guiGraphics), getX(), getY(), icon, isHoveredOrFocused());
    }
}
