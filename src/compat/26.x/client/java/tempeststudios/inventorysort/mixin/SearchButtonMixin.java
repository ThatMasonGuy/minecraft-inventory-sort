package tempeststudios.inventorysort.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.InventorySortIconButton;
import tempeststudios.inventorysort.RecipeBookAwareButtonScreen;
import tempeststudios.inventorysort.SearchModalScreen;
import tempeststudios.inventorysort.api.InventoryScreenButtonSlots;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

@Mixin(AbstractContainerScreen.class)
public abstract class SearchButtonMixin implements RecipeBookAwareButtonScreen {

    @Unique private static final int inventorySearch$BUTTON_SIZE = 12;
    @Unique private static final String inventorySearch$OWNER = "inventorysearch";
    @Unique private static final String inventorySearch$SEARCH_SLOT = "inventory_search";

    @Unique private Button inventorySearch$searchButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void inventorySearch$onInit(CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        ScreenAccessor screenAccessor = (ScreenAccessor) this;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;

        inventorySearch$searchButton = null;
        InventoryScreenButtonSlots.releaseOwner(screen, inventorySearch$OWNER);

        if (inventorySearch$isContainer(screen)) {
            return;
        }

        int[] pos = inventorySearch$positionFor(screen, (AbstractContainerScreenAccessor) this);
        inventorySearch$searchButton = new InventorySortIconButton(
                pos[0],
                pos[1],
                InventorySortIconButton.SEARCH,
                Component.literal("Search inventory"),
                btn -> {
                    MinecraftApiCompat.setScreen(client, new SearchModalScreen(screen));
                    inventorySearch$clearFocus(client, screen, btn);
                });
        screenAccessor.invokeAddRenderableWidget(inventorySearch$searchButton);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void inventorySearch$onExtractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        inventorySearch$updateButtonPosition();
    }

    @Override
    public void inventorysearch$updateButtonPositionsFromRecipeBookRender() {
        inventorySearch$updateButtonPosition();
    }

    @Unique
    private void inventorySearch$updateButtonPosition() {
        if (inventorySearch$searchButton == null) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int[] pos = inventorySearch$positionFor(screen, (AbstractContainerScreenAccessor) this);
        inventorySearch$searchButton.setX(pos[0]);
        inventorySearch$searchButton.setY(pos[1]);
    }

    @Unique
    private static boolean inventorySearch$isContainer(AbstractContainerScreen<?> screen) {
        return InventoryScreenButtonSlots.isInventoryModsContainer(screen);
    }

    @Unique
    private static int[] inventorySearch$positionFor(AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor accessor) {
        InventoryScreenButtonSlots.SlotPlacement placement = InventoryScreenButtonSlots.reserveRightSlot(
                screen,
                InventoryScreenButtonSlots.RightSlotGroup.PLAYER_INVENTORY,
                inventorySearch$OWNER,
                inventorySearch$SEARCH_SLOT,
                InventoryScreenButtonSlots.FIRST_PARTY_SEARCH_PRIORITY,
                inventorySearch$BUTTON_SIZE
        );
        return new int[]{placement.x(), placement.y()};
    }

    @Unique
    private static void inventorySearch$clearFocus(Minecraft client, AbstractContainerScreen<?> screen, Button btn) {
        client.execute(() -> {
            btn.setFocused(false);
            screen.setFocused(null);
        });
    }
}
