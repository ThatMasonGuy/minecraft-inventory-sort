package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * In-game InvSort rules editor. The controls are split into two tabs - a
 * spatial "Slots" editor for protected / item-specific slots and an "Order"
 * editor for category and item priority - so each view stays readable. A scope
 * selector at the top keeps the player / container / per-container choice in
 * one obvious place.
 */
public class InventorySortConfigScreen extends Screen {
    private static final int MAX_PANEL_W = 480;
    private static final int MAX_PANEL_H = 290;
    private static final int MIN_PANEL_W = 318;
    private static final int MIN_PANEL_H = 232;
    private static final int PAD = 12;
    private static final int ROW_H = 16;

    private static final int TAB_SLOTS = 0;
    private static final int TAB_ORDER = 1;

    private static final int ACCENT = InvUi.ACCENT_SORT;
    private static final int MARK_PROTECTED = 0xFFD15B4A;
    private static final int MARK_PROTECTED_SOFT = 0x66D15B4A;

    private final AbstractContainerScreen<?> parent;
    private final Player player;
    private final boolean containerTarget;
    private final SortRuleStore store = SortRuleStore.getInstance();
    private final ContainerIdentity containerIdentity;
    private final String screenClassName;
    private final String containerType;
    private final List<InventorySortHitboxButton> slotButtons = new ArrayList<>();
    private final List<InventorySortHitboxButton> rowButtons = new ArrayList<>();
    private final Set<Integer> selectedSlots = new LinkedHashSet<>();
    private boolean editingOverride;
    private boolean showingItems;
    private int tab = TAB_SLOTS;

    private int panelW;
    private int panelH;
    private int panelX;
    private int panelY;
    private int contentTop;
    private int scopeRowY;
    private int tabRowY;

    // Slots tab geometry.
    private int gridX;
    private int gridY;
    private int gridW;
    private int gridH;
    private int slotSize;
    private int actionsY;
    private int infoX;
    private int infoY;
    private int infoW;
    private int infoH;

    // Order tab geometry.
    private int subTabY;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int ctrlX;
    private int ctrlW;

    private int anchorSlot = -1;
    private int categoryIndex = 0;
    private int itemIndex = 0;
    private int rowScroll = 0;

    public InventorySortConfigScreen(AbstractContainerScreen<?> parent, Player player, boolean containerTarget) {
        super(Component.literal("InvSort Rules"));
        this.parent = parent;
        this.player = player;
        this.containerTarget = containerTarget;
        if (parent instanceof InventorySortContainerContext context) {
            this.containerIdentity = context.inventorysort$getContainerIdentity();
            this.screenClassName = context.inventorysort$getScreenClassName();
            this.containerType = context.inventorysort$getContainerType();
        } else {
            this.containerIdentity = null;
            this.screenClassName = parent.getClass().getSimpleName();
            this.containerType = "Container";
        }
        this.editingOverride = containerTarget && containerIdentity != null;
    }

    @Override
    protected void init() {
        computeLayout();
        sanitizeSelection();
        this.clearWidgets();

        this.addRenderableWidget(new InventorySortModalIconButton(panelX + panelW - PAD - 16, panelY + 7, 16,
                InventorySortModalIconButton.CLOSE, Component.literal("Close"), button -> closeToParent()));

        if (containerTarget) {
            addChromeHit(scopeRect(0), () -> setScope(false));
            addChromeHit(scopeRect(1), () -> setScope(true));
        }
        addChromeHit(tabRect(0), () -> setTab(TAB_SLOTS));
        addChromeHit(tabRect(1), () -> setTab(TAB_ORDER));

        if (tab == TAB_SLOTS) {
            addSlotsTabWidgets();
        } else {
            addChromeHit(subTabRect(0), () -> setListMode(false));
            addChromeHit(subTabRect(1), () -> setListMode(true));
            addOrderTabWidgets();
        }
        buildHitboxes();
    }

