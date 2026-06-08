package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

import java.util.ArrayList;
import java.util.List;

public class InventorySortConfigScreen extends Screen {
    private static final int PANEL_W = 540;
    private static final int PANEL_H = 318;
    private static final int PAD = 12;
    private static final int SLOT = 20;
    private static final int ROW_H = 14;

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
    private boolean editingOverride;

    private int panelW;
    private int panelH;
    private int panelX;
    private int panelY;
    private int slotGridX;
    private int slotGridY;
    private int selectedSlot = -1;
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
        this.clearWidgets();

        int buttonY = panelY + panelH - 28;
        this.addRenderableWidget(new InventorySortTextButton(panelX + panelW - 64, panelY + 8, 52, 18,
                Component.literal("Done"), button -> closeToParent()));
        this.addRenderableWidget(new InventorySortTextButton(panelX + PAD, buttonY, 52, 18,
                Component.literal("Lock"), button -> toggleSelectedLock()));
        this.addRenderableWidget(new InventorySortTextButton(panelX + PAD + 58, buttonY, 68, 18,
                Component.literal("Reserve"), button -> reserveSelectedSlot()));
        this.addRenderableWidget(new InventorySortTextButton(panelX + PAD + 132, buttonY, 68, 18,
                Component.literal("Clear Slot"), button -> clearSelectedSlot()));

        int rightX = panelX + 292;
        this.addRenderableWidget(new InventorySortTextButton(rightX, buttonY, 54, 18,
                Component.literal("Cat Up"), button -> moveCategory(-1)));
        this.addRenderableWidget(new InventorySortTextButton(rightX + 60, buttonY, 62, 18,
                Component.literal("Cat Down"), button -> moveCategory(1)));

        this.addRenderableWidget(new InventorySortTextButton(rightX + 130, buttonY, 56, 18,
                Component.literal("Add Item"), button -> addSelectedItemOrder()));
        this.addRenderableWidget(new InventorySortTextButton(rightX + 192, buttonY, 34, 18,
                Component.literal("Up"), button -> moveItem(-1)));

        int topButtonY = panelY + 34;
        if (containerTarget) {
            this.addRenderableWidget(new InventorySortTextButton(panelX + PAD, topButtonY, 96, 18,
                    Component.literal(editingOverride ? overrideScopeName() : "Container Default"),
                    button -> toggleScope()));
            this.addRenderableWidget(new InventorySortTextButton(panelX + PAD + 102, topButtonY, 80, 18,
                    Component.literal("Clear Rules"), button -> clearCurrentRules()));
        } else {
            this.addRenderableWidget(new InventorySortTextButton(panelX + PAD, topButtonY, 96, 18,
                    Component.literal("Player Rules"), button -> {
                    }));
            this.addRenderableWidget(new InventorySortTextButton(panelX + PAD + 102, topButtonY, 80, 18,
                    Component.literal("Clear Rules"), button -> clearCurrentRules()));
        }

        this.addRenderableWidget(new InventorySortTextButton(rightX, topButtonY, 104, 18,
                Component.literal(currentRules().absoluteItemOrder ? "Exact Items: On" : "Exact Items: Off"),
                button -> toggleAbsoluteItemOrder()));
        this.addRenderableWidget(new InventorySortTextButton(rightX + 112, topButtonY, 54, 18,
                Component.literal("Item Dn"), button -> moveItem(1)));
        this.addRenderableWidget(new InventorySortTextButton(rightX + 172, topButtonY, 54, 18,
                Component.literal("Remove"), button -> removeItem()));

