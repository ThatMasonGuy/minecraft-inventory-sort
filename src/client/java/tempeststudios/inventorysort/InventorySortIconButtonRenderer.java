package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphics;

final class InventorySortIconButtonRenderer {
    static final int SORT = 0;
    static final int MATCHING = 1;
    static final int ALL = 2;
    static final int SEARCH = 3;

    static final int SIZE = 12;

    private static final int ICON_COLOR = 0xFF1C1C1C;
    private static final int ICON_HOVER_COLOR = 0xFF000000;

    private InventorySortIconButtonRenderer() {
    }

    static void render(GuiGraphics guiGraphics, int x, int y, int icon, boolean hovered) {
        drawBeveledBackground(guiGraphics, x, y, SIZE, SIZE, hovered);

        int color = hovered ? ICON_HOVER_COLOR : ICON_COLOR;

        switch (icon) {
            case SORT -> drawSortIcon(guiGraphics, x, y, color);
            case MATCHING -> drawMatchingIcon(guiGraphics, x, y, color);
            case ALL -> drawAllIcon(guiGraphics, x, y, color);
            case SEARCH -> drawSearchIcon(guiGraphics, x, y, color);
            default -> drawSortIcon(guiGraphics, x, y, color);
        }
    }

    private static void drawBeveledBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered) {
        fill(guiGraphics, x + 2, y, x + width - 2, y + 1, 0xFF000000);
        fill(guiGraphics, x + 2, y + height - 1, x + width - 2, y + height, 0xFF000000);
        fill(guiGraphics, x, y + 2, x + 1, y + height - 2, 0xFF000000);
        fill(guiGraphics, x + width - 1, y + 2, x + width, y + height - 2, 0xFF000000);
        fill(guiGraphics, x + 1, y + 1, x + 2, y + 2, 0xFF000000);
        fill(guiGraphics, x + width - 2, y + 1, x + width - 1, y + 2, 0xFF000000);
        fill(guiGraphics, x + 1, y + height - 2, x + 2, y + height - 1, 0xFF000000);
        fill(guiGraphics, x + width - 2, y + height - 2, x + width - 1, y + height - 1, 0xFF000000);

        int centerColor = hovered ? 0xFFE0E0E0 : 0xFFC6C6C6;
        fill(guiGraphics, x + 2, y + 2, x + width - 2, y + height - 2, centerColor);

        fill(guiGraphics, x + 2, y + 1, x + width - 2, y + 2, 0xFFFFFFFF);
        fill(guiGraphics, x + 1, y + 2, x + 2, y + height - 2, 0xFFFFFFFF);

        fill(guiGraphics, x + 2, y + height - 2, x + width - 2, y + height - 1, 0xFF555555);
        fill(guiGraphics, x + width - 2, y + 2, x + width - 1, y + height - 2, 0xFF555555);
    }

    private static void drawSortIcon(GuiGraphics guiGraphics, int x, int y, int color) {
        fill(guiGraphics, x + 3, y + 3, x + 9, y + 4, color);
        fill(guiGraphics, x + 3, y + 5, x + 7, y + 6, color);
        fill(guiGraphics, x + 3, y + 7, x + 5, y + 8, color);
    }

    private static void drawMatchingIcon(GuiGraphics guiGraphics, int x, int y, int color) {
        fill(guiGraphics, x + 3, y + 3, x + 9, y + 4, color);
        fill(guiGraphics, x + 4, y + 5, x + 8, y + 6, color);
        fill(guiGraphics, x + 5, y + 7, x + 7, y + 8, color);
    }

    private static void drawAllIcon(GuiGraphics guiGraphics, int x, int y, int color) {
        fill(guiGraphics, x + 4, y + 3, x + 5, y + 4, color);
        fill(guiGraphics, x + 3, y + 4, x + 6, y + 5, color);
        fill(guiGraphics, x + 4, y + 5, x + 5, y + 9, color);

        fill(guiGraphics, x + 7, y + 3, x + 8, y + 7, color);
        fill(guiGraphics, x + 6, y + 7, x + 9, y + 8, color);
        fill(guiGraphics, x + 7, y + 8, x + 8, y + 9, color);
    }

    private static void drawSearchIcon(GuiGraphics guiGraphics, int x, int y, int color) {
        x += 1;
        y += 1;

        fill(guiGraphics, x + 3, y + 2, x + 6, y + 3, color);
        fill(guiGraphics, x + 2, y + 3, x + 3, y + 5, color);
        fill(guiGraphics, x + 6, y + 3, x + 7, y + 5, color);
        fill(guiGraphics, x + 3, y + 5, x + 6, y + 6, color);

        fill(guiGraphics, x + 6, y + 6, x + 7, y + 7, color);
        fill(guiGraphics, x + 7, y + 7, x + 8, y + 8, color);
    }

    private static void fill(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, bottom, color);
    }
}
