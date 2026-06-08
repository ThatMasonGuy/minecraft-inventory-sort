package tempeststudios.inventorysort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import tempeststudios.inventorysort.compat.core.KeyBindingCompat;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;
import tempeststudios.inventorysort.core.InventorySortEvents;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ServerWorldProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_PROFILE = "default";
    private static final long USE_TOUCH_INTERVAL_MILLIS = 60_000L;
    private static ServerWorldProfileManager instance;
    private static KeyMapping confirmWorldKey;
    private static KeyMapping openProfilesKey;

    private final Path saveFile;
    private final Map<String, ServerProfiles> profilesByServer = new HashMap<>();
    private final Set<String> confirmedThisSession = new HashSet<>();

    private ServerWorldProfileManager() {
        Path modDir = Minecraft.getInstance().gameDirectory.toPath().resolve("inventorysort");
        this.saveFile = modDir.resolve("server_world_profiles.json");
        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to create inventorysort directory", e);
        }
        load();
    }

    public static ServerWorldProfileManager getInstance() {
        if (instance == null) {
            instance = new ServerWorldProfileManager();
        }
        return instance;
    }

    public static void registerKeybindings() {
        if (confirmWorldKey != null && openProfilesKey != null) {
            return;
        }

        confirmWorldKey = KeyBindingCompat.register(
                "key.inventorysort.confirm_world_profile",
                GLFW.GLFW_KEY_ENTER);
        openProfilesKey = KeyBindingCompat.register(
                "key.inventorysort.open_world_profiles",
                GLFW.GLFW_KEY_BACKSPACE);
    }

    public String getActiveProfile(String serverKey) {
        if (serverKey == null || serverKey.isBlank()) {
            return DEFAULT_PROFILE;
        }
        return profilesFor(serverKey).activeProfile;
    }

    public List<String> getProfiles(String serverKey) {
        if (serverKey == null || serverKey.isBlank()) {
            return Collections.singletonList(DEFAULT_PROFILE);
        }
        return new ArrayList<>(profilesFor(serverKey).profiles);
    }

    /** Epoch millis a profile was last activated, or 0 if never recorded. */
    public long getLastUsed(String serverKey, String profile) {
        if (serverKey == null || serverKey.isBlank() || profile == null) {
            return 0L;
        }
        Long value = profilesFor(serverKey).lastUsed.get(profile);
        return value == null ? 0L : value;
    }

    public boolean needsConfirmation(Minecraft client) {
        String serverKey = TrackingNamespace.currentServerKey(client);
        if (serverKey == null) {
            return false;
        }
        ServerProfiles profiles = profilesFor(serverKey);
        if (profiles.profiles.size() <= 1) {
            return false;
        }
        return !confirmedThisSession.contains(confirmationKey(serverKey, profiles.activeProfile));
    }

    public boolean trackingAllowed(Minecraft client) {
        return !needsConfirmation(client);
    }

    /**
     * Records that the active world is currently being played, so the world
     * picker can show a real "last used" time. Called every client tick; only
     * stamps a confirmed/tracked world and persists at most once a minute.
     */
    public void markActiveUsed(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        String serverKey = TrackingNamespace.currentServerKey(client);
        if (serverKey == null || serverKey.isBlank() || !trackingAllowed(client)) {
            return;
        }
        ServerProfiles serverProfiles = profilesFor(serverKey);
        String profile = serverProfiles.activeProfile;
        long now = System.currentTimeMillis();
        long last = serverProfiles.lastUsed.getOrDefault(profile, 0L);
        if (now - last < USE_TOUCH_INTERVAL_MILLIS) {
            return;
        }
        serverProfiles.lastUsed.put(profile, now);
        save();
    }

    public void confirmActiveProfile(String serverKey) {
        if (serverKey == null || serverKey.isBlank()) {
            return;
        }
        String active = getActiveProfile(serverKey);
        confirmedThisSession.add(confirmationKey(serverKey, active));
        profilesFor(serverKey).lastUsed.put(active, System.currentTimeMillis());
        save();
    }

    public void handleConfirmationInput(Minecraft client) {
        if (client == null || client.player == null || client.level == null || MinecraftApiCompat.isScreenOpen(client)) {
            clearQueuedKeypresses();
            return;
        }
        if (!needsConfirmation(client)) {
            clearQueuedKeypresses();
            return;
        }

        if (consumeKeypress(confirmWorldKey)) {
            String serverKey = TrackingNamespace.currentServerKey(client);
            confirmActiveProfile(serverKey);
            MinecraftApiCompat.sendSystemMessage(client, Component.literal("Tracking world confirmed: "
                    + getActiveProfile(serverKey)).withStyle(ChatFormatting.GREEN));
            clearQueuedKeypresses();
        } else if (consumeKeypress(openProfilesKey)) {
            MinecraftApiCompat.setScreen(client, new ServerWorldProfileScreen(null, true));
            clearQueuedKeypresses();
        }
    }

    private static boolean consumeKeypress(KeyMapping key) {
        boolean clicked = false;
        while (key != null && key.consumeClick()) {
            clicked = true;
        }
        return clicked;
    }

    private static void clearQueuedKeypresses() {
        consumeKeypress(confirmWorldKey);
        consumeKeypress(openProfilesKey);
    }

    public void setActiveProfile(String serverKey, String profileName) {
        if (serverKey == null || serverKey.isBlank()) {
            return;
        }
        String profile = sanitizeProfile(profileName);
        ServerProfiles serverProfiles = profilesFor(serverKey);
        serverProfiles.profiles.remove(profile);
        serverProfiles.profiles.add(0, profile);
        serverProfiles.activeProfile = profile;
        serverProfiles.lastUsed.put(profile, System.currentTimeMillis());
        save();
        confirmActiveProfile(serverKey);
        publishNamespaceChanged(serverKey, profile);
    }

    public String displayName(String profile) {
        if (profile == null || profile.equals(DEFAULT_PROFILE)) {
            return "default";
        }
        return profile;
    }

    private ServerProfiles profilesFor(String serverKey) {
        return profilesByServer.computeIfAbsent(serverKey, ignored -> new ServerProfiles());
    }

    private String sanitizeProfile(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return DEFAULT_PROFILE;
        }
        String profile = profileName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "_");
        return profile.isBlank() ? DEFAULT_PROFILE : profile;
    }

    private String confirmationKey(String serverKey, String profile) {
        return serverKey + ":" + profile;
    }

    private void publishNamespaceChanged(String serverKey, String profile) {
        Minecraft client = Minecraft.getInstance();
        InventorySortEvents.NAMESPACE_CHANGED.invoker().onNamespaceChanged(
                new InventorySortEvents.NamespaceChangedContext(
                        client,
                        serverKey,
                        profile,
                        TrackingNamespace.current(client)));
    }

    private void load() {
        if (!Files.exists(saveFile)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(saveFile)) {
            Type type = new TypeToken<Map<String, ServerProfiles>>(){}.getType();
            Map<String, ServerProfiles> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                profilesByServer.clear();
                for (Map.Entry<String, ServerProfiles> entry : loaded.entrySet()) {
                    ServerProfiles profiles = entry.getValue() != null ? entry.getValue() : new ServerProfiles();
                    profiles.normalize();
                    profilesByServer.put(entry.getKey(), profiles);
                }
            }
        } catch (Exception e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to load server world profiles", e);
        }
    }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(saveFile)) {
            GSON.toJson(profilesByServer, writer);
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to save server world profiles", e);
        }
    }

    private static final class ServerProfiles {
        String activeProfile = DEFAULT_PROFILE;
        List<String> profiles = new ArrayList<>();
        Map<String, Long> lastUsed = new HashMap<>();

        ServerProfiles() {
            profiles.add(DEFAULT_PROFILE);
        }

        void normalize() {
            if (profiles == null) {
                profiles = new ArrayList<>();
            }
            if (lastUsed == null) {
                lastUsed = new HashMap<>();
            }
            if (!profiles.contains(DEFAULT_PROFILE)) {
                profiles.add(DEFAULT_PROFILE);
            }
            if (activeProfile == null || activeProfile.isBlank()) {
                activeProfile = DEFAULT_PROFILE;
            }
            if (!profiles.contains(activeProfile)) {
                profiles.add(0, activeProfile);
            }
        }
    }
}
