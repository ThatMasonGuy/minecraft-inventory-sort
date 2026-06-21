package tempeststudios.inventorysort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;
import tempeststudios.inventorysort.compat.sort.ContainerClickCompat;
import tempeststudios.inventorysort.compat.sort.ItemStackCompat;
import tempeststudios.inventorysort.mixin.AbstractContainerScreenInvoker;

import java.util.*;

public class InventorySorter {
	private static final Component SORT_BLOCKED_FULL_INVENTORY = Component.literal(
			"InvSort couldn't finish: free one inventory slot and try again.");

	public static void sortInventory(AbstractContainerScreen<?> screen, Player player) {
		tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info("Sort button clicked! Screen: {}", screen.getClass().getSimpleName());

		AbstractContainerMenu menu = screen.getMenu();
		AbstractContainerScreenInvoker invoker = (AbstractContainerScreenInvoker) screen;

		// Robust: find player slots by backing inventory index (0-35),
		// NOT by "last N slots" (player inventory menu can have offhand after hotbar).
		PlayerSlotRegions regions = getPlayerSlotRegions(menu, player);
		List<Slot> playerHotbar = regions.hotbar;
		List<Slot> playerMain = regions.main;
		SortRuleStore.SortRules playerRules = SortRuleStore.getInstance().playerRules();

		tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("Found {} hotbar slots, {} main inventory slots",
				playerHotbar.size(), playerMain.size());

		// 1) Top up partial stacks in hotbar (pull from player main only)
		if (!playerHotbar.isEmpty() && !playerMain.isEmpty()) {
			if (!ensureCursorEmpty(menu, invoker, playerMain, playerHotbar)) {
				alertSortBlockedByFullInventory();
				return;
			}
			topUpHotbar(menu, invoker, playerHotbar, playerMainSourceSlots(playerMain, playerRules), playerRules);
			if (!ensureCursorEmpty(menu, invoker, playerMain, playerHotbar)) {
				alertSortBlockedByFullInventory();
				return;
			}
		}

		// Determine what to sort:
		// - Container screens: sort only the container part (no player inventory, no hotbar)
		// - Player inventory: sort only MAIN inventory (leave hotbar alone)
		List<Slot> slotsToSort = getSortableSlots(menu, screen, playerMain);
		tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("Found {} slots to sort", slotsToSort.size());

		if (!sortSlots(menu, invoker, slotsToSort, playerMain, playerHotbar, containerRulesFor(screen), false)) {
			alertSortBlockedByFullInventory();
			return;
		}

		tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info("Sorting complete!");
	}

	public static void sortPlayerInventory(AbstractContainerScreen<?> screen, Player player) {
		tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info("Player inventory sort button clicked! Screen: {}", screen.getClass().getSimpleName());

		AbstractContainerMenu menu = screen.getMenu();
		AbstractContainerScreenInvoker invoker = (AbstractContainerScreenInvoker) screen;

		PlayerSlotRegions regions = getPlayerSlotRegions(menu, player);
		List<Slot> playerHotbar = regions.hotbar;
		List<Slot> playerMain = regions.main;
		SortRuleStore.SortRules rules = SortRuleStore.getInstance().playerRules();

		if (!playerHotbar.isEmpty() && !playerMain.isEmpty()) {
			if (!ensureCursorEmpty(menu, invoker, playerMain, playerHotbar)) {
				alertSortBlockedByFullInventory();
				return;
			}
			topUpHotbar(menu, invoker, playerHotbar, playerMainSourceSlots(playerMain, rules), rules);
			if (!ensureCursorEmpty(menu, invoker, playerMain, playerHotbar)) {
				alertSortBlockedByFullInventory();
				return;
			}
		}

		List<Slot> playerSlots = getPlayerInventorySlots(regions);
		if (!sortSlots(menu, invoker, playerSlots, playerSlots, playerHotbar, rules, true)) {
			alertSortBlockedByFullInventory();
			return;
		}

		tempeststudios.inventorysort.core.InventorySortCore.LOGGER.info("Player inventory sorting complete!");
	}

