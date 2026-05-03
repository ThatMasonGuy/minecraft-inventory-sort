package tempeststudios.inventorysort.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tempeststudios.inventorysort.ContainerIdentity;
import tempeststudios.inventorysort.ContainerPositionCapture;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void inventorySort$captureUsedBlock(LocalPlayer player,
                                                InteractionHand hand,
                                                BlockHitResult hitResult,
                                                CallbackInfoReturnable<?> cir) {
        ContainerPositionCapture.setLastInteractedBlock(hitResult.getBlockPos());
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void inventorySort$captureInteractedEntity(Player player,
                                                       Entity entity,
                                                       InteractionHand hand,
                                                       CallbackInfoReturnable<InteractionResult> cir) {
        inventorySort$captureEntityContainer(entity);
    }

    @Inject(method = "interactAt", at = @At("HEAD"))
    private void inventorySort$captureInteractedEntityAt(Player player,
                                                         Entity entity,
                                                         EntityHitResult hitResult,
                                                         InteractionHand hand,
                                                         CallbackInfoReturnable<InteractionResult> cir) {
        inventorySort$captureEntityContainer(entity);
    }

    private void inventorySort$captureEntityContainer(Entity entity) {
        ContainerIdentity identity = ContainerIdentity.fromEntity(net.minecraft.client.Minecraft.getInstance(), entity);
        if (identity != null) {
            ContainerPositionCapture.setLastInteractedEntityContainer(identity);
        }
    }
}
