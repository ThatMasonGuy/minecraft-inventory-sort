package tempeststudios.inventorysort;

import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
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
    private int scrollOffset = 0;

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

    private static final int PAD = 14;
    private static final int ROW_H = 20;
    private static final int DETAILS_H = 60; // Height for expanded details (header + 3 locations + "+X more")

    private static final int MAX_VISIBLE = 10;

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

        // Search box
        int boxX = modalX + PAD;
        int boxW = (scrollColX - 6) - boxX; // stop before right-side columns
        this.searchBox = new EditBox(this.font, boxX, searchBoxY, boxW, 18, Component.literal("Search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue("");
        this.addRenderableWidget(this.searchBox);

        // Close button — keep it inside panel ✅
        int closeX = modalX + modalW - 22;
        int closeY = modalY + 6;
        this.addRenderableWidget(Button.builder(Component.literal("✕"), btn -> closeToParent())
                .bounds(closeX, closeY, 18, 18)
                .build());

        // Scroll buttons in their own column (never overlap rows now) ✅
        scrollUpBtn = Button.builder(Component.literal("▲"), btn -> scrollBy(-1))
                .bounds(scrollColX, listTopY, 18, 18)
                .build();

        scrollDownBtn = Button.builder(Component.literal("▼"), btn -> scrollBy(1))
                .bounds(scrollColX, listTopY + 20, 18, 18)
                .build();

        this.addRenderableWidget(scrollUpBtn);
        this.addRenderableWidget(scrollDownBtn);

        // Expand buttons pool (only interactive element per row)
        expandButtons.clear();
        for (int i = 0; i < MAX_VISIBLE; i++) {
            int idx = i;
            Button b = Button.builder(Component.literal("▶"), btn -> {
                        int realIndex = scrollOffset + idx;
                        if (realIndex >= 0 && realIndex < results.size()) {
                            String id = results.get(realIndex).id;
                            toggleExpanded(id);
                        }
                    })
                    .bounds(0, 0, 18, 18)
                    .build();

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
                this.scrollOffset = 0;
                updateResults(q);
                updateLayout();
            }
        }
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
        if (delta == 0) return;
        int max = Math.max(0, results.size() - MAX_VISIBLE);
        scrollOffset = clamp(scrollOffset + delta, 0, max);
        updateLayout();
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
        g.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF1E1E1E);
        g.fill(modalX, modalY, modalX + modalW, modalY + 1, 0xFF5A5A5A);
        g.fill(modalX, modalY, modalX + 1, modalY + modalH, 0xFF5A5A5A);
        g.fill(modalX, modalY + modalH - 1, modalX + modalW, modalY + modalH, 0xFF0F0F0F);
        g.fill(modalX + modalW - 1, modalY, modalX + modalW, modalY + modalH, 0xFF0F0F0F);

        g.drawString(this.font, "Inventory Search", modalX + PAD, modalY + 8, 0xFFFFFFFF, false);

        // Column headers
        int headerY = modalY + 48;
        g.drawString(this.font, "Item", listX + 0, headerY, 0xFFAAAAAA, false);
        g.drawString(this.font, "Count", listX + 230, headerY, 0xFFAAAAAA, false);

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
            g.drawString(this.font, msg, listX, listTopY + 6, 0xFF777777, false);
        } else {
            int y = listTopY;

            for (int i = 0; i < MAX_VISIBLE; i++) {
                int realIndex = scrollOffset + i;
                if (realIndex >= results.size()) break;

                ResultRow row = results.get(realIndex);
                boolean isOpen = expanded.contains(row.id);

                int rowHeight = ROW_H + (isOpen ? DETAILS_H : 0);

                // Row background (this MUST be before widgets, otherwise it covers them)
                int bg = (i % 2 == 0) ? 0xFF2A2A2A : 0xFF242424;
                g.fill(listX - 2, y, rowRightX, y + ROW_H, bg);

                // Icon
                g.renderItem(row.icon, listX, y + 2);
                g.renderItemDecorations(this.font, row.icon, listX, y + 2);

                // Name + count
                int nameX = listX + 16 + 8;
                int nameMaxW = Math.max(80, listContentW - 16 - 8 - 70);
                String name = this.font.plainSubstrByWidth(row.name, nameMaxW);
                g.drawString(this.font, name, nameX, y + 6, 0xFFFFFFFF, false);

                // Count display
                String countStr;
                int countColor;
                if (row.seen) {
                    // Item in current inventory - show count in white
                    countStr = "x" + row.count;
                    countColor = 0xFFFFFFFF;
                } else if (row.trackedCount > 0) {
                    // Item tracked but not in inventory - show tracked count in gray
                    countStr = "x" + row.trackedCount;
                    countColor = 0xFF888888; // Gray
                } else {
                    // Never seen
                    countStr = "—";
                    countColor = 0xFF777777; // Darker gray
                }
                g.drawString(this.font, countStr, listX + 230, y + 6, countColor, false);

                // Expanded details
                if (isOpen) {
                    int dy = y + ROW_H + 4;

                    // Show tracked locations if available
                    if (!row.trackedLocations.isEmpty()) {
                        g.drawString(this.font, "Tracked locations:", nameX, dy, 0xFFB0B0B0, false);
                        dy += 10;
                        for (int j = 0; j < Math.min(3, row.trackedLocations.size()); j++) {
                            String loc = row.trackedLocations.get(j);
                            if (this.font.width(loc) > listContentW - 24) {
                                loc = this.font.plainSubstrByWidth(loc, listContentW - 34) + "...";
                            }
                            g.drawString(this.font, "• " + loc, nameX, dy, 0xFF888888, false);
                            dy += 10;
                        }

                        // Show "+" indicator if there are more locations
                        if (row.trackedLocations.size() > 3) {
                            int remaining = row.trackedLocations.size() - 3;
                            g.drawString(this.font, "  +" + remaining + " more", nameX, dy, 0xFF666666, false);
                        }
                    } else if (!row.seen) {
                        g.drawString(this.font, "Never seen this item yet. No history available.",
                                nameX, dy, 0xFF777777, false);
                    } else {
                        // Fallback to original display if tracked but seen in snapshot
                        String line1 = "Last seen: " + row.timestamp;
                        String line2 = "Where: " + row.location + " @ " + row.coords;
                        g.drawString(this.font, line1, nameX, dy, 0xFFB0B0B0, false);
                        g.drawString(this.font, line2, nameX, dy + 10, 0xFFB0B0B0, false);
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
                modalX + PAD, modalY + modalH - 14, 0xFF777777, false);
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
        int max = Math.max(0, results.size() - MAX_VISIBLE);
        scrollUpBtn.active = scrollOffset > 0;
        scrollDownBtn.active = scrollOffset < max;

        int y = listTopY;

        for (int i = 0; i < expandButtons.size(); i++) {
            Button b = expandButtons.get(i);

            int realIndex = scrollOffset + i;
            if (realIndex >= 0 && realIndex < results.size()) {
                ResultRow row = results.get(realIndex);
                boolean isOpen = expanded.contains(row.id);

                b.setMessage(Component.literal(isOpen ? "▼" : "▶"));

                int bx = expandColX;
                int by = y + 1;

                boolean withinClip = (by + 18) <= listBottomY;

                b.setX(bx);
                b.setY(by);
                b.visible = withinClip;
                b.active = withinClip;

                int rowHeight = ROW_H + (isOpen ? DETAILS_H : 0);
                y += rowHeight + 4;
            } else {
                b.visible = false;
                b.active = false;
            }
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

                results.add(buildRowForEntry(entry, mc));
                added++;
                if (added >= 10) break;
            }
        } else {
            for (RegistryEntry e : REGISTRY_CACHE) {
                if (e.searchName.contains(q) || e.searchId.contains(q)) {
                    results.add(buildRowForEntry(e, mc));
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

    private ResultRow buildRowForEntry(RegistryEntry entry, Minecraft mc) {
        InvSnapshot snap = invSnapshot.get(entry.id);
        boolean seen = snap != null && snap.count > 0;

        String ts = LocalDateTime.now().format(TS_FMT).toLowerCase(Locale.ROOT);

        int px = mc.player.blockPosition().getX();
        int py = mc.player.blockPosition().getY();
        int pz = mc.player.blockPosition().getZ();
        String coords = px + " / " + py + " / " + pz;

        String location = seen ? snap.locationLabel : "—";

        // Query tracked locations
        List<String> trackedLocations = new ArrayList<>();
        if (seen) {
            trackedLocations.add(formatCurrentInventoryLocation(snap));
        }
        int trackedCount = 0;
        try {
            List<LocationEntry> locations = ItemLocationTracker.getInstance().getLocations(entry.item);
            locations.removeIf(loc -> loc.getType() == LocationEntry.LocationType.INVENTORY);

            // Only log if we actually found tracking data
            if (!locations.isEmpty()) {
                InventorySortClient.LOGGER.info("Querying tracking for {}: found {} locations", entry.id, locations.size());

                for (LocationEntry loc : locations) {
                    String formatted = formatLocation(loc);
                    trackedLocations.add(formatted);
                    InventorySortClient.LOGGER.info("  - {}", formatted);
                }
            }

            // Get count from most recent location (first in list)
            if (!locations.isEmpty()) {
                trackedCount = locations.get(0).getCount();
            }
        } catch (Exception e) {
            InventorySortClient.LOGGER.error("Failed to query tracking for " + entry.id, e);
        }

        return new ResultRow(
                entry.id,
                entry.displayName,
                entry.icon,
                entry.searchName,
                entry.searchId,
                seen,
                seen ? snap.count : 0,
                ts,
                location,
                coords,
                trackedLocations,
                trackedCount
        );
    }

    private String formatCurrentInventoryLocation(InvSnapshot snap) {
        return String.format("%s - x%d now", snap.locationLabel, snap.count);
    }

    private String formatLocation(LocationEntry loc) {
        switch (loc.getType()) {
            case CONTAINER:
                String dim = loc.getDimensionKey().replace("minecraft:", "").replace("the_", "");
                String timeAgo = formatTimeAgo(loc.getLastSeen());
                String location = loc.getPositionLabel() != null
                        ? loc.getPositionLabel()
                        : String.format("%d, %d, %d", loc.getPos().getX(), loc.getPos().getY(), loc.getPos().getZ());
                return String.format("%s @ %s (%s) - x%d - %s",
                        loc.getContainerType(),
                        location,
                        dim, loc.getCount(), timeAgo);
            case INVENTORY:
                // Legacy support: If position exists, show it; otherwise just show "Player Inventory"
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

    private String formatTimeAgo(long timestamp) {
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
                    display.toLowerCase(Locale.ROOT),
                    id.toLowerCase(Locale.ROOT),
                    item
            );

            list.add(entry);
            REGISTRY_BY_ID.put(id, entry);
        }

        REGISTRY_CACHE = list;
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
        final int count;

        final String timestamp;
        final String location;
        final String coords;
        final List<String> trackedLocations;
        final int trackedCount; // Count from most recent tracked location

        ResultRow(String id, String name, ItemStack icon,
                  String searchName, String searchId,
                  boolean seen, int count,
                  String timestamp, String location, String coords,
                  List<String> trackedLocations, int trackedCount) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.searchName = searchName;
            this.searchId = searchId;
            this.seen = seen;
            this.count = count;
            this.timestamp = timestamp;
            this.location = location;
            this.coords = coords;
            this.trackedLocations = trackedLocations;
            this.trackedCount = trackedCount;
        }
    }
}
