package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-game InvCatalogue report browser. The browser view lists saved snapshots
 * grouped by world; selecting one opens a detail view with a searchable item
 * grid and a selected-item sidebar. Both views share the suite's InvUi theme
 * with a green Catalogue accent.
 */
public class CatalogReportBrowserScreen extends Screen {
    private static final int PAD = 14;
    private static final int HEADER_H = 22;
    private static final int REPORT_ROW_H = 38;
    private static final int CELL = 28;
    private static final int TILE = 24;
    private static final int GRID_INSET = 8;
    private static final int DETAIL_INSET = 10;
    private static final int DETAIL_SCROLL_STEP = 18;
    private static final int DETAIL_CONTENT_H = 168;

    private static final int ACCENT = InvUi.ACCENT_CATALOGUE;

    private static final Map<String, ItemEntry> ITEM_CACHE = new HashMap<>();
    private static boolean itemCacheLoaded = false;

    private final Screen parent;
    private final List<CatalogReportSnapshot> reports = new ArrayList<>();
    private final List<ReportRow> reportRows = new ArrayList<>();
    private final List<CatalogReportSnapshot.ItemCount> filteredItems = new ArrayList<>();
    private final List<InventorySortHitboxButton> reportButtons = new ArrayList<>();
    private final List<InventorySortHitboxButton> itemButtons = new ArrayList<>();

    private CatalogReportSnapshot selectedReport;
    private String selectedItemId;
    private EditBox searchBox;
    private String lastQuery = "";

    private int panelW;
    private int panelH;
    private int panelX;
    private int panelY;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int bandY;
    private int gridX;
    private int gridY;
    private int gridW;
    private int gridH;
    private int detailX;
    private int detailY;
    private int detailW;
    private int detailH;
    private int reportScroll;
    private int itemScroll;
    private int detailScroll;
    private boolean detailMode;

