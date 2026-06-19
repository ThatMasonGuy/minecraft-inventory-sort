package tempeststudios.inventorysort.compat.search;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class InventorySearchServerNetworking {
    public static final ResourceLocation AUTO_WORLD_ID = new ResourceLocation("inventorysearch", "auto_world");

    private InventorySearchServerNetworking() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendAutoWorld(handler.player, server));
    }

    private static void sendAutoWorld(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null || !ServerPlayNetworking.canSend(player, AUTO_WORLD_ID)) {
            return;
        }
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeUtf(profileName(server), 128);
        ServerPlayNetworking.send(player, AUTO_WORLD_ID, buffer);
    }

    private static String profileName(MinecraftServer server) {
        String levelName = server.getWorldData().getLevelName();
        return levelName == null || levelName.isBlank() ? "server" : levelName;
    }
}
