package tempeststudios.inventorysort.compat.search;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import tempeststudios.inventorysort.ServerWorldProfileManager;
import tempeststudios.inventorysort.compat.search.InventorySearchServerNetworking.AutoWorldPayload;

public final class InventorySearchClientNetworking {
    private InventorySearchClientNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(AutoWorldPayload.TYPE, AutoWorldPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(AutoWorldPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        ServerWorldProfileManager.getInstance().applyAutoProfile(context.client(), payload.profile())));
    }
}
