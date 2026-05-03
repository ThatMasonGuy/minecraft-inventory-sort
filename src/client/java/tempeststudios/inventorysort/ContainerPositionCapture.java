package tempeststudios.inventorysort;

import net.minecraft.core.BlockPos;

/**
 * Utility to capture the block position used to open containers.
 */
public class ContainerPositionCapture {
    private static BlockPos lastInteractedBlock = null;
    private static long lastInteractionTime = 0;
    private static final long POSITION_TIMEOUT = 3000; // server/container open can arrive a few ticks later

    public static void setLastLookedAtBlock(BlockPos pos) {
        setLastInteractedBlock(pos);
    }

    public static void setLastInteractedBlock(BlockPos pos) {
        lastInteractedBlock = pos;
        lastInteractionTime = System.currentTimeMillis();
    }

    public static BlockPos getLastLookedAtBlock() {
        // Return null if too much time has passed since the actual block interaction.
        if (System.currentTimeMillis() - lastInteractionTime > POSITION_TIMEOUT) {
            return null;
        }
        return lastInteractedBlock;
    }

    public static void clear() {
        lastInteractedBlock = null;
        lastInteractionTime = 0;
    }
}
