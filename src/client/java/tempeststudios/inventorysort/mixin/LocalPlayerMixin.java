package tempeststudios.inventorysort.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.inventorysort.ContainerPositionCapture;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    /**
     * Capture the block position when player opens a container
     * This runs when the player right-clicks on a block
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        
        // Check if player is looking at a block
        HitResult hitResult = player.pick(20.0D, 0.0F, false);
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos pos = blockHit.getBlockPos();
            
            // Store this position - it might be used when a container opens
            ContainerPositionCapture.setLastLookedAtBlock(pos);
        }
    }
}