    private void addChromeHit(int[] r, Runnable action) {
        this.addRenderableWidget(new InventorySortHitboxButton(r[0], r[1], r[2], r[3],
                Component.empty(), button -> action.run()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        InventorySortDrawContext ui = InventorySortDrawContexts.wrap(g);
        InvUi.scrim(ui, this.width, this.height);
        InvUi.window(ui, panelX, panelY, panelW, panelH, ACCENT);

        text(g, "Sorting Rules", panelX + PAD, panelY + 9, InvUi.TEXT);

        renderScopeRow(g, ui, mouseX, mouseY);
        renderTabRow(g, ui, mouseX, mouseY);
        InvUi.divider(ui, panelX + PAD, tabRowY + 21, panelW - PAD * 2);

        if (tab == TAB_SLOTS) {
            renderSlotsTab(g, ui, mouseX, mouseY);
        } else {
            renderOrderTab(g, ui, mouseX, mouseY);
        }

        updateHitboxes();
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    // --------------------------------------------------------------- chrome

    private void renderScopeRow(GuiGraphicsExtractor g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        if (!containerTarget) {
            text(g, "World player inventory", panelX + PAD, scopeRowY + 4, InvUi.TEXT_MUTED);
            return;
        }
        text(g, "Applies to", panelX + PAD, scopeRowY + 4, InvUi.TEXT_DIM);
        for (int i = 0; i < 2; i++) {
            int[] r = scopeRect(i);
            boolean selected = (i == 1) == editingOverride;
            boolean hovered = isInside(mouseX, mouseY, r[0], r[1], r[2], r[3]);
            int color = InvUi.segment(ui, r[0], r[1], r[2], r[3], hovered, selected, ACCENT);
            String label = i == 0 ? "World Containers" : "This Container";
            centeredText(g, label, r[0], r[2], r[1] + 4, color);
        }
    }

    private void renderTabRow(GuiGraphicsExtractor g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        String[] labels = {"Slots", "Order"};
        for (int i = 0; i < 2; i++) {
            int[] r = tabRect(i);
            boolean hovered = isInside(mouseX, mouseY, r[0], r[1], r[2], r[3]);
            int color = InvUi.segment(ui, r[0], r[1], r[2], r[3], hovered, tab == i, ACCENT);
            centeredText(g, labels[i], r[0], r[2], r[1] + 5, color);
        }
    }

    // ----------------------------------------------------------- slots tab

    private void addSlotsTabWidgets() {
        int bw = (gridW - 8) / 3;
        InventorySortTextButton protect = new InventorySortTextButton(gridX, actionsY, bw, 18,
                Component.literal("Protect"), button -> protectSelectedSlots());
        protect.setTooltip(Tooltip.create(Component.literal(
                "Sorting will not move items into or out of the selected slots.")));
        this.addRenderableWidget(protect);

        InventorySortTextButton assign = new InventorySortTextButton(gridX + bw + 4, actionsY, bw, 18,
                Component.literal("Assign Item"), button -> reserveSelectedSlots());
        assign.setTooltip(Tooltip.create(Component.literal(
                "Reserve the selected slots for the held or selected item type.")));
        this.addRenderableWidget(assign);

        InventorySortTextButton clear = new InventorySortTextButton(gridX + (bw + 4) * 2, actionsY,
                gridW - (bw + 4) * 2, 18, Component.literal("Clear"), button -> clearSelectedSlots());
        clear.setTooltip(Tooltip.create(Component.literal("Remove any rule from the selected slots.")));
        this.addRenderableWidget(clear);

        InventorySortTextButton clearAll = new InventorySortTextButton(panelX + panelW - PAD - 78, tabRowY, 78, 18,
                Component.literal("Reset Scope"), button -> clearCurrentRules());
        clearAll.setTooltip(Tooltip.create(Component.literal("Clear every rule for the current scope.")));
        this.addRenderableWidget(clearAll);
    }

    private void renderSlotsTab(GuiGraphicsExtractor g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        List<Slot> slots = targetSlots();
        InvUi.inset(ui, gridX - 5, gridY - 5, gridW, gridH);

        for (int i = 0; i < slots.size(); i++) {
            int x = gridX + (i % 9) * slotSize;
            int y = gridY + (i / 9) * slotSize;
            int draw = slotSize - 2;
            boolean hovered = isInside(mouseX, mouseY, x, y, draw, draw);
            boolean selected = selectedSlots.contains(i);
            SortRuleStore.SlotRule rule = currentRules().ruleFor(i);

            InvUi.slot(ui, x, y, draw, hovered, selected, ACCENT);

            ItemStack stack = slots.get(i).getItem();
            if (!stack.isEmpty()) {
                g.item(stack, x + (draw - 16) / 2, y + (draw - 16) / 2);
                g.itemDecorations(this.font, stack, x + (draw - 16) / 2, y + (draw - 16) / 2);
            }

            if (rule.restricted) {
                ui.fill(x + 1, y + 1, x + draw - 1, y + 2, MARK_PROTECTED);
                ui.fill(x + 1, y + draw - 5, x + 5, y + draw - 1, MARK_PROTECTED);
            } else if (rule.hasReservation()) {
                ui.fill(x + 1, y + 1, x + 6, y + 3, ACCENT);
                ui.fill(x + 1, y + 1, x + 3, y + 6, ACCENT);
            }
        }

        renderSelectionInfo(g, ui);
    }

    private void renderSelectionInfo(GuiGraphicsExtractor g, InventorySortDrawContext ui) {
        InvUi.inset(ui, infoX, infoY, infoW, infoH);
        int tx = infoX + 9;
        int ty = infoY + 9;
        int maxW = infoW - 18;
        int count = selectedSlots.size();

        if (count == 0) {
            text(g, "No slots selected", tx, ty, InvUi.TEXT);
            wrapText(g, "Click a slot to begin. Hold Ctrl to add slots, Shift to pick a range.",
                    tx, ty + 14, maxW, InvUi.TEXT_MUTED, 3);
        } else if (count == 1) {
            int slotIndex = primarySelectedSlot();
            SortRuleStore.SlotRule rule = currentRules().ruleFor(slotIndex);
            ItemStack stack = slotIndex >= 0 && slotIndex < targetSlots().size()
                    ? targetSlots().get(slotIndex).getItem() : ItemStack.EMPTY;
            String item = stack.isEmpty() ? "empty" : InventorySorter.itemId(stack);
            text(g, "Slot " + slotIndex, tx, ty, InvUi.TEXT);
            text(g, truncate("Holds: " + shortItemId(item), maxW), tx, ty + 13, InvUi.TEXT_MUTED);
            String state = rule.restricted ? "Protected from sorting"
                    : rule.hasReservation() ? "Reserved: " + shortItemId(rule.reservedItemId)
                    : "Normal sort slot";
            int color = rule.restricted ? MARK_PROTECTED : rule.hasReservation() ? ACCENT : InvUi.TEXT_DIM;
            text(g, truncate(state, maxW), tx, ty + 26, color);
        } else {
            text(g, count + " slots selected", tx, ty, InvUi.TEXT);
            wrapText(g, "Protect, Assign Item, or Clear will apply to all of them.",
                    tx, ty + 14, maxW, InvUi.TEXT_MUTED, 2);
        }

        // Legend pinned to the bottom of the info panel.
        int ly = infoY + infoH - 30;
        ui.fill(tx, ly, tx + 9, ly + 9, MARK_PROTECTED);
        text(g, "Protected slot", tx + 15, ly + 1, InvUi.TEXT_MUTED);
        int ly2 = ly + 14;
        ui.fill(tx, ly2, tx + 6, ly2 + 2, ACCENT);
        ui.fill(tx, ly2, tx + 2, ly2 + 6, ACCENT);
        text(g, "Item-specific slot", tx + 15, ly2 + 1, InvUi.TEXT_MUTED);
    }

    // ----------------------------------------------------------- order tab

    private void addOrderTabWidgets() {
        InventorySortTextButton clearAll = new InventorySortTextButton(panelX + panelW - PAD - 78, tabRowY, 78, 18,
                Component.literal("Reset Scope"), button -> clearCurrentRules());
        clearAll.setTooltip(Tooltip.create(Component.literal("Clear every rule for the current scope.")));
        this.addRenderableWidget(clearAll);

        if (showingItems) {
            int half = (ctrlW - 4) / 2;
            InventorySortTextButton add = new InventorySortTextButton(ctrlX, listY, ctrlW, 18,
                    Component.literal("Add Selected Item"), button -> addSelectedItemOrder());
            add.setTooltip(Tooltip.create(Component.literal(
                    "Add the held or slot-selected item to the priority list.")));
            this.addRenderableWidget(add);
            this.addRenderableWidget(new InventorySortTextButton(ctrlX, listY + 22, half, 18,
                    Component.literal("Up"), button -> moveItem(-1)));
            this.addRenderableWidget(new InventorySortTextButton(ctrlX + half + 4, listY + 22, ctrlW - half - 4, 18,
                    Component.literal("Down"), button -> moveItem(1)));
            this.addRenderableWidget(new InventorySortTextButton(ctrlX, listY + 44, ctrlW, 18,
                    Component.literal("Remove"), button -> removeItem()));
            InventorySortTextButton exact = new InventorySortTextButton(ctrlX, listY + 70, ctrlW, 18,
                    Component.literal(currentRules().absoluteItemOrder ? "Exact order: On" : "Exact order: Off"),
                    button -> toggleAbsoluteItemOrder());
            exact.setTooltip(Tooltip.create(Component.literal(
                    "Exact order sorts strictly by this list. Off keeps it as a soft priority.")));
            this.addRenderableWidget(exact);
        } else {
            int half = (ctrlW - 4) / 2;
            this.addRenderableWidget(new InventorySortTextButton(ctrlX, listY, half, 18,
                    Component.literal("Up"), button -> moveCategory(-1)));
            this.addRenderableWidget(new InventorySortTextButton(ctrlX + half + 4, listY, ctrlW - half - 4, 18,
                    Component.literal("Down"), button -> moveCategory(1)));
        }
    }

    private void renderOrderTab(GuiGraphicsExtractor g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        String[] labels = {"Categories", "Exact Items"};
        for (int i = 0; i < 2; i++) {
            int[] r = subTabRect(i);
            boolean hovered = isInside(mouseX, mouseY, r[0], r[1], r[2], r[3]);
            boolean selected = (i == 1) == showingItems;
            int color = InvUi.segment(ui, r[0], r[1], r[2], r[3], hovered, selected, ACCENT);
            centeredText(g, labels[i], r[0], r[2], r[1] + 4, color);
        }

        InvUi.inset(ui, listX, listY, listW, listH);
        g.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
        if (showingItems) {
            renderItemList(g, ui, mouseX, mouseY);
        } else {
            renderCategoryList(g, ui, mouseX, mouseY);
        }
        g.disableScissor();
        InvUi.insetBorder(ui, listX, listY, listW, listH);

        renderOrderHelp(g);
    }

    private void renderCategoryList(GuiGraphicsExtractor g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        List<String> order = categoryOrderView();
        int visible = listVisibleRows();
        rowScroll = clamp(rowScroll, 0, Math.max(0, order.size() - visible));
        for (int i = 0; i < visible && i + rowScroll < order.size(); i++) {
            int index = i + rowScroll;
            int y = listY + 5 + i * ROW_H;
            boolean selected = index == categoryIndex;
            boolean hovered = isInside(mouseX, mouseY, listX + 4, y, listW - 8, ROW_H - 1);
            InvUi.row(ui, listX + 4, y, listW - 8, ROW_H - 1, hovered, selected, ACCENT);
            text(g, truncate(categoryLabel(order.get(index)), listW - 20), listX + 12, y + 4,
                    selected ? InvUi.TEXT : InvUi.TEXT_MUTED);
        }
        InvUi.scrollbar(ui, listX + listW - 6, listY + 4, listH - 8, order.size() * ROW_H,
                visible * ROW_H, rowScroll * ROW_H, ACCENT);
    }

    private void renderItemList(GuiGraphicsExtractor g, InventorySortDrawContext ui, int mouseX, int mouseY) {
        List<String> order = currentRules().itemOrder;
        if (order.isEmpty()) {
            text(g, "No pinned items yet", listX + 10, listY + 10, InvUi.TEXT_MUTED);
            wrapText(g, "Select a slot or hold an item, then Add Selected Item.",
                    listX + 10, listY + 24, listW - 20, InvUi.TEXT_DIM, 2);
            return;
        }
        int visible = listVisibleRows();
        rowScroll = clamp(rowScroll, 0, Math.max(0, order.size() - visible));
        for (int i = 0; i < visible && i + rowScroll < order.size(); i++) {
            int index = i + rowScroll;
            int y = listY + 5 + i * ROW_H;
            boolean selected = index == itemIndex;
            boolean hovered = isInside(mouseX, mouseY, listX + 4, y, listW - 8, ROW_H - 1);
            InvUi.row(ui, listX + 4, y, listW - 8, ROW_H - 1, hovered, selected, ACCENT);
            text(g, truncate((index + 1) + ". " + shortItemId(order.get(index)), listW - 20), listX + 12, y + 4,
                    selected ? InvUi.TEXT : InvUi.TEXT_MUTED);
        }
        InvUi.scrollbar(ui, listX + listW - 6, listY + 4, listH - 8, order.size() * ROW_H,
                visible * ROW_H, rowScroll * ROW_H, ACCENT);
    }

    private void renderOrderHelp(GuiGraphicsExtractor g) {
        int hx = ctrlX;
        int hy = showingItems ? listY + 94 : listY + 28;
        if (showingItems) {
            wrapText(g, "Pinned items sort to the front in this order. Turn on Exact order for a strict layout.",
                    hx, hy, ctrlW, InvUi.TEXT_DIM, 5);
        } else {
            wrapText(g, "Reorder the categories items are grouped into when sorting. Switch to Exact Items to pin individual items first.",
                    hx, hy, ctrlW, InvUi.TEXT_DIM, 6);
        }
    }

    // -------------------------------------------------------------- input

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private boolean handleMouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (tab != TAB_ORDER) {
            return false;
        }
        int delta = (int) Math.signum(-verticalAmount);
        if (delta == 0 || !isInside(mouseX, mouseY, listX, listY, listW, listH)) {
            return false;
        }
        int size = showingItems ? currentRules().itemOrder.size() : categoryOrderView().size();
        rowScroll = clamp(rowScroll + delta, 0, Math.max(0, size - listVisibleRows()));
        return true;
    }

    // ----------------------------------------------------------- hitboxes

    private void buildHitboxes() {
        slotButtons.clear();
        rowButtons.clear();

        for (int i = 0; i < targetSlots().size(); i++) {
            InventorySortHitboxButton b = new InventorySortHitboxButton(0, 0, 1, 1, Component.literal("Slot"), hitbox -> {
                String id = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (id != null) {
                    selectSlot(Integer.parseInt(id));
                }
            });
            slotButtons.add(b);
            this.addRenderableWidget(b);
        }

        int rows = Math.max(1, listVisibleRows()) + 2;
        for (int i = 0; i < rows; i++) {
            InventorySortHitboxButton b = new InventorySortHitboxButton(0, 0, 1, 1, Component.literal("Row"), hitbox -> {
                String id = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (id != null) {
                    selectRow(Integer.parseInt(id));
                }
            });
            b.visible = false;
            b.active = false;
            rowButtons.add(b);
            this.addRenderableWidget(b);
        }
        updateHitboxes();
    }

    private void updateHitboxes() {
        List<Slot> slots = targetSlots();
        for (int i = 0; i < slotButtons.size(); i++) {
            InventorySortHitboxButton b = slotButtons.get(i);
            if (tab != TAB_SLOTS || i >= slots.size()) {
                hide(b);
                continue;
            }
            int x = gridX + (i % 9) * slotSize;
            int y = gridY + (i / 9) * slotSize;
            b.setBounds(x, y, slotSize - 2, slotSize - 2);
            b.setTargetId(String.valueOf(i));
            b.visible = true;
            b.active = true;
        }

        int listSize = tab == TAB_ORDER
                ? (showingItems ? currentRules().itemOrder.size() : categoryOrderView().size())
                : 0;
        int visible = listVisibleRows();
        for (int i = 0; i < rowButtons.size(); i++) {
            InventorySortHitboxButton b = rowButtons.get(i);
            int index = i + rowScroll;
            if (tab != TAB_ORDER || i >= visible || index >= listSize) {
                hide(b);
                continue;
            }
            b.setBounds(listX + 4, listY + 5 + i * ROW_H, listW - 8, ROW_H - 1);
            b.setTargetId(String.valueOf(index));
            b.visible = true;
            b.active = true;
        }
    }

    private void hide(InventorySortHitboxButton b) {
        b.visible = false;
        b.active = false;
        b.setTargetId(null);
    }

    private void selectRow(int index) {
        if (showingItems) {
            itemIndex = index;
        } else {
            categoryIndex = index;
        }
    }

    // ------------------------------------------------------------- actions

    private void selectSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= targetSlots().size()) {
            return;
        }
        boolean ctrl = isControlDown();
        boolean shift = isShiftDown();
        if (shift && anchorSlot >= 0) {
            if (!ctrl) {
                selectedSlots.clear();
            }
            int start = Math.min(anchorSlot, slotIndex);
            int end = Math.max(anchorSlot, slotIndex);
            for (int i = start; i <= end; i++) {
                selectedSlots.add(i);
            }
            return;
        }
        if (ctrl) {
            if (!selectedSlots.add(slotIndex)) {
                selectedSlots.remove(slotIndex);
            }
            anchorSlot = slotIndex;
            return;
        }
        selectedSlots.clear();
        selectedSlots.add(slotIndex);
        anchorSlot = slotIndex;
    }

