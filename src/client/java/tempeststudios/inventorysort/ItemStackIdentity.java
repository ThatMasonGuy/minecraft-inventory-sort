package tempeststudios.inventorysort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import tempeststudios.inventorysort.compat.core.ItemStackIdentityCompat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared item identity model for Search and Catalogue.
 *
 * <p>Plain stacks keep the historic {@code minecraft:item_id} key. Stacks with
 * NBT/data-component differences get a stable variant key so enchantments,
 * potions, named stacks, and other component-bearing items do not collapse into
 * their base item in tracking or reports.
 */
public final class ItemStackIdentity {
    private static final String VARIANT_MARKER = "|v1|";
    private static final int SLUG_TOKEN_LIMIT = 14;
    private static final int HASH_LENGTH = 18;

    private ItemStackIdentity() {
    }

    public static Info info(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return legacyInfo("minecraft:air");
        }

        String baseItemId = baseItemId(stack);
        String displayName = stack.getHoverName().getString();
        boolean variant = ItemStackIdentityCompat.hasVariantData(stack);
        String payload = variant ? ItemStackIdentityCompat.identityPayload(stack) : "";
        String variantText = variant ? enrichedVariantText(baseItemId, payload) : "";
        String key = variant ? variantKey(baseItemId, payload, variantText) : baseItemId;
        String searchText = searchText(baseItemId, displayName, key, variantText);
        return new Info(key, baseItemId, displayName, searchText, variantText, variant);
    }

    public static Info legacyInfo(String itemKey) {
        String baseItemId = baseItemId(itemKey);
        String displayName = displayNameForBaseId(baseItemId);
        String variantText = variantTextFromKey(itemKey);
        boolean variant = isVariantKey(itemKey);
        if (variant && !variantText.isBlank()) {
            displayName = displayName + " (" + titleCase(variantText) + ")";
        }
        return new Info(itemKey, baseItemId, displayName,
                searchText(baseItemId, displayName, itemKey, variantText), variantText, variant);
    }

    public static String key(ItemStack stack) {
        return info(stack).key();
    }

    public static String baseItemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static String baseItemId(String itemKey) {
        if (itemKey == null || itemKey.isBlank()) {
            return "minecraft:air";
        }
        int marker = itemKey.indexOf(VARIANT_MARKER);
        return marker > 0 ? itemKey.substring(0, marker) : itemKey;
    }

    public static boolean isVariantKey(String itemKey) {
        return itemKey != null && itemKey.contains(VARIANT_MARKER);
    }

    public static Info infoOrFallback(String itemKey, Info info) {
        if (info != null && info.key() != null && !info.key().isBlank()) {
            return info;
        }
        return legacyInfo(itemKey);
    }

    public static String displayName(String itemKey, Info info) {
        return infoOrFallback(itemKey, info).displayName();
    }

    public static String searchText(String itemKey, Info info) {
        return infoOrFallback(itemKey, info).searchText();
    }

    public static String variantSearchText(String itemKey, Info info) {
        return infoOrFallback(itemKey, info).variantText();
    }

    public static boolean matchesSearch(String itemKey, Info info, String normalizedQuery) {
        String query = normalizedQuery == null ? "" : normalizedQuery.trim().toLowerCase(Locale.ROOT);
        return query.isEmpty() || searchText(itemKey, info).contains(query);
    }

    public static boolean matchesVariantQuery(String itemKey, Info info, String normalizedQuery) {
        if (!isVariantKey(itemKey)) {
            return false;
        }
        String query = normalizedQuery == null ? "" : normalizedQuery.trim().toLowerCase(Locale.ROOT);
        String text = variantSearchText(itemKey, info);
        return query.isEmpty() || text.contains(query);
    }

    public static String normalizeSearchText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text.toLowerCase(Locale.ROOT).replace("minecraft:", "minecraft_");
        StringBuilder out = new StringBuilder(cleaned.length());
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            out.append(Character.isLetterOrDigit(c) || c == '_' ? c : ' ');
        }

        Set<String> tokens = new LinkedHashSet<>();
        for (String token : out.toString().split("\\s+")) {
            addToken(tokens, token);
            if (token.contains("_")) {
                for (String part : token.split("_+")) {
                    addToken(tokens, part);
                }
            }
        }
        return String.join(" ", tokens);
    }

    private static void addToken(Set<String> tokens, String token) {
        if (token == null || token.length() < 2) {
            return;
        }
        tokens.add(token);
        if (token.startsWith("minecraft_") && token.length() > "minecraft_".length()) {
            tokens.add(token.substring("minecraft_".length()));
        }
    }

    private static String variantKey(String baseItemId, String payload, String variantText) {
        String hash = shortHash(payload);
        String slug = Arrays.stream(variantText.split("\\s+"))
                .filter(token -> token.length() > 1)
                .filter(token -> !token.equals("minecraft"))
                .limit(SLUG_TOKEN_LIMIT)
                .collect(Collectors.joining("-"));
        if (slug.isBlank()) {
            slug = "data";
        }
        return baseItemId + VARIANT_MARKER + hash + "|" + slug;
    }

    private static String enrichedVariantText(String baseItemId, String payload) {
        String text = normalizeSearchText(payload);
        if (payload.toLowerCase(Locale.ROOT).contains("enchant")) {
            text = appendTerms(text, "enchanted enchantment");
        }
        if (baseItemId.contains("potion") || payload.toLowerCase(Locale.ROOT).contains("potion")) {
            text = appendTerms(text, "potion effect status");
        }
        return text;
    }

    private static String searchText(String baseItemId, String displayName, String itemKey, String variantText) {
        String text = normalizeSearchText(displayName + " " + baseItemId + " " + itemKey + " " + variantText);
        if (baseItemId.equals("minecraft:potion")) {
            text = appendTerms(text, "water bottle awkward potion mundane thick");
        } else if (baseItemId.equals("minecraft:splash_potion")) {
            text = appendTerms(text, "splash water bottle awkward potion mundane thick");
        } else if (baseItemId.equals("minecraft:lingering_potion")) {
            text = appendTerms(text, "lingering water bottle awkward potion mundane thick");
        }
        return text;
    }

    private static String appendTerms(String existing, String terms) {
        String extra = normalizeSearchText(terms);
        if (existing == null || existing.isBlank()) {
            return extra;
        }
        if (extra.isBlank()) {
            return existing;
        }
        return existing + " " + extra;
    }

    private static String variantTextFromKey(String itemKey) {
        if (!isVariantKey(itemKey)) {
            return "";
        }
        String[] parts = itemKey.split("\\|", 4);
        if (parts.length < 4) {
            return "";
        }
        return normalizeSearchText(parts[3].replace('-', ' '));
    }

    private static String displayNameForBaseId(String baseItemId) {
        String name = baseItemId == null ? "unknown" : baseItemId;
        if (name.contains(":")) {
            name = name.substring(name.lastIndexOf(':') + 1);
        }
        return titleCase(name.replace('_', ' '));
    }

    private static String titleCase(String text) {
        if (text == null || text.isBlank()) {
            return "Unknown";
        }
        StringBuilder out = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.length() == 0 ? "Unknown" : out.toString();
    }

    private static String shortHash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed).substring(0, HASH_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toUnsignedString((payload == null ? "" : payload).hashCode(), 36);
        }
    }

    public static final class Info {
        private String key;
        private String baseItemId;
        private String displayName;
        private String searchText;
        private String variantText;
        private boolean variant;

        public Info() {
        }

        private Info(String key,
                     String baseItemId,
                     String displayName,
                     String searchText,
                     String variantText,
                     boolean variant) {
            this.key = key;
            this.baseItemId = baseItemId;
            this.displayName = displayName;
            this.searchText = searchText;
            this.variantText = variantText;
            this.variant = variant;
        }

        public String key() {
            return key;
        }

        public String baseItemId() {
            return baseItemId != null && !baseItemId.isBlank() ? baseItemId : ItemStackIdentity.baseItemId(key);
        }

        public String displayName() {
            return displayName != null && !displayName.isBlank()
                    ? displayName
                    : displayNameForBaseId(baseItemId());
        }

        public String searchText() {
            return searchText != null && !searchText.isBlank()
                    ? searchText
                    : ItemStackIdentity.searchText(baseItemId(), displayName(), key, variantText());
        }

        public String variantText() {
            return variantText != null ? variantText : variantTextFromKey(key);
        }

        public boolean variant() {
            return variant || isVariantKey(key);
        }
    }
}
