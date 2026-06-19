package tempeststudios.inventorysort;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tempeststudios.inventorysort.compat.search.InventorySearchServerNetworking;

public class InventorySearchServer implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("InventorySearch");
    private static final String SERVER_SMOKE_PROPERTY = "inventorysearch.serverSmokeTest";

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Inventory Search server hooks");
        InventorySearchServerNetworking.initialize();
        registerServerSmokeHook();
    }

    private static void registerServerSmokeHook() {
        if (!Boolean.getBoolean(SERVER_SMOKE_PROPERTY)) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("INVENTORYSEARCH_SERVER_SMOKE_TEST_PASS");
            server.halt(false);
        });
    }
}
