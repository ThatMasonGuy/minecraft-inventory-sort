package tempeststudios.inventorysort.core;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import tempeststudios.inventorysort.ContainerIdentity;

import java.util.List;

public final class InventorySortEvents {
    public static final Event<NamespaceChanged> NAMESPACE_CHANGED = EventFactory.createArrayBacked(
            NamespaceChanged.class,
            listeners -> context -> {
                for (NamespaceChanged listener : listeners) {
                    listener.onNamespaceChanged(context);
                }
            }
    );

    public static final Event<ContainerSnapshot> CONTAINER_SNAPSHOT = EventFactory.createArrayBacked(
            ContainerSnapshot.class,
            listeners -> context -> {
                for (ContainerSnapshot listener : listeners) {
                    listener.onContainerSnapshot(context);
                }
            }
    );

    public static final Event<InventorySnapshot> INVENTORY_SNAPSHOT = EventFactory.createArrayBacked(
            InventorySnapshot.class,
            listeners -> context -> {
                for (InventorySnapshot listener : listeners) {
                    listener.onInventorySnapshot(context);
                }
            }
    );

    private InventorySortEvents() {
    }

    public interface NamespaceChanged {
        void onNamespaceChanged(NamespaceChangedContext context);
    }

    public interface ContainerSnapshot {
        void onContainerSnapshot(ContainerSnapshotContext context);
    }

    public interface InventorySnapshot {
        void onInventorySnapshot(InventorySnapshotContext context);
    }

    public record NamespaceChangedContext(
            Minecraft client,
            String serverKey,
            String profile,
            String namespace
    ) {
    }

    public record ContainerSnapshotContext(
            Minecraft client,
            String screenClassName,
            String containerType,
            ContainerIdentity identity,
            boolean portableShulker,
            String portableShulkerId,
            List<ItemStack> items
    ) {
        public ContainerSnapshotContext {
            items = copyStacks(items);
        }

        public boolean hasIdentity() {
            return identity != null;
        }
    }

    public record InventorySnapshotContext(
            Minecraft client,
            List<ItemStack> items
    ) {
        public InventorySnapshotContext {
            items = copyStacks(items);
        }
    }

    private static List<ItemStack> copyStacks(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(ItemStack::copy)
                .toList();
    }
}
