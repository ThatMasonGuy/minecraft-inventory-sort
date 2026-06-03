package tempeststudios.inventorysort;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public interface InventorySortDrawContext {
    void fill(int left, int top, int right, int bottom, int color);

    void drawString(Font font, String text, int x, int y, int color, boolean shadow);

    void drawString(Font font, Component text, int x, int y, int color, boolean shadow);

    void pushPose();

    void scale(float scale);

    void popPose();
}
