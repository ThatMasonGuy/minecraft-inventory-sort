package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphics;

public class InventorySortUIUtils {

    public static final int COLOR_BACKGROUND = 0xFFC6C6C6;
    public static final int COLOR_BACKGROUND_HOVER = 0xFFE0E0E0;
    public static final int COLOR_BORDER = 0xFF000000;
    public static final int COLOR_HIGHLIGHT = 0xFFFFFFFF;
    public static final int COLOR_SHADOW = 0xFF555555;
    
    public static final int COLOR_RECESSED_BACKGROUND = 0xFF121212;

    public static void drawBeveledPanel(GuiGraphics g, int x, int y, int width, int height, boolean hovered) {
        int centerColor = hovered ? COLOR_BACKGROUND_HOVER : COLOR_BACKGROUND;
        
        // Border (Black), 2px cut on corners
        g.fill(x + 2, y, x + width - 2, y + 1, COLOR_BORDER); // Top
        g.fill(x + 2, y + height - 1, x + width - 2, y + height, COLOR_BORDER); // Bottom
        g.fill(x, y + 2, x + 1, y + height - 2, COLOR_BORDER); // Left
        g.fill(x + width - 1, y + 2, x + width, y + height - 2, COLOR_BORDER); // Right
        
        // Corner pixels
        g.fill(x + 1, y + 1, x + 2, y + 2, COLOR_BORDER); // TL
        g.fill(x + width - 2, y + 1, x + width - 1, y + 2, COLOR_BORDER); // TR
        g.fill(x + 1, y + height - 2, x + 2, y + height - 1, COLOR_BORDER); // BL
        g.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, COLOR_BORDER); // BR

        // Center fill
        g.fill(x + 2, y + 2, x + width - 2, y + height - 2, centerColor);

        // Top/Left highlight (White)
        g.fill(x + 2, y + 1, x + width - 2, y + 2, COLOR_HIGHLIGHT); // Top
        g.fill(x + 1, y + 2, x + 2, y + height - 2, COLOR_HIGHLIGHT); // Left

        // Bottom/Right shadow (Dark Grey)
        g.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, COLOR_SHADOW); // Bottom
        g.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, COLOR_SHADOW); // Right
    }

    public static void drawRecessedPanel(GuiGraphics g, int x, int y, int width, int height) {
        // Outer border (Black)
        g.fill(x, y, x + width, y + height, COLOR_BORDER);
        
        // Inner fill (Dark)
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, COLOR_RECESSED_BACKGROUND);
        
        // Shadows (Top/Left)
        g.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF000000); // Top shadow
        g.fill(x + 1, y + 2, x + 2, y + height - 1, 0xFF000000); // Left shadow
        
        // Highlights (Bottom/Right) - very subtle
        g.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF333333); // Bottom highlight
        g.fill(x + width - 2, y + 1, x + width - 1, y + height - 2, 0xFF333333); // Right highlight
    }
}
