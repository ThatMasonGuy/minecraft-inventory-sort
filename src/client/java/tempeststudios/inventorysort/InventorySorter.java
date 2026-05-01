package tempeststudios.inventorysort;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tempeststudios.inventorysort.mixin.AbstractContainerScreenInvoker;

import java.util.*;

public class InventorySorter {

	public static void sortInventory(AbstractContainerScreen<?> screen, Player player) {
		InventorySortClient.LOGGER.info("Sort button clicked! Screen: {}", screen.getClass().getSimpleName());

		AbstractContainerMenu menu = screen.getMenu();
		AbstractContainerScreenInvoker invoker = (AbstractContainerScreenInvoker) screen;

		// Robust: find player slots by backing inventory index (0-35),
		// NOT by "last N slots" (player inventory menu can have offhand after hotbar).
		PlayerSlotRegions regions = getPlayerSlotRegions(menu, player);
		List<Slot> playerHotbar = regions.hotbar;
		List<Slot> playerMain = regions.main;

		InventorySortClient.LOGGER.debug("Found {} hotbar slots, {} main inventory slots",
				playerHotbar.size(), playerMain.size());

		// 1) Top up partial stacks in hotbar (pull from player main only)
		if (!playerHotbar.isEmpty() && !playerMain.isEmpty()) {
			if (!ensureCursorEmpty(menu, invoker, playerMain, playerHotbar))
				return;
			topUpHotbar(menu, invoker, playerHotbar, playerMain);
			if (!ensureCursorEmpty(menu, invoker, playerMain, playerHotbar))
				return;
		}

		// Determine what to sort:
		// - Container screens: sort only the container part (no player inventory, no
		// hotbar)
		// - Player inventory: sort only MAIN inventory (leave hotbar alone)
		List<Slot> slotsToSort = getSortableSlots(menu, screen, playerMain);
		InventorySortClient.LOGGER.debug("Found {} slots to sort", slotsToSort.size());

		if (slotsToSort.isEmpty())
			return;

		if (!ensureCursorEmpty(menu, invoker, slotsToSort, playerMain))
			return;

		// A) Compact empties to the end (stable)
		stableCompact(slotsToSort, invoker, menu);

		// B) Restack within region
		restack(menu, invoker, slotsToSort);

		// C) Sort by: maxStackSize DESC (64 first), then category grouping, then
		// alphabetical, then components hash
		List<ItemStack> desired = buildDesiredLayout(slotsToSort);

		// D) Apply layout (treat same item+components as "already correct", ignore
		// counts; restack handles fullness)
		applyLayout(menu, invoker, slotsToSort, desired);

		// E) Restack again now that like-items are adjacent (full stacks first
		// naturally)
		restack(menu, invoker, slotsToSort);

		// F) Final compact
		stableCompact(slotsToSort, invoker, menu);

		// Final safety
		ensureCursorEmpty(menu, invoker, slotsToSort, playerMain);

		InventorySortClient.LOGGER.info("Sorting complete!");
	}

	// ─────────────────────────────────────────────────────────────
	// Transfer Buttons
	// ─────────────────────────────────────────────────────────────

	/**
	 * ▲ Up button
	 * - No shift: move items from player inventory -> container ONLY if container already contains that item (item+components).
	 * - Shift: move ALL items from player MAIN inventory (exclude hotbar) -> container.
	 */
	public static void transferUp(AbstractContainerScreen<?> screen, Player player, boolean shiftAllExcludeHotbar) {
		AbstractContainerMenu menu = screen.getMenu();
		AbstractContainerScreenInvoker invoker = (AbstractContainerScreenInvoker) screen;

		List<Slot> containerSlots = getContainerSlots(menu, screen);
		if (containerSlots.isEmpty()) {
			InventorySortClient.LOGGER.debug("transferUp: no container slots detected, skipping.");
			return;
		}

		PlayerSlotRegions regions = getPlayerSlotRegions(menu, player);

		// ALWAYS ignore hotbar (both normal and shift)
		List<Slot> playerSlots = new ArrayList<>(regions.main);

		if (!ensureCursorEmpty(menu, invoker, playerSlots, containerSlots)) return;

		Set<StackKey> containerTypes = buildTypeSet(containerSlots);

		for (Slot from : playerSlots) {
			ItemStack stack = from.getItem();
			if (stack.isEmpty()) continue;

			if (!shiftAllExcludeHotbar) {
				// Only move if container already has this exact item+components
				if (!containerTypes.contains(StackKey.of(stack))) continue;
			}

			quickMove(invoker, from);
		}

		ensureCursorEmpty(menu, invoker, playerSlots, containerSlots);
	}