    private void setTab(int next) {
        if (tab == next) {
            return;
        }
        tab = next;
        rowScroll = 0;
        this.rebuildWidgets();
    }

    private void setListMode(boolean items) {
        if (showingItems == items) {
            return;
        }
        showingItems = items;
        rowScroll = 0;
        this.rebuildWidgets();
    }

    private void setScope(boolean override) {
        if (!containerTarget || editingOverride == override) {
            return;
        }
        editingOverride = override;
        this.rebuildWidgets();
    }

    private void clearCurrentRules() {
        if (containerTarget && editingOverride) {
            store.clearContainerOverride(containerIdentity, screenClassName);
            editingOverride = false;
        } else {
            currentRules().clear();
            store.save();
        }
        selectedSlots.clear();
        anchorSlot = -1;
        this.rebuildWidgets();
    }

    private void protectSelectedSlots() {
        if (selectedSlots.isEmpty()) {
            return;
        }
        for (Integer slotIndex : selectedSlots) {
            SortRuleStore.SlotRule rule = currentRules().mutableRuleFor(slotIndex);
            rule.restricted = true;
            rule.reservedItemId = null;
        }
        store.save();
    }

    private void reserveSelectedSlots() {
        if (selectedSlots.isEmpty()) {
            return;
        }
        String itemId = selectedItemId();
        if (itemId == null) {
            return;
        }
        for (Integer slotIndex : selectedSlots) {
            SortRuleStore.SlotRule rule = currentRules().mutableRuleFor(slotIndex);
            rule.restricted = false;
            rule.reservedItemId = itemId;
        }
        store.save();
    }

