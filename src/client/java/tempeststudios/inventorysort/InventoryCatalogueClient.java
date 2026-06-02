package tempeststudios.inventorysort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import tempeststudios.inventorysort.core.InventorySortCore;

public class InventoryCatalogueClient implements ClientModInitializer {
    public static final Logger LOGGER = InventorySortCore.LOGGER;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Inventory Catalogue Mod");
        InventoryCatalogueFeature.initialize();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("inventorysort")
                    .then(InventoryCatalogueCommands.build()));
            LOGGER.info("Registered inventory catalogue commands");
        });
    }
}
