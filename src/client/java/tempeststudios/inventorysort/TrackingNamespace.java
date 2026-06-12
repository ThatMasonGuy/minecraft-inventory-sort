package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

import java.nio.file.Path;

public final class TrackingNamespace {
    private static final String UNKNOWN = "unknown";

    private TrackingNamespace() {
    }

    public static String current(Minecraft client) {
        if (client == null) {
            return UNKNOWN;
        }

        if (MinecraftApiCompat.isSingleplayer(client) && client.getSingleplayerServer() != null) {
            String worldId = UNKNOWN;
            try {
                Path serverDir = MinecraftApiCompat.singleplayerServerDirectory(client);
                if (serverDir != null && serverDir.getFileName() != null) {
                    worldId = serverDir.getFileName().toString();
                }
            } catch (Exception ignored) {
                worldId = client.getSingleplayerServer().getWorldData().getLevelName();
            }
            return TempestStudiosData.singleplayerNamespace(client, worldId);
        }

        ServerData server = client.getCurrentServer();
        if (server != null) {
            String serverKey = currentServerKey(client);
            String serverNamespace = TempestStudiosData.accountScopedServerNamespace(client, "server:" + serverKey);
            String profile = ServerWorldProfileManager.getInstance().getActiveProfile(serverKey);
            if (profile == null || profile.equals("default")) {
                return serverNamespace;
            }
            return serverNamespace + ":world:" + TempestStudiosData.sanitize(profile);
        }

        return UNKNOWN;
    }

    public static String currentServerKey(Minecraft client) {
        if (client == null) {
            return null;
        }
        ServerData server = client.getCurrentServer();
        if (server == null) {
            return null;
        }
        String serverId = server.ip != null && !server.ip.isBlank() ? server.ip : server.name;
        return TempestStudiosData.sanitize(serverId);
    }

    public static boolean isMultiplayerServer(Minecraft client) {
        return currentServerKey(client) != null;
    }

    public static String fileNameSafe(String namespace) {
        return TempestStudiosData.fileNameSafe(namespace);
    }
}
