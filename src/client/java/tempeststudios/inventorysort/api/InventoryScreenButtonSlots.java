package tempeststudios.inventorysort.api;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.DispenserMenu;
import tempeststudios.inventorysort.mixin.AbstractContainerScreenAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

public final class InventoryScreenButtonSlots {
    public static final int DEFAULT_BUTTON_SIZE = 12;
    public static final int DEFAULT_BUTTON_GAP = 1;
    public static final int FIRST_PARTY_SORT_PRIORITY = 0;
    public static final int FIRST_PARTY_SEARCH_PRIORITY = 100;
    public static final int THIRD_PARTY_DEFAULT_PRIORITY = 1000;

    private static final int EDGE_OVERLAP = 3;
    private static final Map<AbstractContainerScreen<?>, ScreenReservations> SCREENS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private InventoryScreenButtonSlots() {
    }

    public static SlotPlacement reserveRightSlot(AbstractContainerScreen<?> screen,
                                                 RightSlotGroup group,
                                                 String ownerId,
                                                 String slotId,
                                                 int priority) {
        return reserveRightSlot(screen, group, ownerId, slotId, priority, DEFAULT_BUTTON_SIZE);
    }

    public static SlotPlacement reserveRightSlot(AbstractContainerScreen<?> screen,
                                                 RightSlotGroup group,
                                                 String ownerId,
                                                 String slotId,
                                                 int priority,
                                                 int buttonSize) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(group, "group");
        ReservationKey key = key(ownerId, slotId);
        if (buttonSize < 1) {
            throw new IllegalArgumentException("buttonSize must be positive");
        }

