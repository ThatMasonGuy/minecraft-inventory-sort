package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.nio.file.Path;
import java.util.Locale;

public final class TrackingNamespace {
    private static final String UNKNOWN = "unknown";

    private TrackingNamespace() {
    }

    public static String current(Minecraft client) {
        if (client == null) {
            return UNKNOWN;
        }

        if (client.isSingleplayer() && client.getSingleplayerServer() != null) {
            String worldId = UNKNOWN;
            try {
                Path serverDir = client.getSingleplayerServer().getServerDirectory();
                if (serverDir != null && serverDir.getFileName() != null) {
                    worldId = serverDir.getFileName().toString();
                }
            } catch (Exception ignored) {
                worldId = client.getSingleplayerServer().getWorldData().getLevelName();
            }
            return "singleplayer:" + sanitize(worldId);
        }

        ServerData server = client.getCurrentServer();
        if (server != null) {
            String serverId = server.ip != null && !server.ip.isBlank() ? server.ip : server.name;
            return "server:" + sanitize(serverId);
        }

        return UNKNOWN;
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._:-]+", "_");
    }
}
