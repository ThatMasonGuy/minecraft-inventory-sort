# Inventory Search TODO

Current checkpoint: `2.6.3` + unreleased hard-coupling break

## Project Workflow

- After every major change, update `TODO.md`, update `CHANGELOG.md`, verify the
  relevant Gradle build/task, and commit before starting the next major step.
- If a session includes more than one major change, stop between major boundaries
  to update notes and commit each checkpoint separately.

## Confirmed Working

- Fixed block containers track current contents correctly.
- Ender chests track as player-scoped storage.
- Placed shulkers use same-content cleanup when moved, intentionally favoring undercounting over stale duplicate locations.
- Chest minecarts and hopper minecarts track correctly.
- Server/world tracking profiles work for multiplayer HC resets.
- World confirmation HUD blocks writes until confirmed, without blocking gameplay.
- Catalog `includeInventory` uses a stable per-session player-inventory fingerprint.

## Recently Fixed

-7. Public module structure (unreleased):
   - Added Gradle subprojects for `inventorysort-core`, `inventorysort`,
     `inventorysearch`, and `inventorycatalogue`.
   - Public feature jars now build separately as `inventory-sort`,
     `inventory-search`, and `inventory-catalogue`.
   - Each public feature jar nests the shared Core jar, so users do not need to
     manually download a separate Core mod.
   - Module source ownership is selected from the existing shared `src/client/java`
     tree, preserving one code location while producing separate artifacts.
   - Added module-specific `fabric.mod.json` and mixin configs for Core, Sort, and
     Search; Catalogue has no mixins.

-6. Module-readiness split prep (unreleased):
   - Split the old combined `HandledScreenMixin` so Sort owns sort/transfer buttons
     and Search owns its inventory search button through `SearchButtonMixin`.
   - Added separate client entrypoints for Core, Search, and Catalogue while keeping
     the existing Sort client entrypoint.
   - Moved feature/shared logging onto `InventorySortCore.LOGGER` so Search and
     Catalogue no longer depend on the Sort client class.
   - Updated the temporary combined `fabric.mod.json` entrypoint list so local
     single-project runs still initialize all features before the Gradle module
     split lands.

-5. Core event wiring / hard-coupling break (unreleased):
   - `ContainerTrackingMixin` now publishes Core inventory/container snapshot events
     instead of directly calling Search (`ItemLocationTracker`) and Catalogue
     (`CatalogSession`) internals.
   - Added `InventorySearchFeature` and `InventoryCatalogueFeature` as event
     subscribers around the existing Search and Catalogue behavior.
   - `ServerWorldProfileManager` now publishes a Core namespace-change event instead
     of directly reloading Search tracker state.
   - `InventorySortClient` delegates Search/Catalogue startup to feature bridge
     classes.
   - Split command implementation into `InventoryCatalogueCommands` and
     `WorldProfileCommands`, with `ModCommands` now acting only as the combined
     `/inventorysort` root aggregator.

-4. Core foundation / split prep (unreleased):
   - Added `tempeststudios.inventorysort.core.InventorySortCore` for shared mod id
     and logger ownership.
   - Added `tempeststudios.inventorysort.core.InventorySortEvents` with Core event
     contracts for namespace changes, container snapshots, and inventory snapshots.
   - Switched `InventorySortClient` to read its active mod id/logger from Core.
   - Behavior is intentionally unchanged; the next checkpoint wires Search and
     Catalogue through these events to remove direct coupling.

-3. Baseline cleanup / split prep (unreleased):
   - Added `AGENTS.md` so future work keeps `TODO.md`, `CHANGELOG.md`, verification,
     and commits synchronized after every major step.
   - Added `CHANGELOG.md` and recorded the current baseline cleanup.
   - Removed unused Fabric template scaffolding: `InventorySort.java`,
     `ExampleMixin.java`, and the unused client mixin config referencing the missing
     `ExampleClientMixin`.
   - Fixed `fabric.mod.json` icon path to match `assets/inventory-sort/icon.png`.

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

