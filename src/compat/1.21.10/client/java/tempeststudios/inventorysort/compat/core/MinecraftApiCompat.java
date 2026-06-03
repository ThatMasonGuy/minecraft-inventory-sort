package tempeststudios.inventorysort.compat.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class MinecraftApiCompat {
    private MinecraftApiCompat() {
    }

    public static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }
}
