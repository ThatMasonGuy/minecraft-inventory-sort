package tempeststudios.inventorysort;

import net.fabricmc.api.ClientModInitializer;
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
	}
}