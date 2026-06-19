package tempeststudios.inventorysort;

import net.minecraft.client.gui.Font;

/**
 * Shared modern UI theme for the InvSort / InvSearch / InvCatalogue in-game
 * screens.
 *
 * <p>Everything draws through {@link InventorySortDrawContext} (solid fills plus
 * text) so the exact same look renders on the 1.x {@code GuiGraphics} lane and
 * the 26.x extractor lane. The design language is a cool dark "slate" window
 * with a per-mod accent strip, recessed content wells, slot-style tiles, flat
 * buttons, and segmented tabs. The three screens keep identical chrome but a
 * different accent colour, so they read as one product family while staying
 * individually recognisable.
 *
 * <p>Container surfaces (window, panels, rows, fields, chips) use a 1px corner
 * chamfer for a softened, modern silhouette. Interactive buttons fill their full
 * bounds (no chamfer) so they always paint over the vanilla widget sprite, and
 * item slots stay square to match Minecraft's inventory grid.
 */
public final class InvUi {
    private InvUi() {
    }

    // Per-mod accents: same chrome, different identity.
    public static final int ACCENT_SORT = 0xFFE6B23C;       // gold
    public static final int ACCENT_SEARCH = 0xFF54A9EE;     // sky blue
    public static final int ACCENT_CATALOGUE = 0xFF84C76A;  // green

    // Shared modal dimensions used by the three suite screens.
    public static final int STANDARD_MODAL_W = 480;
    public static final int STANDARD_MODAL_H = 290;
    public static final int MIN_MODAL_W = 318;
    public static final int MIN_MODAL_H = 232;

    // Background and window chrome.
    public static final int SCRIM = 0xC4000000;
    public static final int SHADOW = 0x55000000;
    public static final int WINDOW_BG = 0xF21B1E25;
    public static final int HEADER_BG = 0xFF23272F;
    public static final int BORDER_DARK = 0xFF090A0D;
    public static final int BORDER = 0xFF373C47;
    public static final int BORDER_HI = 0xFF4C5260;

    // Content surfaces.
    public static final int WELL = 0xFF101218;          // deepest recess (fields, list bg)
    public static final int PANEL = 0xFF161922;         // content panel
    public static final int CARD = 0xFF222630;          // raised row / card
    public static final int CARD_HOVER = 0xFF2B313C;
    public static final int DIVIDER = 0xFF282D38;

    // Item slot tiles.
    public static final int SLOT = 0xFF2A2E37;
    public static final int SLOT_HOVER = 0xFF353B45;

    // Text.
    public static final int TEXT = 0xFFECEEF2;
    public static final int TEXT_MUTED = 0xFF9CA2AE;
    public static final int TEXT_DIM = 0xFF686E78;
    public static final int TEXT_DISABLED = 0xFF565C66;
    public static final int TEXT_ON_ACCENT = 0xFF1A1206;

    // Buttons.
    private static final int BTN = 0xFF272C36;
    private static final int BTN_HOVER = 0xFF323845;
    private static final int BTN_BORDER = 0xFF3E4450;
    private static final int BTN_BORDER_HOVER = 0xFF565E6D;
    private static final int BTN_DISABLED = 0xFF1D2027;

    // ---------------------------------------------------------------- scrim

    public static void scrim(InventorySortDrawContext g, int width, int height) {
        g.fill(0, 0, width, height, SCRIM);
    }

    // ------------------------------------------------------ chamfered shapes

    /** Fills a rectangle with the 4 corner pixels left empty (1px chamfer). */
    public static void fillRound(InventorySortDrawContext g, int x, int y, int w, int h, int color) {
        int x2 = x + w;
        int y2 = y + h;
        g.fill(x + 1, y, x2 - 1, y2, color);
        g.fill(x, y + 1, x + 1, y2 - 1, color);
        g.fill(x2 - 1, y + 1, x2, y2 - 1, color);
    }

    /** Draws a 1px border that follows the {@link #fillRound} chamfer. */
    public static void borderRound(InventorySortDrawContext g, int x, int y, int w, int h, int color) {
        int x2 = x + w;
        int y2 = y + h;
        g.fill(x + 1, y, x2 - 1, y + 1, color);
        g.fill(x + 1, y2 - 1, x2 - 1, y2, color);
        g.fill(x, y + 1, x + 1, y2 - 1, color);
        g.fill(x2 - 1, y + 1, x2, y2 - 1, color);
    }

    // ----------------------------------------------------------- window

