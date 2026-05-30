# Inventory Search TODO

Current checkpoint: `2.6.3`

## Confirmed Working

- Fixed block containers track current contents correctly.
- Ender chests track as player-scoped storage.
- Placed shulkers use same-content cleanup when moved, intentionally favoring undercounting over stale duplicate locations.
- Chest minecarts and hopper minecarts track correctly.
- Server/world tracking profiles work for multiplayer HC resets.
- World confirmation HUD blocks writes until confirmed, without blocking gameplay.
- Catalog `includeInventory` uses a stable per-session player-inventory fingerprint.

## Recently Fixed

-2. Tracker/search hardening (2.6.3):
   - Modded/custom dimensions now round-trip: `LocationEntry` stores the dimension as
     a generic id string and deserialization keeps it verbatim (no more overworld
     collapse). Legacy entries self-heal on next container visit.
   - Search results compute tracker data lazily: `buildRowForEntry` does cheap work
     only; tracked counts/locations are queried + formatted on demand and cached, so
     a 400-result query no longer hits the tracker for off-screen/collapsed rows.
   - Wheel/▲▼ scroll snaps to row boundaries (one notch = one row) regardless of
     whether the top row is expanded.

-1. Small grid containers (2.6.2):
   - Sort + transfer buttons now appear on droppers and dispensers via a
     `DispenserMenu` gate in `HandledScreenMixin`; dispenser counts as a 3-row grid.
   - Hoppers, furnaces, and brewing stands stay excluded (functional / not wanted).

0. Quick-win patch (2.6.1) — from the deep-dive audit:
   - Legible input text: world-selector and search boxes now use light text on the
     dark recessed field (was black-on-near-black, invisible).
   - Search columns: count column position scales from modal width and the item
     name truncates before it, so long names no longer overlap the count.
   - Esc returns to parent: `SearchModalScreen` and `ServerWorldProfileScreen`
     override `onClose()` to go back to the screen that opened them.
   - Shulker tracking now flushed to disk on container close instead of waiting for
     the next unrelated save / shutdown hook.

1. Placed shulker identical-content collision:
   - Restored same-content cleanup for placed shulkers.
   - This can undercount separate identical shulkers, but avoids permanent stale duplicates when a shulker is broken and placed elsewhere.

2. Catalog `includeInventory` double-counting:
   - Player inventory fingerprint no longer varies by player position.
   - Moving and reopening inventory during one catalog session should not count it again.

3. Search tracked count semantics:
   - Collapsed search rows now show total known tracked storage count when the item is not in the live inventory.

4. Routine log noise:
   - Normal container open/close tracking, save/update messages, and search result diagnostics moved from `info` to `debug`.

## P2 Accuracy And Data Hardening

1. Potion/component-aware tracking:
   - Search aliases let `water bottle` find `minecraft:potion`, but item identity is still base item only.
   - Catalog and tracking currently merge component variants such as potions, enchanted books, named items, and filled containers.
   - True variant tracking needs component-aware keys and display names.

2. Custom/modded dimensions:
   - DONE (2.6.3): dimension ids are now stored/restored generically in the tracker
     (`LocationEntry`) and were already generic in `CatalogStore`; no more overworld
     fallback for custom dimensions.

3. Corrupt JSON hardening:
   - Bad tracking/profile JSON should not crash or poison load.
   - Catch parse/runtime exceptions, skip bad entries, and consider writing a `.bak` before overwriting.

4. Catalog mode cleanup:
   - DONE: Rebuilt catalogue on the ContainerIdentity/namespace model (`CatalogStore` + `CatalogSession`).
   - Now per-world, persistent, identity-keyed (single vs double chest, per shulker/ender/minecart), and reopening refreshes instead of double-counting.
   - Added `catalog report`/`catalog clear` commands and a full plain-text report file under `inventorysort/catalog/`.
   - Remaining: catalogue totals are still item-id based, so they merge component variants (potions, enchanted books, named items) — needs the shared component-aware key work.

5. Old data cleanup/migration:
   - Earlier dev builds may have stale locations such as crafting tables or old fake inventory coordinates.
   - Add a cleanup/migration command if old data becomes annoying.

## UX Polish

1. Portable shulkers:
   - Shulkers opened from inventory are still weak/history-ish because identity is based on contents.
   - Decide whether to skip them or build a deliberate portable-container model.

2. Search default view:
   - Empty search only shows in-session recent inventory items.
   - Consider recent tracked storage hits or a more useful default summary after restart.

3. Component-specific search:
   - Search cannot query potion type, enchantment, custom name, shulker contents, or similar stack details yet.

4. Server world profile UI:
   - Profile selector only shows the first few profiles.
   - Add scrolling/search if HC world count keeps climbing.

