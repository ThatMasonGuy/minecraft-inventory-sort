package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class InventorySortModalIconButton extends Button {
    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int EXPAND = 2;
    public static final int COLLAPSE = 3;
    public static final int CLOSE = 4;

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
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        boolean hovered = isHoveredOrFocused();

        InventorySortUIUtils.drawBeveledPanel(guiGraphics, x, y, size, size, hovered);

        int color = hovered ? 0xFF000000 : 0xFF1C1C1C;

        switch (icon) {
            case UP -> drawUpIcon(guiGraphics, x, y, color);
            case DOWN -> drawDownIcon(guiGraphics, x, y, color);
            case EXPAND -> drawExpandIcon(guiGraphics, x, y, color);
            case COLLAPSE -> drawCollapseIcon(guiGraphics, x, y, color);
            case CLOSE -> drawCloseIcon(guiGraphics, x, y, color);
        }
    }

    private void drawUpIcon(GuiGraphics g, int x, int y, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.fill(cx, cy - 1, cx + 1, cy, color);
        g.fill(cx - 1, cy, cx + 2, cy + 1, color);
        g.fill(cx - 2, cy + 1, cx + 3, cy + 2, color);
    }

    private void drawDownIcon(GuiGraphics g, int x, int y, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.fill(cx - 2, cy - 1, cx + 3, cy, color);
        g.fill(cx - 1, cy, cx + 2, cy + 1, color);
        g.fill(cx, cy + 1, cx + 1, cy + 2, color);
    }

    private void drawExpandIcon(GuiGraphics g, int x, int y, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.fill(cx - 1, cy - 2, cx, cy + 3, color);
        g.fill(cx, cy - 1, cx + 1, cy + 2, color);
        g.fill(cx + 1, cy, cx + 2, cy + 1, color);
    }

    private void drawCollapseIcon(GuiGraphics g, int x, int y, int color) {
        drawDownIcon(g, x, y, color);
    }
    
    private void drawCloseIcon(GuiGraphics g, int x, int y, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.fill(cx - 2, cy - 2, cx - 1, cy - 1, color);
        g.fill(cx - 1, cy - 1, cx, cy, color);
        g.fill(cx, cy, cx + 1, cy + 1, color);
        g.fill(cx + 1, cy + 1, cx + 2, cy + 2, color);
        g.fill(cx + 2, cy + 2, cx + 3, cy + 3, color);

        g.fill(cx + 2, cy - 2, cx + 3, cy - 1, color);
        g.fill(cx + 1, cy - 1, cx + 2, cy, color);
        g.fill(cx - 1, cy + 1, cx, cy + 2, color);
        g.fill(cx - 2, cy + 2, cx - 1, cy + 3, color);
    }
}
