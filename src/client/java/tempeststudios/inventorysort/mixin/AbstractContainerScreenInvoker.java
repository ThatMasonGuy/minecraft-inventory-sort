package tempeststudios.inventorysort.mixin;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenInvoker {

    @Invoker("slotClicked")
    void invokeSlotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType);
}