        buildHitboxes();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x99000000);
        drawPanel(g, panelX, panelY, panelW, panelH);
        text(g, "InvSort Rules", panelX + PAD, panelY + 10, 0xFFE8E8E8);
        text(g, scopeLabel(), panelX + PAD, panelY + 24, 0xFFB9B9B9);

        renderSlotGrid(g, mouseX, mouseY);
        renderSlotDetails(g);
        renderCategoryList(g, mouseX, mouseY);
        renderItemOrderList(g, mouseX, mouseY);

        updateHitboxes(mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderSlotGrid(GuiGraphics g, int mouseX, int mouseY) {
        List<Slot> slots = targetSlots();
        int rows = Math.max(1, (slots.size() + 8) / 9);
        drawPanel(g, slotGridX - 6, slotGridY - 6, 9 * SLOT + 12, rows * SLOT + 12, 0xFF111111);

        for (int i = 0; i < slots.size(); i++) {
            int x = slotGridX + (i % 9) * SLOT;
            int y = slotGridY + (i / 9) * SLOT;
            boolean hovered = isInside(mouseX, mouseY, x, y, SLOT - 2, SLOT - 2);
            boolean selected = i == selectedSlot;
            SortRuleStore.SlotRule rule = currentRules().ruleFor(i);
            int fill = selected ? 0xFFE3D48A : hovered ? 0xFFA7A7A7 : 0xFF858585;
            g.fill(x, y, x + SLOT - 2, y + SLOT - 2, 0xFF070707);
            g.fill(x + 1, y + 1, x + SLOT - 3, y + SLOT - 3, fill);

            ItemStack stack = slots.get(i).getItem();
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 1, y + 1);
                g.renderItemDecorations(this.font, stack, x + 1, y + 1);
            }

            if (rule.restricted) {
                g.fill(x + 1, y + 1, x + SLOT - 3, y + SLOT - 3, 0x88AA2020);
                text(g, "L", x + 6, y + 5, 0xFFFFFFFF);
            } else if (rule.hasReservation()) {
                g.fill(x, y, x + SLOT - 2, y + 2, 0xFFFFD95A);
                g.fill(x, y, x + 2, y + SLOT - 2, 0xFFFFD95A);
                text(g, "R", x + 6, y + 5, 0xFFFFD95A);
            }
        }
    }

    private void renderSlotDetails(GuiGraphics g) {
        int x = panelX + PAD;
        int y = slotGridY + 146;
        text(g, "Slot " + (selectedSlot < 0 ? "-" : selectedSlot), x, y, 0xFFE8E8E8);
        if (selectedSlot < 0 || selectedSlot >= targetSlots().size()) {
            text(g, "Select a slot to edit its lock or reservation.", x, y + 13, 0xFFB9B9B9);
            return;
        }
        SortRuleStore.SlotRule rule = currentRules().ruleFor(selectedSlot);
        Slot slot = targetSlots().get(selectedSlot);
        String item = slot.getItem().isEmpty() ? "empty" : InventorySorter.itemId(slot.getItem());
        text(g, truncate("Current: " + item, 250), x, y + 13, 0xFFB9B9B9);
        String state = rule.restricted
                ? "Locked"
                : rule.hasReservation() ? "Reserved: " + rule.reservedItemId : "Normal";
        text(g, truncate(state, 250), x, y + 26, rule.restricted ? 0xFFFF8A8A : rule.hasReservation() ? 0xFFFFD95A : 0xFFB9B9B9);
    }

    private void renderCategoryList(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 292;
        int y = panelY + 62;
        text(g, "Categories", x, y - 12, 0xFFE8E8E8);
        drawPanel(g, x - 4, y - 4, 112, 168, 0xFF111111);
        List<String> order = categoryOrderView();
        int visible = 11;
        categoryScroll = clamp(categoryScroll, 0, Math.max(0, order.size() - visible));
        for (int i = 0; i < visible && i + categoryScroll < order.size(); i++) {
            int index = i + categoryScroll;
            int rowY = y + i * ROW_H;
            boolean selected = index == categoryIndex;
            if (selected || isInside(mouseX, mouseY, x, rowY, 104, ROW_H)) {
                g.fill(x, rowY, x + 104, rowY + ROW_H - 1, selected ? 0xFF3A3520 : 0xFF242424);
            }
            text(g, truncate(categoryLabel(order.get(index)), 100), x + 3, rowY + 3, selected ? 0xFFFFD95A : 0xFFB9B9B9);
        }
    }

    private void renderItemOrderList(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 412;
        int y = panelY + 62;
        text(g, "Items", x, y - 12, 0xFFE8E8E8);
        drawPanel(g, x - 4, y - 4, 104, 168, 0xFF111111);
        List<String> order = currentRules().itemOrder;
        int visible = 11;
        itemScroll = clamp(itemScroll, 0, Math.max(0, order.size() - visible));
        if (order.isEmpty()) {
            text(g, "No item order", x + 3, y + 3, 0xFF8E8E8E);
            return;
        }
        for (int i = 0; i < visible && i + itemScroll < order.size(); i++) {
            int index = i + itemScroll;
            int rowY = y + i * ROW_H;
            boolean selected = index == itemIndex;
            if (selected || isInside(mouseX, mouseY, x, rowY, 96, ROW_H)) {
                g.fill(x, rowY, x + 96, rowY + ROW_H - 1, selected ? 0xFF3A3520 : 0xFF242424);
            }
            text(g, truncate(shortItemId(order.get(index)), 92), x + 3, rowY + 3, selected ? 0xFFFFD95A : 0xFFB9B9B9);
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
        if (delta == 0) {
            return false;
        }
        if (isInside(mouseX, mouseY, panelX + 288, panelY + 58, 116, 172)) {
            categoryScroll = clamp(categoryScroll + delta, 0, Math.max(0, categoryOrderView().size() - 11));
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 408, panelY + 58, 108, 172)) {
            itemScroll = clamp(itemScroll + delta, 0, Math.max(0, currentRules().itemOrder.size() - 11));
            return true;
        }
        return false;
    }

    private void buildHitboxes() {
        slotButtons.clear();
        for (int i = 0; i < targetSlots().size(); i++) {
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, SLOT - 2, SLOT - 2,
                    Component.literal("Slot"), hitbox -> {
                String targetId = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (targetId != null) {
                    selectedSlot = Integer.parseInt(targetId);
                }
            });
            slotButtons.add(button);
            this.addRenderableWidget(button);
        }

        categoryButtons.clear();
        for (int i = 0; i < 11; i++) {
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, 104, ROW_H,
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
        for (int i = 0; i < 11; i++) {
            InventorySortHitboxButton button = new InventorySortHitboxButton(0, 0, 96, ROW_H,
                    Component.literal("Item"), hitbox -> {
                String targetId = ((InventorySortHitboxButton) hitbox).getTargetId();
                if (targetId != null) {
                    itemIndex = Integer.parseInt(targetId);
                }
            });
            itemButtons.add(button);
            this.addRenderableWidget(button);
        }

        updateHitboxes(0, 0);
    }

    private void updateHitboxes(int mouseX, int mouseY) {
        List<Slot> slots = targetSlots();
        for (int i = 0; i < slotButtons.size(); i++) {
            InventorySortHitboxButton button = slotButtons.get(i);
            if (i >= slots.size()) {
                button.visible = false;
                button.active = false;
                button.setTargetId(null);
                continue;
            }
            int x = slotGridX + (i % 9) * SLOT;
            int y = slotGridY + (i / 9) * SLOT;
            button.setBounds(x, y, SLOT - 2, SLOT - 2);
            button.setTargetId(String.valueOf(i));
            button.visible = true;
            button.active = true;
        }

        int categoryX = panelX + 292;
        int categoryY = panelY + 62;
        List<String> categoryOrder = categoryOrderView();
        for (int i = 0; i < categoryButtons.size(); i++) {
            InventorySortHitboxButton button = categoryButtons.get(i);
            int index = i + categoryScroll;
            if (index >= categoryOrder.size()) {
                button.visible = false;
                button.active = false;
                button.setTargetId(null);
                continue;
            }
            button.setBounds(categoryX, categoryY + i * ROW_H, 104, ROW_H);
            button.setTargetId(String.valueOf(index));
            button.visible = true;
            button.active = true;
        }

        int itemX = panelX + 412;
        int itemY = panelY + 62;
        List<String> itemOrder = currentRules().itemOrder;
        for (int i = 0; i < itemButtons.size(); i++) {
            InventorySortHitboxButton button = itemButtons.get(i);
            int index = i + itemScroll;
            if (index >= itemOrder.size()) {
                button.visible = false;
                button.active = false;
                button.setTargetId(null);
                continue;
            }
            button.setBounds(itemX, itemY + i * ROW_H, 96, ROW_H);
            button.setTargetId(String.valueOf(index));
            button.visible = true;
            button.active = true;
        }
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
        this.rebuildWidgets();
    }

    private void toggleSelectedLock() {
        if (selectedSlot < 0) {
            return;
        }
        SortRuleStore.SlotRule rule = currentRules().mutableRuleFor(selectedSlot);
        rule.restricted = !rule.restricted;
        if (rule.restricted) {
            rule.reservedItemId = null;
        }
        currentRules().cleanupSlotRule(selectedSlot);
        store.save();
    }

    private void reserveSelectedSlot() {
        if (selectedSlot < 0 || selectedSlot >= targetSlots().size()) {
            return;
        }
        String itemId = selectedItemId();
        if (itemId == null) {
            return;
        }
        SortRuleStore.SlotRule rule = currentRules().mutableRuleFor(selectedSlot);
        rule.restricted = false;
        rule.reservedItemId = itemId;
        store.save();
    }

    private void clearSelectedSlot() {
        if (selectedSlot < 0) {
            return;
        }
        currentRules().slotRules.remove(selectedSlot);
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
        if (selectedSlot >= 0 && selectedSlot < targetSlots().size()) {
            ItemStack stack = targetSlots().get(selectedSlot).getItem();
            if (!stack.isEmpty()) {
                return InventorySorter.itemId(stack);
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
            return "Override: " + store.containerOverrideLabel(containerIdentity, screenClassName);
        }
        return "Global container default" + (containerType == null ? "" : " - " + containerType);
    }

    private String overrideScopeName() {
        return containerIdentity == null ? "This Screen" : "This Container";
    }

    private void closeToParent() {
        MinecraftApiCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    private void computeLayout() {
        panelW = Math.min(PANEL_W, Math.max(360, this.width - 20));
        panelH = Math.min(PANEL_H, Math.max(260, this.height - 20));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        slotGridX = panelX + PAD;
        slotGridY = panelY + 68;
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