5. HUD/profile display:
   - Make HUD prompt position configurable if it clashes with other HUD mods.
   - Consider shortening profile names in HUD if they are long.

## Performance And Logging

1. Inventory sampler save churn:
   - Inventory sampler saves whenever inventory signature changes.
   - Watch long sessions and busy servers for save churn.

2. Log/chat noise:
   - Watch for remaining chat/log spam during manual testing.
   - Keep important warnings visible, move routine diagnostics to `debug`.

## Deep-Dive Audit (2026-05-30)

Full read-through of all three segments (Sort / Search / Catalogue) plus infra,
mixins, and config. Severity: 🔴 high, 🟠 medium, 🟡 low, 🟢 nit. Nothing here is
implemented yet — this is the backlog backup.

### Confirmed UI bugs

1. 🔴 (DONE 2.6.1) Black text on near-black input background (both text inputs):
   - `ServerWorldProfileScreen` world-name box sets `setTextColor(0xFF000000)`
     (`ServerWorldProfileScreen.java:44`) over `drawRecessedPanel` fill
     `COLOR_RECESSED_BACKGROUND = 0xFF121212` → typed text is invisible. This is
     the reported world-selector bug.
   - Same root cause in the Search modal search box (`SearchModalScreen.java:120`
     + recessed panel at `:222`). Fix both: use a light text color (or lighten
     the recessed fill) so input is legible. Consider a shared helper so all mod
     inputs stay consistent.

### Other UI issues

1. 🔴 (DONE 2.6.1) Search results: name column overlaps the count column. Count
   column now scales from content width and the name truncates before it.
2. 🟠 (DONE 2.6.1) Count column position no longer hardcoded — derived from
   `listContentW` with reserved room on the right.
3. 🟠 (DONE 2.6.1) Esc abandons the parent screen — `SearchModalScreen` and
   `ServerWorldProfileScreen` now override `onClose()` to route back to parent.
4. 🟡 (DONE 2.6.3) Wheel scroll always moves `ROW_H+4` even past expanded rows —
   scroll now snaps to row boundaries so one notch advances exactly one row.
5. 🟡 Bottom-most visible row often can't be expanded: `updateLayout` `withinClip`
   check (`SearchModalScreen.java:367`) hides a ▶ button if partially clipped even
   though the row text is on screen.
6. 🟡 Sort buttons vs recipe book: `calcButtonX` (`HandledScreenMixin.java:44`)
   anchors right then falls back to left edge; when the recipe book shifts `leftPos`
   on narrow screens the buttons can float over the GUI.

### Dead scaffolding / consistency

1. 🔴 Dead template code (safe to delete):
   - `InventorySort.java` (`ModInitializer`) is never invoked — no `main`
     entrypoint in `fabric.mod.json`.
   - `ExampleMixin.java` is not listed in `inventorysort.mixins.json`.
   - `inventory-sort.client.mixins.json` points at a non-existent
     `ExampleClientMixin` in package `…mixin.client` and is not referenced by
     `fabric.mod.json`; wiring it in would crash at load.
2. 🟡 `MOD_ID` inconsistent: `InventorySort.MOD_ID = "inventory-sort"` vs
   `InventorySortClient.MOD_ID = "inventorysort"` vs json id `inventorysort`.

### Sort segment

1. 🟠 (DONE 2.6.2) Small containers get no sort/transfer buttons — gate now also
   accepts `DispenserMenu` (dropper/dispenser). Hoppers, furnaces, and brewing
   stands remain excluded on purpose. `InventorySorter`'s existing name-based slot
   detection already handled these once buttons were wired.
2. 🟠 Click-storm desync risk: sorting/transfers drive hundreds of synchronous
   `slotClicked` packets (compact→restack→apply→restack→compact; shift-all quick-
   moves every slot). Risk of ghost items / rollback / kicks on rate-limited
   servers. Consider throttling/batching or a known-limitation note.
3. 🟡 `"Crafting"` name match (`InventorySorter.java:257`, `:834`) would include the
   crafting result slot if ever reached (`containerSize = total-36`). Currently
   unreachable but a latent trap.
4. 🟡 Dead/misleading code:
   - `fillPlayerStacksFromContainer` (`:199`) never called.
   - `findFirstEmptyNonBundle` (`:722`) is identical to `findFirstEmpty` — does not
     actually skip bundles despite the name.
   - Bundle-skip branches in `ensureCursorEmpty` (`:749`, `:759`) are unreachable
     (sit after an `isEmpty()` early-return).
5. 🟢 "Sort container" also tops up the hotbar from main inventory first (`:34-40`)
   — a container action silently reshuffles player inventory.

### Search segment

