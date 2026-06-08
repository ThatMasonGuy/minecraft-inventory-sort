package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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

public class CatalogReportBrowserScreen extends Screen {
    private static final int PAD = 16;
    private static final int HEADER_H = 20;
    private static final int REPORT_ROW_H = 36;
    private static final int CELL = 28;
    private static final int SEARCH_H = 20;
    private static final int DETAIL_TOP_H = 66;
    private static final int CLOSE_SIZE = 18;
    private static final int BACK_W = 74;
    private static final int BACK_H = 18;

    private static final int OVERLAY = 0xAA000000;
    private static final int FRAME_BG = 0xF0202020;
    private static final int FRAME_HEADER = 0xF02C302D;
    private static final int FRAME_BORDER = 0xFF777B72;
    private static final int FRAME_SHADOW = 0xFF070707;
    private static final int PANEL_BG = 0xFF111111;
    private static final int PANEL_ALT = 0xFF1A1A1A;
    private static final int PANEL_EDGE = 0xFF3F423D;
    private static final int PANEL_EDGE_LIGHT = 0xFF757A70;
    private static final int SLOT_BG = 0xFF858585;
    private static final int SLOT_HOVER = 0xFFA7A7A7;
    private static final int SLOT_SELECTED = 0xFFE3D48A;
    private static final int TEXT_MAIN = 0xFFE8E8E8;
    private static final int TEXT_MUTED = 0xFFB9B9B9;
    private static final int TEXT_DIM = 0xFF8E8E8E;
    private static final int ACCENT = 0xFFFFD95A;
    private static final int ACCENT_GREEN = 0xFF90C76E;

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

        this.addRenderableWidget(new InventorySortHitboxButton(closeButtonX(), closeButtonY(), CLOSE_SIZE, CLOSE_SIZE,
                Component.literal("Close"), button -> closeToParent()));

        if (detailMode && selectedReport != null) {
            this.addRenderableWidget(new InventorySortHitboxButton(backButtonX(), backButtonY(), BACK_W, BACK_H,
                    Component.literal("Back"), button -> openBrowser()));

            int boxX = gridX;
            int boxY = panelY + 40;
            int boxW = gridW;
            this.searchBox = new EditBox(this.font, boxX + 8, boxY + 6, boxW - 16, 14,
                    Component.literal("Search"));
            this.searchBox.setMaxLength(64);
            this.searchBox.setBordered(false);
            this.searchBox.setTextColor(TEXT_MAIN);
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
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, OVERLAY);
        drawFrame(g);

        if (detailMode && selectedReport != null) {
            renderDetail(g, mouseX, mouseY);
        } else {
            renderBrowser(g, mouseX, mouseY);
        }

        drawCloseControl(g, mouseX, mouseY);
        if (detailMode && selectedReport != null) {
            drawBackControl(g, mouseX, mouseY);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void renderBrowser(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        text(g, "Inventory Catalogue Reports", panelX + PAD, panelY + 10, TEXT_MAIN);
        text(g, "Saved reports grouped by world", panelX + PAD, panelY + 25, TEXT_MUTED);

        drawPanel(g, listX, listY, listW, listH);
        g.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);

        if (reportRows.isEmpty()) {
            text(g, "No saved catalogue reports yet.", listX + 10, listY + 12, TEXT_MUTED);
        } else {
            updateReportHitboxes(mouseX, mouseY);
            int y = listY + 4 - reportScroll;
            for (ReportRow row : reportRows) {
                int rowH = row.header ? HEADER_H : REPORT_ROW_H;
                if (y + rowH < listY) {
                    y += rowH;
                    continue;
                }
                if (y > listY + listH) {
                    break;
                }

                if (row.header) {
                    g.fill(listX + 6, y + 2, listX + listW - 6, y + HEADER_H - 2, 0xFF202720);
                    g.fill(listX + 6, y + 2, listX + 9, y + HEADER_H - 2, ACCENT_GREEN);
                    text(g, displayNamespace(row.namespace), listX + 14, y + 6, TEXT_MAIN);
                } else {
                    boolean hovered = mouseX >= listX + 4 && mouseX <= listX + listW - 4
                            && mouseY >= y && mouseY <= y + REPORT_ROW_H - 3;
                    int bg = hovered ? 0xFF2C2C2C : PANEL_ALT;
                    drawPanel(g, listX + 6, y, listW - 12, REPORT_ROW_H - 4, bg);
                    g.fill(listX + 9, y + 4, listX + 11, y + REPORT_ROW_H - 8, ACCENT);
                    text(g, row.report.displayTime(), listX + 16, y + 6, TEXT_MAIN);
                    String summary = String.format(Locale.ROOT, "%,d items  |  %,d types  |  %,d locations",
                            row.report.getTotalItems(), row.report.getUniqueItems(), row.report.getLocationCount());
                    text(g, truncate(summary, listW - 44), listX + 16, y + 20, TEXT_MUTED);
                    text(g, ">", listX + listW - 18, y + 12, hovered ? ACCENT : TEXT_DIM);
                }
                y += rowH;
            }
        }

        g.disableScissor();
    }

