package tempeststudios.inventorysort;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ServerWorldProfileScreen extends Screen {
    private final Screen parent;
    private final boolean requiresConfirmation;
    private EditBox profileBox;
    private String serverKey;

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

        this.profileBox = new EditBox(this.font, panelX + 14, panelY + 48, panelW - 92, 18,
                Component.literal("World name"));
        this.profileBox.setMaxLength(32);
        this.addRenderableWidget(profileBox);

        this.addRenderableWidget(Button.builder(Component.literal("Use"), button -> useTypedProfile())
                .bounds(panelX + panelW - 72, panelY + 48, 58, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Default"), button -> useProfile("default"))
                .bounds(panelX + 14, panelY + panelH - 28, 72, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal(requiresConfirmation ? "Confirm" : "Back"), button -> confirmOrClose())
                .bounds(panelX + panelW - 72, panelY + panelH - 28, 58, 18)
                .build());

        if (serverKey != null) {
            List<String> profiles = ServerWorldProfileManager.getInstance().getProfiles(serverKey);
            String active = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);
            int y = panelY + 74;
            int added = 0;
            for (String profile : profiles) {
                if (added >= 5 || y + 18 > panelY + panelH - 34) {
                    break;
                }
                String label = profile.equals(active) ? "* " + profile : profile;
                this.addRenderableWidget(Button.builder(Component.literal(label), button -> useProfile(profile))
                        .bounds(panelX + 14, y, panelW - 28, 18)
                        .build());
                y += 21;
                added++;
            }
        }

        this.setInitialFocus(profileBox);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int panelW = Math.min(300, this.width - 24);
        int panelH = Math.min(220, this.height - 24);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF1E1E1E);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF5A5A5A);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF5A5A5A);
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF0F0F0F);
        g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF0F0F0F);

        g.drawString(this.font, "Tracked World", panelX + 14, panelY + 10, 0xFFFFFFFF, false);
        if (serverKey == null) {
            g.drawString(this.font, "Multiplayer only", panelX + 14, panelY + 30, 0xFFFF5555, false);
        } else {
            String active = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);
            g.drawString(this.font, serverKey, panelX + 14, panelY + 28, 0xFF888888, false);
            if (requiresConfirmation) {
                g.drawString(this.font, "Confirm before tracking starts", panelX + 14, panelY + 39, 0xFFFFDD55, false);
            }
            g.drawString(this.font, "Active: " + active, panelX + 14, panelY + panelH - 42, 0xFF88FF88, false);
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
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("Tracking world: "
                    + ServerWorldProfileManager.getInstance().getActiveProfile(serverKey)).withStyle(ChatFormatting.GREEN), false);
        }
        closeToParent();
    }

    private void confirmOrClose() {
        if (serverKey != null && requiresConfirmation) {
            ServerWorldProfileManager.getInstance().confirmActiveProfile(serverKey);
            InventoryHistorySampler.reset();
        }
        closeToParent();
    }

    private void closeToParent() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !requiresConfirmation;
    }
}
