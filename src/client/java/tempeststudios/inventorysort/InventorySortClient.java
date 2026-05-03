package tempeststudios.inventorysort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventorySortClient implements ClientModInitializer {
	public static final String MOD_ID = "inventorysort";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Inventory Sort Mod");
		LOGGER.info("Sort button will render on all container screens");
		LOGGER.info("Click handling via mixins");

		// Initialize item location tracker
		try {
			ItemLocationTracker.getInstance();
			LOGGER.info("Item location tracking enabled");
		} catch (Exception e) {
			LOGGER.error("Failed to initialize item location tracker", e);
		}

		// Register commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			ModCommands.register(dispatcher, registryAccess);
			LOGGER.info("Registered mod commands");
		});

		ClientTickEvents.END_CLIENT_TICK.register(InventoryHistorySampler::sample);

		// Register shutdown hook to save tracking data
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				LOGGER.info("Saving item location data on shutdown");
				ItemLocationTracker.getInstance().save();
			} catch (Exception e) {
				LOGGER.error("Failed to save item location data", e);
			}
		}, "ItemLocationTracker-Shutdown"));
	}
}