	private static boolean sortSlots(AbstractContainerMenu menu,
									 AbstractContainerScreenInvoker invoker,
									 List<Slot> slotsToSort,
									 List<Slot> fallbackSlots,
									 List<Slot> hotbarBufferSlots,
									 SortRuleStore.SortRules rules,
									 boolean hotbarDefaultLocked) {
		if (slotsToSort.isEmpty())
			return true;

		if (!ensureCursorEmpty(menu, invoker, slotsToSort, fallbackSlots))
			return false;

		Slot hotbarSwapBuffer = findHotbarBuffer(hotbarBufferSlots, slotsToSort);

		if (rules != null && rules.enabled) {
			enforceReservedSlots(menu, invoker, slotsToSort, rules, hotbarSwapBuffer, hotbarDefaultLocked);
			if (!ensureCursorEmpty(menu, invoker, slotsToSort, fallbackSlots))
				return false;
		}

		SortWorkSlots workSlots = sortWorkSlots(slotsToSort, rules, hotbarDefaultLocked);
		if (workSlots.slots().isEmpty())
			return true;

		List<Slot> sortableSlots = moveBundlesToFront(invoker, workSlots.slots(), hotbarSwapBuffer);
		if (!ensureCursorEmpty(menu, invoker, slotsToSort, fallbackSlots))
			return false;
		if (sortableSlots.isEmpty())
			return true;

		// A) Compact empties to the end (stable)
		stableCompact(sortableSlots, invoker, menu, hotbarSwapBuffer);

		// B) Restack within region
		restack(menu, invoker, sortableSlots);

		// C) Sort by: maxStackSize DESC (64 first), then category grouping, then
		// alphabetical, then components hash
		List<ItemStack> desired = buildDesiredLayout(sortableSlots, rules);

		// D) Apply layout (treat same item+components as "already correct", ignore
		// counts; restack handles fullness)
		applyLayout(menu, invoker, sortableSlots, desired, hotbarSwapBuffer);

		// E) Restack again now that like-items are adjacent (full stacks first naturally)
		restack(menu, invoker, sortableSlots);

		// F) Final compact
		stableCompact(sortableSlots, invoker, menu, hotbarSwapBuffer);
		clearTemporarySlots(invoker, workSlots.temporarySlots(), workSlots.slots(), hotbarSwapBuffer);

		// Final safety
		return ensureCursorEmpty(menu, invoker, sortableSlots, fallbackSlots);
	}

