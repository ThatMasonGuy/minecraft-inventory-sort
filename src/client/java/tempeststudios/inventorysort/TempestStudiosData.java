package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class TempestStudiosData {
    private static final String UNKNOWN = "unknown";

    public enum ModDataFolder {
        CORE("InvCore", "inv-core"),
        SORT("InvSort", "inv-sort"),
        SEARCH("InvSearch", "inv-search"),
        CATALOGUE("InvCatalogue", "inv-catalogue");

        private final String desktopFolder;
        private final String linuxFolder;

        ModDataFolder(String desktopFolder, String linuxFolder) {
            this.desktopFolder = desktopFolder;
            this.linuxFolder = linuxFolder;
        }
    }

    private TempestStudiosData() {
    }

    public static Path modRoot(ModDataFolder folder) {
        return appDataRoot().resolve(isLinux() ? folder.linuxFolder : folder.desktopFolder);
    }

    public static Path legacyRoot(Minecraft client) {
        return gameDirectory(client).resolve("inventorysort");
    }

    public static Path gameDirectory(Minecraft client) {
        if (client != null && client.gameDirectory != null) {
            return client.gameDirectory.toPath();
        }
        return Path.of(System.getProperty("user.dir", "."));
    }

    public static String singleplayerNamespace(Minecraft client, String worldId) {
        return "singleplayer:" + instanceId(client) + ":" + sanitize(worldId);
    }

    public static String accountScopedServerNamespace(Minecraft client, String namespace) {
        String normalized = sanitize(namespace);
        if (!normalized.startsWith("server:") || normalized.contains(":account:")) {
            return normalized;
        }

        int worldIndex = normalized.lastIndexOf(":world:");
        String suffix = "";
        if (worldIndex >= 0) {
            suffix = normalized.substring(worldIndex);
            normalized = normalized.substring(0, worldIndex);
        }
        return normalized + ":account:" + accountId(client) + suffix;
    }

    public static String mapLegacyNamespace(Minecraft client, String namespace) {
        String normalized = sanitize(namespace);
        String prefix = "singleplayer:";
        if (normalized.startsWith(prefix)) {
            String worldId = normalized.substring(prefix.length());
            if (hasInstanceScope(worldId)) {
                return normalized;
            }
            return prefix + instanceId(client) + ":" + worldId;
        }
        return accountScopedServerNamespace(client, normalized);
    }

    public static String fileNameSafe(String namespace) {
        return sanitize(namespace).replace(':', '_');
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._:-]+", "_");
    }

    public static String instanceId(Minecraft client) {
        Path path = canonicalGameDirectory(client);
        return "instance_" + shortHash(path.toString());
    }

    public static String accountId(Minecraft client) {
        try {
            if (client != null && client.getUser() != null && client.getUser().getProfileId() != null) {
                return "account_" + shortHash(client.getUser().getProfileId().toString());
            }
            if (client != null && client.getUser() != null && client.getUser().getName() != null) {
                return "account_" + shortHash(client.getUser().getName());
            }
        } catch (Exception ignored) {
        }
        return "account_unknown";
    }

    private static boolean hasInstanceScope(String singleplayerSuffix) {
        if (!singleplayerSuffix.startsWith("instance_")) {
            return false;
        }
        int separator = singleplayerSuffix.indexOf(':');
        return separator > "instance_".length() && separator < singleplayerSuffix.length() - 1;
    }

    private static Path canonicalGameDirectory(Minecraft client) {
        Path path = gameDirectory(client).toAbsolutePath().normalize();
        try {
            return path.toRealPath();
        } catch (IOException ignored) {
            return path;
        }
    }

    private static Path appDataRoot() {
        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "TempestStudios");
            }
            return Path.of(userHome(), "AppData", "Roaming", "TempestStudios");
        }
        if (isMac()) {
            return Path.of(userHome(), "Library", "Application Support", "TempestStudios");
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        Path base = xdgDataHome != null && !xdgDataHome.isBlank()
                ? Path.of(xdgDataHome)
                : Path.of(userHome(), ".local", "share");
        return base.resolve("tempest-studios");
    }

    private static String userHome() {
        return System.getProperty("user.home", ".");
    }

    private static boolean isWindows() {
        return osName().contains("win");
    }

    private static boolean isMac() {
        return osName().contains("mac");
    }

    private static boolean isLinux() {
        return !isWindows() && !isMac();
    }

    private static String osName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(6, digest.length); i++) {
                builder.append(String.format(Locale.ROOT, "%02x", digest[i] & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
