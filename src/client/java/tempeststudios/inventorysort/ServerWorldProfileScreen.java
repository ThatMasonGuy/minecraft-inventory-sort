package tempeststudios.inventorysort;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

import java.util.List;

public class ServerWorldProfileScreen extends Screen {
    private final Screen parent;
    private final boolean requiresConfirmation;
    private EditBox profileBox;
    private String serverKey;
    private int scrollOffset = 0;

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

        int panelW = Math.min(300, this.width - 24);
        int panelH = Math.min(220, this.height - 24);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        // Recessed field is at panelY+48, 18px tall. EditBox is unbordered, so it draws
        // text flush at its Y (no auto-centering) - offset by +5 to center text in the field.
        this.profileBox = new EditBox(this.font, panelX + 16, panelY + 53, panelW - 96, 14,
                Component.literal("World name"));
        this.profileBox.setMaxLength(32);
        this.profileBox.setBordered(false);
        this.profileBox.setTextColor(0xFFE0E0E0); // Light text on the dark recessed field
        this.addRenderableWidget(profileBox);

        this.addRenderableWidget(new InventorySortTextButton(panelX + panelW - 72, panelY + 48, 58, 18, Component.literal("Use"), button -> useTypedProfile()));

        this.addRenderableWidget(new InventorySortTextButton(panelX + 14, panelY + panelH - 28, 72, 18, Component.literal("Default"), button -> useProfile("default")));

        this.addRenderableWidget(new InventorySortTextButton(panelX + panelW - 72, panelY + panelH - 28, 58, 18, Component.literal(requiresConfirmation ? "Confirm" : "Back"), button -> confirmOrClose()));

        if (serverKey != null) {
            List<String> profiles = ServerWorldProfileManager.getInstance().getProfiles(serverKey);
            String active = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);
            
            int visibleCount = 5;
            int maxScroll = Math.max(0, profiles.size() - visibleCount);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            
            int y = panelY + 74;
            int added = 0;
            
            for (int i = scrollOffset; i < profiles.size(); i++) {
                if (added >= visibleCount || y + 18 > panelY + panelH - 34) {
                    break;
                }
                String profile = profiles.get(i);
                String label = profile.equals(active) ? "* " + profile : profile;
                // Leave room for scroll buttons by reducing width
                int buttonWidth = profiles.size() > visibleCount ? panelW - 46 : panelW - 28;
                this.addRenderableWidget(new InventorySortTextButton(panelX + 14, y, buttonWidth, 18, Component.literal(label), button -> useProfile(profile)));
                y += 21;
                added++;
            }
            
            if (maxScroll > 0) {
                int scrollColX = panelX + panelW - 32;
                this.addRenderableWidget(new InventorySortModalIconButton(scrollColX, panelY + 74, 18, InventorySortModalIconButton.UP, Component.literal("Scroll Up"), btn -> {
                    scrollOffset--;
                    this.rebuildWidgets();
                }));
                this.addRenderableWidget(new InventorySortModalIconButton(scrollColX, panelY + 74 + 21, 18, InventorySortModalIconButton.DOWN, Component.literal("Scroll Down"), btn -> {
                    scrollOffset++;
                    this.rebuildWidgets();
                }));
            }
        }

        this.setInitialFocus(profileBox);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        InventorySortDrawContext context = InventorySortDrawContexts.wrap(g);
        int panelW = Math.min(300, this.width - 24);
        int panelH = Math.min(220, this.height - 24);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        context.fill(0, 0, this.width, this.height, 0x88000000);
        
        // Main panel
        InventorySortUIUtils.drawBeveledPanel(context, panelX, panelY, panelW, panelH, false);

        context.drawString(this.font, "Tracked World", panelX + 14, panelY + 10, 0xFF1C1C1C, false);
        
        // Recessed edit box area
        InventorySortUIUtils.drawRecessedPanel(context, panelX + 14, panelY + 48, panelW - 92, 18);

        if (serverKey == null) {
            context.drawString(this.font, "Multiplayer only", panelX + 14, panelY + 30, 0xFFFF5555, false);
        } else {
            String active = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);
            context.drawString(this.font, serverKey, panelX + 14, panelY + 28, 0xFF555555, false);
            if (requiresConfirmation) {
                context.drawString(this.font, "Confirm before tracking starts", panelX + 14, panelY + 39, 0xFF775500, false);
            }
            context.drawString(this.font, "Active: " + active, panelX + 14, panelY + panelH - 42, 0xFF007700, false);
        }

        super.render(g, mouseX, mouseY, partialTick);
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
        // Return to whatever opened us (e.g. the search modal) rather than the game.
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
            int maxScroll = Math.max(0, profiles.size() - 5);
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