	private static void alertSortBlockedByFullInventory() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}
		MinecraftApiCompat.sendOverlayMessage(client, SORT_BLOCKED_FULL_INVENTORY);
	}

	private static SortRuleStore.SortRules containerRulesFor(AbstractContainerScreen<?> screen) {
		SortRuleStore store = SortRuleStore.getInstance();
		if (screen instanceof InventorySortContainerContext context) {
			return store.effectiveContainerRules(
					context.inventorysort$getContainerIdentity(),
					context.inventorysort$getScreenClassName());
		}
		return store.containerDefaultRules();
	}

	private static List<Slot> playerMainSourceSlots(List<Slot> playerMain, SortRuleStore.SortRules rules) {
		if (rules == null || !rules.enabled) {
			return new ArrayList<>(playerMain);
		}
		List<Slot> sources = new ArrayList<>();
		for (int i = 0; i < playerMain.size(); i++) {
			int ruleIndex = 9 + i;
			SortRuleStore.SlotRule rule = rules.ruleFor(ruleIndex);
			if (!isLockedByRule(rules, ruleIndex, true) && !rule.hasReservation()) {
				sources.add(playerMain.get(i));
			}
		}
		return sources;
	}

	private static SortWorkSlots sortWorkSlots(List<Slot> slots,
											   SortRuleStore.SortRules rules,
											   boolean hotbarDefaultLocked) {
		if (rules == null || !rules.enabled) {
			return new SortWorkSlots(new ArrayList<>(slots), Collections.emptyList());
		}

		List<Slot> movable = new ArrayList<>();
		List<Slot> temporary = new ArrayList<>();
		for (int i = 0; i < slots.size(); i++) {
			SortRuleStore.SlotRule rule = rules.ruleFor(i);
			if (isLockedByRule(rules, i, hotbarDefaultLocked)) {
				continue;
			}
			Slot slot = slots.get(i);
			if (rule.hasReservation()) {
				if (slot.getItem().isEmpty()) {
					temporary.add(slot);
				}
				continue;
			}
			movable.add(slot);
		}
		if (!temporary.isEmpty()) {
			movable.addAll(temporary);
		}
		return new SortWorkSlots(movable, temporary);
	}

	private record SortWorkSlots(List<Slot> slots, List<Slot> temporarySlots) {
	}

	private static boolean isLockedByRule(SortRuleStore.SortRules rules,
										  int slotIndex,
										  boolean hotbarDefaultLocked) {
		if (rules == null || !rules.enabled) {
			return false;
		}
		SortRuleStore.SlotRule rule = rules.ruleFor(slotIndex);
		if (rule.restricted) {
			return true;
		}
		return hotbarDefaultLocked
				&& isHotbarRuleIndex(slotIndex)
				&& !rule.unlocked
				&& !rule.hasReservation();
	}

	private static boolean isHotbarRuleIndex(int slotIndex) {
		return slotIndex >= 0 && slotIndex <= 8;
	}

	private static void enforceReservedSlots(AbstractContainerMenu menu,
											 AbstractContainerScreenInvoker invoker,
											 List<Slot> slots,
											 SortRuleStore.SortRules rules,
											 Slot hotbarSwapBuffer,
											 boolean hotbarDefaultLocked) {
		for (int i = 0; i < slots.size(); i++) {
			SortRuleStore.SlotRule rule = rules.ruleFor(i);
			if (rule.restricted || !rule.hasReservation()) {
				continue;
			}

			Slot target = slots.get(i);
			ItemStack targetStack = target.getItem();
			String reservedItemId = rule.reservedItemId;
			if (!targetStack.isEmpty() && isBundle(targetStack) && !itemIdEquals(targetStack, reservedItemId)) {
				continue;
			}

			if (targetStack.isEmpty() || !itemIdEquals(targetStack, reservedItemId)) {
				int matching = findReservedSource(slots, rules, reservedItemId, i, hotbarDefaultLocked);
				if (matching != -1) {
					swapSafely(invoker, target, slots.get(matching), hotbarSwapBuffer);
				} else if (!targetStack.isEmpty()) {
					int empty = findEmptyMovableSlot(slots, rules, i, hotbarDefaultLocked);
					if (empty != -1) {
						swapSafely(invoker, target, slots.get(empty), hotbarSwapBuffer);
					}
				}
			}

			fillReservedSlot(menu, invoker, target, slots, rules, reservedItemId, i, hotbarDefaultLocked);
		}
	}

	private static void fillReservedSlot(AbstractContainerMenu menu,
										 AbstractContainerScreenInvoker invoker,
										 Slot target,
										 List<Slot> slots,
										 SortRuleStore.SortRules rules,
										 String reservedItemId,
										 int targetIndex,
										 boolean hotbarDefaultLocked) {
		ItemStack targetStack = target.getItem();
		if (targetStack.isEmpty() || !itemIdEquals(targetStack, reservedItemId) || isBundle(targetStack)) {
			return;
		}

		int max = targetStack.getMaxStackSize();
		if (max <= 1 || targetStack.getCount() >= max) {
			return;
		}

		for (int i = 0; i < slots.size(); i++) {
			if (i == targetIndex) {
				continue;
			}
			SortRuleStore.SlotRule rule = rules.ruleFor(i);
			if (isLockedByRule(rules, i, hotbarDefaultLocked) || rule.hasReservation()) {
				continue;
			}

			ItemStack source = slots.get(i).getItem();
			if (source.isEmpty() || isBundle(source) || !sameItemAndComponents(target.getItem(), source)) {
				continue;
			}

			click(invoker, slots.get(i));
			click(invoker, target);
			if (!menu.getCarried().isEmpty()) {
				click(invoker, slots.get(i));
			}

			targetStack = target.getItem();
			if (targetStack.isEmpty() || targetStack.getCount() >= max) {
				return;
			}
		}
	}

	private static int findReservedSource(List<Slot> slots,
										  SortRuleStore.SortRules rules,
										  String reservedItemId,
										  int targetIndex,
										  boolean hotbarDefaultLocked) {
		for (int i = 0; i < slots.size(); i++) {
			if (i == targetIndex) {
				continue;
			}
			SortRuleStore.SlotRule rule = rules.ruleFor(i);
			if (isLockedByRule(rules, i, hotbarDefaultLocked) || rule.hasReservation()) {
				continue;
			}
			ItemStack stack = slots.get(i).getItem();
			if (!stack.isEmpty() && !isBundle(stack) && itemIdEquals(stack, reservedItemId)) {
				return i;
			}
		}
		return -1;
	}

	private static int findEmptyMovableSlot(List<Slot> slots,
											SortRuleStore.SortRules rules,
											int targetIndex,
											boolean hotbarDefaultLocked) {
		for (int i = 0; i < slots.size(); i++) {
			if (i == targetIndex) {
				continue;
			}
			SortRuleStore.SlotRule rule = rules.ruleFor(i);
			if (isLockedByRule(rules, i, hotbarDefaultLocked) || rule.hasReservation()) {
				continue;
			}
			if (slots.get(i).getItem().isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	private static List<Slot> moveBundlesToFront(AbstractContainerScreenInvoker invoker,
												 List<Slot> slots,
												 Slot hotbarBuffer) {
		int bundleCount = countBundles(slots);
		if (bundleCount == 0) {
			return slots;
		}

		if (hotbarBuffer == null) {
			tempeststudios.inventorysort.core.InventorySortCore.LOGGER.warn(
					"Cannot partition bundles before sorting because no hotbar buffer slot was available.");
			return slots;
		}

		int targetIndex = 0;
		for (int scanIndex = 0; scanIndex < slots.size(); scanIndex++) {
			Slot source = slots.get(scanIndex);
			if (!isBundle(source.getItem())) {
				continue;
			}
			if (scanIndex != targetIndex) {
				swapSafely(invoker, slots.get(targetIndex), source, hotbarBuffer);
			}
			targetIndex++;
		}

		if (bundleCount >= slots.size()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(slots.subList(bundleCount, slots.size()));
	}

	private static int countBundles(List<Slot> slots) {
		int count = 0;
		for (Slot slot : slots) {
			if (isBundle(slot.getItem())) {
				count++;
			}
		}
		return count;
	}

	private static Slot findHotbarBuffer(List<Slot> hotbarSlots, List<Slot> protectedSlots) {
		for (Slot slot : hotbarSlots) {
			if (slot.getContainerSlot() >= 0 && slot.getContainerSlot() <= 8) {
				return slot;
			}
		}
		return null;
	}

	private static void swapUsingHotbarBuffer(AbstractContainerScreenInvoker invoker,
											 Slot target,
											 Slot source,
											 Slot hotbarBuffer) {
		if (target == source) {
			return;
		}

		int hotbarIndex = hotbarBuffer.getContainerSlot();
		if (target == hotbarBuffer) {
			ContainerClickCompat.hotbarSwap(invoker, source, hotbarIndex);
			return;
		}
		if (source == hotbarBuffer) {
			ContainerClickCompat.hotbarSwap(invoker, target, hotbarIndex);
			return;
		}

		ContainerClickCompat.hotbarSwap(invoker, target, hotbarIndex);
		ContainerClickCompat.hotbarSwap(invoker, source, hotbarIndex);
		ContainerClickCompat.hotbarSwap(invoker, target, hotbarIndex);
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
			tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("transferUp: no container slots detected, skipping.");
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
	 * Down button
	 * - No shift: move container items only if the player already has that item.
	 * - Shift: move ALL items from container -> player inventory (as much as fits).
	 */
	public static void transferDown(AbstractContainerScreen<?> screen, Player player, boolean shiftMoveAllFromContainer) {
		AbstractContainerMenu menu = screen.getMenu();
		AbstractContainerScreenInvoker invoker = (AbstractContainerScreenInvoker) screen;

		List<Slot> containerSlots = getContainerSlots(menu, screen);
		if (containerSlots.isEmpty()) {
			tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("transferDown: no container slots detected, skipping.");
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

		Set<StackKey> playerTypes = buildTypeSet(playerSlots);
		for (Slot from : containerSlots) {
			ItemStack stack = from.getItem();
			if (stack.isEmpty()) continue;
			if (!playerTypes.contains(StackKey.of(stack))) continue;

			quickMove(invoker, from);
		}

		ensureCursorEmpty(menu, invoker, containerSlots, playerSlots);
	}

	private static void quickMove(AbstractContainerScreenInvoker invoker, Slot slot) {
		ContainerClickCompat.quickMove(invoker, slot);
	}

	public static List<Slot> getContainerSlots(AbstractContainerMenu menu, AbstractContainerScreen<?> screen) {
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
				screenName.contains("Brewing");

		if (!(isContainer || isContainerByName)) return Collections.emptyList();

		int containerSize = totalSlots - 36;
		if (containerSize <= 0) return Collections.emptyList();

		return new ArrayList<>(menu.slots.subList(0, containerSize));
	}

	public static List<Slot> getPlayerMainSlots(AbstractContainerMenu menu, Player player) {
		return new ArrayList<>(getPlayerSlotRegions(menu, player).main);
	}

	public static List<Slot> getPlayerInventorySlots(AbstractContainerMenu menu, Player player) {
		return getPlayerInventorySlots(getPlayerSlotRegions(menu, player));
	}

	private static List<Slot> getPlayerInventorySlots(PlayerSlotRegions regions) {
		List<Slot> slots = new ArrayList<>(regions.hotbar.size() + regions.main.size());
		slots.addAll(regions.hotbar);
		slots.addAll(regions.main);
		return slots;
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
			return new StackKey(stack.getItem(), ItemStackCompat.identityData(stack));
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
									List<Slot> mainSlots,
									SortRuleStore.SortRules rules) {

		for (int hotbarIndex = 0; hotbarIndex < hotbarSlots.size(); hotbarIndex++) {
			Slot hotbarSlot = hotbarSlots.get(hotbarIndex);
			SortRuleStore.SlotRule rule = rules == null ? SortRuleStore.SlotRule.EMPTY : rules.ruleFor(hotbarIndex);
			ItemStack target = hotbarSlot.getItem();
			if (target.isEmpty())
				continue;

			if (isLockedByRule(rules, hotbarIndex, true))
				continue;
			if (rule.hasReservation() && !itemIdEquals(target, rule.reservedItemId))
				continue;

			// Skip bundles - don't want to accidentally fill them
			if (isBundle(target))
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

				// Skip bundles as source
				if (isBundle(from))
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

	private static List<ItemStack> buildDesiredLayout(List<Slot> slots, SortRuleStore.SortRules rules) {
		List<ItemStack> stacks = new ArrayList<>();
		for (Slot s : slots) {
			ItemStack st = s.getItem();
			if (!st.isEmpty())
				stacks.add(st.copy());
		}

		stacks.sort(comparatorFor(rules));

		List<ItemStack> desired = new ArrayList<>(slots.size());
		desired.addAll(stacks);
		while (desired.size() < slots.size())
			desired.add(ItemStack.EMPTY);

		return desired;
	}

	public record CategoryDefinition(String key, String label) {
	}

	public static final List<CategoryDefinition> DEFAULT_CATEGORIES = InventorySortCategories.DEFAULT_CATEGORIES.stream()
			.map(category -> new CategoryDefinition(category.key(), category.label()))
			.toList();

	// 64-stack items first, then smaller. Within that, group by category. Then alphabetical.
	private static final Comparator<ItemStack> STACK_COMPARATOR = Comparator
			.comparingInt((ItemStack s) -> -s.getMaxStackSize())
			.thenComparing(InventorySortCategories::categoryKey)
			.thenComparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
			.thenComparingInt(ItemStackCompat::identityHash)
			.thenComparingInt(s -> -s.getCount());

	private static Comparator<ItemStack> comparatorFor(SortRuleStore.SortRules rules) {
		if (rules == null || !rules.enabled || !rules.usesCustomOrder()) {
			return STACK_COMPARATOR;
		}

		return (a, b) -> {
			String aId = itemId(a);
			String bId = itemId(b);
			int aItemRank = rules.itemRank(aId);
			int bItemRank = rules.itemRank(bId);

			if (rules.absoluteItemOrder && (aItemRank >= 0 || bItemRank >= 0)) {
				int cmp = compareRank(aItemRank, bItemRank);
				if (cmp != 0) {
					return cmp;
				}
			}

			int aCategoryRank = rules.categoryRank(InventorySortCategories.categoryKey(a));
			int bCategoryRank = rules.categoryRank(InventorySortCategories.categoryKey(b));
			if (aCategoryRank >= 0 || bCategoryRank >= 0) {
				int cmp = compareRank(aCategoryRank, bCategoryRank);
				if (cmp != 0) {
					return cmp;
				}
			}

			if (!rules.absoluteItemOrder && (aItemRank >= 0 || bItemRank >= 0)) {
				int cmp = compareRank(aItemRank, bItemRank);
				if (cmp != 0) {
					return cmp;
				}
			}

			return STACK_COMPARATOR.compare(a, b);
		};
	}

	private static int compareRank(int left, int right) {
		if (left == right) {
			return 0;
		}
		if (left < 0) {
			return 1;
		}
		if (right < 0) {
			return -1;
		}
		return Integer.compare(left, right);
	}

	public static String categoryKey(ItemStack stack) {
		return InventorySortCategories.categoryKey(stack);
	}

	@SuppressWarnings("unused")
	private static String legacyCategoryKey(ItemStack stack) {
		if (stack.isEmpty())
			return "99_empty";

		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;

		// ═══════════════════════════════════════════════════════════════
		// PORTABLE STORAGE - Items that can contain other items (top priority)
		// ═══════════════════════════════════════════════════════════════

		if (path.contains("bundle"))
			return "00_storage_bundle";

		if (path.equals("ender_chest"))
			return "00_storage_ender_chest";

		if (path.contains("shulker_box"))
			return "00_storage_shulker";

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

		if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.contains("hyphae"))
			return "01_wood_logs";

		if (path.endsWith("_planks"))
			return "02_wood_planks";

		if (path.equals("stick") || path.equals("bowl") || path.equals("ladder") || path.equals("scaffolding"))
			return "03_wood_items";

		if (path.endsWith("_leaves"))
			return "04_wood_leaves";

		if (path.endsWith("_sapling"))
			return "05_wood_saplings";

		// ═══════════════════════════════════════════════════════════════
		// STONE & TERRAIN BLOCKS
		// ═══════════════════════════════════════════════════════════════

		if (path.contains("dirt") || path.contains("grass_block") || path.contains("podzol") ||
				path.contains("mycelium") || path.contains("mud"))
			return "10_terrain_dirt";

		if (path.contains("stone") || path.contains("cobblestone") || path.contains("deepslate") ||
				path.contains("granite") || path.contains("diorite") || path.contains("andesite") ||
				path.contains("tuff") || path.contains("calcite") || path.contains("basalt") ||
				path.contains("blackstone") || path.contains("netherrack") || path.contains("end_stone"))
			return "11_terrain_stone";

		if (path.contains("sand") || path.contains("gravel") || path.contains("clay"))
			return "12_terrain_sand";

		// ═══════════════════════════════════════════════════════════════
		// ORES, INGOTS, GEMS & MINERALS
		// ═══════════════════════════════════════════════════════════════

		if (path.endsWith("_ore") || path.contains("_ore_") || path.startsWith("raw_"))
			return "20_minerals_ores";

		if (path.equals("diamond") || path.equals("emerald") || path.equals("amethyst_shard") ||
				path.equals("lapis_lazuli") || path.equals("prismarine_shard") || path.equals("prismarine_crystals") ||
				path.equals("quartz") || path.equals("echo_shard"))
			return "21_minerals_gems";

		if (path.endsWith("_ingot"))
			return "22_minerals_ingots";

		if (path.endsWith("_nugget"))
			return "23_minerals_nuggets";

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

		if (path.contains("slab"))
			return "40_build_slabs";

		if (path.contains("stairs"))
			return "41_build_stairs";

		if (path.contains("fence") || path.contains("wall") || path.contains("gate"))
			return "42_build_edges";

		if (path.contains("door") || path.contains("trapdoor"))
			return "43_build_doors";

		if (path.contains("glass") || path.contains("pane"))
			return "44_build_glass";

		if (path.contains("wool") || path.contains("carpet"))
			return "45_build_wool";

		if (path.contains("concrete") || path.contains("terracotta"))
			return "46_build_concrete";

		// ═══════════════════════════════════════════════════════════════
		// TOOLS, WEAPONS & ARMOR
		// ═══════════════════════════════════════════════════════════════

		if (path.contains("sword") || path.contains("bow") || path.contains("crossbow") ||
				path.contains("trident") || path.equals("arrow") || path.equals("spectral_arrow") ||
				path.contains("tipped_arrow"))
			return "60_combat_weapons";

		if (path.contains("axe") || path.contains("pickaxe") || path.contains("shovel") ||
				path.contains("hoe") || path.contains("shears") || path.equals("flint_and_steel") ||
				path.equals("fishing_rod"))
			return "61_tools";

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

		// Storage & containers (non-portable - chests, barrels, buckets)
		if (path.contains("chest") || path.contains("barrel") || path.equals("bucket"))
			return "80_misc_storage";

		if (path.contains("book") || path.equals("paper") || path.equals("writable_book") ||
				path.equals("written_book") || path.equals("enchanted_book"))
			return "81_misc_books";

		if (path.equals("string") || path.equals("leather") || path.equals("feather") ||
				path.equals("bone") || path.equals("rotten_flesh") || path.equals("slime_ball") ||
				path.equals("ender_pearl") || path.equals("blaze_rod"))
			return "82_misc_mob_drops";

		return "90_misc";
	}

	private static void applyLayout(AbstractContainerMenu menu,
									AbstractContainerScreenInvoker invoker,
									List<Slot> slots,
									List<ItemStack> desired,
									Slot hotbarSwapBuffer) {

		for (int i = 0; i < slots.size(); i++) {
			ItemStack want = desired.get(i);
			ItemStack have = slots.get(i).getItem();

			// Skip if this slot has a bundle - don't move bundles around via normal clicks
			if (isBundle(have))
				continue;

			// "Correct enough" if same item+components, ignore counts.
			if (sameTypeIgnoringCount(have, want))
				continue;

			int j = findMatchingSlot(slots, i + 1, want);
			if (j == -1)
				continue;

			// Skip if the source slot is a bundle
			if (isBundle(slots.get(j).getItem()))
				continue;

			swapSafely(invoker, slots.get(i), slots.get(j), hotbarSwapBuffer);

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

			// Skip bundles entirely - we don't want to accidentally insert items into them
			if (isBundle(target))
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

				// Skip bundles as source too
				if (isBundle(from))
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
									  AbstractContainerMenu menu, Slot hotbarSwapBuffer) {
		for (int i = 0; i < slots.size(); i++) {
			if (!slots.get(i).getItem().isEmpty())
				continue;

			int j = i + 1;
			while (j < slots.size() && (slots.get(j).getItem().isEmpty() || isBundle(slots.get(j).getItem())))
				j++;
			if (j >= slots.size())
				return;

			// Double-check we're not swapping with a bundle
			if (isBundle(slots.get(j).getItem()))
				continue;

			swapSafely(invoker, slots.get(i), slots.get(j), hotbarSwapBuffer);

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

	private static void clearTemporarySlots(AbstractContainerScreenInvoker invoker,
											List<Slot> temporarySlots,
											List<Slot> workSlots,
											Slot hotbarSwapBuffer) {
		for (Slot temporary : temporarySlots) {
			if (temporary.getItem().isEmpty()) {
				continue;
			}
			Slot empty = findFirstEmptyExcluding(workSlots, temporarySlots);
			if (empty != null) {
				swapSafely(invoker, temporary, empty, hotbarSwapBuffer);
			}
		}
	}

	private static Slot findFirstEmptyExcluding(List<Slot> slots, List<Slot> excluded) {
		for (Slot slot : slots) {
			if (!excluded.contains(slot) && slot.getItem().isEmpty()) {
				return slot;
			}
		}
		return null;
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
		ContainerClickCompat.pickup(invoker, slot);
	}

	private static void swap(AbstractContainerScreenInvoker invoker, Slot a, Slot b) {
		click(invoker, a);
		click(invoker, b);
		click(invoker, a);
	}

	private static void swapSafely(AbstractContainerScreenInvoker invoker, Slot a, Slot b, Slot hotbarSwapBuffer) {
		if (a == b) {
			return;
		}
		if (hotbarSwapBuffer != null) {
			swapUsingHotbarBuffer(invoker, a, b, hotbarSwapBuffer);
			return;
		}
		swap(invoker, a, b);
	}

	// ─────────────────────────────────────────────────────────────
	// Equality (1.21+ components) - Use vanilla stackability check
	// ─────────────────────────────────────────────────────────────

	private static boolean sameItemAndComponents(ItemStack a, ItemStack b) {
		if (a.isEmpty() || b.isEmpty())
			return false;
		return ItemStackCompat.sameItemAndComponents(a, b);
	}

	private static boolean sameTypeIgnoringCount(ItemStack a, ItemStack b) {
		if (a.isEmpty() && b.isEmpty())
			return true;
		if (a.isEmpty() || b.isEmpty())
			return false;
		return sameItemAndComponents(a, b);
	}

	public static String itemId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static boolean itemIdEquals(ItemStack stack, String itemId) {
		return itemId != null && itemId.equals(itemId(stack));
	}

	// ─────────────────────────────────────────────────────────────
	// Bundle detection - Skip bundles during sorting to avoid auto-insertion
	// ─────────────────────────────────────────────────────────────

	private static boolean isBundle(ItemStack stack) {
		return ItemStackCompat.isBundle(stack);
	}

	// ─────────────────────────────────────────────────────────────
	// Slot selection - IMPROVED to handle more screen types
	// ─────────────────────────────────────────────────────────────

	private static List<Slot> getSortableSlots(AbstractContainerMenu handler,
											   AbstractContainerScreen<?> screen,
											   List<Slot> playerMainSlots) {
		String screenName = screen.getClass().getSimpleName();
		int totalSlots = handler.slots.size();

		tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("Screen: {}, Total slots: {}", screenName, totalSlots);

		boolean isContainer = totalSlots > 46;

		boolean isContainerByName = screenName.contains("Container") ||
				screenName.contains("Chest") ||
				screenName.contains("Shulker") ||
				screenName.contains("Barrel") ||
				screenName.contains("Hopper") ||
				screenName.contains("Dispenser") ||
				screenName.contains("Dropper") ||
				screenName.contains("Furnace") ||
				screenName.contains("Brewing");

		if (isContainer || isContainerByName) {
			int containerSize = totalSlots - 36;

			if (containerSize <= 0) {
				tempeststudios.inventorysort.core.InventorySortCore.LOGGER.warn("Container size is {}, falling back to player main", containerSize);
				return new ArrayList<>(playerMainSlots);
			}

			tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("Detected container with {} slots", containerSize);
			return new ArrayList<>(handler.slots.subList(0, containerSize));
		}

		tempeststudios.inventorysort.core.InventorySortCore.LOGGER.debug("Detected player inventory, sorting {} main slots", playerMainSlots.size());
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
			if (slot.container != inv)
				continue;

			int idx = slot.getContainerSlot();
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