	/**
	 * ▼ Down button
	 * - No shift: top up existing stacks in player inventory using container contents.
	 * - Shift: move ALL items from container -> player inventory (as much as fits).
	 */
	public static void transferDown(AbstractContainerScreen<?> screen, Player player, boolean shiftMoveAllFromContainer) {
		AbstractContainerMenu menu = screen.getMenu();
		AbstractContainerScreenInvoker invoker = (AbstractContainerScreenInvoker) screen;

		List<Slot> containerSlots = getContainerSlots(menu, screen);
		if (containerSlots.isEmpty()) {
			InventorySortClient.LOGGER.debug("transferDown: no container slots detected, skipping.");
			return;
		}

		PlayerSlotRegions regions = getPlayerSlotRegions(menu, player);

		// Down (both modes): affect main + hotbar
		List<Slot> playerSlots = new ArrayList<>();
		playerSlots.addAll(regions.hotbar);
		playerSlots.addAll(regions.main);

		if (!ensureCursorEmpty(menu, invoker, containerSlots, playerSlots)) return;

		if (shiftMoveAllFromContainer) {
			// Shift+▼ : quick-move everything from container into player inventory
			for (Slot from : containerSlots) {
				if (from.getItem().isEmpty()) continue;
				quickMove(invoker, from);
			}
			ensureCursorEmpty(menu, invoker, containerSlots, playerSlots);
			return;
		}

		// ▼ : Fill existing stacks in inventory from container
		fillPlayerStacksFromContainer(menu, invoker, playerSlots, containerSlots);

		ensureCursorEmpty(menu, invoker, containerSlots, playerSlots);
	}

	private static void fillPlayerStacksFromContainer(AbstractContainerMenu menu,
													  AbstractContainerScreenInvoker invoker,
													  List<Slot> playerSlots,
													  List<Slot> containerSlots) {
		for (Slot targetSlot : playerSlots) {
			ItemStack target = targetSlot.getItem();
			if (target.isEmpty()) continue;

			int max = target.getMaxStackSize();
			if (max <= 1) continue; // tools, etc.
			if (target.getCount() >= max) continue;

			for (Slot fromSlot : containerSlots) {
				ItemStack from = fromSlot.getItem();
				if (from.isEmpty()) continue;
				if (!sameItemAndComponents(target, from)) continue;

				// Pick up FROM -> click TARGET (merge) -> if remainder, return to FROM
				click(invoker, fromSlot);
				click(invoker, targetSlot);

				if (!menu.getCarried().isEmpty()) {
					click(invoker, fromSlot);
				}

				target = targetSlot.getItem();
				if (target.isEmpty()) break;
				if (target.getCount() >= max) break;
			}
		}
	}

	private static void quickMove(AbstractContainerScreenInvoker invoker, Slot slot) {
		invoker.invokeSlotClicked(slot, slot.index, 0, ClickType.QUICK_MOVE);
	}

	private static List<Slot> getContainerSlots(AbstractContainerMenu menu, AbstractContainerScreen<?> screen) {
		String screenName = screen.getClass().getSimpleName();
		int totalSlots = menu.slots.size();

		boolean isContainer = totalSlots > 46;

		boolean isContainerByName = screenName.contains("Container") ||
				screenName.contains("Chest") ||
				screenName.contains("Shulker") ||
				screenName.contains("Barrel") ||
				screenName.contains("Hopper") ||
				screenName.contains("Dispenser") ||
				screenName.contains("Dropper") ||
				screenName.contains("Furnace") ||
				screenName.contains("Brewing") ||
				screenName.contains("Crafting");

		if (!(isContainer || isContainerByName)) return Collections.emptyList();

		int containerSize = totalSlots - 36;
		if (containerSize <= 0) return Collections.emptyList();

		return new ArrayList<>(menu.slots.subList(0, containerSize));
	}