    private void clearSelectedSlots() {
        if (selectedSlots.isEmpty()) {
            return;
        }
        for (Integer slotIndex : selectedSlots) {
            currentRules().slotRules.remove(slotIndex);
        }
        store.save();
    }

    private void moveCategory(int delta) {
        List<String> order = ensureCustomCategoryOrder();
        int next = categoryIndex + delta;
        if (next < 0 || next >= order.size()) {
            return;
        }
        String value = order.remove(categoryIndex);
        order.add(next, value);
        categoryIndex = next;
        rowScroll = keepVisible(categoryIndex, rowScroll, listVisibleRows());
        store.save();
    }

    private void toggleAbsoluteItemOrder() {
        currentRules().absoluteItemOrder = !currentRules().absoluteItemOrder;
        store.save();
        this.rebuildWidgets();
    }

    private void addSelectedItemOrder() {
        String itemId = selectedItemId();
        if (itemId == null) {
            return;
        }
        List<String> order = currentRules().itemOrder;
        order.remove(itemId);
        order.add(itemId);
        itemIndex = order.size() - 1;
        rowScroll = keepVisible(itemIndex, rowScroll, listVisibleRows());
        store.save();
    }

    private void moveItem(int delta) {
        List<String> order = currentRules().itemOrder;
        if (order.isEmpty()) {
            return;
        }
        itemIndex = clamp(itemIndex, 0, order.size() - 1);
        int next = itemIndex + delta;
        if (next < 0 || next >= order.size()) {
            return;
        }
        String value = order.remove(itemIndex);
        order.add(next, value);
        itemIndex = next;
        rowScroll = keepVisible(itemIndex, rowScroll, listVisibleRows());
        store.save();
    }

