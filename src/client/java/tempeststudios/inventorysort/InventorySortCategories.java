package tempeststudios.inventorysort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class InventorySortCategories {
    private InventorySortCategories() {
    }

    public record CategoryDefinition(String key, String label) {
    }

    public record CategoryQuery(String alias, Set<String> categoryKeys) {
        public boolean known() {
            return !categoryKeys.isEmpty();
        }

        public boolean matches(Item item) {
            return InventorySortCategories.matches(item, this);
        }

        public int rank(String categoryKey) {
            int index = 0;
            for (String key : categoryKeys) {
                if (key.equals(categoryKey)) {
                    return index;
                }
                index++;
            }
            return Integer.MAX_VALUE;
        }
    }

    public static final List<CategoryDefinition> DEFAULT_CATEGORIES = List.of(
            new CategoryDefinition("00_storage_bundle", "Bundles"),
            new CategoryDefinition("00_storage_ender_chest", "Ender Chests"),
            new CategoryDefinition("00_storage_shulker", "Shulker Boxes"),
            new CategoryDefinition("01_wood_logs", "Logs"),
            new CategoryDefinition("02_wood_planks", "Planks"),
            new CategoryDefinition("03_wood_items", "Wood Items"),
            new CategoryDefinition("04_wood_leaves", "Leaves"),
            new CategoryDefinition("05_wood_saplings", "Saplings"),
            new CategoryDefinition("10_terrain_dirt", "Dirt"),
            new CategoryDefinition("11_terrain_stone", "Stone"),
            new CategoryDefinition("12_terrain_sand", "Sand"),
            new CategoryDefinition("20_minerals_ores", "Ores"),
            new CategoryDefinition("21_minerals_gems", "Gems"),
            new CategoryDefinition("22_minerals_ingots", "Ingots"),
            new CategoryDefinition("23_minerals_nuggets", "Nuggets"),
            new CategoryDefinition("24_minerals_dusts", "Dusts"),
            new CategoryDefinition("30_redstone", "Redstone"),
            new CategoryDefinition("40_build_slabs", "Slabs"),
            new CategoryDefinition("41_build_stairs", "Stairs"),
            new CategoryDefinition("42_build_edges", "Fences/Walls"),
            new CategoryDefinition("43_build_doors", "Doors"),
            new CategoryDefinition("44_build_glass", "Glass"),
            new CategoryDefinition("45_build_wool", "Wool"),
            new CategoryDefinition("46_build_concrete", "Concrete"),
            new CategoryDefinition("50_food_raw_meat", "Raw Meat"),
            new CategoryDefinition("51_food_cooked_meat", "Cooked Meat"),
            new CategoryDefinition("52_food_crops", "Crops"),
            new CategoryDefinition("53_food_prepared", "Prepared Food"),
            new CategoryDefinition("54_food_fruits", "Fruit"),
            new CategoryDefinition("60_combat_weapons", "Weapons"),
            new CategoryDefinition("61_tools", "Tools"),
            new CategoryDefinition("62_armor", "Armor"),
            new CategoryDefinition("70_potions_brewing", "Potions/Brewing"),
            new CategoryDefinition("80_misc_storage", "Storage"),
            new CategoryDefinition("81_misc_books", "Books"),
            new CategoryDefinition("82_misc_mob_drops", "Mob Drops"),
            new CategoryDefinition("90_misc", "Misc")
    );

    private static final Map<String, Set<String>> CATEGORY_ALIASES = buildAliases();

    public static CategoryQuery categoryQuery(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith(":")) {
            return null;
        }

        String alias = normalize(trimmed.substring(1));
        if (alias.isEmpty()) {
            return new CategoryQuery("", Collections.emptySet());
        }

        Set<String> keys = CATEGORY_ALIASES.get(alias);
        return new CategoryQuery(alias, keys == null ? Collections.emptySet() : keys);
    }

    public static boolean matches(Item item, CategoryQuery query) {
        if (item == null || query == null || !query.known()) {
            return false;
        }
        return query.categoryKeys().contains(categoryKey(item));
    }

    public static String categoryKey(Item item) {
        if (item == null) {
            return "99_empty";
        }
        return categoryKey(new ItemStack(item));
    }

    public static String categoryKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "99_empty";
        }

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;

        if (path.contains("bundle")) {
            return "00_storage_bundle";
        }
        if (path.equals("ender_chest")) {
            return "00_storage_ender_chest";
        }
        if (path.contains("shulker_box")) {
            return "00_storage_shulker";
        }

        if (path.equals("beef") || path.equals("porkchop") || path.equals("chicken")
                || path.equals("mutton") || path.equals("rabbit") || path.equals("cod")
                || path.equals("salmon") || path.equals("tropical_fish") || path.equals("pufferfish")) {
            return "50_food_raw_meat";
        }
        if (path.equals("cooked_beef") || path.equals("cooked_porkchop")
                || path.equals("cooked_chicken") || path.equals("cooked_mutton")
                || path.equals("cooked_rabbit") || path.equals("cooked_cod")
                || path.equals("cooked_salmon")) {
            return "51_food_cooked_meat";
        }
        if (path.equals("potato") || path.equals("carrot") || path.equals("beetroot")
                || path.equals("wheat") || path.equals("wheat_seeds") || path.equals("beetroot_seeds")
                || path.equals("pumpkin_seeds") || path.equals("melon_seeds") || path.equals("torchflower_seeds")
                || path.equals("pitcher_pod")) {
            return "52_food_crops";
        }
        if (path.equals("baked_potato") || path.equals("bread") || path.equals("cookie")
                || path.equals("pumpkin_pie") || path.equals("cake") || path.contains("stew")
                || path.contains("soup")) {
            return "53_food_prepared";
        }
        if (path.equals("apple") || path.equals("golden_apple") || path.equals("enchanted_golden_apple")
                || path.equals("melon_slice") || path.equals("sweet_berries") || path.equals("glow_berries")
                || path.equals("chorus_fruit") || path.equals("honey_bottle") || path.equals("honeycomb")) {
            return "54_food_fruits";
        }

        if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.contains("hyphae")) {
            return "01_wood_logs";
        }
        if (path.endsWith("_planks")) {
            return "02_wood_planks";
        }
        if (path.equals("stick") || path.equals("bowl") || path.equals("ladder") || path.equals("scaffolding")) {
            return "03_wood_items";
        }
        if (path.endsWith("_leaves")) {
            return "04_wood_leaves";
        }
        if (path.endsWith("_sapling")) {
            return "05_wood_saplings";
        }

        if (path.contains("dirt") || path.contains("grass_block") || path.contains("podzol")
                || path.contains("mycelium") || path.contains("mud")) {
            return "10_terrain_dirt";
        }
        if (path.contains("stone") || path.contains("cobblestone") || path.contains("deepslate")
                || path.contains("granite") || path.contains("diorite") || path.contains("andesite")
                || path.contains("tuff") || path.contains("calcite") || path.contains("basalt")
                || path.contains("blackstone") || path.contains("netherrack") || path.contains("end_stone")) {
            return "11_terrain_stone";
        }
        if (path.contains("sand") || path.contains("gravel") || path.contains("clay")) {
            return "12_terrain_sand";
        }

        if (path.endsWith("_ore") || path.contains("_ore_") || path.startsWith("raw_")) {
            return "20_minerals_ores";
        }
        if (path.equals("diamond") || path.equals("emerald") || path.equals("amethyst_shard")
                || path.equals("lapis_lazuli") || path.equals("prismarine_shard") || path.equals("prismarine_crystals")
                || path.equals("quartz") || path.equals("echo_shard")) {
            return "21_minerals_gems";
        }
        if (path.endsWith("_ingot")) {
            return "22_minerals_ingots";
        }
        if (path.endsWith("_nugget")) {
            return "23_minerals_nuggets";
        }
        if (path.equals("redstone") || path.equals("glowstone_dust") || path.equals("gunpowder")
                || path.equals("blaze_powder") || path.equals("bone_meal")) {
            return "24_minerals_dusts";
        }

        if (path.contains("redstone") || path.contains("repeater") || path.contains("comparator")
                || path.contains("piston") || path.contains("observer") || path.contains("hopper")
                || path.contains("dispenser") || path.contains("dropper") || path.contains("lever")
                || path.contains("button") || path.contains("pressure_plate") || path.contains("rail")
                || path.contains("detector")) {
            return "30_redstone";
        }

        if (path.contains("slab")) {
            return "40_build_slabs";
        }
        if (path.contains("stairs")) {
            return "41_build_stairs";
        }
        if (path.contains("fence") || path.contains("wall") || path.contains("gate")) {
            return "42_build_edges";
        }
        if (path.contains("door") || path.contains("trapdoor")) {
            return "43_build_doors";
        }
        if (path.contains("glass") || path.contains("pane")) {
            return "44_build_glass";
        }
        if (path.contains("wool") || path.contains("carpet")) {
            return "45_build_wool";
        }
        if (path.contains("concrete") || path.contains("terracotta")) {
            return "46_build_concrete";
        }

        if (path.contains("sword") || path.contains("bow") || path.contains("crossbow")
                || path.contains("trident") || path.equals("arrow") || path.equals("spectral_arrow")
                || path.contains("tipped_arrow")) {
            return "60_combat_weapons";
        }
        if (path.contains("axe") || path.contains("pickaxe") || path.contains("shovel")
                || path.contains("hoe") || path.contains("shears") || path.equals("flint_and_steel")
                || path.equals("fishing_rod")) {
            return "61_tools";
        }
        if (path.contains("helmet") || path.contains("chestplate") || path.contains("leggings")
                || path.contains("boots") || path.equals("shield") || path.equals("elytra")) {
            return "62_armor";
        }

        if (path.contains("potion") || path.equals("glass_bottle") || path.equals("dragon_breath")
                || path.equals("fermented_spider_eye") || path.equals("ghast_tear") || path.equals("magma_cream")
                || path.equals("blaze_rod") || path.equals("nether_wart") || path.equals("spider_eye")
                || path.equals("phantom_membrane")) {
            return "70_potions_brewing";
        }

        if (path.contains("chest") || path.contains("barrel") || path.equals("bucket")) {
            return "80_misc_storage";
        }
        if (path.contains("book") || path.equals("paper") || path.equals("writable_book")
                || path.equals("written_book") || path.equals("enchanted_book")) {
            return "81_misc_books";
        }
        if (path.equals("string") || path.equals("leather") || path.equals("feather")
                || path.equals("bone") || path.equals("rotten_flesh") || path.equals("slime_ball")
                || path.equals("ender_pearl") || path.equals("blaze_rod")) {
            return "82_misc_mob_drops";
        }

        return "90_misc";
    }

    private static Map<String, Set<String>> buildAliases() {
        Map<String, LinkedHashSet<String>> aliases = new LinkedHashMap<>();
        for (CategoryDefinition category : DEFAULT_CATEGORIES) {
            addAlias(aliases, category.key(), category.key());
            addAlias(aliases, withoutRank(category.key()), category.key());
            addAlias(aliases, tail(category.key()), category.key());
            addAlias(aliases, category.label(), category.key());
        }

        addGroup(aliases, "wood", "01_wood_logs", "02_wood_planks", "03_wood_items", "04_wood_leaves", "05_wood_saplings");
        addGroup(aliases, "woods", "01_wood_logs", "02_wood_planks", "03_wood_items", "04_wood_leaves", "05_wood_saplings");
        addGroup(aliases, "wooden", "01_wood_logs", "02_wood_planks", "03_wood_items", "04_wood_leaves", "05_wood_saplings");
        addGroup(aliases, "trees", "01_wood_logs", "04_wood_leaves", "05_wood_saplings");
        addGroup(aliases, "tree", "01_wood_logs", "04_wood_leaves", "05_wood_saplings");
        addGroup(aliases, "log", "01_wood_logs");
        addGroup(aliases, "plank", "02_wood_planks");
        addGroup(aliases, "leaf", "04_wood_leaves");
        addGroup(aliases, "sapling", "05_wood_saplings");

        addGroup(aliases, "terrain", "10_terrain_dirt", "11_terrain_stone", "12_terrain_sand");
        addGroup(aliases, "ground", "10_terrain_dirt", "11_terrain_stone", "12_terrain_sand");
        addGroup(aliases, "earth", "10_terrain_dirt", "12_terrain_sand");
        addGroup(aliases, "stones", "11_terrain_stone");
        addGroup(aliases, "rock", "11_terrain_stone");
        addGroup(aliases, "rocks", "11_terrain_stone");

        addGroup(aliases, "minerals", "20_minerals_ores", "21_minerals_gems", "22_minerals_ingots", "23_minerals_nuggets", "24_minerals_dusts");
        addGroup(aliases, "mineral", "20_minerals_ores", "21_minerals_gems", "22_minerals_ingots", "23_minerals_nuggets", "24_minerals_dusts");
        addGroup(aliases, "resources", "20_minerals_ores", "21_minerals_gems", "22_minerals_ingots", "23_minerals_nuggets", "24_minerals_dusts");
        addGroup(aliases, "materials", "20_minerals_ores", "21_minerals_gems", "22_minerals_ingots", "23_minerals_nuggets", "24_minerals_dusts");
        addGroup(aliases, "ore", "20_minerals_ores");
        addGroup(aliases, "gem", "21_minerals_gems");
        addGroup(aliases, "ingot", "22_minerals_ingots");
        addGroup(aliases, "nugget", "23_minerals_nuggets");
        addGroup(aliases, "dust", "24_minerals_dusts");
        addGroup(aliases, "dusts", "24_minerals_dusts");

        addGroup(aliases, "mechanisms", "30_redstone");
        addGroup(aliases, "mechanism", "30_redstone");
        addGroup(aliases, "contraptions", "30_redstone");
        addGroup(aliases, "contraption", "30_redstone");

        addGroup(aliases, "building", "40_build_slabs", "41_build_stairs", "42_build_edges", "43_build_doors", "44_build_glass", "45_build_wool", "46_build_concrete");
        addGroup(aliases, "build", "40_build_slabs", "41_build_stairs", "42_build_edges", "43_build_doors", "44_build_glass", "45_build_wool", "46_build_concrete");
        addGroup(aliases, "blocks", "40_build_slabs", "41_build_stairs", "42_build_edges", "43_build_doors", "44_build_glass", "45_build_wool", "46_build_concrete");
        addGroup(aliases, "block", "40_build_slabs", "41_build_stairs", "42_build_edges", "43_build_doors", "44_build_glass", "45_build_wool", "46_build_concrete");
        addGroup(aliases, "decor", "40_build_slabs", "41_build_stairs", "42_build_edges", "43_build_doors", "44_build_glass", "45_build_wool", "46_build_concrete");
        addGroup(aliases, "decoration", "40_build_slabs", "41_build_stairs", "42_build_edges", "43_build_doors", "44_build_glass", "45_build_wool", "46_build_concrete");
        addGroup(aliases, "slab", "40_build_slabs");
        addGroup(aliases, "stair", "41_build_stairs");
        addGroup(aliases, "fence", "42_build_edges");
        addGroup(aliases, "fences", "42_build_edges");
        addGroup(aliases, "wall", "42_build_edges");
        addGroup(aliases, "walls", "42_build_edges");
        addGroup(aliases, "door", "43_build_doors");

        addGroup(aliases, "food", "50_food_raw_meat", "51_food_cooked_meat", "52_food_crops", "53_food_prepared", "54_food_fruits");
        addGroup(aliases, "foods", "50_food_raw_meat", "51_food_cooked_meat", "52_food_crops", "53_food_prepared", "54_food_fruits");
        addGroup(aliases, "meat", "50_food_raw_meat", "51_food_cooked_meat");
        addGroup(aliases, "meats", "50_food_raw_meat", "51_food_cooked_meat");
        addGroup(aliases, "crop", "52_food_crops");
        addGroup(aliases, "fruit", "54_food_fruits");
        addGroup(aliases, "fruits", "54_food_fruits");

        addGroup(aliases, "tool", "61_tools");
        addGroup(aliases, "gear", "60_combat_weapons", "61_tools", "62_armor");
        addGroup(aliases, "equipment", "60_combat_weapons", "61_tools", "62_armor");
        addGroup(aliases, "weapon", "60_combat_weapons");
        addGroup(aliases, "combat", "60_combat_weapons");
        addGroup(aliases, "armour", "62_armor");

        addGroup(aliases, "potion", "70_potions_brewing");
        addGroup(aliases, "brewing", "70_potions_brewing");

        addGroup(aliases, "storage", "00_storage_bundle", "00_storage_ender_chest", "00_storage_shulker", "80_misc_storage");
        addGroup(aliases, "container", "00_storage_ender_chest", "00_storage_shulker", "80_misc_storage");
        addGroup(aliases, "containers", "00_storage_ender_chest", "00_storage_shulker", "80_misc_storage");
        addGroup(aliases, "bundle", "00_storage_bundle");
        addGroup(aliases, "enderchest", "00_storage_ender_chest");
        addGroup(aliases, "shulker", "00_storage_shulker");
        addGroup(aliases, "shulkers", "00_storage_shulker");

        addGroup(aliases, "book", "81_misc_books");
        addGroup(aliases, "paper", "81_misc_books");
        addGroup(aliases, "drop", "82_misc_mob_drops");
        addGroup(aliases, "drops", "82_misc_mob_drops");
        addGroup(aliases, "mobdrop", "82_misc_mob_drops");
        addGroup(aliases, "mobdrops", "82_misc_mob_drops");

        Map<String, Set<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : aliases.entrySet()) {
            out.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }

    private static void addAlias(Map<String, LinkedHashSet<String>> aliases, String alias, String key) {
        addGroup(aliases, alias, key);
    }

    private static void addGroup(Map<String, LinkedHashSet<String>> aliases, String alias, String... keys) {
        String normalized = normalize(alias);
        if (normalized.isEmpty()) {
            return;
        }
        LinkedHashSet<String> values = aliases.computeIfAbsent(normalized, ignored -> new LinkedHashSet<>());
        Collections.addAll(values, keys);
    }

    private static String withoutRank(String key) {
        int firstUnderscore = key.indexOf('_');
        if (firstUnderscore > 0 && key.substring(0, firstUnderscore).chars().allMatch(Character::isDigit)) {
            return key.substring(firstUnderscore + 1);
        }
        return key;
    }

    private static String tail(String key) {
        String withoutRank = withoutRank(key);
        int lastUnderscore = withoutRank.lastIndexOf('_');
        return lastUnderscore >= 0 ? withoutRank.substring(lastUnderscore + 1) : withoutRank;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
