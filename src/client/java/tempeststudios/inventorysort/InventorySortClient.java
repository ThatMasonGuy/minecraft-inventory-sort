package tempeststudios.inventorysort;

import net.fabricmc.api.ClientModInitializer;
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
	}
}