    public CatalogReportBrowserScreen(Screen parent) {
        super(Component.literal("Inventory Catalogue Reports"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ensureItemCache();
        computeLayout();
        this.clearWidgets();

        this.addRenderableWidget(new InventorySortModalIconButton(closeButtonX(), closeButtonY(), 16,
                InventorySortModalIconButton.CLOSE, Component.literal("Close"), button -> closeToParent()));

        if (detailMode && selectedReport != null) {
            InventorySortTextButton back = new InventorySortTextButton(panelX + PAD, panelY + 7, 80, 18,
                    Component.literal("< Reports"), button -> openBrowser());
            back.setTooltip(Tooltip.create(Component.literal("Back to the list of saved reports.")));
            this.addRenderableWidget(back);

            this.searchBox = new EditBox(this.font, gridX + 6, bandY + 5, gridW - 12, 14, Component.literal("Search"));
            this.searchBox.setMaxLength(64);
            this.searchBox.setBordered(false);
            this.searchBox.setTextColor(InvUi.TEXT);
            this.searchBox.setValue(lastQuery);
            this.addRenderableWidget(searchBox);
            updateFilteredItems();
            buildItemHitboxes();
            this.setInitialFocus(searchBox);
        } else {
            this.searchBox = null;
            reloadReports();
            buildReportHitboxes();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (detailMode && searchBox != null) {
            String query = searchBox.getValue();
            if (!query.equals(lastQuery)) {
                lastQuery = query;
                itemScroll = 0;
                updateFilteredItems();
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        InventorySortDrawContext ui = InventorySortDrawContexts.wrap(g);
        InvUi.scrim(ui, this.width, this.height);
        InvUi.window(ui, panelX, panelY, panelW, panelH, ACCENT);

        if (detailMode && selectedReport != null) {
            renderDetail(g, ui, mouseX, mouseY);
        } else {
            renderBrowser(g, ui, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderBrowser(GuiGraphics g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        text(g, "Catalogue Reports", panelX + PAD, panelY + 9, InvUi.TEXT);
        text(g, "Saved snapshots grouped by world", panelX + PAD, panelY + 24, InvUi.TEXT_MUTED);

        InvUi.inset(ui, listX, listY, listW, listH);
        g.enableScissor(listX + 2, listY + 2, listX + listW - 2, listY + listH - 2);

        if (reportRows.isEmpty()) {
            text(g, "No saved catalogue reports yet.", listX + 12, listY + 12, InvUi.TEXT_MUTED);
            text(g, "Run /inventorycatalogue start, then stop to capture one.", listX + 12, listY + 26, InvUi.TEXT_DIM);
        } else {
            updateReportHitboxes(mouseX, mouseY);
            int y = listY + 6 - reportScroll;
            for (ReportRow row : reportRows) {
                int rowH = row.header ? HEADER_H : REPORT_ROW_H;
                if (y + rowH < listY || y > listY + listH) {
                    y += rowH;
                    continue;
                }
                if (row.header) {
                    InvUi.sectionBand(ui, this.font, displayNamespace(row.namespace),
                            listX + 6, y + 2, listW - 12, HEADER_H - 4, ACCENT);
                } else {
                    boolean hovered = mouseX >= listX + 6 && mouseX <= listX + listW - 6
                            && mouseY >= y && mouseY <= y + REPORT_ROW_H - 4;
                    InvUi.row(ui, listX + 6, y, listW - 12, REPORT_ROW_H - 4, hovered, false, ACCENT);
                    text(g, row.report.displayTime(), listX + 14, y + 6, InvUi.TEXT);
                    String items = String.format(Locale.ROOT, "%,d", row.report.getTotalItems());
                    text(g, items, listX + 14, y + 20, ACCENT);
                    String rest = String.format(Locale.ROOT, " items  -  %,d types  -  %,d locations",
                            row.report.getUniqueItems(), row.report.getLocationCount());
                    text(g, truncate(rest, listW - 64 - this.font.width(items)),
                            listX + 14 + this.font.width(items), y + 20, InvUi.TEXT_MUTED);
                    text(g, ">", listX + listW - 20, y + 13, hovered ? ACCENT : InvUi.TEXT_DIM);
                }
                y += rowH;
            }
        }
        g.disableScissor();
        InvUi.scrollbar(ui, listX + listW - 7, listY + 5, listH - 10, browserContentHeight(), listH, reportScroll, ACCENT);
    }

    private void renderDetail(GuiGraphics g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        text(g, "Catalogue Report", panelX + PAD + 90, panelY + 9, InvUi.TEXT);
        text(g, truncate(displayNamespace(selectedReport.getNamespace()), panelW - 230),
                panelX + PAD + 90 + this.font.width("Catalogue Report ") + 4, panelY + 9, InvUi.TEXT_MUTED);

        // Search field above the grid.
        InvUi.field(ui, gridX, bandY, gridW, 18, searchBox != null && searchBox.isFocused(), ACCENT);
        if (searchBox != null && searchBox.getValue().isEmpty()) {
            text(g, "Filter items or :category...", gridX + 6, bandY + 5, InvUi.TEXT_DIM);
        }

        // Summary above the sidebar.
        text(g, truncate(selectedReport.displayTime(), detailW), detailX, bandY + 1, InvUi.TEXT);
        String total = String.format(Locale.ROOT, "%,d", selectedReport.getTotalItems());
        text(g, total, detailX, bandY + 14, ACCENT);
        text(g, truncate(String.format(Locale.ROOT, " items  -  %,d types", selectedReport.getUniqueItems()),
                detailW - this.font.width(total)), detailX + this.font.width(total), bandY + 14, InvUi.TEXT_MUTED);

        // Item grid.
        InvUi.inset(ui, gridX, gridY, gridW, gridH);
        updateItemHitboxes(mouseX, mouseY);
        renderItemGrid(g, ui, mouseX, mouseY);
        InvUi.insetBorder(ui, gridX, gridY, gridW, gridH);

        // Selected-item sidebar.
        InvUi.inset(ui, detailX, detailY, detailW, detailH);
        renderSelectedItemDetails(g, ui);
        InvUi.insetBorder(ui, detailX, detailY, detailW, detailH);
    }

    private void renderItemGrid(GuiGraphics g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        g.enableScissor(gridX + 2, gridY + 2, gridX + gridW - 2, gridY + gridH - 2);
        int columns = gridColumns();
        int yStart = gridY + GRID_INSET - itemScroll;

        if (filteredItems.isEmpty()) {
            text(g, "No items match this filter.", gridX + GRID_INSET, gridY + GRID_INSET, InvUi.TEXT_MUTED);
        } else {
            for (int i = 0; i < filteredItems.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = gridX + GRID_INSET + col * CELL;
                int y = yStart + row * CELL;
                if (y + CELL < gridY) {
                    continue;
                }
                if (y > gridY + gridH) {
                    break;
                }

                CatalogReportSnapshot.ItemCount item = filteredItems.get(i);
                boolean selected = item.itemId().equals(selectedItemId);
                boolean hovered = mouseX >= x && mouseX <= x + TILE && mouseY >= y && mouseY <= y + TILE;
                InvUi.slot(ui, x, y, TILE, hovered, selected, ACCENT);

                ItemEntry entry = itemEntry(item.itemId());
                if (!entry.icon.isEmpty()) {
                    g.renderItem(entry.icon, x + (TILE - 16) / 2, y + (TILE - 16) / 2);
                    g.renderItemDecorations(this.font, entry.icon, x + (TILE - 16) / 2, y + (TILE - 16) / 2);
                } else {
                    text(g, "?", x + 9, y + 8, InvUi.TEXT_DIM);
                }

                String count = compactCount(item.count());
                InvUi.countBadge(ui, this.font, count, x + TILE, y + TILE + 1, ACCENT);
            }
        }
        g.disableScissor();
        InvUi.scrollbar(ui, gridX + gridW - 6, gridY + 4, gridH - 8, gridContentHeight(), gridH, itemScroll, ACCENT);
    }

    private void renderSelectedItemDetails(GuiGraphics g, InventorySortDrawContext ui) {
        int clipX = detailX + 3;
        int clipY = detailY + 3;
        int clipW = Math.max(1, detailW - 6);
        int clipH = Math.max(1, detailH - 6);
        g.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
        try {
            int contentY = detailY + DETAIL_INSET - detailScroll;
            CatalogReportSnapshot.ItemCount selected = selectedItemId == null ? null : findFilteredItem(selectedItemId);
            if (selected == null) {
                text(g, "Select an item", detailX + DETAIL_INSET, contentY, InvUi.TEXT_MUTED);
                text(g, "Click a tile to see its totals.", detailX + DETAIL_INSET, contentY + 13, InvUi.TEXT_DIM);
                return;
            }

            ItemEntry entry = itemEntry(selected.itemId());
            int iconX = detailX + DETAIL_INSET;
            int iconY = contentY;
            InvUi.slot(ui, iconX, iconY, 22, false, false, ACCENT);
            if (!entry.icon.isEmpty()) {
                g.renderItem(entry.icon, iconX + 3, iconY + 3);
                g.renderItemDecorations(this.font, entry.icon, iconX + 3, iconY + 3);
            }

            int textX = iconX + 30;
            int fullW = detailW - DETAIL_INSET * 2;
            text(g, truncate(entry.displayName, fullW - 30), textX, contentY + 2, InvUi.TEXT);
            text(g, truncate(selected.itemId(), fullW - 30), textX, contentY + 13, InvUi.TEXT_DIM);

            String total = String.format(Locale.ROOT, "%,d", selected.count());
            text(g, total, detailX + DETAIL_INSET, contentY + 34, ACCENT);
            text(g, " total in this report", detailX + DETAIL_INSET + this.font.width(total), contentY + 34, InvUi.TEXT_MUTED);
            text(g, truncate(stackSummary(entry.icon, selected.count()), fullW), detailX + DETAIL_INSET, contentY + 48, InvUi.TEXT);

            int share = selectedReport.getTotalItems() <= 0
                    ? 0
                    : Math.round((selected.count() * 100.0f) / selectedReport.getTotalItems());
            renderShareBar(ui, detailX + DETAIL_INSET, contentY + 64, fullW, share);
            text(g, share + "% of report total", detailX + DETAIL_INSET, contentY + 74, InvUi.TEXT_MUTED);

            int dividerY = contentY + 92;
            ui.fill(detailX + DETAIL_INSET, dividerY, detailX + detailW - DETAIL_INSET, dividerY + 1, InvUi.DIVIDER);
            text(g, "Report file", detailX + DETAIL_INSET, contentY + 100, InvUi.TEXT_DIM);
            text(g, truncate(selectedReport.getReportFileName(), fullW), detailX + DETAIL_INSET, contentY + 112, InvUi.TEXT_MUTED);
        } finally {
            g.disableScissor();
        }

        InvUi.scrollbar(ui, detailX + detailW - 6, detailY + 5, detailH - 10, DETAIL_CONTENT_H,
                detailH - DETAIL_INSET, detailScroll, ACCENT);
    }

    private void renderShareBar(InventorySortDrawContext ui, int x, int y, int w, int share) {
        ui.fill(x, y, x + w, y + 4, InvUi.WELL);
        int fill = Math.max(1, Math.min(w, w * share / 100));
        ui.fill(x, y, x + fill, y + 4, ACCENT);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private boolean handleMouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        int delta = (int) Math.signum(-verticalAmount);
        if (delta == 0) {
            return false;
        }
        if (detailMode && isInside(mouseX, mouseY, detailX, detailY, detailW, detailH)) {
            detailScroll = clamp(detailScroll + delta * DETAIL_SCROLL_STEP, 0, maxDetailScroll());
            return true;
        }
        if (detailMode && isInside(mouseX, mouseY, gridX, gridY, gridW, gridH)) {
            itemScroll = clamp(itemScroll + delta * CELL, 0, maxItemScroll());
            updateItemHitboxes((int) mouseX, (int) mouseY);
            return true;
        }
        if (!detailMode && isInside(mouseX, mouseY, listX, listY, listW, listH)) {
            reportScroll = clamp(reportScroll + delta * REPORT_ROW_H, 0, maxReportScroll());
            updateReportHitboxes((int) mouseX, (int) mouseY);
            return true;
        }
        return false;
    }

    private void reloadReports() {
        reports.clear();
        reports.addAll(CatalogReportHistory.loadAll());
        reportRows.clear();

        Map<String, List<CatalogReportSnapshot>> grouped = new LinkedHashMap<>();
        for (CatalogReportSnapshot report : reports) {
            grouped.computeIfAbsent(report.getNamespace(), ignored -> new ArrayList<>()).add(report);
        }
        for (Map.Entry<String, List<CatalogReportSnapshot>> group : grouped.entrySet()) {
            reportRows.add(ReportRow.header(group.getKey()));
            for (CatalogReportSnapshot report : group.getValue()) {
                reportRows.add(ReportRow.report(report));
            }
        }
        reportScroll = clamp(reportScroll, 0, maxReportScroll());
    }

    private void updateFilteredItems() {
        filteredItems.clear();
        if (selectedReport == null) {
            selectedItemId = null;
            return;
        }

        String query = searchBox == null ? lastQuery : searchBox.getValue();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        InventorySortCategories.CategoryQuery categoryQuery = InventorySortCategories.categoryQuery(query);
        for (CatalogReportSnapshot.ItemCount item : selectedReport.sortedItems()) {
            ItemEntry entry = itemEntry(item.itemId());
            boolean matches = normalizedQuery.isEmpty()
                    || (categoryQuery != null
                            ? categoryQuery.matches(entry.item)
                            : item.itemId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                                    || entry.searchName.contains(normalizedQuery));
            if (matches) {
                filteredItems.add(item);
            }
        }

        if (filteredItems.isEmpty()) {
            selectedItemId = null;
        } else if (selectedItemId == null || findFilteredItem(selectedItemId) == null) {
            selectedItemId = filteredItems.get(0).itemId();
        }
        detailScroll = clamp(detailScroll, 0, maxDetailScroll());
        itemScroll = clamp(itemScroll, 0, maxItemScroll());
        updateItemHitboxes(0, 0);
    }

    private void buildReportHitboxes() {
        reportButtons.clear();
        int maxVisibleReports = Math.max(12, (listH / Math.max(1, REPORT_ROW_H)) + 4);
        for (int i = 0; i < maxVisibleReports; i++) {
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, 1, 1, Component.literal("Report"), hitbox -> {
                String targetId = ((InventorySortHitboxButton) hitbox).getTargetId();
                CatalogReportSnapshot report = findReport(targetId);
                if (report != null) {
                    openDetail(report);
                }
            });
            button.visible = false;
            button.active = false;
            reportButtons.add(button);
            this.addRenderableWidget(button);
        }
        updateReportHitboxes(0, 0);
    }

    private void buildItemHitboxes() {
        itemButtons.clear();
        int maxVisibleItems = Math.max(24, gridColumns() * ((gridH / CELL) + 3));
        for (int i = 0; i < maxVisibleItems; i++) {
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, TILE, TILE, Component.literal("Item"), hitbox -> {
                String targetId = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (targetId != null) {
                    selectedItemId = targetId;
                    detailScroll = 0;
                }
            });
            button.visible = false;
            button.active = false;
            itemButtons.add(button);
            this.addRenderableWidget(button);
        }
        updateItemHitboxes(0, 0);
    }

    private void updateReportHitboxes(int mouseX, int mouseY) {
        for (InventorySortHitboxButton button : reportButtons) {
            button.visible = false;
            button.active = false;
            button.setTargetId(null);
        }

        int index = 0;
        int y = listY + 6 - reportScroll;
        for (ReportRow row : reportRows) {
            int rowH = row.header ? HEADER_H : REPORT_ROW_H;
            if (!row.header && y + REPORT_ROW_H > listY && y < listY + listH && index < reportButtons.size()) {
                InventorySortHitboxButton button = reportButtons.get(index++);
                button.setBounds(listX + 6, y, listW - 12, REPORT_ROW_H - 4);
                button.setTargetId(row.report.getId());
                button.visible = true;
                button.active = true;
            }
            y += rowH;
        }
    }

    private void updateItemHitboxes(int mouseX, int mouseY) {
        for (InventorySortHitboxButton button : itemButtons) {
            button.visible = false;
            button.active = false;
            button.setTargetId(null);
        }

        int columns = gridColumns();
        int yStart = gridY + GRID_INSET - itemScroll;
        int buttonIndex = 0;
        for (int i = 0; i < filteredItems.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = gridX + GRID_INSET + col * CELL;
            int y = yStart + row * CELL;
            if (y + CELL < gridY) {
                continue;
            }
            if (y > gridY + gridH || buttonIndex >= itemButtons.size()) {
                break;
            }
            InventorySortHitboxButton button = itemButtons.get(buttonIndex++);
            button.setBounds(x, y, TILE, TILE);
            button.setTargetId(filteredItems.get(i).itemId());
            button.visible = true;
            button.active = true;
        }
    }

    private CatalogReportSnapshot.ItemCount findFilteredItem(String itemId) {
        for (CatalogReportSnapshot.ItemCount item : filteredItems) {
            if (item.itemId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    private CatalogReportSnapshot findReport(String reportId) {
        if (reportId == null) {
            return null;
        }
        for (CatalogReportSnapshot report : reports) {
            if (reportId.equals(report.getId())) {
                return report;
            }
        }
        return null;
    }

    private void openDetail(CatalogReportSnapshot report) {
        selectedReport = report;
        detailMode = true;
        selectedItemId = null;
        itemScroll = 0;
        detailScroll = 0;
        lastQuery = "";
        this.rebuildWidgets();
    }

    private void openBrowser() {
        detailMode = false;
        selectedReport = null;
        selectedItemId = null;
        detailScroll = 0;
        lastQuery = "";
        this.rebuildWidgets();
    }

    private void computeLayout() {
        panelW = Math.min(620, this.width - 24);
        panelH = Math.min(330, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        listX = panelX + PAD;
        listY = panelY + 42;
        listW = panelW - PAD * 2;
        listH = panelH - 42 - PAD;

        bandY = panelY + 40;
        gridX = panelX + PAD;
        gridY = panelY + 70;
        gridW = Math.max(210, (panelW - PAD * 3) * 3 / 5);
        gridH = panelY + panelH - PAD - gridY;
        detailX = gridX + gridW + PAD;
        detailY = gridY;
        detailW = panelX + panelW - PAD - detailX;
        detailH = gridH;
    }

    private int gridColumns() {
        return Math.max(1, (gridW - GRID_INSET * 2) / CELL);
    }

    private int gridContentHeight() {
        if (filteredItems.isEmpty()) {
            return 0;
        }
        int rows = (filteredItems.size() + gridColumns() - 1) / gridColumns();
        return rows * CELL + GRID_INSET * 2;
    }

    private int browserContentHeight() {
        int contentH = 12;
        for (ReportRow row : reportRows) {
            contentH += row.header ? HEADER_H : REPORT_ROW_H;
        }
        return contentH;
    }

    private int maxItemScroll() {
        return Math.max(0, gridContentHeight() - gridH);
    }

    private int maxDetailScroll() {
        int visibleH = Math.max(1, detailH - DETAIL_INSET * 2);
        return Math.max(0, DETAIL_CONTENT_H - visibleH);
    }

    private int maxReportScroll() {
        return Math.max(0, browserContentHeight() - listH);
    }

    private void closeToParent() {
        MinecraftApiCompat.setScreen(Minecraft.getInstance(), parent);
    }

    private int closeButtonX() {
        return panelX + panelW - PAD - 16;
    }

    private int closeButtonY() {
        return panelY + 7;
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void text(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(this.font, text, x, y, color, false);
    }

    private String truncate(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
    }

    private static String displayNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return "Unknown world";
        }
        String value = namespace;
        if (value.startsWith("singleplayer:")) {
            value = value.substring("singleplayer:".length());
            return "Singleplayer - " + value;
        }
        if (value.startsWith("server:")) {
            value = value.substring("server:".length());
            return "Server - " + value;
        }
        return value;
    }

    private static String compactCount(int count) {
        if (count < 1_000) {
            return String.valueOf(count);
        }

        String[] suffixes = {"", "K", "M", "B"};
        double value = count;
        int suffixIndex = 0;
        while (value >= 999.5 && suffixIndex < suffixes.length - 1) {
            value /= 1_000.0;
            suffixIndex++;
        }

        String formatted = value < 10.0
                ? String.format(Locale.ROOT, "%.1f", value)
                : String.format(Locale.ROOT, "%.0f", value);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted + suffixes[suffixIndex];
    }

    private static String stackSummary(ItemStack icon, int count) {
        int maxStack = icon.isEmpty() ? 64 : Math.max(1, icon.getMaxStackSize());
        if (maxStack <= 1) {
            return String.format(Locale.ROOT, "%,d individual item%s", count, count == 1 ? "" : "s");
        }
        int stacks = count / maxStack;
        int remainder = count % maxStack;
        if (remainder == 0) {
            return String.format(Locale.ROOT, "%,d full stack%s", stacks, stacks == 1 ? "" : "s");
        }
        return String.format(Locale.ROOT, "%,d stack%s + %d",
                stacks, stacks == 1 ? "" : "s", remainder);
    }

    private static void ensureItemCache() {
        if (itemCacheLoaded) {
            return;
        }
        ITEM_CACHE.clear();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            ItemStack icon = new ItemStack(item);
            String displayName = icon.getHoverName().getString();
            ITEM_CACHE.put(id, new ItemEntry(id, displayName, icon, item));
        }
        itemCacheLoaded = true;
    }

    private static ItemEntry itemEntry(String itemId) {
        ensureItemCache();
        ItemEntry entry = ITEM_CACHE.get(itemId);
        return entry != null ? entry : new ItemEntry(itemId, formatItemName(itemId), ItemStack.EMPTY, null);
    }

    private static String formatItemName(String itemId) {
        String name = itemId == null ? "unknown" : itemId;
        if (name.contains(":")) {
            name = name.substring(name.lastIndexOf(':') + 1);
        }
        String[] words = name.split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1));
            }
        }
        return out.length() == 0 ? "Unknown" : out.toString();
    }

    private static final class ItemEntry {
        final String itemId;
        final String displayName;
        final ItemStack icon;
        final Item item;
        final String searchName;

        ItemEntry(String itemId, String displayName, ItemStack icon, Item item) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.icon = icon;
            this.item = item;
            this.searchName = (displayName + " " + itemId).toLowerCase(Locale.ROOT);
        }
    }

    private static final class ReportRow {
        final boolean header;
        final String namespace;
        final CatalogReportSnapshot report;

        private ReportRow(boolean header, String namespace, CatalogReportSnapshot report) {
            this.header = header;
            this.namespace = namespace;
            this.report = report;
        }

        static ReportRow header(String namespace) {
            return new ReportRow(true, namespace, null);
        }

        static ReportRow report(CatalogReportSnapshot report) {
            return new ReportRow(false, report.getNamespace(), report);
        }
    }
}
