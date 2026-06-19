package tempeststudios.inventorysort.compat.search;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import tempeststudios.inventorysort.ServerWorldProfileManager;

public final class InventorySearchClientNetworking {
    private InventorySearchClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(InventorySearchServerNetworking.AUTO_WORLD_ID,
                (client, handler, buffer, responseSender) -> {
                    String profile = buffer.readUtf(128);
                    client.execute(() -> ServerWorldProfileManager.getInstance().applyAutoProfile(client, profile));
                });
    }
}