        synchronized (SCREENS) {
            ScreenReservations reservations = SCREENS.computeIfAbsent(screen, ignored -> new ScreenReservations());
            reservations.reserve(group, key, priority);
            int slotIndex = reservations.slotIndex(group, key);
            int slotCount = reservations.slotCount(group);
            Coordinates coordinates = coordinatesFor(screen, group, slotIndex, slotCount, buttonSize);
            return new SlotPlacement(group, key.ownerId(), key.slotId(), priority, slotIndex,
                    coordinates.x(), coordinates.y(), coordinates.onPreferredRightSide());
        }
    }

    public static void releaseRightSlot(AbstractContainerScreen<?> screen,
                                        RightSlotGroup group,
                                        String ownerId,
                                        String slotId) {
        if (screen == null || group == null) {
            return;
        }
        ReservationKey key = key(ownerId, slotId);

        synchronized (SCREENS) {
            ScreenReservations reservations = SCREENS.get(screen);
            if (reservations == null) {
                return;
            }
            reservations.release(group, key);
            if (reservations.isEmpty()) {
                SCREENS.remove(screen);
            }
        }
    }

    public static void releaseOwner(AbstractContainerScreen<?> screen, String ownerId) {
        if (screen == null || ownerId == null || ownerId.isBlank()) {
            return;
        }

        synchronized (SCREENS) {
            ScreenReservations reservations = SCREENS.get(screen);
            if (reservations == null) {
                return;
            }
            reservations.releaseOwner(ownerId);
            if (reservations.isEmpty()) {
                SCREENS.remove(screen);
            }
        }
    }

    public static List<SlotReservation> getOccupiedRightSlots(AbstractContainerScreen<?> screen,
                                                              RightSlotGroup group) {
        if (screen == null || group == null) {
            return List.of();
        }

        synchronized (SCREENS) {
            ScreenReservations reservations = SCREENS.get(screen);
            if (reservations == null) {
                return List.of();
            }
            return reservations.occupiedSlots(group);
        }
    }

    public static Set<Integer> getOccupiedRightSlotIndexes(AbstractContainerScreen<?> screen,
                                                           RightSlotGroup group) {
        LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
        for (SlotReservation reservation : getOccupiedRightSlots(screen, group)) {
            indexes.add(reservation.slotIndex());
        }
        return Collections.unmodifiableSet(indexes);
    }

    public static int getNextRightSlotIndex(AbstractContainerScreen<?> screen, RightSlotGroup group) {
        return getOccupiedRightSlots(screen, group).size();
    }

    public static SlotPlacement rightSlotPlacement(AbstractContainerScreen<?> screen,
                                                   RightSlotGroup group,
                                                   int slotIndex) {
        return rightSlotPlacement(screen, group, slotIndex, DEFAULT_BUTTON_SIZE);
    }

    public static SlotPlacement rightSlotPlacement(AbstractContainerScreen<?> screen,
                                                   RightSlotGroup group,
                                                   int slotIndex,
                                                   int buttonSize) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(group, "group");
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be zero or greater");
        }
        if (buttonSize < 1) {
            throw new IllegalArgumentException("buttonSize must be positive");
        }

        int slotCount = Math.max(slotIndex + 1, getOccupiedRightSlots(screen, group).size());
        Coordinates coordinates = coordinatesFor(screen, group, slotIndex, slotCount, buttonSize);
        return new SlotPlacement(group, "", "", 0, slotIndex,
                coordinates.x(), coordinates.y(), coordinates.onPreferredRightSide());
    }

    public static boolean isInventoryModsContainer(AbstractContainerScreen<?> screen) {
        if (screen == null || screen instanceof CreativeModeInventoryScreen) {
            return false;
        }
        int totalSlots = screen.getMenu().slots.size();
        return totalSlots > 46 || screen.getMenu() instanceof DispenserMenu;
    }

    public static int containerRowCount(AbstractContainerScreen<?> screen) {
        if (screen == null || !isInventoryModsContainer(screen)) {
            return 0;
        }
        if (screen.getMenu() instanceof DispenserMenu) {
            return 3;
        }
        int containerSlots = screen.getMenu().slots.size() - 36;
        return (int) Math.ceil(containerSlots / 9.0);
    }

    private static Coordinates coordinatesFor(AbstractContainerScreen<?> screen,
                                              RightSlotGroup group,
                                              int slotIndex,
                                              int slotCount,
                                              int buttonSize) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        XCoordinate x = rightColumnX(accessor.getLeftPos(), accessor.getImageWidth(), screen.width, buttonSize);
        int baseY = baseY(screen, accessor, group, slotCount, buttonSize);
        int y = baseY + slotIndex * (buttonSize + DEFAULT_BUTTON_GAP);
        return new Coordinates(x.x(), y, x.onPreferredRightSide());
    }

    private static XCoordinate rightColumnX(int leftPos, int imageWidth, int screenWidth, int buttonSize) {
        int maxX = Math.max(0, screenWidth - buttonSize);
        int preferredRightX = leftPos + imageWidth - EDGE_OVERLAP;
        if (preferredRightX + buttonSize <= screenWidth) {
            return new XCoordinate(Math.max(0, preferredRightX), true);
        }

        int leftFallbackX = leftPos - buttonSize + EDGE_OVERLAP;
        if (leftFallbackX >= 0) {
            return new XCoordinate(leftFallbackX, false);
        }

        int clampedX = Math.max(0, Math.min(preferredRightX, maxX));
        boolean onPreferredRightSide = clampedX == preferredRightX && clampedX + buttonSize <= screenWidth;
        return new XCoordinate(clampedX, onPreferredRightSide);
    }

    private static int baseY(AbstractContainerScreen<?> screen,
                             AbstractContainerScreenAccessor accessor,
                             RightSlotGroup group,
                             int slotCount,
                             int buttonSize) {
        if (group == RightSlotGroup.PLAYER_INVENTORY) {
            return accessor.getTopPos() + accessor.getImageHeight() - 83;
        }

        int rows = Math.max(1, containerRowCount(screen));
        int visibleSlots = Math.max(1, slotCount);
        int groupHeight = (visibleSlots - 1) * (buttonSize + DEFAULT_BUTTON_GAP) + buttonSize;
        int containerHeight = rows * 18;
        return accessor.getTopPos() + 17 + Math.max(0, (containerHeight - groupHeight) / 2);
    }

    private static ReservationKey key(String ownerId, String slotId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        if (slotId == null || slotId.isBlank()) {
            throw new IllegalArgumentException("slotId must not be blank");
        }
        return new ReservationKey(ownerId, slotId);
    }

    public enum RightSlotGroup {
        PLAYER_INVENTORY,
        CONTAINER
    }

    public record SlotReservation(
            RightSlotGroup group,
            String ownerId,
            String slotId,
            int priority,
            int slotIndex
    ) {
    }

    public record SlotPlacement(
            RightSlotGroup group,
            String ownerId,
            String slotId,
            int priority,
            int slotIndex,
            int x,
            int y,
            boolean onPreferredRightSide
    ) {
    }

    private record ReservationKey(String ownerId, String slotId) {
    }

    private record Reservation(ReservationKey key, int priority, long sequence) {
    }

    private record Coordinates(int x, int y, boolean onPreferredRightSide) {
    }

    private record XCoordinate(int x, boolean onPreferredRightSide) {
    }

    private static final class ScreenReservations {
        private final EnumMap<RightSlotGroup, LinkedHashMap<ReservationKey, Reservation>> reservations =
                new EnumMap<>(RightSlotGroup.class);
        private long nextSequence = 0;

        private void reserve(RightSlotGroup group, ReservationKey key, int priority) {
            LinkedHashMap<ReservationKey, Reservation> groupReservations = reservations.computeIfAbsent(
                    group,
                    ignored -> new LinkedHashMap<>()
            );
            Reservation previous = groupReservations.get(key);
            long sequence = previous == null ? nextSequence++ : previous.sequence();
            groupReservations.put(key, new Reservation(key, priority, sequence));
        }

        private void release(RightSlotGroup group, ReservationKey key) {
            LinkedHashMap<ReservationKey, Reservation> groupReservations = reservations.get(group);
            if (groupReservations == null) {
                return;
            }
            groupReservations.remove(key);
            if (groupReservations.isEmpty()) {
                reservations.remove(group);
            }
        }

        private void releaseOwner(String ownerId) {
            List<RightSlotGroup> emptyGroups = new ArrayList<>();
            for (Map.Entry<RightSlotGroup, LinkedHashMap<ReservationKey, Reservation>> entry : reservations.entrySet()) {
                entry.getValue().entrySet().removeIf(candidate -> ownerId.equals(candidate.getKey().ownerId()));
                if (entry.getValue().isEmpty()) {
                    emptyGroups.add(entry.getKey());
                }
            }
            for (RightSlotGroup group : emptyGroups) {
                reservations.remove(group);
            }
        }

        private int slotIndex(RightSlotGroup group, ReservationKey key) {
            List<Reservation> sorted = sortedReservations(group);
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).key().equals(key)) {
                    return i;
                }
            }
            return sorted.size();
        }

        private int slotCount(RightSlotGroup group) {
            LinkedHashMap<ReservationKey, Reservation> groupReservations = reservations.get(group);
            return groupReservations == null ? 0 : groupReservations.size();
        }

        private List<SlotReservation> occupiedSlots(RightSlotGroup group) {
            List<Reservation> sorted = sortedReservations(group);
            if (sorted.isEmpty()) {
                return List.of();
            }
            List<SlotReservation> occupied = new ArrayList<>(sorted.size());
            for (int i = 0; i < sorted.size(); i++) {
                Reservation reservation = sorted.get(i);
                occupied.add(new SlotReservation(
                        group,
                        reservation.key().ownerId(),
                        reservation.key().slotId(),
                        reservation.priority(),
                        i
                ));
            }
            return Collections.unmodifiableList(occupied);
        }

        private boolean isEmpty() {
            return reservations.isEmpty();
        }

        private List<Reservation> sortedReservations(RightSlotGroup group) {
            LinkedHashMap<ReservationKey, Reservation> groupReservations = reservations.get(group);
            if (groupReservations == null || groupReservations.isEmpty()) {
                return List.of();
            }
            List<Reservation> sorted = new ArrayList<>(groupReservations.values());
            sorted.sort((left, right) -> {
                int priority = Integer.compare(left.priority(), right.priority());
                if (priority != 0) {
                    return priority;
                }
                return Long.compare(left.sequence(), right.sequence());
            });
            return sorted;
        }
    }
}
