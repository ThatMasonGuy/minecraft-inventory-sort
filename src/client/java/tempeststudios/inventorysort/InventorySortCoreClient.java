package tempeststudios.inventorysort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import tempeststudios.inventorysort.core.InventorySortCore;

public class InventorySortCoreClient implements ClientModInitializer {
    public static final Logger LOGGER = InventorySortCore.LOGGER;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Inventory Sort Core");
        ClientTickEvents.END_CLIENT_TICK.register(client -> ServerWorldProfileManager.getInstance().handleConfirmationInput(client));
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> ServerWorldProfileHud.render(graphics));
    }
}
