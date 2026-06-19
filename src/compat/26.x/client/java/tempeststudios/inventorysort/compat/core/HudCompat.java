package tempeststudios.inventorysort.compat.core;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import tempeststudios.inventorysort.InventorySortDrawContexts;
import tempeststudios.inventorysort.ServerWorldProfileHud;
import tempeststudios.inventorysort.core.InventorySortCore;

public final class HudCompat {
    private HudCompat() {
    }

    public static void registerWorldProfileHud() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(InventorySortCore.MOD_ID, "world_profile"),
                (graphics, tickCounter) -> ServerWorldProfileHud.render(InventorySortDrawContexts.wrap(graphics)));
    }
}
