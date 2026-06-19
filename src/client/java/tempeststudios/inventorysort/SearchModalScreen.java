package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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

    // Most-recently-seen item keys (persist across opens)
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

    // Layout
    private int modalW;
    private int modalH;
    private int modalX;
    private int modalY;

    private int listX;
    private int listTopY;
    private int listBottomY;

    private int railX;       // right rail for scroll arrows + scrollbar
    private int rowRightX;   // right edge of a row card
    private int chevronX;    // expand control x
    private int countRightX; // right edge of the count text
    private int nameX;       // start of the item name
    private int searchY;

    private static final int ACCENT = InvUi.ACCENT_SEARCH;
    private static final int COUNT_HELD = 0xFF8CC9FF;
    private static final int PAD = 12;
    private static final int ROW_H = 20;
    private static final int DETAILS_H = 58;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mma");

    public SearchModalScreen(Screen parent) {
        super(Component.literal("Search"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        this.modalW = Math.min(InvUi.STANDARD_MODAL_W, this.width - 24);
        this.modalH = Math.min(InvUi.STANDARD_MODAL_H, this.height - 24);
        this.modalX = (this.width - modalW) / 2;
        this.modalY = (this.height - modalH) / 2;

        ensureRegistryCache();
        buildInventorySnapshot(mc.player.getInventory());

        this.listX = modalX + PAD;
        this.searchY = modalY + 26;
        this.listTopY = modalY + 62;
        this.listBottomY = modalY + modalH - PAD - 12;

        this.railX = modalX + modalW - PAD - 14;
        this.rowRightX = railX - 6;
        this.chevronX = rowRightX - 16;
        this.countRightX = chevronX - 6;
        this.nameX = listX + 22;

        // Search field (recessed background drawn in render).
        int boxX = modalX + PAD;
        int boxW = (modalX + modalW - PAD) - boxX;
        this.searchBox = new EditBox(this.font, boxX + 6, searchY + 5, boxW - 12, 14, Component.literal("Search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue("");
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(InvUi.TEXT);
        this.addRenderableWidget(this.searchBox);

        // Close button (top-right).
        int closeX = modalX + modalW - PAD - 16;
        int closeY = modalY + 7;
        this.addRenderableWidget(new InventorySortModalIconButton(closeX, closeY, 16, InventorySortModalIconButton.CLOSE,
                Component.literal("Close"), btn -> closeToParent()));

        if (TrackingNamespace.isMultiplayerServer(mc)) {
            InventorySortTextButton world = new InventorySortTextButton(closeX - 56, closeY, 52, 16,
                    Component.literal("World"), btn -> mc.setScreen(new ServerWorldProfileScreen(this)));
            world.setTooltip(Tooltip.create(Component.literal("Choose which tracked world these results come from.")));
            this.addRenderableWidget(world);
        }

        // Scroll arrows on the right rail.
        scrollUpBtn = new InventorySortModalIconButton(railX, listTopY, 14, InventorySortModalIconButton.UP,
                Component.literal("Scroll up"), btn -> scrollBy(-1));
        scrollDownBtn = new InventorySortModalIconButton(railX, listBottomY - 14, 14, InventorySortModalIconButton.DOWN,
                Component.literal("Scroll down"), btn -> scrollBy(1));
        this.addRenderableWidget(scrollUpBtn);
        this.addRenderableWidget(scrollDownBtn);

        // Expand controls pool (one interactive element per visible row).
        expandButtons.clear();
        for (int i = 0; i < 50; i++) {
            InventorySortModalIconButton b = new InventorySortModalIconButton(0, 0, 14, InventorySortModalIconButton.EXPAND,
                    Component.literal("Show locations"), btn -> {
                String target = ((InventorySortModalIconButton) btn).getTargetId();
                if (target != null) {
                    toggleExpanded(target);
                }
            });
            b.visible = false;
            b.active = false;
            this.addRenderableWidget(b);
            expandButtons.add(b);
        }

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
        closeToParent();
    }

    private void closeToParent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.setScreen(parent);
    }

    private void toggleExpanded(String id) {
        if (!expanded.add(id)) {
            expanded.remove(id);
        }
        updateLayout();
    }

    private void scrollBy(int delta) {
        if (delta == 0 || results.isEmpty()) return;

        int current = scrollOffsetPixels;
        int target;
        if (delta > 0) {
            int top = 0;
            target = current;
            for (ResultRow row : results) {
                if (top > current + 1) { target = top; break; }
                top += rowSpan(row);
            }
            if (target == current) target = top;
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
        return mouseX >= listX - 2 && mouseX <= railX + 14
                && mouseY >= listTopY && mouseY <= listBottomY;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        InventorySortDrawContext ui = InventorySortDrawContexts.wrap(g);
        InvUi.scrim(ui, this.width, this.height);
        InvUi.window(ui, modalX, modalY, modalW, modalH, ACCENT);

        g.drawString(this.font, "Inventory Search", modalX + PAD, modalY + 9, InvUi.TEXT, false);

        // Search field.
        int boxX = modalX + PAD;
        int boxW = (modalX + modalW - PAD) - boxX;
        boolean focused = searchBox != null && searchBox.isFocused();
        InvUi.field(ui, boxX, searchY, boxW, 18, focused, ACCENT);
        if (searchBox != null && searchBox.getValue().isEmpty()) {
            g.drawString(this.font, "Search items, ids, :category, or <enchant/potion...", boxX + 6, searchY + 5, InvUi.TEXT_DIM, false);
        }

        // Context label for the list.
        boolean searching = lastQuery != null && !lastQuery.trim().isEmpty();
        InventorySortCategories.CategoryQuery categoryQuery = InventorySortCategories.categoryQuery(lastQuery);
        String context = categoryQuery != null ? "Category Results" : (searching ? "Results" : "Recently seen");
        g.drawString(this.font, context, listX, listTopY - 11, InvUi.TEXT_DIM, false);
        g.drawString(this.font, "count", countRightX - this.font.width("count"), listTopY - 11, InvUi.TEXT_DIM, false);

        // List well.
        InvUi.inset(ui, listX - 4, listTopY - 2, (railX + 14) - (listX - 4), listBottomY - (listTopY - 2));

        int clipLeft = listX - 3;
        int clipTop = listTopY;
        int clipRight = railX - 2;
        int clipBottom = listBottomY - 1;
        g.enableScissor(clipLeft, clipTop, clipRight, clipBottom);

        if (results.isEmpty()) {
            String msg = categoryQuery != null ? "No items match that category."
                    : (searching ? "No items match that search." : "No recent items yet.");
            g.drawString(this.font, msg, listX + 4, listTopY + 6, InvUi.TEXT_MUTED, false);
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

                boolean hovered = mouseX >= listX && mouseX <= rowRightX && mouseY >= y && mouseY <= y + ROW_H;
                InvUi.row(ui, listX, y, rowRightX - listX, ROW_H, hovered, isOpen, ACCENT);

                // Item icons are flushed outside the scissor on some versions, so only
                // draw them when the whole icon fits inside the list, never spilling out.
                if (y + 2 >= listTopY && y + 18 <= clipBottom) {
                    g.renderItem(row.icon, listX + 3, y + 2);
                    g.renderItemDecorations(this.font, row.icon, listX + 3, y + 2);
                }

                int nameMaxW = Math.max(30, countRightX - 24 - nameX);
                String name = this.font.plainSubstrByWidth(row.name, nameMaxW);
                g.drawString(this.font, name, nameX, y + 6, isOpen ? InvUi.TEXT : InvUi.TEXT_MUTED, false);

                String countStr;
                int countColor;
                if (row.seen) {
                    countStr = "x" + row.count;
                    countColor = COUNT_HELD;
                } else {
                    int tc = row.trackedCount();
                    if (tc > 0) {
                        countStr = "x" + tc;
                        countColor = InvUi.TEXT_MUTED;
                    } else {
                        countStr = "-";
                        countColor = InvUi.TEXT_DIM;
                    }
                }
                g.drawString(this.font, countStr, countRightX - this.font.width(countStr), y + 6, countColor, false);

                if (isOpen) {
                    renderDetails(g, ui, row, y + ROW_H + 2);
                }

                y += rowHeight + 4;
            }
        }
        g.disableScissor();

        // Redraw the well frame on top so scrolled rows never paint over its edges.
        InvUi.insetBorder(ui, listX - 4, listTopY - 2, (railX + 14) - (listX - 4), listBottomY - (listTopY - 2));

        // Scrollbar between the rail arrows.
        int trackTop = listTopY + 16;
        int trackH = (listBottomY - 16) - trackTop;
        InvUi.scrollbar(ui, railX + 5, trackTop, trackH, totalContentHeight(), listBottomY - listTopY,
                scrollOffsetPixels, ACCENT);

        super.render(g, mouseX, mouseY, partialTick);

        g.drawString(this.font, "Click the arrow to view tracked locations. Scroll wheel works too.",
                modalX + PAD, modalY + modalH - 12, InvUi.TEXT_DIM, false);
    }

    private void renderDetails(GuiGraphics g, InventorySortDrawContext ui, ResultRow row, int dy) {
        int dx = nameX - 4;
        int dw = rowRightX - dx;
        ui.fill(dx, dy, dx + dw, dy + DETAILS_H - 4, InvUi.WELL);
        ui.fill(dx, dy, dx + 2, dy + DETAILS_H - 4, ACCENT);

        int tx = dx + 8;
        int ty = dy + 5;
        List<String> tracked = row.trackedLocations();
        if (tracked.isEmpty()) {
            g.drawString(this.font, "No tracked history for this item yet.", tx, ty, InvUi.TEXT_MUTED, false);
            return;
        }
        g.drawString(this.font, "Tracked locations", tx, ty, ACCENT, false);
        ty += 11;
        int shown = Math.min(3, tracked.size());
        for (int j = 0; j < shown; j++) {
            String loc = tracked.get(j);
            int maxW = dw - 18;
            if (this.font.width(loc) > maxW) {
                loc = this.font.plainSubstrByWidth(loc, maxW - this.font.width("...")) + "...";
            }
            ui.fill(tx, ty + 3, tx + 3, ty + 6, InvUi.TEXT_DIM);
            g.drawString(this.font, loc, tx + 8, ty, InvUi.TEXT_MUTED, false);
            ty += 11;
        }
        if (tracked.size() > shown) {
            g.drawString(this.font, "+" + (tracked.size() - shown) + " more location"
                    + (tracked.size() - shown == 1 ? "" : "s"), tx + 8, ty, InvUi.TEXT_DIM, false);
        }
    }

    private int totalContentHeight() {
        int total = 0;
        for (ResultRow row : results) {
            total += rowSpan(row);
        }
        return total;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private boolean handleMouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (isMouseOverList(mouseX, mouseY)) {
            scrollBy((int) Math.signum(-verticalAmount));
            return true;
        }
        return false;
    }

    private void updateLayout() {
        int totalHeight = totalContentHeight();
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
                boolean withinClip = (y + 2) >= listTopY && (y + 2 + 14) <= listBottomY;
                if (withinClip && btnIdx < expandButtons.size()) {
                    InventorySortModalIconButton b = (InventorySortModalIconButton) expandButtons.get(btnIdx);
                    b.setX(chevronX);
                    b.setY(y + 3);
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
        String normalizedQ = ItemStackIdentity.normalizeSearchText(q);
        InventorySortCategories.CategoryQuery categoryQuery = InventorySortCategories.categoryQuery(queryRaw);
        boolean variantSearch = q.startsWith("<");
        String variantQ = variantSearch ? ItemStackIdentity.normalizeSearchText(q.substring(1)) : normalizedQ;
        results.clear();
        LinkedHashMap<String, ResultRow> rows = new LinkedHashMap<>();

        if (q.isEmpty()) {
            int added = 0;
            for (String id : RECENT_IDS) {
                if (addRow(rows, buildRowForKey(id))) {
                    added++;
                    if (added >= 10) break;
                }
            }
        } else if (variantSearch) {
            for (String itemKey : knownItemKeys(true)) {
                ItemStackIdentity.Info info = infoForKey(itemKey);
                if (ItemStackIdentity.matchesVariantQuery(itemKey, info, variantQ)) {
                    addRow(rows, buildRowForKey(itemKey));
                    if (rows.size() >= 400) break;
                }
            }
        } else if (categoryQuery != null) {
            for (RegistryEntry e : REGISTRY_CACHE) {
                if (categoryQuery.matches(e.item)) {
                    addRow(rows, buildRowForEntry(e));
                    if (rows.size() >= 400) break;
                }
            }

            results.addAll(rows.values());
            results.sort((a, b) -> compareCategoryResults(categoryQuery, a, b));
            expanded.retainAll(idsOf(results));
            return;
        } else {
            for (RegistryEntry e : REGISTRY_CACHE) {
                if (e.searchName.contains(q) || e.searchId.contains(q)) {
                    addRow(rows, buildRowForEntry(e));
                    if (rows.size() >= 400) break;
                }
            }
            if (rows.size() < 400) {
                for (String itemKey : knownItemKeys(true)) {
                    ItemStackIdentity.Info info = infoForKey(itemKey);
                    if (ItemStackIdentity.matchesSearch(itemKey, info, normalizedQ)) {
                        addRow(rows, buildRowForKey(itemKey));
                        if (rows.size() >= 400) break;
                    }
                }
            }
        }

        results.addAll(rows.values());
        if (!q.isEmpty() && categoryQuery == null) {
            String scoreQ = variantSearch ? variantQ : q;
            results.sort((a, b) -> {
                int sa = relevanceScore(scoreQ, a.searchName, a.searchId);
                int sb = relevanceScore(scoreQ, b.searchName, b.searchId);
                if (sa != sb) return Integer.compare(sb, sa);

                int la = a.name.length();
                int lb = b.name.length();
                if (la != lb) return Integer.compare(la, lb);

                return a.name.compareToIgnoreCase(b.name);
            });
        }

        expanded.retainAll(idsOf(results));
    }

    private boolean addRow(LinkedHashMap<String, ResultRow> rows, ResultRow row) {
        if (row == null || rows.containsKey(row.id)) {
            return false;
        }
        rows.put(row.id, row);
        return true;
    }

    private static int compareCategoryResults(InventorySortCategories.CategoryQuery query, ResultRow a, ResultRow b) {
        int categoryCompare = Integer.compare(
                query.rank(InventorySortCategories.categoryKey(a.item)),
                query.rank(InventorySortCategories.categoryKey(b.item))
        );
        if (categoryCompare != 0) {
            return categoryCompare;
        }

        int nameCompare = a.name.compareToIgnoreCase(b.name);
        if (nameCompare != 0) {
            return nameCompare;
        }
        return a.id.compareToIgnoreCase(b.id);
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

    private ResultRow buildRowForKey(String itemKey) {
        if (!ItemStackIdentity.isVariantKey(itemKey)) {
            RegistryEntry entry = REGISTRY_BY_ID.get(itemKey);
            if (entry != null) {
                return buildRowForEntry(entry);
            }
        }

        InvSnapshot snap = invSnapshot.get(itemKey);
        boolean seen = snap != null && snap.count > 0;
        String currentInvLine = seen ? formatCurrentInventoryLocation(snap) : null;
        ItemStackIdentity.Info info = snap != null ? snap.info : ItemStackIdentity.legacyInfo(itemKey);
        RegistryEntry baseEntry = REGISTRY_BY_ID.get(info.baseItemId());
        ItemStack icon = snap != null && snap.icon != null && !snap.icon.isEmpty()
                ? snap.icon
                : (baseEntry != null ? baseEntry.icon : ItemStack.EMPTY);
        Item item = baseEntry != null ? baseEntry.item : null;

        return new ResultRow(
                itemKey,
                info.displayName(),
                icon,
                info.searchText(),
                ItemStackIdentity.normalizeSearchText(itemKey),
                seen,
                seen ? snap.count : 0,
                currentInvLine,
                item
        );
    }

    private Set<String> knownItemKeys(boolean variantsOnly) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.addAll(invSnapshot.keySet());
        try {
            keys.addAll(ItemLocationTracker.getInstance().getTrackedItemKeys());
        } catch (Exception e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to query tracked item keys", e);
        }
        if (variantsOnly) {
            keys.removeIf(key -> !ItemStackIdentity.isVariantKey(key));
        }
        return keys;
    }

    private ItemStackIdentity.Info infoForKey(String itemKey) {
        InvSnapshot snap = invSnapshot.get(itemKey);
        return snap != null ? snap.info : ItemStackIdentity.legacyInfo(itemKey);
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
        String searchText = ItemStackIdentity.normalizeSearchText(displayName + " " + id);
        if (id.equals("minecraft:potion")) {
            searchText += " " + ItemStackIdentity.normalizeSearchText("water bottle awkward potion mundane thick");
        } else if (id.equals("minecraft:splash_potion")) {
            searchText += " " + ItemStackIdentity.normalizeSearchText("splash water bottle awkward potion mundane thick");
        } else if (id.equals("minecraft:lingering_potion")) {
            searchText += " " + ItemStackIdentity.normalizeSearchText("lingering water bottle awkward potion mundane thick");
        }
        return searchText;
    }

    private void buildInventorySnapshot(Inventory inv) {
        invSnapshot.clear();

        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.isEmpty()) continue;

            ItemStackIdentity.Info info = ItemStackIdentity.info(stack);
            String id = info.key();

            InvSnapshot snap = invSnapshot.get(id);
            if (snap == null) {
                snap = new InvSnapshot();
                invSnapshot.put(id, snap);
            }

            snap.info = info;
            if (snap.icon == null || snap.icon.isEmpty()) {
                snap.icon = stack.copy();
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
        ItemStackIdentity.Info info;
        ItemStack icon = ItemStack.EMPTY;
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
        final String currentInvLine;
        final Item item;

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
                List<LocationEntry> locations = ItemLocationTracker.getInstance().getLocationsByKey(id);
                locations.removeIf(loc -> loc.getType() == LocationEntry.LocationType.INVENTORY);
                trackedEntries = locations;
                int sum = 0;
                for (LocationEntry loc : locations) {
                    sum += loc.getCount();
                }
                trackedCount = sum;
            } catch (Exception e) {
                tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to query tracking for " + id, e);
            }
        }
    }
}
