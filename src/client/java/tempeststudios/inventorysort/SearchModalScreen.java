package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class SearchModalScreen extends Screen {

    private final Screen parent;

    private EditBox searchBox;
    private String lastQuery = "";

    // Registry cache (built once)
    private static List<RegistryEntry> REGISTRY_CACHE = null;
    private static final Map<String, RegistryEntry> REGISTRY_BY_ID = new HashMap<>();

    // Most-recently-seen item ids (persist across opens)
    private static final Deque<String> RECENT_IDS = new ArrayDeque<>();
    private static final int RECENT_LIMIT = 50;

    // Current inventory snapshot (count + whether hotbar/inv)
    private final Map<String, InvSnapshot> invSnapshot = new HashMap<>();

    // Results for the current query
    private final List<ResultRow> results = new ArrayList<>();

    // Expand state per result id
    private final Set<String> expanded = new HashSet<>();

    // UI buttons
    private final List<Button> expandButtons = new ArrayList<>();
    private Button scrollUpBtn;
    private Button scrollDownBtn;

    // Scrolling
    private int scrollOffsetPixels = 0;

    // Layout / constants
    private int modalW;
    private int modalH;
    private int modalX;
    private int modalY;

    private int listX;
    private int listTopY;
    private int listBottomY;

    // Two right-side columns:
    // [ list content ... ][ expand ▶ ][ scroll ▲▼ ]
    private int expandColX;
    private int scrollColX;
    private int rowRightX;     // right edge of row background (stops before scroll column)
    private int listContentW;  // width available for icon+text before expand column
    private int countColX;     // x of the count column, scaled from content width

    private static final int PAD = 14;
    private static final int ROW_H = 20;
    private static final int DETAILS_H = 60; // Height for expanded details (header + 3 locations + "+X more")

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mma");

    public SearchModalScreen(Screen parent) {
        super(Component.literal("Search"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        // Clamp modal size so it never overflows the screen
        this.modalW = Math.min(420, this.width - 24);
        this.modalH = Math.min(260, this.height - 24);
        this.modalX = (this.width - modalW) / 2;
        this.modalY = (this.height - modalH) / 2;

        // Build registry cache (once)
        ensureRegistryCache();

        // Build inventory snapshot (counts per registry id)
        buildInventorySnapshot(mc.player.getInventory());

        // Layout constants for this init
        this.listX = modalX + PAD;

        // Add a slightly larger gap between search bar and list ✅
        int searchBoxY = modalY + 22;
        this.listTopY = modalY + 60;

        // Leave room for footer hint
        this.listBottomY = modalY + modalH - PAD - 20;

        // Columns on the right:
        this.scrollColX = modalX + modalW - PAD - 18;
        this.expandColX = scrollColX - 20; // 2px gap between expand and scroll
        this.rowRightX = expandColX + 18;  // row background includes expand button column
        this.listContentW = (expandColX - 6) - listX; // icon+text space ends before expand
        // Count column scales with content width and always reserves room on the right,
        // so the name truncates before it instead of overlapping (see updateResults render).
        this.countColX = listX + Math.max(60, listContentW - 56);

        // Search box
        int boxX = modalX + PAD;
        int boxW = (scrollColX - 6) - boxX; // stop before right-side columns
        // Recessed field is at searchBoxY, 18px tall. EditBox is unbordered, so it draws text
        // flush at its Y (no auto-centering) - offset by +5 to center text in the field.
        this.searchBox = new EditBox(this.font, boxX + 2, searchBoxY + 5, boxW - 4, 14, Component.literal("Search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue("");
        this.searchBox.setBordered(false); // We draw our own beveled border
        this.searchBox.setTextColor(0xFFE0E0E0); // Light text on the dark recessed field
        this.addRenderableWidget(this.searchBox);

        // Close button — keep it inside panel ✅
        int closeX = modalX + modalW - 22;
        int closeY = modalY + 6;
        this.addRenderableWidget(new InventorySortModalIconButton(closeX, closeY, 16, InventorySortModalIconButton.CLOSE, Component.literal("Close"), btn -> closeToParent()));

        if (TrackingNamespace.isMultiplayerServer(mc)) {
            this.addRenderableWidget(new InventorySortTextButton(closeX - 52, closeY, 48, 16, Component.literal("World"), btn -> mc.setScreen(new ServerWorldProfileScreen(this))));
        }

        // Scroll buttons in their own column (never overlap rows now) ✅
        scrollUpBtn = new InventorySortModalIconButton(scrollColX, listTopY, 18, InventorySortModalIconButton.UP, Component.literal("Scroll Up"), btn -> scrollBy(-1));
        scrollDownBtn = new InventorySortModalIconButton(scrollColX, listTopY + 20, 18, InventorySortModalIconButton.DOWN, Component.literal("Scroll Down"), btn -> scrollBy(1));

        this.addRenderableWidget(scrollUpBtn);
        this.addRenderableWidget(scrollDownBtn);

        // Expand buttons pool (only interactive element per row)
        expandButtons.clear();
        for (int i = 0; i < 50; i++) {
            InventorySortModalIconButton b = new InventorySortModalIconButton(0, 0, 16, InventorySortModalIconButton.EXPAND, Component.empty(), btn -> {
                String target = ((InventorySortModalIconButton)btn).getTargetId();
                if (target != null) {
                    toggleExpanded(target);
                }
            });
            b.visible = false;
            b.active = false;
            this.addRenderableWidget(b);
            expandButtons.add(b);
        }

        // Initial results: query empty => recents
        updateResults("");
        updateLayout();

        this.lastQuery = "";
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.searchBox != null) {
            String q = this.searchBox.getValue();
            if (!q.equals(this.lastQuery)) {
                this.lastQuery = q;
                this.scrollOffsetPixels = 0;
                updateResults(q);
                updateLayout();
            }
        }
    }

    @Override
    public void onClose() {
        // Esc should return to the container/inventory we opened from, not drop to the game.
        closeToParent();
    }

    private void closeToParent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.setScreen(parent);
    }

    private void toggleExpanded(String id) {
        if (expanded.contains(id)) expanded.remove(id);
        else expanded.add(id);
        updateLayout();
    }

    private void scrollBy(int delta) {
        if (delta == 0 || results.isEmpty()) return;

        // Snap to row boundaries so one notch moves exactly one row, whether the row at the top
        // edge is expanded (tall) or collapsed (short) - fixes uneven scrolling past open rows.
        int current = scrollOffsetPixels;
        int target;
        if (delta > 0) {
            int top = 0;
            target = current;
            for (ResultRow row : results) {
                if (top > current + 1) { target = top; break; }
                top += rowSpan(row);
            }
            if (target == current) target = top; // past the last boundary; clamp handles the rest
        } else {
            int top = 0;
            target = 0;
            for (ResultRow row : results) {
                if (top >= current - 1) break;
                target = top;
                top += rowSpan(row);
            }
        }
        scrollOffsetPixels = target;
        updateLayout();
    }

    private int rowSpan(ResultRow row) {
        return ROW_H + (expanded.contains(row.id) ? DETAILS_H : 0) + 4;
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        return mouseX >= listX - 2
                && mouseX <= rowRightX
                && mouseY >= listTopY
                && mouseY <= listBottomY;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Dim background
        g.fill(0, 0, this.width, this.height, 0x88000000);

        // Panel
        InventorySortUIUtils.drawBeveledPanel(g, modalX, modalY, modalW, modalH, false);

        g.drawString(this.font, "Inventory Search", modalX + PAD, modalY + 8, 0xFF1C1C1C, false);

        // Column headers
        int headerY = modalY + 48;
        g.drawString(this.font, "Item", listX + 0, headerY, 0xFF555555, false);
        g.drawString(this.font, "Count", countColX, headerY, 0xFF555555, false);

        // Search Box recessed area
        int boxX = modalX + PAD;
        int boxW = (scrollColX - 6) - boxX;
        InventorySortUIUtils.drawRecessedPanel(g, boxX, modalY + 22, boxW, 18);

        // List area: clip to the list ✅
        int clipLeft = listX - 2;
        int clipTop = listTopY;
        int clipRight = rowRightX; // your list content width boundary
        int clipBottom = listBottomY;

        g.enableScissor(clipLeft, clipTop, clipRight, clipBottom);

        if (results.isEmpty()) {
            String msg = (lastQuery == null || lastQuery.trim().isEmpty())
                    ? "No recent items yet."
                    : "No matches found.";
            g.drawString(this.font, msg, listX, listTopY + 6, 0xFF555555, false);
        } else {
            int y = listTopY - scrollOffsetPixels;

            for (int i = 0; i < results.size(); i++) {
                ResultRow row = results.get(i);
                boolean isOpen = expanded.contains(row.id);

                int rowHeight = ROW_H + (isOpen ? DETAILS_H : 0);

                if (y + rowHeight + 4 <= listTopY) {
                    y += rowHeight + 4;
                    continue;
                }
                if (y >= listBottomY) {
                    break;
                }

                // Row background (subtle light grey)
                int bg = (i % 2 == 0) ? 0xFFBDBDBD : 0xFFB5B5B5;
                g.fill(listX - 2, y, rowRightX, y + ROW_H, bg);

                // Icon
                g.renderItem(row.icon, listX, y + 2);
                g.renderItemDecorations(this.font, row.icon, listX, y + 2);

                // Name + count
                int nameX = listX + 16 + 8;
                // Stop the name before the count column so long names never overlap it.
                int nameMaxW = Math.max(40, countColX - nameX - 6);
                String name = this.font.plainSubstrByWidth(row.name, nameMaxW);
                g.drawString(this.font, name, nameX, y + 6, 0xFF000000, false);

                // Count display
                String countStr;
                int countColor;
                if (row.seen) {
                    // Item in current inventory - show count in black
                    countStr = "x" + row.count;
                    countColor = 0xFF000000;
                } else {
                    // Item tracked but not in inventory - show total known tracked count in gray.
                    // trackedCount() lazily queries the tracker only for the rows actually drawn.
                    int tc = row.trackedCount();
                    if (tc > 0) {
                        countStr = "x" + tc;
                        countColor = 0xFF555555; // Gray
                    } else {
                        countStr = "—";
                        countColor = 0xFF777777; // Darker gray
                    }
                }
                g.drawString(this.font, countStr, countColX, y + 6, countColor, false);

                // Expanded details - tracked locations are formatted lazily here, only for the
                // handful of rows that are actually open and on screen.
                if (isOpen) {
                    int dy = y + ROW_H + 4;
                    List<String> tracked = row.trackedLocations();

                    if (!tracked.isEmpty()) {
                        g.drawString(this.font, "Tracked locations:", nameX, dy, 0xFF333333, false);
                        dy += 10;
                        for (int j = 0; j < Math.min(3, tracked.size()); j++) {
                            String loc = tracked.get(j);
                            if (this.font.width(loc) > listContentW - 24) {
                                loc = this.font.plainSubstrByWidth(loc, listContentW - 34) + "...";
                            }
                            g.drawString(this.font, "• " + loc, nameX, dy, 0xFF555555, false);
                            dy += 10;
                        }

                        // Show "+" indicator if there are more locations
                        if (tracked.size() > 3) {
                            int remaining = tracked.size() - 3;
                            g.drawString(this.font, "  +" + remaining + " more", nameX, dy, 0xFF777777, false);
                        }
                    } else {
                        g.drawString(this.font, "Never seen this item yet. No history available.",
                                nameX, dy, 0xFF555555, false);
                    }
                }

                y += rowHeight + 4;
            }
        }

        g.disableScissor();

        // ✅ NOW render widgets so they appear on top (search box, close, scroll, expand buttons)
        super.render(g, mouseX, mouseY, partialTick);

        // Footer hint
        g.drawString(this.font, "▶ expands details. ▲▼ scrolls (mouse wheel works too).",
                modalX + PAD, modalY + modalH - 14, 0xFF555555, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOverList(mouseX, mouseY)) {
            scrollBy((int) Math.signum(-verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateLayout() {
        int totalHeight = 0;
        for (ResultRow row : results) {
            totalHeight += ROW_H + (expanded.contains(row.id) ? DETAILS_H : 0) + 4;
        }
        int maxScroll = Math.max(0, totalHeight - (listBottomY - listTopY));
        scrollOffsetPixels = clamp(scrollOffsetPixels, 0, maxScroll);
        
        scrollUpBtn.active = scrollOffsetPixels > 0;
        scrollDownBtn.active = scrollOffsetPixels < maxScroll;

        for (Button b : expandButtons) {
            b.visible = false;
            b.active = false;
        }

        int btnIdx = 0;
        int y = listTopY - scrollOffsetPixels;
        for (int i = 0; i < results.size(); i++) {
            ResultRow row = results.get(i);
            boolean isOpen = expanded.contains(row.id);
            int rowHeight = ROW_H + (isOpen ? DETAILS_H : 0);

            if (y + rowHeight + 4 > listTopY && y < listBottomY) {
                boolean withinClip = (y + 1) >= listTopY && (y + 1 + 16) <= listBottomY;
                if (withinClip && btnIdx < expandButtons.size()) {
                    InventorySortModalIconButton b = (InventorySortModalIconButton) expandButtons.get(btnIdx);
                    b.setX(expandColX);
                    b.setY(y + 1);
                    b.visible = true;
                    b.active = true;
                    b.setIcon(isOpen ? InventorySortModalIconButton.COLLAPSE : InventorySortModalIconButton.EXPAND);
                    b.setTargetId(row.id);
                    btnIdx++;
                }
            }
            y += rowHeight + 4;
        }
    }

    private void updateResults(String queryRaw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        String q = (queryRaw == null ? "" : queryRaw.trim().toLowerCase(Locale.ROOT));
        results.clear();

        if (q.isEmpty()) {
            int added = 0;
            for (String id : RECENT_IDS) {
                RegistryEntry entry = REGISTRY_BY_ID.get(id);
                if (entry == null) continue;

                results.add(buildRowForEntry(entry));
                added++;
                if (added >= 10) break;
            }
        } else {
            for (RegistryEntry e : REGISTRY_CACHE) {
                if (e.searchName.contains(q) || e.searchId.contains(q)) {
                    results.add(buildRowForEntry(e));
                    if (results.size() >= 400) break;
                }
            }

            results.sort((a, b) -> {
                int sa = relevanceScore(q, a.searchName, a.searchId);
                int sb = relevanceScore(q, b.searchName, b.searchId);
                if (sa != sb) return Integer.compare(sb, sa);

                int la = a.name.length();
                int lb = b.name.length();
                if (la != lb) return Integer.compare(la, lb);

                return a.name.compareToIgnoreCase(b.name);
            });
        }

        expanded.retainAll(idsOf(results));
    }

    private static int relevanceScore(String q, String name, String id) {
        int score = 0;

        if (name.equals(q) || id.equals(q)) score += 50_000;

        if (name.startsWith(q) || id.startsWith(q)) score += 20_000;
        else if (name.contains(q) || id.contains(q)) score += 8_000;

        String[] tokens = name.split("[ _\\-]+");
        for (String t : tokens) {
            if (t.equals(q)) score += 6_000;
            else if (t.startsWith(q)) score += 2_000;
        }

        if (name.startsWith(q + " ")) score += 3_000;

        score -= Math.min(300, name.length());

        return score;
    }

    private ResultRow buildRowForEntry(RegistryEntry entry) {
        // Cheap per-result work only: inventory snapshot lookup. Tracker queries and location
        // string formatting are deferred to the row's lazy accessors, so a 400-result query no
        // longer touches the tracker or builds strings for rows that are never drawn/expanded.
        InvSnapshot snap = invSnapshot.get(entry.id);
        boolean seen = snap != null && snap.count > 0;
        String currentInvLine = seen ? formatCurrentInventoryLocation(snap) : null;

        return new ResultRow(
                entry.id,
                entry.displayName,
                entry.icon,
                entry.searchName,
                entry.searchId,
                seen,
                seen ? snap.count : 0,
                currentInvLine,
                entry.item
        );
    }

    private static String formatCurrentInventoryLocation(InvSnapshot snap) {
        return String.format("%s - x%d now", snap.locationLabel, snap.count);
    }

    private static String formatLocation(LocationEntry loc) {
        switch (loc.getType()) {
            case CONTAINER:
                String rawDim = loc.getDimensionKey();
                String dim = rawDim == null ? "" : rawDim.replace("minecraft:", "").replace("the_", "");
                String timeAgo = formatTimeAgo(loc.getLastSeen());
                String location = loc.getPositionLabel() != null
                        ? loc.getPositionLabel()
                        : String.format("%d, %d, %d", loc.getPos().getX(), loc.getPos().getY(), loc.getPos().getZ());
                return String.format("%s @ %s (%s) - x%d - %s",
                        loc.getContainerType(),
                        location,
                        dim, loc.getCount(), timeAgo);
            case INVENTORY:
                if (loc.getPos() != null && loc.getDimensionKey() != null) {
                    String invDim = loc.getDimensionKey().replace("minecraft:", "").replace("the_", "");
                    String invTimeAgo = formatTimeAgo(loc.getLastSeen());
                    return String.format("Player Inventory @ %d, %d, %d (%s) - x%d - %s",
                            loc.getPos().getX(), loc.getPos().getY(), loc.getPos().getZ(),
                            invDim, loc.getCount(), invTimeAgo);
                } else {
                    return String.format("Player Inventory - x%d - %s", loc.getCount(), formatTimeAgo(loc.getLastSeen()));
                }
            case SHULKER_BOX:
                return String.format("Shulker Box - x%d - %s", loc.getCount(), formatTimeAgo(loc.getLastSeen()));
            default:
                return "Unknown";
        }
    }

    private static String formatTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
            return dateTime.format(TS_FMT).toLowerCase(Locale.ROOT);
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "just now";
        }
    }

    private static Set<String> idsOf(List<ResultRow> rows) {
        Set<String> s = new HashSet<>();
        for (ResultRow r : rows) s.add(r.id);
        return s;
    }

    private void ensureRegistryCache() {
        if (REGISTRY_CACHE != null) return;

        List<RegistryEntry> list = new ArrayList<>();
        REGISTRY_BY_ID.clear();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null) continue;

            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            ItemStack icon = new ItemStack(item);
            String display = icon.getHoverName().getString();

            RegistryEntry entry = new RegistryEntry(
                    id,
                    display,
                    icon,
                    searchTextFor(id, display),
                    id.toLowerCase(Locale.ROOT),
                    item
            );

            list.add(entry);
            REGISTRY_BY_ID.put(id, entry);
        }

        REGISTRY_CACHE = list;
    }

    private String searchTextFor(String id, String displayName) {
        String searchText = displayName.toLowerCase(Locale.ROOT);
        if (id.equals("minecraft:potion")) {
            searchText += " water bottle awkward potion mundane thick";
        } else if (id.equals("minecraft:splash_potion")) {
            searchText += " splash water bottle awkward potion mundane thick";
        } else if (id.equals("minecraft:lingering_potion")) {
            searchText += " lingering water bottle awkward potion mundane thick";
        }
        return searchText;
    }

    private void buildInventorySnapshot(Inventory inv) {
        invSnapshot.clear();

        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.isEmpty()) continue;

            Item item = stack.getItem();
            String id = BuiltInRegistries.ITEM.getKey(item).toString();

            InvSnapshot snap = invSnapshot.get(id);
            if (snap == null) {
                snap = new InvSnapshot();
                invSnapshot.put(id, snap);
            }

            snap.count += stack.getCount();
            snap.hasHotbar |= slot < 9;
            snap.hasInventory |= slot >= 9;
        }

        for (Map.Entry<String, InvSnapshot> e : invSnapshot.entrySet()) {
            InvSnapshot snap = e.getValue();
            if (snap.count <= 0) continue;

            if (snap.hasInventory && snap.hasHotbar) snap.locationLabel = "Inventory + Hotbar";
            else if (snap.hasInventory) snap.locationLabel = "Inventory";
            else snap.locationLabel = "Hotbar";

            markRecent(e.getKey());
        }
    }

    private static void markRecent(String id) {
        RECENT_IDS.remove(id);
        RECENT_IDS.addFirst(id);
        while (RECENT_IDS.size() > RECENT_LIMIT) RECENT_IDS.removeLast();
    }

    private static final class InvSnapshot {
        int count = 0;
        boolean hasHotbar = false;
        boolean hasInventory = false;
        String locationLabel = "Inventory";
    }

    private static final class RegistryEntry {
        final String id;
        final String displayName;
        final ItemStack icon;
        final String searchName;
        final String searchId;
        final Item item;

        RegistryEntry(String id, String displayName, ItemStack icon, String searchName, String searchId, Item item) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.searchName = searchName;
            this.searchId = searchId;
            this.item = item;
        }
    }

    private static final class ResultRow {
        final String id;
        final String name;
        final ItemStack icon;

        final String searchName;
        final String searchId;

        final boolean seen;
        final int count;                 // current inventory count (when seen)
        final String currentInvLine;     // formatted "in inventory" line, or null
        final Item item;                 // for lazy tracker lookup

        // Tracker-derived data, computed on first access and cached so off-screen and collapsed
        // rows never pay for it, and visible rows pay only once.
        private boolean trackedLoaded = false;
        private List<LocationEntry> trackedEntries = Collections.emptyList();
        private int trackedCount = 0;
        private List<String> trackedLocationsCache = null;

        ResultRow(String id, String name, ItemStack icon,
                  String searchName, String searchId,
                  boolean seen, int count,
                  String currentInvLine, Item item) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.searchName = searchName;
            this.searchId = searchId;
            this.seen = seen;
            this.count = count;
            this.currentInvLine = currentInvLine;
            this.item = item;
        }

        int trackedCount() {
            ensureTracked();
            return trackedCount;
        }

        List<String> trackedLocations() {
            ensureTracked();
            if (trackedLocationsCache == null) {
                List<String> out = new ArrayList<>();
                if (currentInvLine != null) {
                    out.add(currentInvLine);
                }
                for (LocationEntry loc : trackedEntries) {
                    out.add(formatLocation(loc));
                }
                trackedLocationsCache = out;
            }
            return trackedLocationsCache;
        }

        private void ensureTracked() {
            if (trackedLoaded) return;
            trackedLoaded = true;
            try {
                List<LocationEntry> locations = ItemLocationTracker.getInstance().getLocations(item);
                locations.removeIf(loc -> loc.getType() == LocationEntry.LocationType.INVENTORY);
                trackedEntries = locations;
                int sum = 0;
                for (LocationEntry loc : locations) {
                    sum += loc.getCount();
                }
                trackedCount = sum;
            } catch (Exception e) {
                InventorySortClient.LOGGER.error("Failed to query tracking for " + id, e);
            }
        }
    }
}