	private static Set<StackKey> buildTypeSet(List<Slot> slots) {
		Set<StackKey> set = new HashSet<>();
		for (Slot s : slots) {
			ItemStack st = s.getItem();
			if (st.isEmpty()) continue;
			set.add(StackKey.of(st));
		}
		return set;
	}

	private static final class StackKey {
		private final Object item;
		private final Object components;

		private StackKey(Object item, Object components) {
			this.item = item;
			this.components = components;
		}

		static StackKey of(ItemStack stack) {
			return new StackKey(stack.getItem(), stack.getComponents());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof StackKey other)) return false;
			return this.item == other.item && Objects.equals(this.components, other.components);
		}

		@Override
		public int hashCode() {
			return 31 * System.identityHashCode(item) + (components != null ? components.hashCode() : 0);
		}
	}

	// ─────────────────────────────────────────────────────────────
	// Feature #1: Top up partial stacks in the hotbar
	// ─────────────────────────────────────────────────────────────

	private static void topUpHotbar(AbstractContainerMenu menu,
									AbstractContainerScreenInvoker invoker,
									List<Slot> hotbarSlots,
									List<Slot> mainSlots) {

		for (Slot hotbarSlot : hotbarSlots) {
			ItemStack target = hotbarSlot.getItem();
			if (target.isEmpty())
				continue;

			int max = target.getMaxStackSize();
			if (max <= 1)
				continue; // swords/tools/etc never touched
			if (target.getCount() >= max)
				continue; // already full

			// Pull from main inventory only
			for (Slot fromSlot : mainSlots) {
				ItemStack from = fromSlot.getItem();
				if (from.isEmpty())
					continue;
				if (!sameItemAndComponents(target, from))
					continue;

				// Pick up FROM -> click HOTBAR (merge) -> if remainder, return to FROM
				click(invoker, fromSlot);
				click(invoker, hotbarSlot);

				if (!menu.getCarried().isEmpty()) {
					click(invoker, fromSlot);
				}

				target = hotbarSlot.getItem();
				if (target.isEmpty())
					break;
				if (target.getCount() >= max)
					break;
			}
		}
	}

	// ─────────────────────────────────────────────────────────────
	// Sorting / grouping
	// ─────────────────────────────────────────────────────────────

	private static List<ItemStack> buildDesiredLayout(List<Slot> slots) {
		List<ItemStack> stacks = new ArrayList<>();
		for (Slot s : slots) {
			ItemStack st = s.getItem();
			if (!st.isEmpty())
				stacks.add(st.copy());
		}

		stacks.sort(STACK_COMPARATOR);

		List<ItemStack> desired = new ArrayList<>(slots.size());
		desired.addAll(stacks);
		while (desired.size() < slots.size())
			desired.add(ItemStack.EMPTY);

		return desired;
	}

	// 64-stack items first, then smaller. Within that, group by category. Then
	// alphabetical.
	private static final Comparator<ItemStack> STACK_COMPARATOR = Comparator
			.comparingInt((ItemStack s) -> -s.getMaxStackSize())
			.thenComparing(InventorySorter::categoryKey)
			.thenComparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
			.thenComparingInt(s -> s.getComponents().hashCode())
			.thenComparingInt(s -> -s.getCount());

	private static String categoryKey(ItemStack stack) {
		if (stack.isEmpty())
			return "99_empty";

		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;

		// ═══════════════════════════════════════════════════════════════
		// FOOD ITEMS - Group all food together with subcategories
		// ═══════════════════════════════════════════════════════════════

		// Raw meat
		if (path.equals("beef") || path.equals("porkchop") || path.equals("chicken") ||
				path.equals("mutton") || path.equals("rabbit") || path.equals("cod") ||
				path.equals("salmon") || path.equals("tropical_fish") || path.equals("pufferfish"))
			return "50_food_raw_meat";

		// Cooked meat
		if (path.equals("cooked_beef") || path.equals("cooked_porkchop") ||
				path.equals("cooked_chicken") || path.equals("cooked_mutton") ||
				path.equals("cooked_rabbit") || path.equals("cooked_cod") ||
				path.equals("cooked_salmon"))
			return "51_food_cooked_meat";

		// Vegetables & crops
		if (path.equals("potato") || path.equals("carrot") || path.equals("beetroot") ||
				path.equals("wheat") || path.equals("wheat_seeds") || path.equals("beetroot_seeds") ||
				path.equals("pumpkin_seeds") || path.equals("melon_seeds") || path.equals("torchflower_seeds") ||
				path.equals("pitcher_pod"))
			return "52_food_crops";

		// Prepared foods
		if (path.equals("baked_potato") || path.equals("bread") || path.equals("cookie") ||
				path.equals("pumpkin_pie") || path.equals("cake") || path.contains("stew") ||
				path.contains("soup"))
			return "53_food_prepared";

		// Fruits & sweets
		if (path.equals("apple") || path.equals("golden_apple") || path.equals("enchanted_golden_apple") ||
				path.equals("melon_slice") || path.equals("sweet_berries") || path.equals("glow_berries") ||
				path.equals("chorus_fruit") || path.equals("honey_bottle") || path.equals("honeycomb"))
			return "54_food_fruits";

		// ═══════════════════════════════════════════════════════════════
		// WOOD & WOOD PRODUCTS - Keep together
		// ═══════════════════════════════════════════════════════════════

		// Logs & wood blocks
		if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.contains("hyphae"))
			return "01_wood_logs";

		// Planks
		if (path.endsWith("_planks"))
			return "02_wood_planks";

		// Sticks and wood-related items
		if (path.equals("stick") || path.equals("bowl") || path.equals("ladder") || path.equals("scaffolding"))
			return "03_wood_items";

		// Leaves
		if (path.endsWith("_leaves"))
			return "04_wood_leaves";

		// Saplings
		if (path.endsWith("_sapling"))
			return "05_wood_saplings";

		// ═══════════════════════════════════════════════════════════════
		// STONE & TERRAIN BLOCKS
		// ═══════════════════════════════════════════════════════════════

		// Dirt & grass
		if (path.contains("dirt") || path.contains("grass_block") || path.contains("podzol") ||
				path.contains("mycelium") || path.contains("mud"))
			return "10_terrain_dirt";

		// Stone variants
		if (path.contains("stone") || path.contains("cobblestone") || path.contains("deepslate") ||
				path.contains("granite") || path.contains("diorite") || path.contains("andesite") ||
				path.contains("tuff") || path.contains("calcite") || path.contains("basalt") ||
				path.contains("blackstone") || path.contains("netherrack") || path.contains("end_stone"))
			return "11_terrain_stone";

		// Sand & gravel
		if (path.contains("sand") || path.contains("gravel") || path.contains("clay"))
			return "12_terrain_sand";

		// ═══════════════════════════════════════════════════════════════
		// ORES, INGOTS, GEMS & MINERALS
		// ═══════════════════════════════════════════════════════════════

		// Raw ores
		if (path.endsWith("_ore") || path.contains("_ore_") || path.startsWith("raw_"))
			return "20_minerals_ores";

		// Gems & precious items
		if (path.equals("diamond") || path.equals("emerald") || path.equals("amethyst_shard") ||
				path.equals("lapis_lazuli") || path.equals("prismarine_shard") || path.equals("prismarine_crystals") ||
				path.equals("quartz") || path.equals("echo_shard"))
			return "21_minerals_gems";

		// Ingots
		if (path.endsWith("_ingot"))
			return "22_minerals_ingots";

		// Nuggets
		if (path.endsWith("_nugget"))
			return "23_minerals_nuggets";

		// Dusts & powders
		if (path.equals("redstone") || path.equals("glowstone_dust") || path.equals("gunpowder") ||
				path.equals("blaze_powder") || path.equals("bone_meal"))
			return "24_minerals_dusts";

		// ═══════════════════════════════════════════════════════════════
		// REDSTONE & MECHANISMS
		// ═══════════════════════════════════════════════════════════════

		if (path.contains("redstone") || path.contains("repeater") || path.contains("comparator") ||
				path.contains("piston") || path.contains("observer") || path.contains("hopper") ||
				path.contains("dispenser") || path.contains("dropper") || path.contains("lever") ||
				path.contains("button") || path.contains("pressure_plate") || path.contains("rail") ||
				path.contains("detector"))
			return "30_redstone";

		// ═══════════════════════════════════════════════════════════════
		// BUILDING BLOCKS & DECORATIVE
		// ═══════════════════════════════════════════════════════════════

		// Slabs
		if (path.contains("slab"))
			return "40_build_slabs";

		// Stairs
		if (path.contains("stairs"))
			return "41_build_stairs";

		// Walls, fences, gates
		if (path.contains("fence") || path.contains("wall") || path.contains("gate"))
			return "42_build_edges";

		// Doors & trapdoors
		if (path.contains("door") || path.contains("trapdoor"))
			return "43_build_doors";

		// Glass & panes
		if (path.contains("glass") || path.contains("pane"))
			return "44_build_glass";

		// Wool & carpet
		if (path.contains("wool") || path.contains("carpet"))
			return "45_build_wool";

		// Concrete & terracotta
		if (path.contains("concrete") || path.contains("terracotta"))
			return "46_build_concrete";

		// ═══════════════════════════════════════════════════════════════
		// TOOLS, WEAPONS & ARMOR
		// ═══════════════════════════════════════════════════════════════

		// Weapons
		if (path.contains("sword") || path.contains("bow") || path.contains("crossbow") ||
				path.contains("trident") || path.equals("arrow") || path.equals("spectral_arrow") ||
				path.contains("tipped_arrow"))
			return "60_combat_weapons";

		// Tools
		if (path.contains("axe") || path.contains("pickaxe") || path.contains("shovel") ||
				path.contains("hoe") || path.contains("shears") || path.equals("flint_and_steel") ||
				path.equals("fishing_rod"))
			return "61_tools";

		// Armor
		if (path.contains("helmet") || path.contains("chestplate") || path.contains("leggings") ||
				path.contains("boots") || path.equals("shield") || path.equals("elytra"))
			return "62_armor";

		// ═══════════════════════════════════════════════════════════════
		// POTIONS & BREWING
		// ═══════════════════════════════════════════════════════════════

		if (path.contains("potion") || path.equals("glass_bottle") || path.equals("dragon_breath") ||
				path.equals("fermented_spider_eye") || path.equals("ghast_tear") || path.equals("magma_cream") ||
				path.equals("blaze_rod") || path.equals("nether_wart") || path.equals("spider_eye") ||
				path.equals("phantom_membrane"))
			return "70_potions_brewing";

		// ═══════════════════════════════════════════════════════════════
		// MISCELLANEOUS COMMON ITEMS
		// ═══════════════════════════════════════════════════════════════

		// Storage & containers
		if (path.contains("chest") || path.contains("barrel") || path.contains("shulker_box") ||
				path.equals("bucket") || path.contains("bundle"))
			return "80_misc_storage";

		// Books & paper
		if (path.contains("book") || path.equals("paper") || path.equals("writable_book") ||
				path.equals("written_book") || path.equals("enchanted_book"))
			return "81_misc_books";

		// Mob drops
		if (path.equals("string") || path.equals("leather") || path.equals("feather") ||
				path.equals("bone") || path.equals("rotten_flesh") || path.equals("slime_ball") ||
				path.equals("ender_pearl") || path.equals("blaze_rod"))
			return "82_misc_mob_drops";

		// Default category for anything not matched
		return "90_misc";
	}

	private static void applyLayout(AbstractContainerMenu menu,
									AbstractContainerScreenInvoker invoker,
									List<Slot> slots,
									List<ItemStack> desired) {

		for (int i = 0; i < slots.size(); i++) {
			ItemStack want = desired.get(i);
			ItemStack have = slots.get(i).getItem();

			// "Correct enough" if same item+components, ignore counts.
			if (sameTypeIgnoringCount(have, want))
				continue;

			int j = findMatchingSlot(slots, i + 1, want);
			if (j == -1)
				continue;

			swap(invoker, slots.get(i), slots.get(j));

			if (!menu.getCarried().isEmpty()) {
				int empty = findFirstEmpty(slots);
				if (empty != -1)
					click(invoker, slots.get(empty));
			}
		}
	}

	private static int findMatchingSlot(List<Slot> slots, int start, ItemStack want) {
		if (want.isEmpty()) {
			for (int i = start; i < slots.size(); i++) {
				if (slots.get(i).getItem().isEmpty())
					return i;
			}
			return -1;
		}

		for (int i = start; i < slots.size(); i++) {
			ItemStack have = slots.get(i).getItem();
			if (!have.isEmpty() && sameItemAndComponents(have, want))
				return i;
		}

		return -1;
	}

	// ─────────────────────────────────────────────────────────────
	// Restack + compact
	// ─────────────────────────────────────────────────────────────

	private static void restack(AbstractContainerMenu menu, AbstractContainerScreenInvoker invoker, List<Slot> slots) {
		for (int i = 0; i < slots.size(); i++) {
			Slot targetSlot = slots.get(i);
			ItemStack target = targetSlot.getItem();
			if (target.isEmpty())
				continue;

			int max = target.getMaxStackSize();
			if (max <= 1)
				continue;
			if (target.getCount() >= max)
				continue;

			for (int j = i + 1; j < slots.size(); j++) {
				ItemStack currentTarget = targetSlot.getItem();
				if (currentTarget.isEmpty())
					break;
				if (currentTarget.getCount() >= max)
					break;

				Slot fromSlot = slots.get(j);
				ItemStack from = fromSlot.getItem();
				if (from.isEmpty())
					continue;

				if (!sameItemAndComponents(currentTarget, from))
					continue;

				click(invoker, fromSlot);
				click(invoker, targetSlot);

				if (!menu.getCarried().isEmpty()) {
					click(invoker, fromSlot);
				}
			}
		}
	}

	private static void stableCompact(List<Slot> slots, AbstractContainerScreenInvoker invoker,
									  AbstractContainerMenu menu) {
		for (int i = 0; i < slots.size(); i++) {
			if (!slots.get(i).getItem().isEmpty())
				continue;

			int j = i + 1;
			while (j < slots.size() && slots.get(j).getItem().isEmpty())
				j++;
			if (j >= slots.size())
				return;

			swap(invoker, slots.get(i), slots.get(j));

			if (!menu.getCarried().isEmpty()) {
				int empty = findFirstEmpty(slots);
				if (empty != -1)
					click(invoker, slots.get(empty));
			}
		}
	}

	private static int findFirstEmpty(List<Slot> slots) {
		for (int i = 0; i < slots.size(); i++) {
			if (slots.get(i).getItem().isEmpty())
				return i;
		}
		return -1;
	}

	// ─────────────────────────────────────────────────────────────
	// Cursor safety + click primitives
	// ─────────────────────────────────────────────────────────────

	private static boolean ensureCursorEmpty(AbstractContainerMenu menu,
											 AbstractContainerScreenInvoker invoker,
											 List<Slot> preferred,
											 List<Slot> alsoOk) {
		if (menu.getCarried().isEmpty())
			return true;

		for (Slot s : preferred) {
			if (s.getItem().isEmpty()) {
				click(invoker, s);
				return menu.getCarried().isEmpty();
			}
		}

		for (Slot s : alsoOk) {
			if (s.getItem().isEmpty()) {
				click(invoker, s);
				return menu.getCarried().isEmpty();
			}
		}

		for (Slot s : menu.slots) {
			if (s.getItem().isEmpty()) {
				click(invoker, s);
				return menu.getCarried().isEmpty();
			}
		}

		return false;
	}

	private static void click(AbstractContainerScreenInvoker invoker, Slot slot) {
		invoker.invokeSlotClicked(slot, slot.index, 0, ClickType.PICKUP);
	}

	private static void swap(AbstractContainerScreenInvoker invoker, Slot a, Slot b) {
		click(invoker, a);
		click(invoker, b);
		click(invoker, a);
	}

	// ─────────────────────────────────────────────────────────────
	// Equality (1.21+ components)
	// ─────────────────────────────────────────────────────────────

	private static boolean sameItemAndComponents(ItemStack a, ItemStack b) {
		if (a.isEmpty() || b.isEmpty())
			return false;
		if (a.getItem() != b.getItem())
			return false;
		return Objects.equals(a.getComponents(), b.getComponents());
	}

	private static boolean sameTypeIgnoringCount(ItemStack a, ItemStack b) {
		if (a.isEmpty() && b.isEmpty())
			return true;
		if (a.isEmpty() || b.isEmpty())
			return false;
		return sameItemAndComponents(a, b);
	}

	// ─────────────────────────────────────────────────────────────
	// Slot selection - IMPROVED to handle more screen types
	// ─────────────────────────────────────────────────────────────

	private static List<Slot> getSortableSlots(AbstractContainerMenu handler,
											   AbstractContainerScreen<?> screen,
											   List<Slot> playerMainSlots) {
		String screenName = screen.getClass().getSimpleName();
		int totalSlots = handler.slots.size();

		InventorySortClient.LOGGER.debug("Screen: {}, Total slots: {}", screenName, totalSlots);

		// Check if this is a container screen (has more slots than just player
		// inventory)
		// Player inventory typically has 45 slots (36 main + 9 hotbar = 45 total, or 46
		// with offhand)
		boolean isContainer = totalSlots > 46;

		// Also check by screen name patterns
		boolean isContainerByName = screenName.contains("Container") ||
				screenName.contains("Chest") ||
				screenName.contains("Shulker") ||
				screenName.contains("Barrel") ||
				screenName.contains("Hopper") ||
				screenName.contains("Dispenser") ||
				screenName.contains("Dropper") ||
				screenName.contains("Furnace") ||
				screenName.contains("Brewing") ||
				screenName.contains("Crafting");

		// Container screens: sort only the container portion, not player inv/hotbar
		if (isContainer || isContainerByName) {
			// Container slots are typically at the beginning
			// Player slots (36 total) are at the end
			int containerSize = totalSlots - 36;

			// Safety check
			if (containerSize <= 0) {
				InventorySortClient.LOGGER.warn("Container size is {}, falling back to player main", containerSize);
				return new ArrayList<>(playerMainSlots);
			}

			InventorySortClient.LOGGER.debug("Detected container with {} slots", containerSize);
			return new ArrayList<>(handler.slots.subList(0, containerSize));
		}

		// Player inventory (and other non-container screens): sort main inventory only
		InventorySortClient.LOGGER.debug("Detected player inventory, sorting {} main slots", playerMainSlots.size());
		return new ArrayList<>(playerMainSlots);
	}

	/**
	 * Robustly find player hotbar + main by using the backing Inventory index:
	 * - 0..8 = hotbar
	 * - 9..35 = main
	 * This avoids offhand/crafting/armor slot ordering differences.
	 */
	private static PlayerSlotRegions getPlayerSlotRegions(AbstractContainerMenu menu, Player player) {
		Inventory inv = player.getInventory();

		List<SlotWithInvIndex> hotbar = new ArrayList<>();
		List<SlotWithInvIndex> main = new ArrayList<>();

		for (Slot slot : menu.slots) {
			// Slot.container is the backing inventory/container for that slot.
			if (slot.container != inv)
				continue;

			int idx = slot.getContainerSlot(); // index inside the backing Inventory
			if (idx >= 0 && idx <= 8) {
				hotbar.add(new SlotWithInvIndex(slot, idx));
			} else if (idx >= 9 && idx <= 35) {
				main.add(new SlotWithInvIndex(slot, idx));
			}
		}

		hotbar.sort(Comparator.comparingInt(a -> a.invIndex));
		main.sort(Comparator.comparingInt(a -> a.invIndex));

		PlayerSlotRegions regions = new PlayerSlotRegions();
		regions.hotbar = hotbar.stream().map(s -> s.slot).toList();
		regions.main = main.stream().map(s -> s.slot).toList();
		return regions;
	}

	private static class SlotWithInvIndex {
		final Slot slot;
		final int invIndex;

		SlotWithInvIndex(Slot slot, int invIndex) {
			this.slot = slot;
			this.invIndex = invIndex;
		}
	}

	private static class PlayerSlotRegions {
		List<Slot> hotbar = Collections.emptyList();
		List<Slot> main = Collections.emptyList();
	}
}