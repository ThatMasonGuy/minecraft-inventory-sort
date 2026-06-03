package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class InventorySortModalIconButton extends Button {
    public static final int UP = InventorySortModalIconButtonRenderer.UP;
    public static final int DOWN = InventorySortModalIconButtonRenderer.DOWN;
    public static final int EXPAND = InventorySortModalIconButtonRenderer.EXPAND;
    public static final int COLLAPSE = InventorySortModalIconButtonRenderer.COLLAPSE;
    public static final int CLOSE = InventorySortModalIconButtonRenderer.CLOSE;

    private int icon;
    private final int size;
    private String targetId;

    public InventorySortModalIconButton(int x, int y, int size, int icon, Component tooltip, OnPress onPress) {
        super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.size = size;
        if (tooltip != null && !tooltip.getString().isEmpty()) {
            setTooltip(Tooltip.create(tooltip));
        }
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetId() {
        return targetId;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        InventorySortModalIconButtonRenderer.render(InventorySortDrawContexts.wrap(guiGraphics), getX(), getY(), size, icon, isHoveredOrFocused());
    }
}
