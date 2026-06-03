package tempeststudios.inventorysort.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.DispenserMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.InventorySortIconButton;
import tempeststudios.inventorysort.SearchModalScreen;
import tempeststudios.inventorysort.compat.core.MinecraftApiCompat;

@Mixin(AbstractContainerScreen.class)
public abstract class SearchButtonMixin {

    @Unique private static final int inventorySearch$BUTTON_SIZE = 12;
    @Unique private static final int inventorySearch$BUTTON_GAP = 1;

    @Unique private Button inventorySearch$searchButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void inventorySearch$onInit(CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        ScreenAccessor screenAccessor = (ScreenAccessor) this;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;

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
        var menu = screen.getMenu();
        int totalSlots = menu.slots.size();
        boolean smallGridContainer = menu instanceof DispenserMenu;
        return !(screen instanceof CreativeModeInventoryScreen) && (totalSlots > 46 || smallGridContainer);
    }

    @Unique
    private static int[] inventorySearch$positionFor(AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor accessor) {
        int x = inventorySearch$calcButtonX(accessor.getLeftPos(), accessor.getImageWidth(), screen.width, inventorySearch$BUTTON_SIZE);
        int y = accessor.getTopPos() + accessor.getImageHeight() - 83 + inventorySearch$rowOffset(1);
        return new int[]{x, y};
    }

    @Unique
    private static int inventorySearch$calcButtonX(int leftPos, int imageWidth, int screenWidth, int totalButtonWidth) {
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

    @Unique
    private static int inventorySearch$rowOffset(int row) {
        return row * (inventorySearch$BUTTON_SIZE + inventorySearch$BUTTON_GAP);
    }

    @Unique
    private static void inventorySearch$clearFocus(Minecraft client, AbstractContainerScreen<?> screen, Button btn) {
        client.execute(() -> {
            btn.setFocused(false);
            screen.setFocused(null);
        });
    }
}