1. DONE (baseline cleanup): Dead template code removed:
   - Deleted unused `InventorySort.java` (`ModInitializer`).
   - Deleted unused `ExampleMixin.java`.
   - Deleted unused `inventory-sort.client.mixins.json` that referenced the missing
     `ExampleClientMixin`.
2. DONE (baseline cleanup): `MOD_ID` inconsistency removed with the unused
   `InventorySort` initializer. Active client id remains `inventorysort`, matching
   `fabric.mod.json`.
3. DONE (baseline cleanup): `fabric.mod.json` icon path now matches the checked-in
   `assets/inventory-sort/icon.png` asset.

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

### Implementation Roadmap

1. Baseline cleanup:
   - Remove dead template scaffolding and fix metadata drift while preserving the
     current single-mod behavior.
2. Extract Core:
   - Create the shared Core layer for identity/capture, world scoping, common UI,
     accessor/invoker mixins, and feature events.
   - Current status: Core constants/logger and event contracts are in place.
     Remaining extraction work is moving/wiring shared identity, world, UI, and
     mixin code as modules are split.
3. Break hard coupling:
   - Replace direct Search/Catalogue calls in shared code with Core events.
   - Replace direct namespace reload calls with a namespace-change event.
   - Split feature command registration and client startup responsibilities.
   - Current status: snapshot events, namespace-change event wiring, and feature
     startup bridges are in place. Catalogue/world command implementations are
     split behind the existing `/inventorysort` root.
4. Create public modules:
   - Build separate Sort, Search, and Catalogue modules from the shared Core.
   - Keep user installation simple by packaging Core so users do not install it
     manually.
   - Current status: DONE for the current Minecraft target. Gradle now builds
     Core + Sort/Search/Catalogue modules, with Core nested inside each public
     feature jar.
5. Add one-click local build:
   - Add root Gradle tasks such as `buildAllMods` and later `publishAllModrinth`.
   - Collect public release jars in a predictable output folder.
   - Current status: `buildAllMods` builds the three public feature jars plus the
     internal Core artifact. A dedicated release collection folder is still next.
6. Validate install combinations:
   - Test Sort only, Search only, Catalogue only, pairwise combinations, and all
     three together.
7. Add multi-version support:
   - Once the split is stable on the current target, compile the same modules
     against newer Minecraft/Fabric/Loom targets for the v26 migration.
8. Configure Modrinth publishing:
   - Give each public feature mod its own Modrinth project id and upload metadata.
   - Publish the correct jar, Minecraft version, loader, dependencies, and
     changelog for each target.

### Goes in Core (shared by 2+ features)

- Identity & capture: `ContainerIdentity`, `ContainerPositionCapture`,
  `MultiPlayerGameModeMixin`.
- World scoping: `TrackingNamespace`, `ServerWorldProfileManager`,
  `ServerWorldProfileHud`, `ServerWorldProfileScreen`.
- UI primitives: `InventorySortUIUtils`, the three button classes.
- Accessor/invoker mixins: `ScreenAccessor`, `AbstractContainerScreenAccessor`,
  `AbstractContainerScreenInvoker`, `AbstractContainerMenuInvoker`.

### Hard coupling points that must be broken first

Current status:
- DONE: `ContainerTrackingMixin` publishes Core snapshot events instead of calling
  Search/Catalogue internals directly.
- DONE: `ServerWorldProfileManager.setActiveProfile` publishes a namespace-change
  event instead of directly reloading Search state.
- DONE: Catalogue and world-profile command implementations are split behind the
  existing `/inventorysort` root aggregator.
- PARTIAL: `InventorySortClient` delegates Search/Catalogue startup to bridge
  classes. Full completion waits for separate modules, entrypoints, metadata, and
  mixin configs.

Original audit bullets, retained for context:

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
