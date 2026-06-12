package tempeststudios.inventorysort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import tempeststudios.inventorysort.compat.core.HudCompat;
import tempeststudios.inventorysort.core.InventorySortCore;

public class InventorySortCoreClient implements ClientModInitializer {
    public static final Logger LOGGER = InventorySortCore.LOGGER;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Inventory Sort Core");
        LegacyDataMigration.migrateLegacyData(Minecraft.getInstance());
        ServerWorldProfileManager.registerKeybindings();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ServerWorldProfileManager manager = ServerWorldProfileManager.getInstance();
            manager.handleConfirmationInput(client);
            manager.markActiveUsed(client);
        });
        HudCompat.registerWorldProfileHud();
        InventorySortSmokeTest.registerIfEnabled();
    }
}
