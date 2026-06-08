package tempeststudios.inventorysort;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

import java.util.List;

public class ServerWorldProfileScreen extends Screen {
    private static final int PAD = 12;
    private static final int ACCENT = InvUi.ACCENT_SEARCH;
    private static final int VISIBLE = 5;

    private final Screen parent;
    private final boolean requiresConfirmation;
    private EditBox profileBox;
    private String serverKey;
    private int scrollOffset = 0;

    private int panelW;
    private int panelH;
    private int panelX;
    private int panelY;
    private int listY;

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

        panelW = Math.min(316, this.width - 24);
        panelH = Math.min(234, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        listY = panelY + 88;

        int fieldY = panelY + 40;
        this.profileBox = new EditBox(this.font, panelX + PAD + 6, fieldY + 5, panelW - PAD * 2 - 66, 14,
                Component.literal("World name"));
        this.profileBox.setMaxLength(32);
        this.profileBox.setBordered(false);
        this.profileBox.setTextColor(InvUi.TEXT);
        this.addRenderableWidget(profileBox);

        this.addRenderableWidget(new InventorySortTextButton(panelX + panelW - PAD - 54, fieldY, 54, 18,
                Component.literal("Use"), button -> useTypedProfile()));

        int bottomY = panelY + panelH - PAD - 18;
        this.addRenderableWidget(new InventorySortTextButton(panelX + PAD, bottomY, 76, 18,
                Component.literal("Default"), button -> useProfile("default")));
        this.addRenderableWidget(new InventorySortTextButton(panelX + panelW - PAD - 64, bottomY, 64, 18,
                Component.literal(requiresConfirmation ? "Confirm" : "Back"), button -> confirmOrClose()));

        if (serverKey != null) {
            List<String> profiles = ServerWorldProfileManager.getInstance().getProfiles(serverKey);
            String active = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);

            int maxScroll = Math.max(0, profiles.size() - VISIBLE);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            int y = listY + 2;
            int added = 0;
            boolean scrollable = profiles.size() > VISIBLE;
            int buttonWidth = scrollable ? panelW - PAD * 2 - 18 : panelW - PAD * 2;
            for (int i = scrollOffset; i < profiles.size(); i++) {
                if (added >= VISIBLE) {
                    break;
                }
                String profile = profiles.get(i);
                String label = profile.equals(active) ? profile + "  (active)" : profile;
                this.addRenderableWidget(new InventorySortTextButton(panelX + PAD, y, buttonWidth, 18,
                        Component.literal(label), button -> useProfile(profile)));
                y += 20;
                added++;
            }

            if (scrollable) {
                int scrollColX = panelX + panelW - PAD - 14;
                this.addRenderableWidget(new InventorySortModalIconButton(scrollColX, listY + 2, 14,
                        InventorySortModalIconButton.UP, Component.literal("Scroll up"), btn -> {
                    scrollOffset--;
                    this.rebuildWidgets();
                }));
                this.addRenderableWidget(new InventorySortModalIconButton(scrollColX, listY + 2 + VISIBLE * 20 - 14, 14,
                        InventorySortModalIconButton.DOWN, Component.literal("Scroll down"), btn -> {
                    scrollOffset++;
                    this.rebuildWidgets();
                }));
            }
        }

        this.setInitialFocus(profileBox);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        InventorySortDrawContext ui = InventorySortDrawContexts.wrap(g);
        InvUi.scrim(ui, this.width, this.height);
        InvUi.window(ui, panelX, panelY, panelW, panelH, ACCENT);

        g.drawString(this.font, "Tracked World", panelX + PAD, panelY + 9, InvUi.TEXT, false);

        InvUi.field(ui, panelX + PAD, panelY + 40, panelW - PAD * 2, 18,
                profileBox != null && profileBox.isFocused(), ACCENT);
        if (profileBox != null && profileBox.getValue().isEmpty()) {
            g.drawString(this.font, "Type a world name...", panelX + PAD + 6, panelY + 45, InvUi.TEXT_DIM, false);
        }

        if (serverKey == null) {
            g.drawString(this.font, "Multiplayer servers only.", panelX + PAD, panelY + 24, InvUi.TEXT_MUTED, false);
        } else {
            String active = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);
            g.drawString(this.font, truncate(serverKey, panelW - PAD * 2), panelX + PAD, panelY + 24, InvUi.TEXT_DIM, false);
            g.drawString(this.font, "Saved worlds", panelX + PAD, panelY + 76, InvUi.TEXT_DIM, false);
            String tracking = "Tracking: " + active;
            g.drawString(this.font, truncate(tracking, panelW - PAD * 2 - this.font.width("Saved worlds") - 12),
                    panelX + panelW - PAD - this.font.width(truncate(tracking, panelW - PAD * 2 - this.font.width("Saved worlds") - 12)),
                    panelY + 76, ACCENT, false);
            if (requiresConfirmation) {
                InvUi.divider(ui, panelX + PAD, panelY + 36, panelW - PAD * 2);
            }
        }

        // List well behind the profile buttons.
        InvUi.inset(ui, panelX + PAD - 4, listY - 2, panelW - PAD * 2 + 8, VISIBLE * 20 + 2);

        super.render(g, mouseX, mouseY, partialTick);

        if (requiresConfirmation && serverKey != null) {
            g.drawString(this.font, "Confirm before tracking starts.", panelX + PAD, panelY + panelH - PAD - 30,
                    0xFFE0B341, false);
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
        return handleMouseScrolled(verticalAmount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return handleMouseScrolled(verticalAmount);
    }

    private boolean handleMouseScrolled(double verticalAmount) {
        if (serverKey != null) {
            List<String> profiles = ServerWorldProfileManager.getInstance().getProfiles(serverKey);
            int maxScroll = Math.max(0, profiles.size() - VISIBLE);
            if (maxScroll > 0) {
                int delta = (int) Math.signum(-verticalAmount);
                int oldOffset = scrollOffset;
                scrollOffset = Math.max(0, Math.min(scrollOffset + delta, maxScroll));
                if (scrollOffset != oldOffset) {
                    this.rebuildWidgets();
                    return true;
                }
            }
        }
        return false;
    }
}
