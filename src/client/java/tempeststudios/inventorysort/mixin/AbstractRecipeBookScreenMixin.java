package tempeststudios.inventorysort.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.RecipeBookAwareButtonScreen;

@Pseudo
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen")
public abstract class AbstractRecipeBookScreenMixin {
	@Inject(method = "render", at = @At("HEAD"), require = 0)
	private void inventorySort$updateButtonsOnRecipeBookRender(CallbackInfo ci) {
		if (this instanceof RecipeBookAwareButtonScreen buttonScreen) {
			buttonScreen.inventorysort$updateButtonPositionsFromRecipeBookRender();
		}
	}
}