1. 🟠 (DONE 2.6.1) Shulker tracking never explicitly saved — `ContainerTrackingMixin`
   now flushes `tracker.save()` after tracking a portable shulker's contents.
2. 🔴 (DONE 2.6.3) Modded dimensions corrupt on reload — `LocationEntry` now keeps a
   generic dimension id string and deserialization preserves it verbatim instead of
   round-tripping through `Level.OVERWORLD`.
3. 🟠 (DONE 2.6.3) Per-keystroke eager work — tracker queries and location-string
   formatting moved to lazy, cached accessors on `ResultRow`; `buildRowForEntry` now
   does only cheap snapshot work.
4. 🟡 Dead branch: `formatLocation` INVENTORY case checks `loc.getPos() != null`
   (`SearchModalScreen.java:515`) but inventory entries always have `pos == null`
   and are filtered out before formatting anyway.
5. 🟡 World-confirmation uses global GLFW ENTER/BACKSPACE polling
   (`ServerWorldProfileManager.java:91`); ignores keybind remaps and hijacks those
   keys while active (does correctly bail when a screen is open). A real keybinding
   would be cleaner.

### Catalogue segment (new code, self-review)

1. 🟡 Empty containers are recorded as zero-item snapshots, inflating "locations
   catalogued" without adding items. Conscious decision needed.
2. 🟡 Inherits identical-shulker collision: portable shulkers keyed by a 5-slot
   content hash (`ContainerTrackingMixin.generateContainerHash`), so two identically
   filled shulkers collide and count once.
3. 🟢 No atomic/`.bak` write on `CatalogStore.save()`; a crash mid-write loses that
   namespace's catalogue (loader catches and starts fresh — no crash).
4. 🟢 No in-game catalogue GUI yet (command-only). Natural future feature reusing
   the search modal styling.

### Performance

1. 🟠 Inventory sampler disk churn: `InventoryHistorySampler.sample` runs every tick
   and any inventory-total change triggers a full-file `save()`
   (`ItemLocationTracker.java:141`). Mining/combat → many whole-JSON rewrites/sec.
   Debounce / dirty-flag with periodic flush. (Overlaps "Performance And Logging" #1.)

## Mod Split Plan: InvSort / InvSearch / InvCatalogue

A clean flat 3-way split is not possible without extracting a shared **Core**.
Recommended shape: **Core + Sort + Search + Catalogue** (Sort/Search/Catalogue all
depend on Core; Catalogue and Search both need Core's identity/namespace/event layer).

### Goes in Core (shared by 2+ features)

- Identity & capture: `ContainerIdentity`, `ContainerPositionCapture`,
  `MultiPlayerGameModeMixin`.
- World scoping: `TrackingNamespace`, `ServerWorldProfileManager`,
  `ServerWorldProfileHud`, `ServerWorldProfileScreen`.
- UI primitives: `InventorySortUIUtils`, the three button classes.
- Accessor/invoker mixins: `ScreenAccessor`, `AbstractContainerScreenAccessor`,
  `AbstractContainerScreenInvoker`, `AbstractContainerMenuInvoker`.

### Hard coupling points that must be broken first

1. `ContainerTrackingMixin` calls BOTH `ItemLocationTracker` (Search) and
   `CatalogSession` (Catalogue) directly. Core should own this mixin and fire events
   (`onContainerClosed(identity, items)`, `onInventorySnapshot(items)`); Search and
   Catalogue subscribe. Today Catalogue cannot exist without Search's mixin.
2. `ServerWorldProfileManager.setActiveProfile` reaches into Search
   (`ItemLocationTracker.reloadForCurrentNamespace`, `InventoryHistorySampler.reset`).
   Replace with a "namespace changed" event each store subscribes to (incl.
   `CatalogStore`).
3. `ModCommands` mixes Catalogue commands and World-profile commands under one
   `/inventorysort` root. Three mods can't share a root literal cleanly — give each
   its own root or have Core own the root + a registration hook.
4. `InventorySortClient` is one entrypoint doing everything (tracker init, commands,
   tick sampler + confirmation, HUD, shutdown save). Each mod needs its own
   `ClientModInitializer`, `fabric.mod.json`, mixin json + package, partitioned
   registrations.

### Notes

- Sort is the most independent (only Core invoker/accessor mixins + UI).
- The event-bus refactor (points 1–2) is the keystone; without it Search and
  Catalogue stay welded together through `ContainerTrackingMixin`.
- Repackage `tempeststudios.inventorysort.*` into `…core` / `…invsort` /
  `…invsearch` / `…invcatalogue`; split the single mixin json accordingly.

## Release Process

1. Run `./gradlew.bat compileJava compileClientJava` after each patch chunk.
2. Run full `./gradlew.bat build` before release.
3. Push/verify release build after each patch chunk.
