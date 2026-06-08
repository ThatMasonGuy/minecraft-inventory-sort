package tempeststudios.inventorysort;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

import java.util.List;

public class ServerWorldProfileScreen extends Screen {
    private static final int PAD = 12;
    private static final int ACCENT = InvUi.ACCENT_SEARCH;
    private static final int ROW_STRIDE = 20;

    private final Screen parent;
    private final boolean requiresConfirmation;
    private EditBox profileBox;
    private String serverKey;
    private String activeProfile;
    private int scrollOffset = 0;

    // Layout (computed in init, read in render).
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int fieldY;
    private int labelY;
    private int listY;
    private int listBottom;
    private int rowsX;
    private int rowsW;
    private int wellX;
    private int wellW;
    private int railX;
    private int visibleCount;
    private boolean scrollable;
    private int bottomBarY;
    private int hintH;

    public ServerWorldProfileScreen(Screen parent) {
        this(parent, false);
    }

    public ServerWorldProfileScreen(Screen parent, boolean requiresConfirmation) {
        super(Component.literal("Tracked World"));
        this.parent = parent;
        this.requiresConfirmation = requiresConfirmation;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        serverKey = TrackingNamespace.currentServerKey(mc);
        activeProfile = serverKey == null ? null : ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);

        panelW = Math.min(322, this.width - 24);
        panelH = Math.min(244, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        fieldY = panelY + 38;
        labelY = panelY + 62;
        listY = panelY + 74;
        bottomBarY = panelY + panelH - PAD - 18;
        hintH = (requiresConfirmation && serverKey != null) ? 13 : 0;
        listBottom = bottomBarY - 6 - hintH;

        List<String> profiles = serverKey == null ? List.of() : ServerWorldProfileManager.getInstance().getProfiles(serverKey);
        int listInnerH = Math.max(ROW_STRIDE, listBottom - listY - 4);
        visibleCount = Math.max(1, listInnerH / ROW_STRIDE);
        scrollable = profiles.size() > visibleCount;
        int maxScroll = Math.max(0, profiles.size() - visibleCount);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        wellX = panelX + PAD - 4;
        wellW = panelW - PAD * 2 + 8;
        railX = panelX + panelW - PAD - 14;
        rowsX = panelX + PAD;
        rowsW = panelW - PAD * 2 - (scrollable ? 18 : 0);

        // Input field + Use.
        this.profileBox = new EditBox(this.font, panelX + PAD + 6, fieldY + 5, panelW - PAD * 2 - 66, 14,
                Component.literal("World name"));
        this.profileBox.setMaxLength(32);
        this.profileBox.setBordered(false);
        this.profileBox.setTextColor(InvUi.TEXT);
        this.addRenderableWidget(profileBox);
        this.addRenderableWidget(new InventorySortTextButton(panelX + panelW - PAD - 54, fieldY, 54, 18,
                Component.literal("Use"), button -> useTypedProfile()));

        // Bottom bar.
        this.addRenderableWidget(new InventorySortTextButton(panelX + PAD, bottomBarY, 76, 18,
                Component.literal("Default"), button -> useProfile("default")));
        this.addRenderableWidget(new InventorySortTextButton(panelX + panelW - PAD - 64, bottomBarY, 64, 18,
                Component.literal(requiresConfirmation ? "Confirm" : "Back"), button -> confirmOrClose()));

        // Profile rows (invisible hitboxes; visuals drawn in render).
        if (serverKey != null) {
            int shown = Math.min(visibleCount, profiles.size() - scrollOffset);
            for (int i = 0; i < shown; i++) {
                String profile = profiles.get(scrollOffset + i);
                int y = listY + 2 + i * ROW_STRIDE;
                this.addRenderableWidget(new InventorySortHitboxButton(rowsX, y, rowsW, ROW_STRIDE - 2,
                        Component.literal(profile), button -> useProfile(profile)));
            }

            if (scrollable) {
                this.addRenderableWidget(new InventorySortModalIconButton(railX, listY + 2, 14,
                        InventorySortModalIconButton.UP, Component.literal("Scroll up"), btn -> scrollBy(-1)));
                this.addRenderableWidget(new InventorySortModalIconButton(railX, listBottom - 16, 14,
                        InventorySortModalIconButton.DOWN, Component.literal("Scroll down"), btn -> scrollBy(1)));
            }
        }

        this.setInitialFocus(profileBox);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        InventorySortDrawContext ui = InventorySortDrawContexts.wrap(g);
        InvUi.scrim(ui, this.width, this.height);
        InvUi.window(ui, panelX, panelY, panelW, panelH, ACCENT);

        g.text(this.font, "Tracked World", panelX + PAD, panelY + 9, InvUi.TEXT, false);

        InvUi.field(ui, panelX + PAD, fieldY, panelW - PAD * 2, 18,
                profileBox != null && profileBox.isFocused(), ACCENT);
        if (profileBox != null && profileBox.getValue().isEmpty()) {
            g.text(this.font, "Type a world name...", panelX + PAD + 6, fieldY + 5, InvUi.TEXT_DIM, false);
        }

        if (serverKey == null) {
            g.text(this.font, "Multiplayer servers only.", panelX + PAD, panelY + 24, InvUi.TEXT_MUTED, false);
            InvUi.inset(ui, wellX, listY - 2, wellW, listBottom - (listY - 2));
            super.extractRenderState(g, mouseX, mouseY, partialTick);
            return;
        }

        g.text(this.font, truncate(serverKey, panelW - PAD * 2), panelX + PAD, panelY + 24, InvUi.TEXT_DIM, false);

        // Section label + active indicator.
        g.text(this.font, "Saved worlds", panelX + PAD, labelY, InvUi.TEXT_DIM, false);
        String tracking = "Tracking: " + activeProfile;
        int trackingMax = panelW - PAD * 2 - this.font.width("Saved worlds") - 10;
        tracking = truncate(tracking, trackingMax);
        g.text(this.font, tracking, panelX + panelW - PAD - this.font.width(tracking), labelY, ACCENT, false);

        // List well.
        InvUi.inset(ui, wellX, listY - 2, wellW, listBottom - (listY - 2));
        g.enableScissor(wellX + 1, listY - 1, wellX + wellW - 1, listBottom - 1);
        List<String> profiles = ServerWorldProfileManager.getInstance().getProfiles(serverKey);
        int shown = Math.min(visibleCount, profiles.size() - scrollOffset);
        for (int i = 0; i < shown; i++) {
            String profile = profiles.get(scrollOffset + i);
            boolean active = profile.equals(activeProfile);
            int y = listY + 2 + i * ROW_STRIDE;
            boolean hovered = isInside(mouseX, mouseY, rowsX, y, rowsW, ROW_STRIDE - 2);
            InvUi.row(ui, rowsX, y, rowsW, ROW_STRIDE - 2, hovered, active, ACCENT);
            g.text(this.font, truncate(profile, rowsW - 70), rowsX + 8, y + 5,
                    active ? InvUi.TEXT : InvUi.TEXT_MUTED, false);

            String status;
            int statusColor;
            if (active) {
                status = "active";
                statusColor = ACCENT;
            } else {
                long last = ServerWorldProfileManager.getInstance().getLastUsed(serverKey, profile);
                status = last > 0 ? formatAgo(last) : "";
                statusColor = InvUi.TEXT_DIM;
            }
            if (!status.isEmpty()) {
                g.text(this.font, status, rowsX + rowsW - 8 - this.font.width(status), y + 5, statusColor, false);
            }
        }
        if (shown == 0) {
            g.text(this.font, "No saved worlds yet.", rowsX + 6, listY + 8, InvUi.TEXT_MUTED, false);
        }
        g.disableScissor();
        InvUi.insetBorder(ui, wellX, listY - 2, wellW, listBottom - (listY - 2));
        InvUi.scrollbar(ui, wellX + wellW - 6, listY + 2, listBottom - listY - 4,
                profiles.size() * ROW_STRIDE, visibleCount * ROW_STRIDE, scrollOffset * ROW_STRIDE, ACCENT);

        super.extractRenderState(g, mouseX, mouseY, partialTick);

        if (hintH > 0) {
            g.text(this.font, "Press Confirm to start tracking this world.",
                    panelX + PAD, bottomBarY - 13, 0xFFE0B341, false);
        }
    }