    private void renderDetail(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        text(g, "Catalogue Report", panelX + 104, panelY + 10, TEXT_MAIN);
        text(g, truncate(displayNamespace(selectedReport.getNamespace()), panelW - 238), panelX + 104, panelY + 25, TEXT_MUTED);

        drawPanel(g, gridX, panelY + 40, gridW, SEARCH_H);

        int summaryY = panelY + 40;
        text(g, selectedReport.displayTime(), detailX, summaryY, TEXT_MAIN);
        text(g, String.format(Locale.ROOT, "%,d total items", selectedReport.getTotalItems()), detailX, summaryY + 12, ACCENT);
        text(g, String.format(Locale.ROOT, "%,d item types, %,d locations",
                selectedReport.getUniqueItems(), selectedReport.getLocationCount()), detailX, summaryY + 24, TEXT_MUTED);

        drawPanel(g, gridX, gridY, gridW, gridH);
        updateItemHitboxes(mouseX, mouseY);
        renderItemGrid(g, mouseX, mouseY);

        drawPanel(g, detailX, detailY, detailW, detailH);
        renderSelectedItemDetails(g);
    }

    private void renderItemGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.enableScissor(gridX + 1, gridY + 1, gridX + gridW - 1, gridY + gridH - 1);
        int columns = gridColumns();
        int yStart = gridY + 6 - itemScroll;

        if (filteredItems.isEmpty()) {
            text(g, "No items match this report filter.", gridX + 10, gridY + 12, TEXT_MUTED);
        } else {
            for (int i = 0; i < filteredItems.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = gridX + 6 + col * CELL;
                int y = yStart + row * CELL;
                if (y + CELL < gridY) {
                    continue;
                }
                if (y > gridY + gridH) {
                    break;
                }

                CatalogReportSnapshot.ItemCount item = filteredItems.get(i);
                boolean selected = item.itemId().equals(selectedItemId);
                boolean hovered = mouseX >= x && mouseX <= x + CELL - 3
                        && mouseY >= y && mouseY <= y + CELL - 3;
                drawSlot(g, x, y, CELL - 3, selected, hovered);

                ItemEntry entry = itemEntry(item.itemId());
                if (!entry.icon.isEmpty()) {
                    g.item(entry.icon, x + 5, y + 4);
                    g.itemDecorations(this.font, entry.icon, x + 5, y + 4);
                } else {
                    text(g, "?", x + 10, y + 8, 0xFF1F1F1F);
                }

                String count = compactCount(item.count());
                int countW = this.font.width(count);
                g.fill(x + CELL - 6 - countW, y + CELL - 13, x + CELL - 1, y + CELL - 3, 0xCC101010);
                text(g, count, x + CELL - 4 - countW, y + CELL - 12, ACCENT);
            }
        }

