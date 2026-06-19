package tempeststudios.inventorysort.compat.sort;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import tempeststudios.inventorysort.mixin.AbstractContainerScreenInvoker;

public final class ContainerClickCompat {
    private ContainerClickCompat() {
    }

    public static void pickup(AbstractContainerScreenInvoker invoker, Slot slot) {
        invoker.invokeSlotClicked(slot, slot.index, 0, ContainerInput.PICKUP);
    }

    public static void quickMove(AbstractContainerScreenInvoker invoker, Slot slot) {
        invoker.invokeSlotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
    }

    public static void hotbarSwap(AbstractContainerScreenInvoker invoker, Slot slot, int hotbarIndex) {
        invoker.invokeSlotClicked(slot, slot.index, hotbarIndex, ContainerInput.SWAP);
    }
}
