package tempeststudios.inventorysort.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.RecipeBookAwareButtonScreen;

@Pseudo
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen")
public abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), require = 0)
    private void inventorySort$updateButtonsOnRecipeBookRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this instanceof RecipeBookAwareButtonScreen buttonScreen) {
            buttonScreen.inventorysort$updateButtonPositionsFromRecipeBookRender();
            buttonScreen.inventorysearch$updateButtonPositionsFromRecipeBookRender();
        }
    }
}
