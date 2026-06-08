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
    private static final int PAD = 14;
    private static final int HEADER_H = 18;
    private static final int REPORT_ROW_H = 34;
    private static final int CELL = 30;
    private static final int SEARCH_H = 18;
    private static final int DETAIL_TOP_H = 62;

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

        int closeX = panelX + panelW - 22;
        int closeY = panelY + 6;
        this.addRenderableWidget(new InventorySortModalIconButton(closeX, closeY, 16,
                InventorySortModalIconButton.CLOSE, Component.literal("Close"), button -> closeToParent()));

        if (detailMode && selectedReport != null) {
            this.addRenderableWidget(new InventorySortTextButton(panelX + PAD, panelY + 6, 58, 16,
                    Component.literal("Back"), button -> openBrowser()));

            int boxX = gridX;
            int boxY = panelY + 40;
            int boxW = gridW;
            this.searchBox = new EditBox(this.font, boxX + 2, boxY + 5, boxW - 4, 14,
                    Component.literal("Search"));
            this.searchBox.setMaxLength(64);
            this.searchBox.setBordered(false);
            this.searchBox.setTextColor(0xFFE0E0E0);
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
        InventorySortDrawContext context = InventorySortDrawContexts.wrap(g);
        g.fill(0, 0, this.width, this.height, 0x88000000);
        InventorySortUIUtils.drawBeveledPanel(context, panelX, panelY, panelW, panelH, false);

        if (detailMode && selectedReport != null) {
            renderDetail(g, context, mouseX, mouseY);
        } else {
            renderBrowser(g, context, mouseX, mouseY);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void renderBrowser(GuiGraphicsExtractor g, InventorySortDrawContext context, int mouseX, int mouseY) {
        text(g, "Inventory Catalogue Reports", panelX + PAD, panelY + 10, 0xFF1C1C1C);
        text(g, "Saved reports grouped by world", panelX + PAD, panelY + 25, 0xFF555555);

        InventorySortUIUtils.drawRecessedPanel(context, listX, listY, listW, listH);
        g.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);

        if (reportRows.isEmpty()) {
            text(g, "No saved catalogue reports yet.", listX + 10, listY + 12, 0xFFE0E0E0);
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
                    g.fill(listX + 4, y, listX + listW - 4, y + HEADER_H - 2, 0xFF9F9F9F);
                    text(g, displayNamespace(row.namespace), listX + 10, y + 5, 0xFF1C1C1C);
                } else {
                    boolean hovered = mouseX >= listX + 4 && mouseX <= listX + listW - 4
                            && mouseY >= y && mouseY <= y + REPORT_ROW_H - 3;
                    int bg = hovered ? 0xFFE0E0E0 : 0xFFC6C6C6;
                    g.fill(listX + 6, y, listX + listW - 6, y + REPORT_ROW_H - 4, bg);
                    g.fill(listX + 6, y + REPORT_ROW_H - 5, listX + listW - 6, y + REPORT_ROW_H - 4, 0xFF777777);
                    text(g, row.report.displayTime(), listX + 12, y + 5, 0xFF111111);
                    String summary = String.format(Locale.ROOT, "%,d items  |  %,d types  |  %,d locations",
                            row.report.getTotalItems(), row.report.getUniqueItems(), row.report.getLocationCount());
                    text(g, truncate(summary, listW - 32), listX + 12, y + 18, 0xFF555555);
                }
                y += rowH;
            }
        }

        g.disableScissor();
    }

    private void renderDetail(GuiGraphicsExtractor g, InventorySortDrawContext context, int mouseX, int mouseY) {
        text(g, "Catalogue Report", panelX + 80, panelY + 10, 0xFF1C1C1C);
        text(g, truncate(displayNamespace(selectedReport.getNamespace()), panelW - 210), panelX + 80, panelY + 25, 0xFF555555);

        InventorySortUIUtils.drawRecessedPanel(context, gridX, panelY + 40, gridW, SEARCH_H);

        int summaryY = panelY + 40;
        text(g, selectedReport.displayTime(), detailX, summaryY, 0xFF1C1C1C);
        text(g, String.format(Locale.ROOT, "%,d total items", selectedReport.getTotalItems()), detailX, summaryY + 12, 0xFF444444);
        text(g, String.format(Locale.ROOT, "%,d item types, %,d locations",
                selectedReport.getUniqueItems(), selectedReport.getLocationCount()), detailX, summaryY + 24, 0xFF444444);

        InventorySortUIUtils.drawRecessedPanel(context, gridX, gridY, gridW, gridH);
        updateItemHitboxes(mouseX, mouseY);
        renderItemGrid(g, mouseX, mouseY);

        InventorySortUIUtils.drawRecessedPanel(context, detailX, detailY, detailW, detailH);
        renderSelectedItemDetails(g);
    }

    private void renderItemGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.enableScissor(gridX + 1, gridY + 1, gridX + gridW - 1, gridY + gridH - 1);
        int columns = gridColumns();
        int yStart = gridY + 6 - itemScroll;

        if (filteredItems.isEmpty()) {
            text(g, "No items match this report filter.", gridX + 10, gridY + 12, 0xFFE0E0E0);
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
                int bg = selected ? 0xFFD9D0A6 : hovered ? 0xFFE0E0E0 : 0xFFC6C6C6;
                g.fill(x, y, x + CELL - 3, y + CELL - 3, bg);
                g.fill(x, y, x + CELL - 3, y + 1, selected ? 0xFF6C5B1F : 0xFFFFFFFF);
                g.fill(x, y, x + 1, y + CELL - 3, selected ? 0xFF6C5B1F : 0xFFFFFFFF);
                g.fill(x, y + CELL - 4, x + CELL - 3, y + CELL - 3, 0xFF555555);
                g.fill(x + CELL - 4, y, x + CELL - 3, y + CELL - 3, 0xFF555555);

                ItemEntry entry = itemEntry(item.itemId());
                if (!entry.icon.isEmpty()) {
                    g.item(entry.icon, x + 6, y + 3);
                    g.itemDecorations(this.font, entry.icon, x + 6, y + 3);
                } else {
                    text(g, "?", x + 11, y + 7, 0xFF333333);
                }

                String count = compactCount(item.count());
                int countW = this.font.width(count);
                g.fill(x + CELL - 5 - countW, y + CELL - 13, x + CELL - 1, y + CELL - 3, 0xAA000000);
                text(g, count, x + CELL - 3 - countW, y + CELL - 12, 0xFFFFD84D);
            }
        }

        g.disableScissor();
    }

    private void renderSelectedItemDetails(GuiGraphicsExtractor g) {
        if (selectedItemId == null) {
            text(g, "No item selected", detailX + 10, detailY + 10, 0xFFE0E0E0);
            return;
        }

        CatalogReportSnapshot.ItemCount selected = findFilteredItem(selectedItemId);
        if (selected == null) {
            text(g, "No item selected", detailX + 10, detailY + 10, 0xFFE0E0E0);
            return;
        }

        ItemEntry entry = itemEntry(selected.itemId());
        int iconX = detailX + 12;
        int iconY = detailY + 12;
        if (!entry.icon.isEmpty()) {
            g.item(entry.icon, iconX, iconY);
            g.itemDecorations(this.font, entry.icon, iconX, iconY);
        }

        text(g, truncate(entry.displayName, detailW - 48), detailX + 36, detailY + 10, 0xFFEDEDED);
        text(g, truncate(selected.itemId(), detailW - 28), detailX + 12, detailY + 35, 0xFFBDBDBD);
        text(g, String.format(Locale.ROOT, "%,d total", selected.count()), detailX + 12, detailY + 56, 0xFFFFD84D);
        text(g, stackSummary(entry.icon, selected.count()), detailX + 12, detailY + 70, 0xFFE0E0E0);

        int share = selectedReport.getTotalItems() <= 0
                ? 0
                : Math.round((selected.count() * 100.0f) / selectedReport.getTotalItems());
        text(g, share + "% of report total", detailX + 12, detailY + 84, 0xFFBDBDBD);

        text(g, "Report file", detailX + 12, detailY + 112, 0xFF8A8A8A);
        text(g, truncate(selectedReport.getReportFileName(), detailW - 24),
                detailX + 12, detailY + 124, 0xFFE0E0E0);
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
