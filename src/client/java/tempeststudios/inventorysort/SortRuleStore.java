package tempeststudios.inventorysort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SortRuleStore {
    private static final int CURRENT_VERSION = 3;
    private static final String UNKNOWN_NAMESPACE = "unknown";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SortRuleStore instance;

    private final Path saveFile;
    private SortRuleConfig config = new SortRuleConfig();

    private SortRuleStore() {
        Minecraft mc = Minecraft.getInstance();
        LegacyDataMigration.migrateLegacyData(mc);
        Path modDir = TempestStudiosData.modRoot(TempestStudiosData.ModDataFolder.SORT);
        this.saveFile = modDir.resolve("sort_rules.json");
        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to create InvSort data directory", e);
        }
        load();
    }

    public static SortRuleStore getInstance() {
        if (instance == null) {
            instance = new SortRuleStore();
        }
        return instance;
    }

    public SortRules playerRules() {
        return currentWorldRules().playerRules;
    }

    public SortRules containerDefaultRules() {
        return currentWorldRules().containerRules;
    }

    public SortRules effectiveContainerRules(ContainerIdentity identity, String screenClassName) {
        WorldSortRuleConfig worldRules = currentWorldRules();
        SortRules containerOverride = containerOverride(worldRules, identity);
        if (containerOverride != null && containerOverride.enabled) {
            return containerOverride;
        }

        String screenKey = screenKey(screenClassName);
        if (screenKey != null) {
            SortRules override = worldRules.screenOverrides.get(screenKey);
            if (override != null && override.enabled) {
                return override;
            }
        }

        return worldRules.containerRules;
    }

    public SortRules editableContainerOverride(ContainerIdentity identity, String screenClassName) {
        WorldSortRuleConfig worldRules = currentWorldRules();
        String key = containerKey(identity);
        if (key != null) {
            SortRules legacy = containerOverride(worldRules, identity);
            if (legacy != null) {
                worldRules.containerOverrides.put(key, legacy);
                removeLegacyContainerKey(worldRules, identity);
                return worldRules.containerOverrides.get(key);
            }
            return worldRules.containerOverrides.computeIfAbsent(key, ignored -> worldRules.containerRules.copy());
        }

        String screenKey = screenKey(screenClassName);
        if (screenKey != null) {
            return worldRules.screenOverrides.computeIfAbsent(screenKey, ignored -> worldRules.containerRules.copy());
        }

        return worldRules.containerRules;
    }

    public boolean hasContainerOverride(ContainerIdentity identity, String screenClassName) {
        WorldSortRuleConfig worldRules = currentWorldRules();
        if (containerOverride(worldRules, identity) != null) {
            return true;
        }
        String screenKey = screenKey(screenClassName);
        return screenKey != null && worldRules.screenOverrides.containsKey(screenKey);
    }

    public void clearContainerOverride(ContainerIdentity identity, String screenClassName) {
        WorldSortRuleConfig worldRules = currentWorldRules();
        String key = containerKey(identity);
        if (key != null) {
            worldRules.containerOverrides.remove(key);
            removeLegacyContainerKey(worldRules, identity);
        } else {
            String screenKey = screenKey(screenClassName);
            if (screenKey != null) {
                worldRules.screenOverrides.remove(screenKey);
            }
        }
        save();
    }

    public String containerOverrideLabel(ContainerIdentity identity, String screenClassName) {
        if (identity != null) {
            return identity.getContainerType() + " " + identity.getPositionLabel();
        }
        if (screenClassName == null || screenClassName.isBlank()) {
            return "This screen";
        }
        return screenClassName.replace("Screen", "");
    }

    public void save() {
        normalize();
        try {
            writeJsonAtomically(saveFile, config);
        } catch (Exception e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to save InvSort rules", e);
        }
    }

    private void load() {
        if (!Files.exists(saveFile)) {
            normalize();
            return;
        }

        try (Reader reader = Files.newBufferedReader(saveFile, StandardCharsets.UTF_8)) {
            SortRuleConfig loaded = GSON.fromJson(reader, SortRuleConfig.class);
            if (loaded != null) {
                config = loaded;
            }
        } catch (Exception e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to load InvSort rules", e);
            Path backup = backupFile(saveFile);
            if (Files.exists(backup)) {
                try (Reader reader = Files.newBufferedReader(backup, StandardCharsets.UTF_8)) {
                    SortRuleConfig loaded = GSON.fromJson(reader, SortRuleConfig.class);
                    if (loaded != null) {
                        config = loaded;
                        normalize();
                        return;
                    }
                } catch (Exception backupError) {
                    tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to load InvSort rule backup", backupError);
                }
            }
            config = new SortRuleConfig();
        }
        normalize();
    }

    private WorldSortRuleConfig currentWorldRules() {
        normalize();
        String namespace = currentNamespace();
        migrateLegacyRules(namespace);
        WorldSortRuleConfig rules = config.worldRules.computeIfAbsent(namespace, ignored -> new WorldSortRuleConfig());
        rules.normalize();
        return rules;
    }

    private void migrateLegacyRules(String currentNamespace) {
        if (!hasLegacyRules() || UNKNOWN_NAMESPACE.equals(currentNamespace)) {
            return;
        }

        WorldSortRuleConfig current = config.worldRules.computeIfAbsent(currentNamespace, ignored -> new WorldSortRuleConfig());
        current.normalize();

        if (config.playerRules != null) {
            config.playerRules.normalize();
            if (current.playerRules == null || current.playerRules.isDefault()) {
                current.playerRules = config.playerRules.copy();
            }
        }
        if (config.containerRules != null) {
            config.containerRules.normalize();
            if (current.containerRules == null || current.containerRules.isDefault()) {
                current.containerRules = config.containerRules.copy();
            }
        }
        if (config.screenOverrides != null) {
            config.screenOverrides.forEach((key, rules) -> {
                if (key != null && rules != null) {
                    rules.normalize();
                    current.screenOverrides.putIfAbsent(key, rules.copy());
                }
            });
        }
        if (config.containerOverrides != null) {
            config.containerOverrides.forEach((key, rules) -> {
                if (key == null || rules == null) {
                    return;
                }
                rules.normalize();
                String namespace = currentNamespace;
                String containerKey = key;
                int separator = key.indexOf('|');
                if (separator > 0 && separator < key.length() - 1) {
                    namespace = TempestStudiosData.sanitize(key.substring(0, separator));
                    containerKey = key.substring(separator + 1);
                }
                WorldSortRuleConfig target = config.worldRules.computeIfAbsent(namespace, ignored -> new WorldSortRuleConfig());
                target.normalize();
                target.containerOverrides.putIfAbsent(containerKey, rules.copy());
            });
        }

        config.playerRules = null;
        config.containerRules = null;
        config.containerOverrides = null;
        config.screenOverrides = null;
        config.version = CURRENT_VERSION;
        save();
    }

    private boolean hasLegacyRules() {
        return config != null && (config.playerRules != null
                || config.containerRules != null
                || config.containerOverrides != null
                || config.screenOverrides != null);
    }

    private void normalize() {
        if (config == null) {
            config = new SortRuleConfig();
        }
        if (config.version <= 0) {
            config.version = 1;
        }
        if (config.worldRules == null) {
            config.worldRules = new LinkedHashMap<>();
        }
        config.worldRules.entrySet().removeIf(entry ->
                entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null);
        config.worldRules.values().forEach(WorldSortRuleConfig::normalize);
        if (config.version < 3) {
            migratePlayerSlotRulesToHotbarAwareLayout();
        }
        if (!hasLegacyRules()) {
            config.version = CURRENT_VERSION;
        }
    }

    private void migratePlayerSlotRulesToHotbarAwareLayout() {
        if (config.playerRules != null) {
            config.playerRules.shiftSlotRules(9);
        }
        for (WorldSortRuleConfig worldRules : config.worldRules.values()) {
            worldRules.normalize();
            worldRules.playerRules.shiftSlotRules(9);
        }
        config.version = CURRENT_VERSION;
    }

    private static String containerKey(ContainerIdentity identity) {
        if (identity == null || identity.getIdentityKey() == null || identity.getIdentityKey().isBlank()) {
            return null;
        }
        return identity.getIdentityKey();
    }

    private static String legacyContainerKey(ContainerIdentity identity) {
        String containerKey = containerKey(identity);
        if (containerKey == null) {
            return null;
        }
        String namespace = identity.getNamespace();
        if (namespace == null || namespace.isBlank()) {
            namespace = UNKNOWN_NAMESPACE;
        }
        return namespace + "|" + containerKey;
    }

    private static SortRules containerOverride(WorldSortRuleConfig worldRules, ContainerIdentity identity) {
        String key = containerKey(identity);
        if (key != null) {
            SortRules rules = worldRules.containerOverrides.get(key);
            if (rules != null) {
                return rules;
            }
        }
        String legacyKey = legacyContainerKey(identity);
        return legacyKey == null ? null : worldRules.containerOverrides.get(legacyKey);
    }

    private static void removeLegacyContainerKey(WorldSortRuleConfig worldRules, ContainerIdentity identity) {
        String key = containerKey(identity);
        String legacyKey = legacyContainerKey(identity);
        if (legacyKey != null && !legacyKey.equals(key)) {
            worldRules.containerOverrides.remove(legacyKey);
        }
    }

    private static String screenKey(String screenClassName) {
        if (screenClassName == null || screenClassName.isBlank()) {
            return null;
        }
        return "screen|" + screenClassName.toLowerCase(Locale.ROOT);
    }

    private static String currentNamespace() {
        String namespace = TrackingNamespace.current(Minecraft.getInstance());
        if (namespace == null || namespace.isBlank()) {
            return UNKNOWN_NAMESPACE;
        }
        return TempestStudiosData.sanitize(namespace);
    }

    private static void writeJsonAtomically(Path target, Object data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            if (Files.exists(target)) {
                Files.copy(target, backupFile(target), StandardCopyOption.REPLACE_EXISTING);
            }
            moveIntoPlace(tempFile, target);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    private static void moveIntoPlace(Path tempFile, Path target) throws IOException {
        try {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path backupFile(Path target) {
        return target.resolveSibling(target.getFileName().toString() + ".bak");
    }

    private static final class SortRuleConfig {
        int version = CURRENT_VERSION;
        Map<String, WorldSortRuleConfig> worldRules = new LinkedHashMap<>();
        SortRules playerRules;
        SortRules containerRules;
        Map<String, SortRules> containerOverrides;
        Map<String, SortRules> screenOverrides;
    }

    private static final class WorldSortRuleConfig {
        SortRules playerRules = new SortRules();
        SortRules containerRules = new SortRules();
        Map<String, SortRules> containerOverrides = new LinkedHashMap<>();
        Map<String, SortRules> screenOverrides = new LinkedHashMap<>();

        void normalize() {
            if (playerRules == null) {
                playerRules = new SortRules();
            }
            if (containerRules == null) {
                containerRules = new SortRules();
            }
            if (containerOverrides == null) {
                containerOverrides = new LinkedHashMap<>();
            }
            if (screenOverrides == null) {
                screenOverrides = new LinkedHashMap<>();
            }
            playerRules.normalize();
            containerRules.normalize();
            containerOverrides.entrySet().removeIf(entry -> entry.getKey() == null
                    || entry.getKey().isBlank()
                    || entry.getValue() == null);
            screenOverrides.entrySet().removeIf(entry -> entry.getKey() == null
                    || entry.getKey().isBlank()
                    || entry.getValue() == null);
            containerOverrides.values().forEach(SortRules::normalize);
            screenOverrides.values().forEach(SortRules::normalize);
        }
    }

    public static final class SortRules {
        public boolean enabled = true;
        public boolean absoluteItemOrder = false;
        public List<String> categoryOrder = new ArrayList<>();
        public List<String> itemOrder = new ArrayList<>();
        public Map<Integer, SlotRule> slotRules = new LinkedHashMap<>();

        public SortRules copy() {
            SortRules copy = new SortRules();
            copy.enabled = enabled;
            copy.absoluteItemOrder = absoluteItemOrder;
            copy.categoryOrder = new ArrayList<>(categoryOrder);
            copy.itemOrder = new ArrayList<>(itemOrder);
            for (Map.Entry<Integer, SlotRule> entry : slotRules.entrySet()) {
                copy.slotRules.put(entry.getKey(), entry.getValue().copy());
            }
            return copy;
        }

        public void clear() {
            enabled = true;
            absoluteItemOrder = false;
            categoryOrder.clear();
            itemOrder.clear();
            slotRules.clear();
        }

        public void normalize() {
            if (categoryOrder == null) {
                categoryOrder = new ArrayList<>();
            }
            if (itemOrder == null) {
                itemOrder = new ArrayList<>();
            }
            if (slotRules == null) {
                slotRules = new LinkedHashMap<>();
            }
            slotRules.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
        }

        void shiftSlotRules(int offset) {
            normalize();
            if (slotRules.isEmpty()) {
                return;
            }
            Map<Integer, SlotRule> shifted = new LinkedHashMap<>();
            for (Map.Entry<Integer, SlotRule> entry : slotRules.entrySet()) {
                Integer slot = entry.getKey();
                if (slot != null && slot >= 0) {
                    shifted.put(slot + offset, entry.getValue());
                }
            }
            slotRules = shifted;
        }

        public boolean usesCustomOrder() {
            return !categoryOrder.isEmpty() || !itemOrder.isEmpty();
        }

        private boolean isDefault() {
            normalize();
            return enabled
                    && !absoluteItemOrder
                    && categoryOrder.isEmpty()
                    && itemOrder.isEmpty()
                    && slotRules.isEmpty();
        }

        public SlotRule ruleFor(int slotIndex) {
            SlotRule rule = slotRules.get(slotIndex);
            return rule == null ? SlotRule.EMPTY : rule;
        }

        public SlotRule mutableRuleFor(int slotIndex) {
            SlotRule rule = slotRules.computeIfAbsent(slotIndex, ignored -> new SlotRule());
            rule.normalize();
            return rule;
        }

        public void cleanupSlotRule(int slotIndex) {
            SlotRule rule = slotRules.get(slotIndex);
            if (rule != null && rule.isEmpty()) {
                slotRules.remove(slotIndex);
            }
        }

        public int categoryRank(String categoryKey) {
            return rank(categoryOrder, categoryKey);
        }

        public int itemRank(String itemId) {
            return rank(itemOrder, itemId);
        }

        private static int rank(List<String> values, String value) {
            if (value == null || values == null) {
                return -1;
            }
            for (int i = 0; i < values.size(); i++) {
                if (value.equals(values.get(i))) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static final class SlotRule {
        static final SlotRule EMPTY = new SlotRule();

        public boolean restricted = false;
        public boolean unlocked = false;
        public String reservedItemId;

        SlotRule copy() {
            SlotRule copy = new SlotRule();
            copy.restricted = restricted;
            copy.unlocked = unlocked;
            copy.reservedItemId = reservedItemId;
            return copy;
        }

        void normalize() {
            if (reservedItemId != null && reservedItemId.isBlank()) {
                reservedItemId = null;
            }
        }

        public boolean hasReservation() {
            return reservedItemId != null && !reservedItemId.isBlank();
        }

        public boolean isEmpty() {
            return !restricted && !unlocked && !hasReservation();
        }
    }
}
