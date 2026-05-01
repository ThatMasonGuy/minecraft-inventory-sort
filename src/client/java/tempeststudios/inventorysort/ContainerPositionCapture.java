package tempeststudios.inventorysort;

import net.minecraft.core.BlockPos;

/**
 * Utility to capture the block position of containers when they're opened
 */
public class ContainerPositionCapture {
    private static BlockPos lastLookedAtBlock = null;
    private static long lastLookTime = 0;
    private static final long POSITION_TIMEOUT = 1000; // 1 second timeout

    public static void setLastLookedAtBlock(BlockPos pos) {
        lastLookedAtBlock = pos;
        lastLookTime = System.currentTimeMillis();
    }

    public static BlockPos getLastLookedAtBlock() {
        // Return null if too much time has passed (player moved away)
        if (System.currentTimeMillis() - lastLookTime > POSITION_TIMEOUT) {
            return null;
        }
        return lastLookedAtBlock;
    }

    public static void clear() {
        lastLookedAtBlock = null;
        lastLookTime = 0;
    }
}
