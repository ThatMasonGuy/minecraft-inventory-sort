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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SortRuleStore instance;

    private final Path saveFile;
    private SortRuleConfig config = new SortRuleConfig();

    private SortRuleStore() {
        Minecraft mc = Minecraft.getInstance();
        Path modDir = mc.gameDirectory.toPath().resolve("inventorysort");
        this.saveFile = modDir.resolve("sort_rules.json");
        try {
            Files.createDirectories(modDir);
        } catch (IOException e) {
            tempeststudios.inventorysort.core.InventorySortCore.LOGGER.error("Failed to create inventorysort directory", e);
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
        normalize();
        return config.playerRules;
    }

    public SortRules containerDefaultRules() {
        normalize();
        return config.containerRules;
    }

    public SortRules effectiveContainerRules(ContainerIdentity identity, String screenClassName) {
        normalize();
        String containerKey = containerKey(identity);
        if (containerKey != null) {
            SortRules override = config.containerOverrides.get(containerKey);
            if (override != null && override.enabled) {
                return override;
            }
        }

        String screenKey = screenKey(screenClassName);
        if (screenKey != null) {
            SortRules override = config.screenOverrides.get(screenKey);
            if (override != null && override.enabled) {
                return override;
            }
        }

        return config.containerRules;
    }

    public SortRules editableContainerOverride(ContainerIdentity identity, String screenClassName) {
        normalize();
        String key = containerKey(identity);
        if (key != null) {
            return config.containerOverrides.computeIfAbsent(key, ignored -> config.containerRules.copy());
        }

        String screenKey = screenKey(screenClassName);
        if (screenKey != null) {
            return config.screenOverrides.computeIfAbsent(screenKey, ignored -> config.containerRules.copy());
        }

        return config.containerRules;
    }

    public boolean hasContainerOverride(ContainerIdentity identity, String screenClassName) {
        normalize();
        String key = containerKey(identity);
        if (key != null) {
            return config.containerOverrides.containsKey(key);
        }
        String screenKey = screenKey(screenClassName);
        return screenKey != null && config.screenOverrides.containsKey(screenKey);
    }

    public void clearContainerOverride(ContainerIdentity identity, String screenClassName) {
        normalize();
        String key = containerKey(identity);
        if (key != null) {
            config.containerOverrides.remove(key);
        } else {
            String screenKey = screenKey(screenClassName);
            if (screenKey != null) {
                config.screenOverrides.remove(screenKey);
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

    private void normalize() {
        if (config == null) {
            config = new SortRuleConfig();
        }
        if (config.playerRules == null) {
            config.playerRules = new SortRules();
        }
        if (config.containerRules == null) {
            config.containerRules = new SortRules();
        }
        if (config.containerOverrides == null) {
            config.containerOverrides = new LinkedHashMap<>();
        }
        if (config.screenOverrides == null) {
            config.screenOverrides = new LinkedHashMap<>();
        }
        config.playerRules.normalize();
        config.containerRules.normalize();
        config.containerOverrides.values().forEach(SortRules::normalize);
        config.screenOverrides.values().forEach(SortRules::normalize);
    }

    private static String containerKey(ContainerIdentity identity) {
        if (identity == null || identity.getIdentityKey() == null || identity.getIdentityKey().isBlank()) {
            return null;
        }
        String namespace = identity.getNamespace();
        if (namespace == null || namespace.isBlank()) {
            namespace = "unknown";
        }
        return namespace + "|" + identity.getIdentityKey();
    }

    private static String screenKey(String screenClassName) {
        if (screenClassName == null || screenClassName.isBlank()) {
            return null;
        }
        return "screen|" + screenClassName.toLowerCase(Locale.ROOT);
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
        int version = 1;
        SortRules playerRules = new SortRules();
        SortRules containerRules = new SortRules();
        Map<String, SortRules> containerOverrides = new LinkedHashMap<>();
        Map<String, SortRules> screenOverrides = new LinkedHashMap<>();
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

        public boolean usesCustomOrder() {
            return !categoryOrder.isEmpty() || !itemOrder.isEmpty();
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
        public String reservedItemId;

        SlotRule copy() {
            SlotRule copy = new SlotRule();
            copy.restricted = restricted;
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
            return !restricted && !hasReservation();
        }
    }
}