    private void removeItem() {
        List<String> order = currentRules().itemOrder;
        if (order.isEmpty()) {
            return;
        }
        itemIndex = clamp(itemIndex, 0, order.size() - 1);
        order.remove(itemIndex);
        itemIndex = clamp(itemIndex, 0, Math.max(0, order.size() - 1));
        rowScroll = keepVisible(itemIndex, rowScroll, listVisibleRows());
        store.save();
    }

    private List<String> ensureCustomCategoryOrder() {
        SortRuleStore.SortRules rules = currentRules();
        if (rules.categoryOrder.isEmpty()) {
            rules.categoryOrder = new ArrayList<>(categoryOrderView());
        }
        return rules.categoryOrder;
    }

    private List<String> categoryOrderView() {
        SortRuleStore.SortRules rules = currentRules();
        if (!rules.categoryOrder.isEmpty()) {
            return rules.categoryOrder;
        }
        List<String> keys = new ArrayList<>();
        for (InventorySorter.CategoryDefinition category : InventorySorter.DEFAULT_CATEGORIES) {
            keys.add(category.key());
        }
        return keys;
    }

    private String selectedItemId() {
        ItemStack carried = parent.getMenu().getCarried();
        if (!carried.isEmpty()) {
            return InventorySorter.itemId(carried);
        }
        for (Integer slotIndex : selectedSlots) {
            if (slotIndex >= 0 && slotIndex < targetSlots().size()) {
                ItemStack stack = targetSlots().get(slotIndex).getItem();
                if (!stack.isEmpty()) {
                    return InventorySorter.itemId(stack);
                }
            }
        }
        return null;
    }

