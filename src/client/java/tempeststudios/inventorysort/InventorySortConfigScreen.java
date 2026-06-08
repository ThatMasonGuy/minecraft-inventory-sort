package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

public class InventorySortConfigScreen extends Screen {
    private static final int MAX_PANEL_W = 520;
    private static final int MAX_PANEL_H = 306;
    private static final int MIN_PANEL_W = 320;
    private static final int MIN_PANEL_H = 236;
    private static final int PAD = 10;
    private static final int ROW_H = 14;
    private static final int BUTTON_H = 18;

    private final AbstractContainerScreen<?> parent;
    private final Player player;
    private final boolean containerTarget;
    private final SortRuleStore store = SortRuleStore.getInstance();
    private final ContainerIdentity containerIdentity;
    private final String screenClassName;
    private final String containerType;
    private final List<InventorySortHitboxButton> slotButtons = new ArrayList<>();
    private final List<InventorySortHitboxButton> categoryButtons = new ArrayList<>();
    private final List<InventorySortHitboxButton> itemButtons = new ArrayList<>();
    private final Set<Integer> selectedSlots = new LinkedHashSet<>();
    private boolean editingOverride;
    private boolean showingItems;

    private int panelW;
    private int panelH;
    private int panelX;
    private int panelY;
    private int leftW;
    private int rightX;
    private int rightY;
    private int rightW;
    private int rightH;
    private int slotGridX;
    private int slotGridY;
    private int slotSize;
    private int detailY;
    private int anchorSlot = -1;
    private int categoryIndex = 0;
    private int itemIndex = 0;
    private int categoryScroll = 0;
    private int itemScroll = 0;

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

        this.addRenderableWidget(new InventorySortTextButton(panelX + panelW - 62, panelY + 8, 52, BUTTON_H,
                Component.literal("Done"), button -> closeToParent()));

        int topButtonY = panelY + 48;
        int scopeW = Math.min(116, Math.max(90, leftW / 2 - 4));
        this.addRenderableWidget(new InventorySortTextButton(slotGridX, topButtonY, scopeW, BUTTON_H,
                Component.literal(scopeButtonLabel()), button -> toggleScope()));
        this.addRenderableWidget(new InventorySortTextButton(slotGridX + scopeW + 6, topButtonY, 76, BUTTON_H,
                Component.literal("Clear All"), button -> clearCurrentRules()));

        int modeW = Math.max(48, (rightW - 4) / 2);
        this.addRenderableWidget(new InventorySortTextButton(rightX, topButtonY, modeW, BUTTON_H,
                Component.literal("Category"), button -> setListMode(false)));
        this.addRenderableWidget(new InventorySortTextButton(rightX + modeW + 4, topButtonY, rightW - modeW - 4, BUTTON_H,
                Component.literal("Items"), button -> setListMode(true)));

        int slotActionY = panelY + panelH - 24;
        this.addRenderableWidget(new InventorySortTextButton(slotGridX, slotActionY, 58, BUTTON_H,
                Component.literal("Protect"), button -> protectSelectedSlots()));
        this.addRenderableWidget(new InventorySortTextButton(slotGridX + 64, slotActionY, 66, BUTTON_H,
                Component.literal("Item Slot"), button -> reserveSelectedSlots()));
        this.addRenderableWidget(new InventorySortTextButton(slotGridX + 136, slotActionY, 46, BUTTON_H,
                Component.literal("Clear"), button -> clearSelectedSlots()));