        g.disableScissor();
    }

    private void renderSelectedItemDetails(GuiGraphicsExtractor g) {
        if (selectedItemId == null) {
            text(g, "No item selected", detailX + 10, detailY + 10, TEXT_MUTED);
            return;
        }

        CatalogReportSnapshot.ItemCount selected = findFilteredItem(selectedItemId);
        if (selected == null) {
            text(g, "No item selected", detailX + 10, detailY + 10, TEXT_MUTED);
            return;
        }

        ItemEntry entry = itemEntry(selected.itemId());
        int iconX = detailX + 12;
        int iconY = detailY + 12;
        drawSlot(g, iconX - 4, iconY - 4, 25, false, false);
        if (!entry.icon.isEmpty()) {
            g.item(entry.icon, iconX, iconY);
            g.itemDecorations(this.font, entry.icon, iconX, iconY);
        }

        text(g, truncate(entry.displayName, detailW - 54), detailX + 42, detailY + 10, TEXT_MAIN);
        text(g, truncate(selected.itemId(), detailW - 28), detailX + 12, detailY + 42, TEXT_MUTED);
        text(g, String.format(Locale.ROOT, "%,d total", selected.count()), detailX + 12, detailY + 64, ACCENT);
        text(g, stackSummary(entry.icon, selected.count()), detailX + 12, detailY + 78, TEXT_MAIN);

        int share = selectedReport.getTotalItems() <= 0
                ? 0
                : Math.round((selected.count() * 100.0f) / selectedReport.getTotalItems());
        text(g, share + "% of report total", detailX + 12, detailY + 92, TEXT_MUTED);

        g.fill(detailX + 10, detailY + 116, detailX + detailW - 10, detailY + 117, PANEL_EDGE);
        text(g, "Report file", detailX + 12, detailY + 128, TEXT_DIM);
        text(g, truncate(selectedReport.getReportFileName(), detailW - 24),
                detailX + 12, detailY + 140, TEXT_MAIN);
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
        for (CatalogReportSnapshot.ItemCount item : selectedReport.sortedItems()) {
            ItemEntry entry = itemEntry(item.itemId());
            if (normalizedQuery.isEmpty()
                    || item.itemId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || entry.searchName.contains(normalizedQuery)) {
                filteredItems.add(item);
            }
        }

        if (filteredItems.isEmpty()) {
            selectedItemId = null;
        } else if (selectedItemId == null || findFilteredItem(selectedItemId) == null) {
            selectedItemId = filteredItems.get(0).itemId();
        }
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
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, CELL - 3, CELL - 3, Component.literal("Item"), hitbox -> {
                String targetId = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (targetId != null) {
                    selectedItemId = targetId;
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
        int y = listY + 4 - reportScroll;
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
        int yStart = gridY + 6 - itemScroll;
        int buttonIndex = 0;
        for (int i = 0; i < filteredItems.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = gridX + 6 + col * CELL;
            int y = yStart + row * CELL;
            if (y + CELL < gridY) {
                continue;
            }
            if (y > gridY + gridH || buttonIndex >= itemButtons.size()) {
                break;
            }
            InventorySortHitboxButton button = itemButtons.get(buttonIndex++);
            button.setBounds(x, y, CELL - 3, CELL - 3);
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
        lastQuery = "";
        this.rebuildWidgets();
    }

    private void openBrowser() {
        detailMode = false;
        selectedReport = null;
        selectedItemId = null;
        lastQuery = "";
        this.rebuildWidgets();
    }

    private void computeLayout() {
        panelW = Math.min(620, this.width - 24);
        panelH = Math.min(330, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        listX = panelX + PAD;
        listY = panelY + 48;
        listW = panelW - PAD * 2;
        listH = panelH - 64;

        gridX = panelX + PAD;
        gridY = panelY + DETAIL_TOP_H;
        gridW = Math.max(210, (panelW - PAD * 3) * 3 / 5);
        gridH = panelH - DETAIL_TOP_H - PAD;
        detailX = gridX + gridW + PAD;
        detailY = gridY;
        detailW = panelX + panelW - PAD - detailX;
        detailH = gridH;
    }

    private int gridColumns() {
        return Math.max(1, (gridW - 12) / CELL);
    }

    private int maxItemScroll() {
        if (filteredItems.isEmpty()) {
            return 0;
        }
        int rows = (filteredItems.size() + gridColumns() - 1) / gridColumns();
        int contentH = rows * CELL + 12;
        return Math.max(0, contentH - gridH);
    }

    private int maxReportScroll() {
        int contentH = 8;
        for (ReportRow row : reportRows) {
            contentH += row.header ? HEADER_H : REPORT_ROW_H;
        }
        return Math.max(0, contentH - listH);
    }

    private void closeToParent() {
        MinecraftApiCompat.setScreen(Minecraft.getInstance(), parent);
    }

    private void drawFrame(GuiGraphicsExtractor g) {
        g.fill(panelX + 4, panelY + 4, panelX + panelW + 4, panelY + panelH + 4, 0x66000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, FRAME_BORDER);
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, FRAME_SHADOW);
        g.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + panelH - 2, FRAME_BG);
        g.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 34, FRAME_HEADER);
        g.fill(panelX + 2, panelY + 34, panelX + panelW - 2, panelY + 35, 0xFF4C5249);
        g.fill(panelX + PAD, panelY + 38, panelX + panelW - PAD, panelY + 39, 0x55101010);
    }

    private void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, PANEL_BG);
    }

    private void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, FRAME_SHADOW);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_EDGE);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, PANEL_EDGE_LIGHT);
        g.fill(x + 2, y + 2, x + 3, y + h - 2, PANEL_EDGE_LIGHT);
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y, int size, boolean selected, boolean hovered) {
        int fill = selected ? SLOT_SELECTED : hovered ? SLOT_HOVER : SLOT_BG;
        int edge = selected ? 0xFFB79D47 : 0xFF4B4B4B;
        g.fill(x, y, x + size, y + size, FRAME_SHADOW);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, edge);
        g.fill(x + 2, y + 2, x + size - 2, y + size - 2, fill);
        g.fill(x + 2, y + 2, x + size - 2, y + 3, 0xFFD8D8D8);
        g.fill(x + 2, y + 2, x + 3, y + size - 2, 0xFFD8D8D8);
        g.fill(x + 2, y + size - 3, x + size - 2, y + size - 2, 0xFF555555);
        g.fill(x + size - 3, y + 2, x + size - 2, y + size - 2, 0xFF555555);
    }

    private void drawCloseControl(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = closeButtonX();
        int y = closeButtonY();
        boolean hovered = isInside(mouseX, mouseY, x, y, CLOSE_SIZE, CLOSE_SIZE);
        drawPanel(g, x, y, CLOSE_SIZE, CLOSE_SIZE, hovered ? 0xFF3A2A2A : PANEL_ALT);
        int color = hovered ? 0xFFFF8A8A : TEXT_MAIN;
        text(g, "x", x + 6, y + 5, color);
    }

    private void drawBackControl(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = backButtonX();
        int y = backButtonY();
        boolean hovered = isInside(mouseX, mouseY, x, y, BACK_W, BACK_H);
        drawPanel(g, x, y, BACK_W, BACK_H, hovered ? 0xFF2D342B : PANEL_ALT);
        text(g, "< Reports", x + 8, y + 5, hovered ? ACCENT_GREEN : TEXT_MAIN);
    }

    private int closeButtonX() {
        return panelX + panelW - PAD - CLOSE_SIZE;
    }

    private int closeButtonY() {
        return panelY + 8;
    }

    private int backButtonX() {
        return panelX + PAD;
    }

    private int backButtonY() {
        return panelY + 8;
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

    private void text(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(this.font, text, x, y, color, false);
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
        if (count >= 1_000_000) {
            return (count / 1_000_000) + "m";
        }
        if (count >= 10_000) {
            return (count / 1_000) + "k";
        }
        return String.valueOf(count);
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
            ITEM_CACHE.put(id, new ItemEntry(id, displayName, icon));
        }
        itemCacheLoaded = true;
    }

    private static ItemEntry itemEntry(String itemId) {
        ensureItemCache();
        ItemEntry entry = ITEM_CACHE.get(itemId);
        return entry != null ? entry : new ItemEntry(itemId, formatItemName(itemId), ItemStack.EMPTY);
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
        final String searchName;

        ItemEntry(String itemId, String displayName, ItemStack icon) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.icon = icon;
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
