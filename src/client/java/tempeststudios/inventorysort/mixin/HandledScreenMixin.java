package tempeststudios.inventorysort.mixin;

import tempeststudios.inventorysort.InventorySortClient;
import tempeststudios.inventorysort.InventorySorter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

	@Inject(method = "init", at = @At("TAIL"))
	private void onInit(CallbackInfo ci) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
		ScreenAccessor screenAccessor = (ScreenAccessor) this;

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null)
			return;

		// Calculate button position - OUTSIDE the GUI to the right
		int x = accessor.getLeftPos() + accessor.getImageWidth() + 4;
		int y = accessor.getTopPos() + 4;

		// Create button widget
		Button sortButton = Button.builder(
				Component.literal("Sort"),
				button -> {
					InventorySortClient.LOGGER.info("Sort button pressed!");
					InventorySorter.sortInventory(screen, client.player);
				})
				.bounds(x, y, 55, 12)
				.build();

		// Add the button using the accessor
		screenAccessor.invokeAddRenderableWidget(sortButton);

		InventorySortClient.LOGGER.debug("Added sort button to screen: {}", screen.getClass().getSimpleName());
	}
}