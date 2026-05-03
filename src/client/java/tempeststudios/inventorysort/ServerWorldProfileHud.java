package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ServerWorldProfileHud {
    private ServerWorldProfileHud() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.options.hideGui
                || !ServerWorldProfileManager.getInstance().needsConfirmation(client)) {
            return;
        }

        String serverKey = TrackingNamespace.currentServerKey(client);
        String active = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);
        String text = "InvSearch paused: RIGHT ENTER = " + active + " | BACKSPACE = change";
        int x = 8;
        int y = client.getWindow().getGuiScaledHeight() - 82;

        graphics.drawString(client.font, text, x + 1, y + 1, 0xFF000000, false);
        graphics.drawString(client.font, text, x, y, 0xFFFFFFFF, false);

        String rightEnter = "RIGHT ENTER";
        int keyStart = text.indexOf(rightEnter);
        if (keyStart >= 0) {
            String before = text.substring(0, keyStart);
            graphics.drawString(client.font, rightEnter, x + client.font.width(before), y, 0xFF00FF00, false);
        }

        String backspace = "BACKSPACE";
        keyStart = text.indexOf(backspace);
        if (keyStart >= 0) {
            String before = text.substring(0, keyStart);
            graphics.drawString(client.font, backspace, x + client.font.width(before), y, 0xFFFFFF55, false);
        }
    }
}
