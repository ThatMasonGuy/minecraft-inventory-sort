package tempeststudios.inventorysort.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.InventorySortIconButton;
import tempeststudios.inventorysort.InventorySorter;
import tempeststudios.inventorysort.RecipeBookAwareButtonScreen;
import tempeststudios.inventorysort.SearchModalScreen;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin implements RecipeBookAwareButtonScreen {

	@Unique private static final int inventorySort$BUTTON_SIZE = 12;
	@Unique private static final int inventorySort$BUTTON_GAP = 1;
	@Unique private static final int inventorySort$PLAYER_SORT = 0;
	@Unique private static final int inventorySort$PLAYER_MATCHING_TO_CONTAINER = 1;
	@Unique private static final int inventorySort$PLAYER_ALL_TO_CONTAINER = 2;
	@Unique private static final int inventorySort$CONTAINER_SORT = 3;
	@Unique private static final int inventorySort$CONTAINER_MATCHING_TO_PLAYER = 4;
	@Unique private static final int inventorySort$CONTAINER_ALL_TO_PLAYER = 5;
	@Unique private static final int inventorySort$SEARCH = 6;

	@Unique private final List<Button> inventorySort$trackedButtons = new ArrayList<>();
	@Unique private final List<Integer> inventorySort$trackedButtonRoles = new ArrayList<>();
	@Unique private boolean inventorySort$isContainer = false;
	@Unique private int inventorySort$containerRows = 0;

	/**
	 * Places buttons just outside the right edge of the GUI, falling back to the
	 * left edge when the screen is too narrow.
	 */
	@Unique
	private static int calcButtonX(int leftPos, int imageWidth, int screenWidth, int totalButtonWidth) {
		int rightX = leftPos + imageWidth - 3;
		if (rightX + totalButtonWidth <= screenWidth) {
			return rightX;
		}
		int leftX = leftPos - totalButtonWidth + 3;
		if (leftX >= 0) {
			return leftX;
		}
		return Math.max(0, screenWidth - totalButtonWidth);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void onInit(CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		ScreenAccessor screenAccessor = (ScreenAccessor) this;

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) return;

		inventorySort$trackedButtons.clear();
		inventorySort$trackedButtonRoles.clear();

		int totalSlots = screen.getMenu().slots.size();
		inventorySort$isContainer = totalSlots > 46 && !(screen instanceof CreativeModeInventoryScreen);

		if (inventorySort$isContainer) {
			int containerSlots = totalSlots - 36;
			inventorySort$containerRows = (int) Math.ceil(containerSlots / 9.0);
		} else {
			inventorySort$containerRows = 0;
		}

		inventorySort$addButton(screen, screenAccessor, inventorySort$PLAYER_SORT, InventorySortIconButton.SORT,
				"Sort inventory",
				btn -> {
					InventorySorter.sortPlayerInventory(screen, client.player);
					inventorySort$clearFocus(client, screen, btn);
				});

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
					});
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
		} else {
			inventorySort$addButton(screen, screenAccessor, inventorySort$SEARCH, InventorySortIconButton.SEARCH,
					"Search inventory",
					btn -> {
						client.setScreen(new SearchModalScreen(screen));
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
		int[] pos = inventorySort$positionFor(role, screen, (AbstractContainerScreenAccessor) this);
		Button button = new InventorySortIconButton(pos[0], pos[1], icon, Component.literal(tooltip), onPress);
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
		int x = calcButtonX(accessor.getLeftPos(), accessor.getImageWidth(), screen.width, inventorySort$BUTTON_SIZE);
		int y;
		if (role == inventorySort$CONTAINER_SORT) {
			y = inventorySort$containerGroupY(accessor);
		} else if (role == inventorySort$CONTAINER_MATCHING_TO_PLAYER) {
			y = inventorySort$containerGroupY(accessor) + inventorySort$rowOffset(1);
		} else if (role == inventorySort$CONTAINER_ALL_TO_PLAYER) {
			y = inventorySort$containerGroupY(accessor) + inventorySort$rowOffset(2);
		} else if (role == inventorySort$PLAYER_MATCHING_TO_CONTAINER || role == inventorySort$SEARCH) {
			y = inventorySort$playerGroupY(accessor) + inventorySort$rowOffset(1);
		} else if (role == inventorySort$PLAYER_ALL_TO_CONTAINER) {
			y = inventorySort$playerGroupY(accessor) + inventorySort$rowOffset(2);
		} else {
			y = inventorySort$playerGroupY(accessor);
		}
		return new int[]{x, y};
	}

	@Unique
	private int inventorySort$containerGroupY(AbstractContainerScreenAccessor accessor) {
		int groupHeight = inventorySort$rowOffset(2) + inventorySort$BUTTON_SIZE;
		int containerHeight = Math.max(1, inventorySort$containerRows) * 18;
		return accessor.getTopPos() + 17 + Math.max(0, (containerHeight - groupHeight) / 2);
	}

	@Unique
	private static int inventorySort$playerGroupY(AbstractContainerScreenAccessor accessor) {
		return accessor.getTopPos() + accessor.getImageHeight() - 83;
	}

	@Unique
	private static int inventorySort$rowOffset(int row) {
		return row * (inventorySort$BUTTON_SIZE + inventorySort$BUTTON_GAP);
	}

}
