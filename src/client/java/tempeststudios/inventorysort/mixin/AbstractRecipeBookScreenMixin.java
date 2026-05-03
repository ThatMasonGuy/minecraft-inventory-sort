package tempeststudios.inventorysort.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.RecipeBookAwareButtonScreen;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void inventorySort$updateButtonsOnRecipeBookRender(GuiGraphics guiGraphics,
															   int mouseX,
															   int mouseY,
															   float partialTick,
															   CallbackInfo ci) {
		if (this instanceof RecipeBookAwareButtonScreen buttonScreen) {
			buttonScreen.inventorysort$updateButtonPositionsFromRecipeBookRender();
		}
	}
}
