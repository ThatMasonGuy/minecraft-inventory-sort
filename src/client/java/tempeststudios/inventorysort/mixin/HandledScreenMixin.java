package tempeststudios.inventorysort.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.InventorySorter;
import tempeststudios.inventorysort.SearchModalScreen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

	@Unique private final List<Button> inventorySort$trackedButtons = new ArrayList<>();
	@Unique private boolean inventorySort$isContainer = false;
	@Unique private int inventorySort$containerRows = 0;

	// Cached via class-level statics so reflection only runs once per screen type
	@Unique private static Field inventorySort$recipeBookField = null;
	@Unique private static boolean inventorySort$recipeBookFieldSearched = false;

	private static boolean isShiftDown(Minecraft client) {
		return InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
				|| InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	/**
	 * Tries to place the button column just outside the right edge of the
	 * inventory panel. Falls back to the left edge if there is no room on the
	 * right, avoiding the old clampX() behaviour that pushed buttons back
	 * inside the panel and onto slots.
	 */
	@Unique
	private static int calcButtonX(int leftPos, int imageWidth, int screenWidth, int totalButtonWidth) {
		int rightX = leftPos + imageWidth;
		if (rightX + totalButtonWidth <= screenWidth) {
			return rightX;
		}
		int leftX = leftPos - totalButtonWidth;
		if (leftX >= 0) {
			return leftX;
		}
		return Math.max(0, screenWidth - totalButtonWidth);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void onInit(CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
		ScreenAccessor screenAccessor = (ScreenAccessor) this;

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) return;

		inventorySort$trackedButtons.clear();

		int totalSlots = screen.getMenu().slots.size();
		inventorySort$isContainer = totalSlots > 46;

		if (inventorySort$isContainer) {
			int containerSlots = totalSlots - 36;
			inventorySort$containerRows = (int) Math.ceil(containerSlots / 9.0);
		} else {
			inventorySort$containerRows = 0;
		}

		int[] pos = inventorySort$calculatePositions(screen, accessor);

		Button sortButton = Button.builder(
						Component.literal("≡"),
						btn -> {
							InventorySorter.sortInventory(screen, client.player);
							client.execute(() -> { btn.setFocused(false); screen.setFocused(null); });
						})
				.bounds(pos[0], pos[1], 18, 18)
				.tooltip(Tooltip.create(Component.literal("Sort items")))
				.build();
		screenAccessor.invokeAddRenderableWidget(sortButton);
		inventorySort$trackedButtons.add(sortButton);

		if (inventorySort$isContainer) {
			Button upButton = Button.builder(
							Component.literal("▲"),
							btn -> {
								InventorySorter.transferUp(screen, client.player, isShiftDown(client));
								client.execute(() -> { btn.setFocused(false); screen.setFocused(null); });
							})
					.bounds(pos[2], pos[3], 18, 18)
					.tooltip(Tooltip.create(Component.literal("Deposit matching (Shift: deposit all, no hotbar)")))
					.build();

			Button downButton = Button.builder(
							Component.literal("▼"),
							btn -> {
								InventorySorter.transferDown(screen, client.player, isShiftDown(client));
								client.execute(() -> { btn.setFocused(false); screen.setFocused(null); });
							})
					.bounds(pos[4], pos[5], 18, 18)
					.tooltip(Tooltip.create(Component.literal("Refill stacks (Shift: take all)")))
					.build();

			screenAccessor.invokeAddRenderableWidget(upButton);
			screenAccessor.invokeAddRenderableWidget(downButton);
			inventorySort$trackedButtons.add(upButton);
			inventorySort$trackedButtons.add(downButton);

		} else {
			Button searchButton = Button.builder(
							Component.literal("🔍"),
							btn -> {
								client.setScreen(new SearchModalScreen(screen));
								client.execute(() -> { btn.setFocused(false); screen.setFocused(null); });
							})
					.bounds(pos[2], pos[3], 18, 18)
					.tooltip(Tooltip.create(Component.literal("Search inventory")))
					.build();
			screenAccessor.invokeAddRenderableWidget(searchButton);
			inventorySort$trackedButtons.add(searchButton);
		}
	}

	/**
	 * Reposition buttons every frame so they stay anchored to the inventory
	 * panel even when leftPos/topPos change without a full init() cycle
	 * (e.g. recipe-book toggle).
	 */
	@Inject(method = "render", at = @At("HEAD"))
	private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (inventorySort$trackedButtons.isEmpty()) return;

		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;

		int[] pos = inventorySort$calculatePositions(screen, accessor);

		inventorySort$trackedButtons.get(0).setX(pos[0]);
		inventorySort$trackedButtons.get(0).setY(pos[1]);

		if (inventorySort$isContainer) {
			if (inventorySort$trackedButtons.size() > 1) {
				inventorySort$trackedButtons.get(1).setX(pos[2]);
				inventorySort$trackedButtons.get(1).setY(pos[3]);
			}
			if (inventorySort$trackedButtons.size() > 2) {
				inventorySort$trackedButtons.get(2).setX(pos[4]);
				inventorySort$trackedButtons.get(2).setY(pos[5]);
			}
		} else {
			if (inventorySort$trackedButtons.size() > 1) {
				inventorySort$trackedButtons.get(1).setX(pos[2]);
				inventorySort$trackedButtons.get(1).setY(pos[3]);
			}
		}
	}

	/**
	 * Returns [sortX, sortY, btn1X, btn1Y, btn2X, btn2Y].
	 * btn2X/btn2Y are 0 for non-container screens.
	 */
	@Unique
	private int[] inventorySort$calculatePositions(AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor accessor) {
		int effectiveLeftPos = inventorySort$getEffectiveLeftPos(screen, accessor);
		int topPos = accessor.getTopPos();

		int baseY = inventorySort$isContainer
				? topPos + inventorySort$containerRows * 18 - 1
				: topPos + 84 + 36 - 1;

		int sortY   = baseY - 18;
		int bottomY = baseY;

		if (inventorySort$isContainer) {
			// ▲▼ sit side by side (36px wide); ≡ aligns with the left of that pair
			int buttonsX = calcButtonX(effectiveLeftPos, accessor.getImageWidth(), screen.width, 36);
			return new int[]{buttonsX, sortY, buttonsX, bottomY, buttonsX + 18, bottomY};
		} else {
			// Single column (18px wide)
			int buttonsX = calcButtonX(effectiveLeftPos, accessor.getImageWidth(), screen.width, 18);
			return new int[]{buttonsX, sortY, buttonsX, bottomY, 0, 0};
		}
	}

	/**
	 * Returns the effective leftPos of the inventory panel, accounting for the
	 * recipe book shifting it right in InventoryScreen.
	 */
	@Unique
	private int inventorySort$getEffectiveLeftPos(AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor accessor) {
		if (screen instanceof InventoryScreen && inventorySort$isRecipeBookVisible(screen)) {
			// Vanilla shifts the panel 77px right when the recipe book is open
			return (screen.width - accessor.getImageWidth()) / 2 + 77;
		}
		return accessor.getLeftPos();
	}

	@Unique
	private boolean inventorySort$isRecipeBookVisible(AbstractContainerScreen<?> screen) {
		if (!inventorySort$recipeBookFieldSearched) {
			inventorySort$recipeBookFieldSearched = true;
			inventorySort$recipeBookField = inventorySort$findRecipeBookField(screen.getClass());
		}
		if (inventorySort$recipeBookField != null) {
			try {
				Object rb = inventorySort$recipeBookField.get(screen);
				if (rb instanceof RecipeBookComponent rbc) return rbc.isVisible();
			} catch (Exception ignored) {}
		}
		return false;
	}

	@Unique
	private static Field inventorySort$findRecipeBookField(Class<?> clazz) {
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				if (RecipeBookComponent.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					return field;
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}
}
