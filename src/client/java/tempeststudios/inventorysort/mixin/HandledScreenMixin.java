package tempeststudios.inventorysort.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.InventorySortConfigScreen;
import tempeststudios.inventorysort.InventorySortIconButton;
import tempeststudios.inventorysort.InventorySorter;
import tempeststudios.inventorysort.RecipeBookAwareButtonScreen;
import tempeststudios.inventorysort.api.InventoryScreenButtonSlots;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin implements RecipeBookAwareButtonScreen {

	@Unique private static final int inventorySort$BUTTON_SIZE = 12;
	@Unique private static final String inventorySort$OWNER = "inventorysort";
	@Unique private static final String inventorySort$PLAYER_SORT_SLOT = "player_sort";
	@Unique private static final String inventorySort$PLAYER_MATCHING_TO_CONTAINER_SLOT = "player_matching_to_container";
	@Unique private static final String inventorySort$PLAYER_ALL_TO_CONTAINER_SLOT = "player_all_to_container";
	@Unique private static final String inventorySort$CONTAINER_SORT_SLOT = "container_sort";
	@Unique private static final String inventorySort$CONTAINER_MATCHING_TO_PLAYER_SLOT = "container_matching_to_player";
	@Unique private static final String inventorySort$CONTAINER_ALL_TO_PLAYER_SLOT = "container_all_to_player";
	@Unique private static final int inventorySort$PLAYER_SORT = 0;
	@Unique private static final int inventorySort$PLAYER_MATCHING_TO_CONTAINER = 1;
	@Unique private static final int inventorySort$PLAYER_ALL_TO_CONTAINER = 2;
	@Unique private static final int inventorySort$CONTAINER_SORT = 3;
	@Unique private static final int inventorySort$CONTAINER_MATCHING_TO_PLAYER = 4;
	@Unique private static final int inventorySort$CONTAINER_ALL_TO_PLAYER = 5;

	@Unique private final List<Button> inventorySort$trackedButtons = new ArrayList<>();
	@Unique private final List<Integer> inventorySort$trackedButtonRoles = new ArrayList<>();
	@Unique private boolean inventorySort$isContainer = false;

	@Inject(method = "init", at = @At("TAIL"))
	private void onInit(CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		ScreenAccessor screenAccessor = (ScreenAccessor) this;

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) return;

		inventorySort$trackedButtons.clear();
		inventorySort$trackedButtonRoles.clear();
		InventoryScreenButtonSlots.releaseOwner(screen, inventorySort$OWNER);

		inventorySort$isContainer = InventoryScreenButtonSlots.isInventoryModsContainer(screen);

		inventorySort$addButton(screen, screenAccessor, inventorySort$PLAYER_SORT, InventorySortIconButton.SORT,
				"Sort inventory",
				btn -> {
					InventorySorter.sortPlayerInventory(screen, client.player);
					inventorySort$clearFocus(client, screen, btn);
				},
				btn -> inventorySort$openRulesScreen(client, screen, false, btn));

		if (inventorySort$isContainer) {
			inventorySort$addButton(screen, screenAccessor, inventorySort$PLAYER_MATCHING_TO_CONTAINER, InventorySortIconButton.MATCHING,
					"Move matching items to container",
					btn -> {
						InventorySorter.transferUp(screen, client.player, false);
						inventorySort$clearFocus(client, screen, btn);
					});
			inventorySort$addButton(screen, screenAccessor, inventorySort$PLAYER_ALL_TO_CONTAINER, InventorySortIconButton.ALL,
					"Move all inventory items to container (no hotbar)",
					btn -> {
						InventorySorter.transferUp(screen, client.player, true);
						inventorySort$clearFocus(client, screen, btn);
					});
			inventorySort$addButton(screen, screenAccessor, inventorySort$CONTAINER_SORT, InventorySortIconButton.SORT,
					"Sort container",
					btn -> {
						InventorySorter.sortInventory(screen, client.player);
						inventorySort$clearFocus(client, screen, btn);
					},
					btn -> inventorySort$openRulesScreen(client, screen, true, btn));
			inventorySort$addButton(screen, screenAccessor, inventorySort$CONTAINER_MATCHING_TO_PLAYER, InventorySortIconButton.MATCHING,
					"Move matching items to inventory",
					btn -> {
						InventorySorter.transferDown(screen, client.player, false);
						inventorySort$clearFocus(client, screen, btn);
					});
			inventorySort$addButton(screen, screenAccessor, inventorySort$CONTAINER_ALL_TO_PLAYER, InventorySortIconButton.ALL,
					"Move all container items to inventory",
					btn -> {
						InventorySorter.transferDown(screen, client.player, true);
						inventorySort$clearFocus(client, screen, btn);
					});
		}
	}

	@Unique
	private void inventorySort$addButton(AbstractContainerScreen<?> screen,
										 ScreenAccessor screenAccessor,
										 int role,
										 int icon,
										 String tooltip,
										 Button.OnPress onPress) {
		inventorySort$addButton(screen, screenAccessor, role, icon, tooltip, onPress, null);
	}

	@Unique
	private void inventorySort$addButton(AbstractContainerScreen<?> screen,
										 ScreenAccessor screenAccessor,
										 int role,
										 int icon,
										 String tooltip,
										 Button.OnPress onPress,
										 Button.OnPress secondaryOnPress) {
		int[] pos = inventorySort$positionFor(role, screen, (AbstractContainerScreenAccessor) this);
		Button button = new InventorySortIconButton(pos[0], pos[1], icon, Component.literal(tooltip), onPress, secondaryOnPress);
		screenAccessor.invokeAddRenderableWidget(button);
		inventorySort$trackedButtons.add(button);
		inventorySort$trackedButtonRoles.add(role);
	}

	@Unique
	private static void inventorySort$clearFocus(Minecraft client, AbstractContainerScreen<?> screen, Button btn) {
		client.execute(() -> {
			btn.setFocused(false);
			screen.setFocused(null);
		});
	}

	@Unique
	private static void inventorySort$openRulesScreen(Minecraft client,
													  AbstractContainerScreen<?> screen,
													  boolean containerTarget,
													  Button btn) {
		if (client.player == null) {
			return;
		}
		MinecraftApiCompat.setScreen(client, new InventorySortConfigScreen(screen, client.player, containerTarget));
		inventorySort$clearFocus(client, screen, btn);
	}

	/**
	 * Reposition buttons every frame so they stay anchored to vanilla leftPos.
	 * Recipe-book toggles update leftPos directly in AbstractRecipeBookScreen.
	 */
	@Inject(method = "render", at = @At("HEAD"))
	private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		inventorySort$updateButtonPositions();
	}

	@Override
	public void inventorysort$updateButtonPositionsFromRecipeBookRender() {
		inventorySort$updateButtonPositions();
	}

	@Unique
	private void inventorySort$updateButtonPositions() {
		if (inventorySort$trackedButtons.isEmpty()) return;

		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;

		for (int i = 0; i < inventorySort$trackedButtons.size(); i++) {
			Button button = inventorySort$trackedButtons.get(i);
			int[] pos = inventorySort$positionFor(inventorySort$trackedButtonRoles.get(i), screen, accessor);
			button.setX(pos[0]);
			button.setY(pos[1]);
		}
	}

	@Unique
	private int[] inventorySort$positionFor(int role, AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor accessor) {
		InventoryScreenButtonSlots.SlotPlacement placement = InventoryScreenButtonSlots.reserveRightSlot(
				screen,
				inventorySort$slotGroup(role),
				inventorySort$OWNER,
				inventorySort$slotId(role),
				inventorySort$priority(role),
				inventorySort$BUTTON_SIZE
		);
		return new int[]{placement.x(), placement.y()};
	}

	@Unique
	private static InventoryScreenButtonSlots.RightSlotGroup inventorySort$slotGroup(int role) {
		if (role == inventorySort$CONTAINER_SORT
				|| role == inventorySort$CONTAINER_MATCHING_TO_PLAYER
				|| role == inventorySort$CONTAINER_ALL_TO_PLAYER) {
			return InventoryScreenButtonSlots.RightSlotGroup.CONTAINER;
		}
		return InventoryScreenButtonSlots.RightSlotGroup.PLAYER_INVENTORY;
	}

	@Unique
	private static String inventorySort$slotId(int role) {
		if (role == inventorySort$PLAYER_MATCHING_TO_CONTAINER) {
			return inventorySort$PLAYER_MATCHING_TO_CONTAINER_SLOT;
		}
		if (role == inventorySort$PLAYER_ALL_TO_CONTAINER) {
			return inventorySort$PLAYER_ALL_TO_CONTAINER_SLOT;
		}
		if (role == inventorySort$CONTAINER_SORT) {
			return inventorySort$CONTAINER_SORT_SLOT;
		}
		if (role == inventorySort$CONTAINER_MATCHING_TO_PLAYER) {
			return inventorySort$CONTAINER_MATCHING_TO_PLAYER_SLOT;
		}
		if (role == inventorySort$CONTAINER_ALL_TO_PLAYER) {
			return inventorySort$CONTAINER_ALL_TO_PLAYER_SLOT;
		}
		return inventorySort$PLAYER_SORT_SLOT;
	}

	@Unique
	private static int inventorySort$priority(int role) {
		return InventoryScreenButtonSlots.FIRST_PARTY_SORT_PRIORITY + role;
	}

}
