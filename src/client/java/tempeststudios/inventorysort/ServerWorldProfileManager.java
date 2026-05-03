package tempeststudios.inventorysort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ServerWorldProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_PROFILE = "default";
    private static ServerWorldProfileManager instance;

    private final Path saveFile;
    private final Map<String, ServerProfiles> profilesByServer = new HashMap<>();

    private ServerWorldProfileManager() {
        Path modDir = Minecraft.getInstance().gameDirectory.toPath().resolve("inventorysort");
        this.saveFile = modDir.resolve("server_world_profiles.json");
        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            InventorySortClient.LOGGER.error("Failed to create inventorysort directory", e);
        }
        load();
    }

    public static ServerWorldProfileManager getInstance() {
        if (instance == null) {
            instance = new ServerWorldProfileManager();
        }
        return instance;
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
        List<String> profiles = new ArrayList<>(profilesFor(serverKey).profiles);
        Collections.sort(profiles);
        return profiles;
    }

    public void setActiveProfile(String serverKey, String profileName) {
        if (serverKey == null || serverKey.isBlank()) {
            return;
        }
        String profile = sanitizeProfile(profileName);
        ServerProfiles serverProfiles = profilesFor(serverKey);
        if (!serverProfiles.profiles.contains(profile)) {
            serverProfiles.profiles.add(profile);
        }
        serverProfiles.activeProfile = profile;
        save();
        ItemLocationTracker.getInstance().reloadForCurrentNamespace();
        InventoryHistorySampler.reset();
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
            InventorySortClient.LOGGER.error("Failed to load server world profiles", e);
        }
    }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(saveFile)) {
            GSON.toJson(profilesByServer, writer);
        } catch (IOException e) {
            InventorySortClient.LOGGER.error("Failed to save server world profiles", e);
        }
    }

    private static final class ServerProfiles {
        String activeProfile = DEFAULT_PROFILE;
        List<String> profiles = new ArrayList<>();

        ServerProfiles() {
            profiles.add(DEFAULT_PROFILE);
        }

        void normalize() {
            if (profiles == null) {
                profiles = new ArrayList<>();
            }
            if (!profiles.contains(DEFAULT_PROFILE)) {
                profiles.add(DEFAULT_PROFILE);
            }
            if (activeProfile == null || activeProfile.isBlank()) {
                activeProfile = DEFAULT_PROFILE;
            }
            if (!profiles.contains(activeProfile)) {
                profiles.add(activeProfile);
            }
        }
    }
}
