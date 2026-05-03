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
        String text = "InvSearch paused: ENTER = " + active + " | BACKSPACE = change";
        float scale = 0.78F;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int x = Math.round((screenWidth - client.font.width(text) * scale) / 2.0F);
        int y = screenHeight - 82;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        int scaledX = Math.round(x / scale);
        int scaledY = Math.round(y / scale);
        graphics.drawString(client.font, text, scaledX + 1, scaledY + 1, 0xFF000000, false);
        graphics.drawString(client.font, text, scaledX, scaledY, 0xFFFFFFFF, false);

        String enter = "ENTER";
        int keyStart = text.indexOf(enter);
        if (keyStart >= 0) {
            String before = text.substring(0, keyStart);
            graphics.drawString(client.font, enter, scaledX + client.font.width(before), scaledY, 0xFF00FF00, false);
        }

        String backspace = "BACKSPACE";
        keyStart = text.indexOf(backspace);
        if (keyStart >= 0) {
            String before = text.substring(0, keyStart);
            graphics.drawString(client.font, backspace, scaledX + client.font.width(before), scaledY, 0xFFFFFF55, false);
        }
        graphics.pose().popMatrix();
    }
}
