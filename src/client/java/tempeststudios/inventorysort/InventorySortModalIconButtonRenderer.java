package tempeststudios.inventorysort;

final class InventorySortModalIconButtonRenderer {
    static final int UP = 0;
    static final int DOWN = 1;
    static final int EXPAND = 2;
    static final int COLLAPSE = 3;
    static final int CLOSE = 4;

    private InventorySortModalIconButtonRenderer() {
    }

    static void render(InventorySortDrawContext guiGraphics, int x, int y, int size, int icon, boolean hovered) {
        int fg = InvUi.button(guiGraphics, x, y, size, size, hovered, true, false, InvUi.ACCENT_SORT);

        int color = (icon == CLOSE && hovered) ? 0xFFFF6B6B : fg;

        switch (icon) {
            case UP -> drawUpIcon(guiGraphics, x, y, size, color);
            case DOWN -> drawDownIcon(guiGraphics, x, y, size, color);
            case EXPAND -> drawExpandIcon(guiGraphics, x, y, size, color);
            case COLLAPSE -> drawCollapseIcon(guiGraphics, x, y, size, color);
            case CLOSE -> drawCloseIcon(guiGraphics, x, y, size, color);
        }
    }

    private static void drawUpIcon(InventorySortDrawContext guiGraphics, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        guiGraphics.fill(cx, cy - 1, cx + 1, cy, color);
        guiGraphics.fill(cx - 1, cy, cx + 2, cy + 1, color);
        guiGraphics.fill(cx - 2, cy + 1, cx + 3, cy + 2, color);
    }

    private static void drawDownIcon(InventorySortDrawContext guiGraphics, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        guiGraphics.fill(cx - 2, cy - 1, cx + 3, cy, color);
        guiGraphics.fill(cx - 1, cy, cx + 2, cy + 1, color);
        guiGraphics.fill(cx, cy + 1, cx + 1, cy + 2, color);
    }

    private static void drawExpandIcon(InventorySortDrawContext guiGraphics, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        guiGraphics.fill(cx - 1, cy - 2, cx, cy + 3, color);
        guiGraphics.fill(cx, cy - 1, cx + 1, cy + 2, color);
        guiGraphics.fill(cx + 1, cy, cx + 2, cy + 1, color);
    }

    private static void drawCollapseIcon(InventorySortDrawContext guiGraphics, int x, int y, int size, int color) {
        drawDownIcon(guiGraphics, x, y, size, color);
    }

    private static void drawCloseIcon(InventorySortDrawContext guiGraphics, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        guiGraphics.fill(cx - 2, cy - 2, cx - 1, cy - 1, color);
        guiGraphics.fill(cx - 1, cy - 1, cx, cy, color);
        guiGraphics.fill(cx, cy, cx + 1, cy + 1, color);
        guiGraphics.fill(cx + 1, cy + 1, cx + 2, cy + 2, color);
        guiGraphics.fill(cx + 2, cy + 2, cx + 3, cy + 3, color);

        guiGraphics.fill(cx + 2, cy - 2, cx + 3, cy - 1, color);
        guiGraphics.fill(cx + 1, cy - 1, cx + 2, cy, color);
        guiGraphics.fill(cx - 1, cy + 1, cx, cy + 2, color);
        guiGraphics.fill(cx - 2, cy + 2, cx - 1, cy + 3, color);
    }
}
