package tempeststudios.inventorysort.compat.core;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import tempeststudios.inventorysort.InventorySortDrawContexts;
import tempeststudios.inventorysort.ServerWorldProfileHud;

public final class HudCompat {
    private HudCompat() {
    }

    public static void registerWorldProfileHud() {
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> ServerWorldProfileHud.render(InventorySortDrawContexts.wrap(graphics)));
    }
}
