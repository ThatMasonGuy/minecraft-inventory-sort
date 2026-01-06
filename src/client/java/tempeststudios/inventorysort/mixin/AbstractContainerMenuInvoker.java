package tempeststudios.inventorysort.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {

    @Invoker("doClick")
    void invokeDoClick(int slotId, int button, ClickType clickType, Player player);
}