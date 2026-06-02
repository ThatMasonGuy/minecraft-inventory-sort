package tempeststudios.inventorysort;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import tempeststudios.inventorysort.core.InventorySortCore;

public class InventorySortClient implements ClientModInitializer {
	public static final String MOD_ID = InventorySortCore.MOD_ID;
	public static final Logger LOGGER = InventorySortCore.LOGGER;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Inventory Sort Mod");
		LOGGER.info("Sort button will render on all container screens");
		LOGGER.info("Click handling via mixins");

		InventorySearchFeature.initialize();
		InventoryCatalogueFeature.initialize();

		// Register commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			ModCommands.register(dispatcher, registryAccess);
			LOGGER.info("Registered mod commands");
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ServerWorldProfileManager.getInstance().handleConfirmationInput(client);
			InventorySearchFeature.sampleInventory(client);
		});
		HudRenderCallback.EVENT.register((graphics, tickCounter) -> ServerWorldProfileHud.render(graphics));
	}
}
