package tempeststudios.inventorysort.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.InventorySorter;
import tempeststudios.inventorysort.SearchModalScreen;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

	@Unique private Button inventorySort$sortButton;
	@Unique private Button inventorySort$button1; // transfer-up or search
	@Unique private Button inventorySort$button2; // transfer-down (containers only)
	@Unique private boolean inventorySort$isContainer;
	@Unique private int inventorySort$containerRows;

	private static boolean isShiftDown(Minecraft client) {
		return InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
				|| InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	/**
	 * Tries to place a block of buttons just outside the right edge of the
	 * inventory panel. Falls back to the left edge if there is no room on the
	 * right, and finally clamps to 0 if neither side has room.
	 */
	@Unique
	private static int calcButtonX(int leftPos, int imageWidth, int screenWidth, int totalButtonWidth) {
		int rightX = leftPos + imageWidth; // just outside the panel's right border
		if (rightX + totalButtonWidth <= screenWidth) {
			return rightX;
		}
		int leftX = leftPos - totalButtonWidth; // just outside the panel's left border
		if (leftX >= 0) {
			return leftX;
		}
		// Last resort: clamp to right edge (still outside slots in practice)
		return Math.max(0, screenWidth - totalButtonWidth);
	}

	@Unique
	private int calcBaseY(AbstractContainerScreenAccessor accessor) {
		if (inventorySort$isContainer) {
			return accessor.getTopPos() + inventorySort$containerRows * 18 - 1;
		}
		// Player-inventory style: main-inv starts at topPos+84, hotbar is 36px below
		return accessor.getTopPos() + 84 + 36 - 1;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void onInit(CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
		ScreenAccessor screenAccessor = (ScreenAccessor) this;

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) return;

		int totalSlots = screen.getMenu().slots.size();
		inventorySort$isContainer = totalSlots > 46;

		if (inventorySort$isContainer) {
			int containerSlots = totalSlots - 36;
			inventorySort$containerRows = (int) Math.ceil(containerSlots / 9.0);
		}

		int baseY = calcBaseY(accessor);

		// For containers the bottom row has two 18-wide buttons side by side (36 total).
		// For non-containers it is a single button (18 wide). Use the wider measurement
		// so both rows start at the same X.
		int columnWidth = inventorySort$isContainer ? 36 : 18;
		int buttonsX = calcButtonX(accessor.getLeftPos(), accessor.getImageWidth(), screen.width, columnWidth);

		// Sort button — always present, sits one row above the action buttons
		inventorySort$sortButton = Button.builder(
						Component.literal("≡"),
						btn -> {
							InventorySorter.sortInventory(screen, client.player);
							client.execute(() -> {
								btn.setFocused(false);
								screen.setFocused(null);
							});
						})
				.bounds(buttonsX, baseY - 18, 18, 18)
				.tooltip(Tooltip.create(Component.literal("Sort items")))
				.build();
		screenAccessor.invokeAddRenderableWidget(inventorySort$sortButton);

		if (inventorySort$isContainer) {
			inventorySort$button1 = Button.builder(
							Component.literal("▲"),
							btn -> {
								boolean shift = isShiftDown(client);
								InventorySorter.transferUp(screen, client.player, shift);
								client.execute(() -> {
									btn.setFocused(false);
									screen.setFocused(null);
								});
							})
					.bounds(buttonsX, baseY, 18, 18)
					.tooltip(Tooltip.create(Component.literal("Deposit matching (Shift: deposit all, no hotbar)")))
					.build();

			inventorySort$button2 = Button.builder(
							Component.literal("▼"),
							btn -> {
								boolean shift = isShiftDown(client);
								InventorySorter.transferDown(screen, client.player, shift);
								client.execute(() -> {
									btn.setFocused(false);
									screen.setFocused(null);
								});
							})
					.bounds(buttonsX + 18, baseY, 18, 18)
					.tooltip(Tooltip.create(Component.literal("Refill stacks (Shift: take all)")))
					.build();

			screenAccessor.invokeAddRenderableWidget(inventorySort$button1);
			screenAccessor.invokeAddRenderableWidget(inventorySort$button2);
		} else {
			inventorySort$button1 = Button.builder(
							Component.literal("🔍"),
							btn -> {
								client.setScreen(new SearchModalScreen(screen));
								client.execute(() -> {
									btn.setFocused(false);
									screen.setFocused(null);
								});
							})
					.bounds(buttonsX, baseY, 18, 18)
					.tooltip(Tooltip.create(Component.literal("Search inventory")))
					.build();

			inventorySort$button2 = null;
			screenAccessor.invokeAddRenderableWidget(inventorySort$button1);
		}
	}

	/**
	 * Reposition buttons every frame so they stay anchored to the inventory
	 * panel even when leftPos/topPos change without a full init() cycle
	 * (e.g. recipe-book toggle).
	 */
	@Inject(method = "render", at = @At("HEAD"))
	private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (inventorySort$sortButton == null) return;

		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;

		int baseY = calcBaseY(accessor);
		int columnWidth = inventorySort$isContainer ? 36 : 18;
		int buttonsX = calcButtonX(accessor.getLeftPos(), accessor.getImageWidth(), screen.width, columnWidth);

		inventorySort$sortButton.setPosition(buttonsX, baseY - 18);

		if (inventorySort$button1 != null) {
			inventorySort$button1.setPosition(buttonsX, baseY);
		}
		if (inventorySort$button2 != null) {
			inventorySort$button2.setPosition(buttonsX + 18, baseY);
		}
	}
}