    private void scrollBy(int delta) {
        if (serverKey == null) {
            return;
        }
        int maxScroll = Math.max(0, ServerWorldProfileManager.getInstance().getProfiles(serverKey).size() - visibleCount);
        int next = Math.max(0, Math.min(scrollOffset + delta, maxScroll));
        if (next != scrollOffset) {
            scrollOffset = next;
            this.rebuildWidgets();
        }
    }

    private static String formatAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) {
            return days + "d ago";
        }
        if (hours > 0) {
            return hours + "h ago";
        }
        if (minutes > 0) {
            return minutes + "m ago";
        }
        return "just now";
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

    private void useTypedProfile() {
        if (profileBox == null) {
            return;
        }
        useProfile(profileBox.getValue());
    }

    private void useProfile(String profile) {
        if (serverKey == null) {
            return;
        }
        ServerWorldProfileManager.getInstance().setActiveProfile(serverKey, profile);
        Minecraft mc = Minecraft.getInstance();
        MinecraftApiCompat.sendSystemMessage(mc, Component.literal("Tracking world: "
                + ServerWorldProfileManager.getInstance().getActiveProfile(serverKey)).withStyle(ChatFormatting.GREEN));
        closeToParent();
    }

    private void confirmOrClose() {
        if (serverKey != null && requiresConfirmation) {
            ServerWorldProfileManager.getInstance().confirmActiveProfile(serverKey);
        }
        closeToParent();
    }

    private void closeToParent() {
        MinecraftApiCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private boolean handleMouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (serverKey != null && scrollable
                && isInside(mouseX, mouseY, wellX, listY - 2, wellW, listBottom - (listY - 2))) {
            int delta = (int) Math.signum(-verticalAmount);
            int before = scrollOffset;
            scrollBy(delta);
            return scrollOffset != before;
        }
        return false;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