        addRightControls();
        buildHitboxes();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x99000000);
        drawPanel(g, panelX, panelY, panelW, panelH);
        text(g, "InvSort Rules", panelX + PAD, panelY + 10, 0xFFE8E8E8);
        text(g, truncate(scopeLabel(), panelW - 86), panelX + PAD, panelY + 24, 0xFFB9B9B9);
        text(g, truncate("Ctrl-click adds. Shift-click selects a range.", panelW - 86), panelX + PAD, panelY + 36, 0xFF8F958A);

        renderSlotGrid(g, mouseX, mouseY);
        renderSlotDetails(g);
        renderRuleList(g, mouseX, mouseY);

        updateHitboxes();
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void addRightControls() {
        if (showingItems) {
            int rowOneY = panelY + panelH - 44;
            int rowTwoY = panelY + panelH - 24;
            int halfW = (rightW - 4) / 2;
            this.addRenderableWidget(new InventorySortTextButton(rightX, rowOneY, halfW, BUTTON_H,
                    Component.literal("Add Item"), button -> addSelectedItemOrder()));
            this.addRenderableWidget(new InventorySortTextButton(rightX + halfW + 4, rowOneY, rightW - halfW - 4, BUTTON_H,
                    Component.literal("Remove"), button -> removeItem()));

            this.addRenderableWidget(new InventorySortTextButton(rightX, rowTwoY, 30, BUTTON_H,
                    Component.literal("Up"), button -> moveItem(-1)));
            this.addRenderableWidget(new InventorySortTextButton(rightX + 34, rowTwoY, 40, BUTTON_H,
                    Component.literal("Down"), button -> moveItem(1)));
            this.addRenderableWidget(new InventorySortTextButton(rightX + 78, rowTwoY, rightW - 78, BUTTON_H,
                    Component.literal("Exact"), button -> toggleAbsoluteItemOrder()));
        } else {
            int y = panelY + panelH - 24;
            int halfW = (rightW - 4) / 2;
            this.addRenderableWidget(new InventorySortTextButton(rightX, y, halfW, BUTTON_H,
                    Component.literal("Up"), button -> moveCategory(-1)));
            this.addRenderableWidget(new InventorySortTextButton(rightX + halfW + 4, y, rightW - halfW - 4, BUTTON_H,
                    Component.literal("Down"), button -> moveCategory(1)));
        }
    }

    private void renderSlotGrid(GuiGraphics g, int mouseX, int mouseY) {
        List<Slot> slots = targetSlots();
        int rows = targetRows();
        drawPanel(g, slotGridX - 6, slotGridY - 6, 9 * slotSize + 12, rows * slotSize + 12, 0xFF111111);

        for (int i = 0; i < slots.size(); i++) {
            int x = slotGridX + (i % 9) * slotSize;
            int y = slotGridY + (i / 9) * slotSize;
            boolean hovered = isInside(mouseX, mouseY, x, y, slotSize - 2, slotSize - 2);
            boolean selected = selectedSlots.contains(i);
            SortRuleStore.SlotRule rule = currentRules().ruleFor(i);
            int fill = selected ? 0xFFE3D48A : hovered ? 0xFFA7A7A7 : 0xFF858585;
            g.fill(x, y, x + slotSize - 2, y + slotSize - 2, 0xFF070707);
            g.fill(x + 1, y + 1, x + slotSize - 3, y + slotSize - 3, fill);

            ItemStack stack = slots.get(i).getItem();
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 1, y + 1);
                g.renderItemDecorations(this.font, stack, x + 1, y + 1);
            }

            if (rule.restricted) {
                g.fill(x + 1, y + 1, x + slotSize - 3, y + slotSize - 3, 0x88AA2020);
                text(g, "P", x + 6, y + 5, 0xFFFFFFFF);
            } else if (rule.hasReservation()) {
                g.fill(x, y, x + slotSize - 2, y + 2, 0xFFFFD95A);
                g.fill(x, y, x + 2, y + slotSize - 2, 0xFFFFD95A);
                text(g, "I", x + 7, y + 5, 0xFFFFD95A);
            }
        }
    }

    private void renderSlotDetails(GuiGraphics g) {
        int x = slotGridX;
        int y = Math.min(detailY, panelY + panelH - 58);
        int maxW = Math.max(120, rightX - slotGridX - PAD);
        int selectedCount = selectedSlots.size();
        if (selectedCount == 0) {
            text(g, "No slot selected", x, y, 0xFFE8E8E8);
            text(g, truncate("Protect keeps slots untouched. Item Slot reserves an item.", maxW), x, y + 13, 0xFFB9B9B9);
            return;
        }
        if (selectedCount > 1) {
            text(g, selectedCount + " slots selected", x, y, 0xFFE8E8E8);
            text(g, truncate("Protect, Item Slot, or Clear applies to all selected slots.", maxW), x, y + 13, 0xFFB9B9B9);
            return;
        }

        int slotIndex = primarySelectedSlot();
        if (slotIndex < 0 || slotIndex >= targetSlots().size()) {
            return;
        }
        SortRuleStore.SlotRule rule = currentRules().ruleFor(slotIndex);
        Slot slot = targetSlots().get(slotIndex);
        String item = slot.getItem().isEmpty() ? "empty" : InventorySorter.itemId(slot.getItem());
        text(g, "Slot " + slotIndex, x, y, 0xFFE8E8E8);
        text(g, truncate("Current: " + item, maxW), x, y + 13, 0xFFB9B9B9);
        String state = rule.restricted
                ? "Protected from sorting"
                : rule.hasReservation() ? "Item Slot: " + rule.reservedItemId : "Normal sort slot";
        int color = rule.restricted ? 0xFFFF8A8A : rule.hasReservation() ? 0xFFFFD95A : 0xFFB9B9B9;
        text(g, truncate(state, maxW), x, y + 26, color);
    }

    private void renderRuleList(GuiGraphics g, int mouseX, int mouseY) {
        String title = showingItems
                ? currentRules().absoluteItemOrder ? "Items - Exact" : "Items - Flexible"
                : "Categories";
        text(g, title, rightX, rightY - 14, 0xFFE8E8E8);
        drawPanel(g, rightX - 4, rightY - 4, rightW + 8, rightH + 8, 0xFF111111);
        if (showingItems) {
            renderItemOrderList(g, mouseX, mouseY);
        } else {
            renderCategoryList(g, mouseX, mouseY);
        }
    }

    private void renderCategoryList(GuiGraphics g, int mouseX, int mouseY) {
        List<String> order = categoryOrderView();
        int visible = listVisibleRows();
        categoryScroll = clamp(categoryScroll, 0, Math.max(0, order.size() - visible));
        for (int i = 0; i < visible && i + categoryScroll < order.size(); i++) {
            int index = i + categoryScroll;
            int rowY = rightY + i * ROW_H;
            boolean selected = index == categoryIndex;
            if (selected || isInside(mouseX, mouseY, rightX, rowY, rightW, ROW_H)) {
                g.fill(rightX, rowY, rightX + rightW, rowY + ROW_H - 1, selected ? 0xFF3A3520 : 0xFF242424);
            }
            text(g, truncate(categoryLabel(order.get(index)), rightW - 6), rightX + 3, rowY + 3,
                    selected ? 0xFFFFD95A : 0xFFB9B9B9);
        }
    }

    private void renderItemOrderList(GuiGraphics g, int mouseX, int mouseY) {
        List<String> order = currentRules().itemOrder;
        int visible = listVisibleRows();
        itemScroll = clamp(itemScroll, 0, Math.max(0, order.size() - visible));
        if (order.isEmpty()) {
            text(g, "No custom items", rightX + 3, rightY + 3, 0xFF8E8E8E);
            text(g, truncate("Select a slot, then Add Item.", rightW - 6), rightX + 3, rightY + 16, 0xFF8E8E8E);
            return;
        }
        for (int i = 0; i < visible && i + itemScroll < order.size(); i++) {
            int index = i + itemScroll;
            int rowY = rightY + i * ROW_H;
            boolean selected = index == itemIndex;
            if (selected || isInside(mouseX, mouseY, rightX, rowY, rightW, ROW_H)) {
                g.fill(rightX, rowY, rightX + rightW, rowY + ROW_H - 1, selected ? 0xFF3A3520 : 0xFF242424);
            }
            text(g, truncate(shortItemId(order.get(index)), rightW - 6), rightX + 3, rowY + 3,
                    selected ? 0xFFFFD95A : 0xFFB9B9B9);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private boolean handleMouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        int delta = (int) Math.signum(-verticalAmount);
        if (delta == 0 || !isInside(mouseX, mouseY, rightX - 4, rightY - 4, rightW + 8, rightH + 8)) {
            return false;
        }
        int visible = listVisibleRows();
        if (showingItems) {
            itemScroll = clamp(itemScroll + delta, 0, Math.max(0, currentRules().itemOrder.size() - visible));
        } else {
            categoryScroll = clamp(categoryScroll + delta, 0, Math.max(0, categoryOrderView().size() - visible));
        }
        return true;
    }

    private void buildHitboxes() {
        slotButtons.clear();
        for (int i = 0; i < targetSlots().size(); i++) {
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, slotSize - 2, slotSize - 2,
                    Component.literal("Slot"), hitbox -> {
                String targetId = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (targetId != null) {
                    selectSlot(Integer.parseInt(targetId));
                }
            });
            slotButtons.add(button);
            this.addRenderableWidget(button);
        }

        int visibleRows = listVisibleRows();
        categoryButtons.clear();
        for (int i = 0; i < visibleRows; i++) {
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, rightW, ROW_H,
                    Component.literal("Category"), hitbox -> {
                String targetId = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (targetId != null) {
                    categoryIndex = Integer.parseInt(targetId);
                }
            });
            categoryButtons.add(button);
            this.addRenderableWidget(button);
        }

        itemButtons.clear();
        for (int i = 0; i < visibleRows; i++) {
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, rightW, ROW_H,
                    Component.literal("Item"), hitbox -> {
                String targetId = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (targetId != null) {
                    itemIndex = Integer.parseInt(targetId);
                }
            });
            itemButtons.add(button);
            this.addRenderableWidget(button);
        }

        updateHitboxes();
    }

    private void updateHitboxes() {
        List<Slot> slots = targetSlots();
        for (int i = 0; i < slotButtons.size(); i++) {
            InventorySortHitboxButton button = slotButtons.get(i);
            if (i >= slots.size()) {
                hideButton(button);
                continue;
            }
            int x = slotGridX + (i % 9) * slotSize;
            int y = slotGridY + (i / 9) * slotSize;
            button.setBounds(x, y, slotSize - 2, slotSize - 2);
            button.setTargetId(String.valueOf(i));
            button.visible = true;
            button.active = true;
        }

        List<String> categoryOrder = categoryOrderView();
        for (int i = 0; i < categoryButtons.size(); i++) {
            InventorySortHitboxButton button = categoryButtons.get(i);
            int index = i + categoryScroll;
            if (showingItems || index >= categoryOrder.size()) {
                hideButton(button);
                continue;
            }
            button.setBounds(rightX, rightY + i * ROW_H, rightW, ROW_H);
            button.setTargetId(String.valueOf(index));
            button.visible = true;
            button.active = true;
        }

        List<String> itemOrder = currentRules().itemOrder;
        for (int i = 0; i < itemButtons.size(); i++) {
            InventorySortHitboxButton button = itemButtons.get(i);
            int index = i + itemScroll;
            if (!showingItems || index >= itemOrder.size()) {
                hideButton(button);
                continue;
            }
            button.setBounds(rightX, rightY + i * ROW_H, rightW, ROW_H);
            button.setTargetId(String.valueOf(index));
            button.visible = true;
            button.active = true;
        }
    }

    private void hideButton(InventorySortHitboxButton button) {
        button.visible = false;
        button.active = false;
        button.setTargetId(null);
    }

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
            if (selectedSlots.contains(slotIndex)) {
                selectedSlots.remove(slotIndex);
            } else {
                selectedSlots.add(slotIndex);
            }
            anchorSlot = slotIndex;
            return;
        }

        selectedSlots.clear();
        selectedSlots.add(slotIndex);
        anchorSlot = slotIndex;
    }

    private void setListMode(boolean items) {
        if (showingItems == items) {
            return;
        }
        showingItems = items;
        this.rebuildWidgets();
    }

    private void toggleScope() {
        if (!containerTarget) {
            return;
        }
        editingOverride = !editingOverride;
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
        categoryScroll = keepVisible(categoryIndex, categoryScroll, listVisibleRows());
        store.save();
    }

    private void toggleAbsoluteItemOrder() {
        currentRules().absoluteItemOrder = !currentRules().absoluteItemOrder;
        store.save();
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
        itemScroll = keepVisible(itemIndex, itemScroll, listVisibleRows());
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
        itemScroll = keepVisible(itemIndex, itemScroll, listVisibleRows());
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
        itemScroll = keepVisible(itemIndex, itemScroll, listVisibleRows());
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

    private String scopeLabel() {
        if (!containerTarget) {
            return "Global player inventory";
        }
        if (editingOverride) {
            return "This container: " + store.containerOverrideLabel(containerIdentity, screenClassName);
        }
        return "All containers" + (containerType == null ? "" : ": " + containerType);
    }

    private String scopeButtonLabel() {
        if (!containerTarget) {
            return "Player Rules";
        }
        return editingOverride ? "This Container" : "All Containers";
    }

    private void closeToParent() {
        MinecraftApiCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    private void computeLayout() {
        int availableW = Math.max(1, this.width - 12);
        int availableH = Math.max(1, this.height - 12);
        panelW = Math.min(MAX_PANEL_W, Math.max(MIN_PANEL_W, availableW));
        panelH = Math.min(MAX_PANEL_H, Math.max(MIN_PANEL_H, availableH));
        panelW = Math.min(panelW, Math.max(1, this.width - 4));
        panelH = Math.min(panelH, Math.max(1, this.height - 4));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        rightW = clamp(panelW / 3, 118, 154);
        leftW = panelW - rightW - PAD * 3;
        if (leftW < 196) {
            rightW = Math.max(104, panelW - 196 - PAD * 3);
            leftW = panelW - rightW - PAD * 3;
        }

        slotGridX = panelX + PAD;
        slotGridY = panelY + 76;
        int rows = targetRows();
        int slotFromWidth = Math.max(18, (leftW - 12) / 9);
        int slotFromHeight = Math.max(18, (panelH - 138) / Math.max(1, rows));
        slotSize = clamp(Math.min(slotFromWidth, slotFromHeight), 18, 20);

        rightX = panelX + panelW - PAD - rightW;
        rightY = slotGridY;
        int controlsH = showingItems ? 48 : 28;
        rightH = Math.max(56, panelY + panelH - controlsH - rightY - 8);
        detailY = slotGridY + rows * slotSize + 10;
    }

    private int targetRows() {
        return Math.max(1, (targetSlots().size() + 8) / 9);
    }

    private int listVisibleRows() {
        return Math.max(1, rightH / ROW_H);
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

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, 0xF0202020);
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, 0xFF070707);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF777B72);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
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
