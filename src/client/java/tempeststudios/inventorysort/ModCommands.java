package tempeststudios.inventorysort;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

/**
 * Registers client-side command roots for the current combined mod.
 */
public final class ModCommands {

    private ModCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(
                ClientCommandManager.literal("inventorysort")
                        .then(InventoryCatalogueCommands.build())
                        .then(WorldProfileCommands.build())
        );
    }
}
