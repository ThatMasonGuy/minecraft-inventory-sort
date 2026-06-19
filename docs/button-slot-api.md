# Button Slot API

`InventoryScreenButtonSlots` is the shared Core API for placing small buttons
beside `AbstractContainerScreen` screens without hard-coding vertical offsets.
It tracks reservations per live screen instance and recalculates coordinates
from the screen's current `leftPos`, `topPos`, `imageWidth`, and `imageHeight`,
so recipe-book shifts and alternate screen widths are handled in the same place.

## Quick Use

Import:

```java
import tempeststudios.inventorysort.api.InventoryScreenButtonSlots;
import tempeststudios.inventorysort.api.InventoryScreenButtonSlots.RightSlotGroup;
```

Reserve the same owner/slot id each time you update your button position. The
reservation is updated in place instead of creating duplicate slots.

```java
InventoryScreenButtonSlots.RightSlotAvailability availability =
        InventoryScreenButtonSlots.getRightSlotAvailability(
                screen,
                RightSlotGroup.PLAYER_INVENTORY
        );

if (availability.nextSlotFits()) {
    InventoryScreenButtonSlots.SlotPlacement placement =
            InventoryScreenButtonSlots.reserveRightSlot(
                    screen,
                    RightSlotGroup.PLAYER_INVENTORY,
                    "my_mod",
                    "my_button",
                    InventoryScreenButtonSlots.THIRD_PARTY_DEFAULT_PRIORITY
            );

    button.setX(placement.x());
    button.setY(placement.y());
}
```

If your button should not be shown for a screen state, release your reservation
and skip placement:

```java
InventoryScreenButtonSlots.releaseRightSlot(
        screen,
        RightSlotGroup.PLAYER_INVENTORY,
        "my_mod",
        "my_button"
);
```

If your mod owns several buttons, `releaseOwner(screen, "my_mod")` clears all
of your reservations for that screen.

## Slot Groups

- `PLAYER_INVENTORY`: the right-side column aligned with the player inventory
  area. On vanilla-sized screens this currently fits six default `12px` buttons.
- `CONTAINER`: the right-side column aligned with the container area. This group
  is available only on Inventory Mods free-form container screens: chests,
  barrels, shulker boxes, droppers, and dispensers. Hoppers, furnaces, brewing
  stands, and creative inventory screens are not treated as container slots.

Each group has its own independent slot indexes. Slot `0` is the top entry in
that group, slot `1` is below it, and so on.

## Capacity APIs

Use these before reserving if you need to decide whether to show, hide, or move
your button somewhere else:

- `getAvailableRightSlotCount(screen, group)`: physical slot count that fits in
  the current screen geometry for default `12px` buttons.
- `getAvailableRightSlotCount(screen, group, buttonSize)`: same calculation for
  a custom square button size.
- `getRemainingRightSlotCount(screen, group)`: available slots minus current
  reservations, clamped at zero.
- `canFitRightSlot(screen, group, slotIndex)`: whether that logical slot index
  fits inside the group's current vertical space.
- `canFitNextRightSlot(screen, group)`: whether appending after the current
  reservations fits.
- `getRightSlotAvailability(screen, group)`: one record with available,
  occupied, remaining, next-slot index, and next-slot-fit values.

`reserveRightSlot(...)` does not reject overflowing reservations. It returns a
`SlotPlacement` with `fitsInGroup()` so callers can decide whether to keep the
button visible, fall back to another UI, or deliberately allow overflow.

## Priority Rules

Lower priority numbers are placed first. Equal priorities keep the first
reservation order for that screen.

- `FIRST_PARTY_SORT_PRIORITY = 0`
- `FIRST_PARTY_SEARCH_PRIORITY = 100`
- `THIRD_PARTY_DEFAULT_PRIORITY = 1000`

Use `THIRD_PARTY_DEFAULT_PRIORITY` unless you intentionally need to appear
between first-party Inventory Mods buttons. Values `6` through `99` currently
place a button after InvSort's first-party buttons but before InvSearch on
normal inventory screens. That is allowed, but it is a stronger coupling than
appending after first-party reservations.

## First-Party Reservations

Inventory Mods reserves buttons during `AbstractContainerScreen.init` and
refreshes their positions during render or render-state extraction. The owner
and slot ids below are treated as the stable first-party contract for companion
mods.

### Normal Inventory Screens

These are screens where `InventoryScreenButtonSlots.isInventoryModsContainer`
returns `false`.

| Group | Priority | Owner | Slot id | Button |
| --- | ---: | --- | --- | --- |
| `PLAYER_INVENTORY` | `0` | `inventorysort` | `player_sort` | Sort inventory |
| `PLAYER_INVENTORY` | `100` | `inventorysearch` | `inventory_search` | Search inventory |

If InvSort is not installed, InvSearch is the only first-party reservation and
therefore occupies slot `0`. If InvSearch is not installed, it reserves nothing.

### Container Screens

These are screens where `InventoryScreenButtonSlots.isInventoryModsContainer`
returns `true`: chests, barrels, shulker boxes, droppers, and dispensers.

| Group | Priority | Owner | Slot id | Button |
| --- | ---: | --- | --- | --- |
| `PLAYER_INVENTORY` | `0` | `inventorysort` | `player_sort` | Sort inventory |
| `PLAYER_INVENTORY` | `1` | `inventorysort` | `player_matching_to_container` | Move matching items to container |
| `PLAYER_INVENTORY` | `2` | `inventorysort` | `player_all_to_container` | Move all inventory items to container |
| `CONTAINER` | `3` | `inventorysort` | `container_sort` | Sort container |
| `CONTAINER` | `4` | `inventorysort` | `container_matching_to_player` | Move matching items to inventory |
| `CONTAINER` | `5` | `inventorysort` | `container_all_to_player` | Move all container items to inventory |

InvSearch does not show its search button on these screens. It releases its
owner reservations during init and does not reserve a `PLAYER_INVENTORY` or
`CONTAINER` slot for container screens.

## Notes For Companion Mods

- Check capacity after first-party buttons have had a chance to reserve. If you
  reserve during the same screen `init` phase, reserve again while updating your
  render position; duplicate owner/slot ids update the existing reservation.
- Use stable owner and slot ids. Changing ids creates new logical reservations.
- Use `releaseRightSlot` or `releaseOwner` when a button is conditionally hidden
  so other mods can fill the freed slot.
- `onPreferredRightSide()` tells you whether the column is on the GUI's right
  edge. It becomes `false` when the API had to fall back to the left side or a
  clamped position because the screen is too narrow.