    private List<Slot> targetSlots() {
        if (containerTarget) {
            return InventorySorter.getContainerSlots(parent.getMenu(), parent);
        }
        return InventorySorter.getPlayerMainSlots(parent.getMenu(), player);
    }

    private SortRuleStore.SortRules currentRules() {
        if (!containerTarget) {
            return store.playerRules();
        }
        return editingOverride
                ? store.editableContainerOverride(containerIdentity, screenClassName)
                : store.containerDefaultRules();
    }

    private void closeToParent() {
        MinecraftApiCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    // -------------------------------------------------------------- layout

    private void computeLayout() {
        int availableW = Math.max(1, this.width - 8);
        int availableH = Math.max(1, this.height - 8);
        panelW = clamp(availableW, MIN_PANEL_W, MAX_PANEL_W);
        panelH = clamp(availableH, MIN_PANEL_H, MAX_PANEL_H);
        panelW = Math.min(panelW, Math.max(1, this.width - 4));
        panelH = Math.min(panelH, Math.max(1, this.height - 4));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        scopeRowY = panelY + 25;
        tabRowY = containerTarget ? panelY + 47 : panelY + 43;
        contentTop = tabRowY + 27;

        int contentBottom = panelY + panelH - PAD;
        int rows = targetRows();

        // Slots tab: responsive slot grid on the left, info panel on the right.
        int contentW = panelW - PAD * 2;
        int gridAvailH = contentBottom - (contentTop + 4) - 6 - 18 - 14;
        int byWidth = (contentW * 9 / 20) / 9;
        int byHeight = gridAvailH / Math.max(1, rows);
        slotSize = clamp(Math.min(Math.min(byWidth, byHeight), 20), 18, 20);
        gridX = panelX + PAD + 5;
        gridY = contentTop + 4 + 5;
        gridW = 9 * slotSize + 10;
        gridH = rows * slotSize + 10;
        actionsY = gridY - 5 + gridH + 6;
        infoX = panelX + PAD + gridW + PAD;
        infoY = contentTop + 4;
        infoW = panelX + panelW - PAD - infoX;
        infoH = (actionsY + 18) - infoY;

        // Order tab: list on the left, controls on the right.
        subTabY = contentTop + 2;
        listX = panelX + PAD;
        listY = subTabY + 22;
        listW = clamp((panelW - PAD * 3) * 11 / 20, 150, 280);
        listH = contentBottom - listY;
        ctrlX = listX + listW + PAD;
        ctrlW = panelX + panelW - PAD - ctrlX;
    }

    private int targetRows() {
        return Math.max(1, (targetSlots().size() + 8) / 9);
    }

    private int listVisibleRows() {
        return Math.max(1, (listH - 8) / ROW_H);
    }

    private int primarySelectedSlot() {
        sanitizeSelection();
        return selectedSlots.isEmpty() ? -1 : selectedSlots.iterator().next();
    }

    private void sanitizeSelection() {
        int slotCount = targetSlots().size();
        selectedSlots.removeIf(slot -> slot < 0 || slot >= slotCount);
        if (anchorSlot >= slotCount) {
            anchorSlot = selectedSlots.isEmpty() ? -1 : selectedSlots.iterator().next();
        }
    }

    private int keepVisible(int selectedIndex, int scroll, int visibleRows) {
        if (selectedIndex < scroll) {
            return selectedIndex;
        }
        if (selectedIndex >= scroll + visibleRows) {
            return selectedIndex - visibleRows + 1;
        }
        return scroll;
    }

    // -------------------------------------------------------------- helpers

    private int[] scopeRect(int i) {
        int sx = panelX + PAD + 56;
        int avail = (panelX + panelW - PAD) - sx;
        int segW = Math.min(118, (avail - 4) / 2);
        return new int[]{sx + i * (segW + 4), scopeRowY, segW, 16};
    }

    private int[] tabRect(int i) {
        return new int[]{panelX + PAD + i * 74, tabRowY, 70, 18};
    }

    private int[] subTabRect(int i) {
        return new int[]{panelX + PAD + i * 94, subTabY, 90, 16};
    }

    private static boolean isControlDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean isShiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static boolean isKeyDown(int key) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        return GLFW.glfwGetKey(MinecraftApiCompat.windowHandle(client), key) == GLFW.GLFW_PRESS;
    }

    private void text(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(this.font, text, x, y, color, false);
    }

    private void centeredText(GuiGraphicsExtractor g, String text, int x, int w, int y, int color) {
        g.text(this.font, text, x + (w - this.font.width(text)) / 2, y, color, false);
    }

    private void wrapText(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int color, int maxLines) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int line0 = 0;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (this.font.width(candidate) > maxW && line.length() > 0) {
                text(g, line.toString(), x, y + line0 * 10, color);
                line0++;
                line = new StringBuilder(word);
                if (line0 >= maxLines - 1) {
                    break;
                }
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line0 < maxLines && line.length() > 0) {
            text(g, truncate(line.toString(), maxW), x, y + line0 * 10, color);
        }
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

    private String categoryLabel(String key) {
        for (InventorySorter.CategoryDefinition category : InventorySorter.DEFAULT_CATEGORIES) {
            if (category.key().equals(key)) {
                return category.label();
            }
        }
        return key;
    }

    private static String shortItemId(String itemId) {
        if (itemId == null) {
            return "";
        }
        int colon = itemId.indexOf(':');
        return colon >= 0 ? itemId.substring(colon + 1) : itemId;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
