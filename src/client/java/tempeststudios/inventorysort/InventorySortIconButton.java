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

	private static final int SIZE = 18;
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
		renderDefaultSprite(guiGraphics);

		int color = isHoveredOrFocused() ? ICON_HOVER_COLOR : ICON_COLOR;
		int x = getX();
		int y = getY();

		switch (icon) {
			case SORT -> drawSortIcon(guiGraphics, x, y, color);
			case MATCHING -> drawMatchingIcon(guiGraphics, x, y, color);
			case ALL -> drawAllIcon(guiGraphics, x, y, color);
			case SEARCH -> drawSearchIcon(guiGraphics, x, y, color);
			default -> drawSortIcon(guiGraphics, x, y, color);
		}
	}

	// Three left-aligned bars in descending width — classic "sorted list" icon
	private static void drawSortIcon(GuiGraphics g, int x, int y, int color) {
		fill(g, x+3, y+5, x+15, y+7, color);
		fill(g, x+3, y+9, x+12, y+11, color);
		fill(g, x+3, y+13, x+8,  y+15, color);
	}

	// Funnel/filter shape — wide at top, narrow stem — represents selective matching
	private static void drawMatchingIcon(GuiGraphics g, int x, int y, int color) {
		fill(g, x+3, y+4,  x+15, y+6,  color);
		fill(g, x+5, y+7,  x+13, y+9,  color);
		fill(g, x+7, y+10, x+11, y+12, color);
		fill(g, x+8, y+12, x+10, y+15, color);
	}

	// Double-shaft bold arrow pointing right — represents "move all" with no filter
	private static void drawAllIcon(GuiGraphics g, int x, int y, int color) {
		fill(g, x+3,  y+6,  x+11, y+8,  color);
		fill(g, x+3,  y+10, x+11, y+12, color);
		fill(g, x+11, y+4,  x+13, y+14, color);
		fill(g, x+13, y+6,  x+15, y+8,  color);
		fill(g, x+13, y+10, x+15, y+12, color);
	}

	// Magnifying glass — circle ring with a diagonal handle
	private static void drawSearchIcon(GuiGraphics g, int x, int y, int color) {
		fill(g, x+3, y+3, x+11, y+5,  color);
		fill(g, x+3, y+9, x+11, y+11, color);
		fill(g, x+3, y+5, x+5,  y+9,  color);
		fill(g, x+9, y+5, x+11, y+9,  color);
		fill(g, x+10, y+10, x+12, y+12, color);
		fill(g, x+12, y+12, x+14, y+14, color);
		fill(g, x+14, y+14, x+16, y+16, color);
	}

	private static void fill(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
		guiGraphics.fill(left, top, right, bottom, color);
	}
}
