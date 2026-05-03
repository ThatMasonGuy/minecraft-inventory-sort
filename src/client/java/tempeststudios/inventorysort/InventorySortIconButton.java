package tempeststudios.inventorysort;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class InventorySortIconButton extends Button {
	public static final int SORT = 0;
	public static final int MATCHING = 1;
	public static final int ALL = 2;
	public static final int SEARCH = 3;

	private static final int SIZE = 12;
	private static final int ICON_COLOR = 0xFF1C1C1C;
	private static final int ICON_HOVER_COLOR = 0xFF000000;
	private final int icon;

	public InventorySortIconButton(int x, int y, int icon, Component tooltip, OnPress onPress) {
		super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
		this.icon = icon;
		setTooltip(Tooltip.create(tooltip));
	}

	@Override
	protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		boolean hovered = isHoveredOrFocused();

		// Custom beveled background
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

	private void drawBeveledBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered) {
		// Border (Black), 2px cut on corners
		fill(guiGraphics, x + 2, y, x + width - 2, y + 1, 0xFF000000); // Top
		fill(guiGraphics, x + 2, y + height - 1, x + width - 2, y + height, 0xFF000000); // Bottom
		fill(guiGraphics, x, y + 2, x + 1, y + height - 2, 0xFF000000); // Left
		fill(guiGraphics, x + width - 1, y + 2, x + width, y + height - 2, 0xFF000000); // Right
		// Corner pixels
		fill(guiGraphics, x + 1, y + 1, x + 2, y + 2, 0xFF000000); // TL
		fill(guiGraphics, x + width - 2, y + 1, x + width - 1, y + 2, 0xFF000000); // TR
		fill(guiGraphics, x + 1, y + height - 2, x + 2, y + height - 1, 0xFF000000); // BL
		fill(guiGraphics, x + width - 2, y + height - 2, x + width - 1, y + height - 1, 0xFF000000); // BR

		// Center fill (Lighter grey on hover)
		int centerColor = hovered ? 0xFFE0E0E0 : 0xFFC6C6C6;
		fill(guiGraphics, x + 2, y + 2, x + width - 2, y + height - 2, centerColor);

		// Top/Left highlight (White)
		fill(guiGraphics, x + 2, y + 1, x + width - 2, y + 2, 0xFFFFFFFF); // Top
		fill(guiGraphics, x + 1, y + 2, x + 2, y + height - 2, 0xFFFFFFFF); // Left

		// Bottom/Right shadow (Dark Grey)
		fill(guiGraphics, x + 2, y + height - 2, x + width - 2, y + height - 1, 0xFF555555); // Bottom
		fill(guiGraphics, x + width - 2, y + 2, x + width - 1, y + height - 2, 0xFF555555); // Right
	}

	// 3 left-aligned horizontal bars in descending width
	private static void drawSortIcon(GuiGraphics g, int x, int y, int color) {
		fill(g, x + 3, y + 3, x + 9, y + 4, color);
		fill(g, x + 3, y + 5, x + 7, y + 6, color);
		fill(g, x + 3, y + 7, x + 5, y + 8, color);
	}

	// Funnel/filter shape — 3 horizontal bars centered in descending width
	private static void drawMatchingIcon(GuiGraphics g, int x, int y, int color) {
		fill(g, x + 3, y + 3, x + 9, y + 4, color);
		fill(g, x + 4, y + 5, x + 8, y + 6, color);
		fill(g, x + 5, y + 7, x + 7, y + 8, color);
	}

	// Double arrows, one up (left) and one down (right)
	private static void drawAllIcon(GuiGraphics g, int x, int y, int color) {
		// Up arrow (left)
		fill(g, x + 4, y + 3, x + 5, y + 4, color);
		fill(g, x + 3, y + 4, x + 6, y + 5, color);
		fill(g, x + 4, y + 5, x + 5, y + 9, color);

		// Down arrow (right)
		fill(g, x + 7, y + 3, x + 8, y + 7, color);
		fill(g, x + 6, y + 7, x + 9, y + 8, color);
		fill(g, x + 7, y + 8, x + 8, y + 9, color);
	}

	// Magnifying glass — rounded pixel lens with short diagonal handle
	private static void drawSearchIcon(GuiGraphics g, int x, int y, int color) {
		x += 1;
		y += 1;

		// Lens: 5x4 rounded outline
		fill(g, x + 3, y + 2, x + 6, y + 3, color); // Top
		fill(g, x + 2, y + 3, x + 3, y + 5, color); // Left
		fill(g, x + 6, y + 3, x + 7, y + 5, color); // Right
		fill(g, x + 3, y + 5, x + 6, y + 6, color); // Bottom

		// Handle: starts from bottom-right side, not the corner-corner
		fill(g, x + 6, y + 6, x + 7, y + 7, color);
		fill(g, x + 7, y + 7, x + 8, y + 8, color);
	}

	private static void fill(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
		guiGraphics.fill(left, top, right, bottom, color);
	}
}
