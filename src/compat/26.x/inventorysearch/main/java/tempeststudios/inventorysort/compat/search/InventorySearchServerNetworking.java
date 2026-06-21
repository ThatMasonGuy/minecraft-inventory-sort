package tempeststudios.inventorysort.compat.search;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class InventorySearchServerNetworking {
    private InventorySearchServerNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(AutoWorldPayload.TYPE, AutoWorldPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendAutoWorld(handler.player, server));
    }

    private static void sendAutoWorld(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null || !ServerPlayNetworking.canSend(player, AutoWorldPayload.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new AutoWorldPayload(profileName(server)));
    }

    private static String profileName(MinecraftServer server) {
        String levelName = server.getWorldData().getLevelName();
        return levelName == null || levelName.isBlank() ? "server" : levelName;
    }

    public record AutoWorldPayload(String profile) implements CustomPacketPayload {
        public static final Type<AutoWorldPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("inventorysearch", "auto_world"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AutoWorldPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, AutoWorldPayload::profile, AutoWorldPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