    /** Main modal window: drop shadow, body, crisp outer border, accent lid. */
    public static void window(InventorySortDrawContext g, int x, int y, int w, int h, int accent) {
        fillRound(g, x + 2, y + 4, w, h, SHADOW);
        fillRound(g, x + 1, y + 2, w, h, SHADOW);
        fillRound(g, x, y, w, h, WINDOW_BG);
        borderRound(g, x, y, w, h, BORDER_DARK);
        // Inner hairline bevel along the top and left for a touch of depth.
        g.fill(x + 1, y + 3, x + 2, y + h - 2, mix(WINDOW_BG, BORDER, 0.6f));
        g.fill(x + w - 2, y + 3, x + w - 1, y + h - 2, BORDER_DARK);
        // Per-mod accent lid just inside the top border.
        g.fill(x + 2, y + 1, x + w - 2, y + 3, accent);
        g.fill(x + 2, y + 3, x + w - 2, y + 4, mix(accent, BORDER_DARK, 0.45f));
    }

    /** Two-tone inset divider line (e.g. under a header). */
    public static void divider(InventorySortDrawContext g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, BORDER_DARK);
        g.fill(x, y + 1, x + w, y + 2, mix(WINDOW_BG, BORDER_HI, 0.5f));
    }

    // ----------------------------------------------------------- panels

    /** Recessed content well with a soft inner shadow. */
    public static void inset(InventorySortDrawContext g, int x, int y, int w, int h) {
        fillRound(g, x, y, w, h, PANEL);
        insetBorder(g, x, y, w, h);
    }

    /** Just the recess frame, for redrawing over clipped/scrolled content. */
    public static void insetBorder(InventorySortDrawContext g, int x, int y, int w, int h) {
        borderRound(g, x, y, w, h, BORDER_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF0B0D12);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFF0B0D12);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, mix(PANEL, BORDER_HI, 0.35f));
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, mix(PANEL, BORDER_HI, 0.35f));
    }

    /** Recessed text-input field. */
    public static void field(InventorySortDrawContext g, int x, int y, int w, int h, boolean focused, int accent) {
        fillRound(g, x, y, w, h, WELL);
        borderRound(g, x, y, w, h, focused ? accent : BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF07080B);
    }

    /** A selectable list row / card. */
    public static void row(InventorySortDrawContext g, int x, int y, int w, int h,
                           boolean hovered, boolean selected, int accent) {
        int bg = selected ? mix(CARD, accent, 0.16f) : hovered ? CARD_HOVER : CARD;
        fillRound(g, x, y, w, h, bg);
        if (selected) {
            borderRound(g, x, y, w, h, mix(BORDER, accent, 0.55f));
            g.fill(x + 1, y + 2, x + 3, y + h - 2, accent);
        } else {
            borderRound(g, x, y, w, h, hovered ? BORDER_HI : BORDER);
        }
    }

    /** A section-header band (e.g. a world group) with an accent tab. */
    public static void sectionBand(InventorySortDrawContext g, Font font, String label,
                                   int x, int y, int w, int h, int accent) {
        fillRound(g, x, y, w, h, mix(PANEL, accent, 0.10f));
        borderRound(g, x, y, w, h, mix(BORDER, accent, 0.30f));
        g.fill(x + 2, y + 2, x + 4, y + h - 2, accent);
        g.drawString(font, label, x + 9, y + (h - 8) / 2, TEXT, false);
    }

    // ----------------------------------------------------------- slots

    /** A square item-slot tile. */
    public static void slot(InventorySortDrawContext g, int x, int y, int size,
                            boolean hovered, boolean selected, int accent) {
        int face = selected ? mix(SLOT, accent, 0.42f) : hovered ? SLOT_HOVER : SLOT;
        int x2 = x + size;
        int y2 = y + size;
        g.fill(x, y, x2, y2, BORDER_DARK);
        g.fill(x + 1, y + 1, x2 - 1, y2 - 1, face);
        // Inner shadow top/left, highlight bottom/right.
        g.fill(x + 1, y + 1, x2 - 1, y + 2, 0xFF15171C);
        g.fill(x + 1, y + 1, x + 2, y2 - 1, 0xFF15171C);
        g.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, mix(face, 0xFFFFFFFF, 0.06f));
        if (selected) {
            g.fill(x, y, x2, y + 1, accent);
            g.fill(x, y2 - 1, x2, y2, accent);
            g.fill(x, y, x + 1, y2, accent);
            g.fill(x2 - 1, y, x2, y2, accent);
        }
    }

    /** Count badge anchored to the bottom-right of a tile. */
    public static void countBadge(InventorySortDrawContext g, Font font, String text,
                                  int rightX, int bottomY, int color) {
        int tw = font.width(text);
        g.fill(rightX - tw - 2, bottomY - 9, rightX + 1, bottomY + 1, 0xD6000000);
        g.drawString(font, text, rightX - tw, bottomY - 8, color, false);
    }

    // ----------------------------------------------------------- buttons

    /**
     * Flat button background filling the full bounds (covers the vanilla widget
     * sprite). Returns the text colour the caller should use for the label.
     */
    public static int button(InventorySortDrawContext g, int x, int y, int w, int h,
                             boolean hovered, boolean enabled, boolean primary, int accent) {
        int x2 = x + w;
        int y2 = y + h;
        if (!enabled) {
            g.fill(x, y, x2, y2, BTN_DISABLED);
            border(g, x, y, w, h, BORDER);
            return TEXT_DISABLED;
        }
        if (primary) {
            int bg = hovered ? mix(accent, 0xFFFFFFFF, 0.14f) : accent;
            g.fill(x, y, x2, y2, bg);
            border(g, x, y, w, h, mix(accent, 0xFF000000, 0.40f));
            g.fill(x + 1, y + 1, x2 - 1, y + 2, mix(bg, 0xFFFFFFFF, 0.28f));
            return TEXT_ON_ACCENT;
        }
        int bg = hovered ? BTN_HOVER : BTN;
        g.fill(x, y, x2, y2, bg);
        border(g, x, y, w, h, hovered ? BTN_BORDER_HOVER : BTN_BORDER);
        g.fill(x + 1, y + 1, x2 - 1, y + 2, mix(bg, 0xFFFFFFFF, 0.07f));
        return hovered ? TEXT : TEXT_MUTED;
    }

    /**
     * Segmented-control / tab cell. Selected cells get an accent underline and a
     * lighter face; unselected cells stay recessed. Returns the label colour.
     */
    public static int segment(InventorySortDrawContext g, int x, int y, int w, int h,
                              boolean hovered, boolean selected, int accent) {
        int x2 = x + w;
        int y2 = y + h;
        int bg = selected ? mix(CARD, accent, 0.18f) : hovered ? 0xFF22262F : 0xFF181B22;
        g.fill(x, y, x2, y2, bg);
        border(g, x, y, w, h, selected ? mix(BORDER, accent, 0.5f) : BORDER);
        if (selected) {
            g.fill(x + 2, y2 - 2, x2 - 2, y2, accent);
            return TEXT;
        }
        return hovered ? TEXT : TEXT_MUTED;
    }

    /** Small stat pill with an accent dot. Returns its drawn width. */
    public static int chip(InventorySortDrawContext g, Font font, String label, int x, int y, int accent) {
        int tw = font.width(label);
        int w = tw + 13;
        int h = 12;
        fillRound(g, x, y, w, h, 0xFF20242D);
        borderRound(g, x, y, w, h, BORDER);
        g.fill(x + 4, y + 4, x + 7, y + 7, accent);
        g.drawString(font, label, x + 9, y + 2, TEXT_MUTED, false);
        return w;
    }

    // ----------------------------------------------------------- scrollbar

    /** Draws a track + thumb for a vertically scrolling region. */
    public static void scrollbar(InventorySortDrawContext g, int x, int y, int trackH,
                                 int contentH, int viewH, int scroll, int accent) {
        if (contentH <= viewH || trackH <= 0) {
            return;
        }
        g.fill(x, y, x + 3, y + trackH, 0x66000000);
        int thumbH = Math.max(12, trackH * viewH / contentH);
        int maxScroll = Math.max(1, contentH - viewH);
        int thumbY = y + (trackH - thumbH) * clamp(scroll, 0, maxScroll) / maxScroll;
        g.fill(x, thumbY, x + 3, thumbY + thumbH, mix(BORDER_HI, accent, 0.30f));
    }

    // ----------------------------------------------------------- helpers

    /** Square 1px border used by buttons/segments (no chamfer). */
    private static void border(InventorySortDrawContext g, int x, int y, int w, int h, int color) {
        int x2 = x + w;
        int y2 = y + h;
        g.fill(x, y, x2, y + 1, color);
        g.fill(x, y2 - 1, x2, y2, color);
        g.fill(x, y + 1, x + 1, y2 - 1, color);
        g.fill(x2 - 1, y + 1, x2, y2 - 1, color);
    }

    /** Linear blend of two opaque ARGB colours (alpha forced opaque). */
    public static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }

    /** Accent at a low-ish alpha for fills over dark surfaces. */
    public static int accentWash(int accent) {
        return (accent & 0x00FFFFFF) | 0x33000000;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
