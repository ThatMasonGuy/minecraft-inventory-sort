package tempeststudios.inventorysort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import tempeststudios.inventorysort.core.InventorySortCore;

public class InventorySearchClient implements ClientModInitializer {
    public static final Logger LOGGER = InventorySortCore.LOGGER;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Inventory Search Mod");
        InventorySearchFeature.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(InventorySearchFeature::sampleInventory);
    }
}
