# Inventory Search TODO

Current checkpoint: `2.6.3` + `1.21.10` compile/build candidate profile

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
- Split release jars launch successfully in a normal launcher when installed as
  Sort-only, Search-only, Catalogue-only, Sort+Search, and all three together.
  Search+Catalogue was not separately tested, but is expected to work after the
  standalone and all-three validation passed.

## Current Command Roots

- Inventory Catalogue owns `/inventorycatalogue start|stop|status|report|clear`.
- Inventory Catalogue also exposes shared world-profile commands as
  `/inventorycatalogue world list|use|default|current`.
- Inventory Search exposes shared world-profile commands as
  `/inventorysearch world list|use|default|current`.
- Core no longer registers a public `/inventorysort` command root.

## Recently Fixed

-16. `1.21.10` compatibility candidate (unreleased):
   - Added `gradle/version-profiles/1.21.10.properties` as a candidate
     compatibility-group profile.
   - Added `src/compat/1.21.10` and `src/compat/1.21.11` source overlays for
     Minecraft API differences that cannot compile from one shared source file.
   - Moved custom button render hooks into version-specific wrapper classes:
     `1.21.10` uses `renderWidget`, while `1.21.11` uses `renderContents`.
   - Added `MinecraftApiCompat.dimensionId(...)` adapters for the
     `ResourceKey.location()` / `ResourceKey.identifier()` API rename.
   - Removed direct Chest/Hopper Minecart class imports from shared identity code
     and now recognizes those containers by stable entity type id.
   - Verified sequential profile builds:
     `.\gradlew.bat clean build` for `1.21.11`, and
     `.\gradlew.bat clean buildAllMods "-Pminecraft_version_profile=1.21.10"`.
   - Verified the `1.21.10` release jars collect under `build/release/1.21.10/`
     and declare `minecraft ~1.21.10`, `java >=21`, and
     `fabricloader >=0.18.4`.
   - Important: `1.21.10` is still not a supported Modrinth listing until the
     release jars pass normal launcher smoke testing.
   - Next Step 7 slice: launcher smoke-test the `1.21.10` jars, then probe
     whether the same adapter shape can cover `1.21.9` or needs another group.

-15. Compatibility-group build profile foundation (unreleased):
   - Extended Gradle profiles with `profile_id`, `minecraft_dependency`,
     `modrinth_game_versions`, and `compat_group`.
   - Generated Fabric metadata now uses `minecraft_dependency` instead of always
     deriving `~<minecraft_version>`.
   - Release jars now collect under `build/release/<profile_id>/`.
   - `printVersionProfile` reports compatibility-group metadata and
     `verifyReleaseJars` checks generated Minecraft/Java dependency metadata.
   - Added `src/compat/<compat_group>/` wiring and documentation for
     version-specific API adapters.
   - Verified `.\gradlew.bat printVersionProfile`, `.\gradlew.bat clean build`,
     and `.\gradlew.bat buildAllVersions` on the current `1.21.11` profile.
   - First non-`1.21.11` compatibility-group profile is now tracked in item 16.

-14. Compatibility-group version strategy (unreleased):
   - Documented that version profiles should become release compatibility groups,
     not necessarily one profile per Minecraft patch version.
   - A profile should compile one jar against an anchor Minecraft version, select
     the appropriate compat source group, declare a Fabric Minecraft dependency
     range, and list only the exact Modrinth game versions that jar has passed
     smoke testing on.
   - Planned release outputs should move toward profile/range folders such as
     `build/release/1.21.6-1.21.11/` instead of assuming the output folder is
     always the compile anchor version.
   - Inserted CI validation as roadmap step 8 and moved Modrinth publishing
     automation to step 9.

-13. Compatibility matrix research (unreleased):
   - Added `COMPATIBILITY.md` with the current Minecraft compatibility probe
     matrix and source links.
   - Confirmed the current split `2.6.3` release jars should be listed on
     Modrinth for Minecraft `1.21.11` only.
   - Compile probes against every `1.21.x`, `1.20.x`, and `1.19.x` release show
     the current source only compiles as-is on `1.21.11`.
   - Next compatibility work should produce dedicated version-profile builds and
     launcher smoke tests before marking additional Modrinth game versions.

-12. Multi-version build profile foundation (unreleased):
   - Added Gradle Minecraft version profiles under `gradle/version-profiles/`.
   - Default and supported release builds remain on the tested `1.21.11` profile.
   - Added candidate `26.1.2` and `26.2-pre-3` profiles for migration work; both
     require Java 25 before source/API compile validation.
   - Upgraded the Gradle wrapper to 9.4.0 for Loom 1.16 candidate profile support.
   - Release jars now collect under `build/release/<profile_id>/`.

-11. License alignment (unreleased):
   - Replaced the old repo license text with LGPL-3.0-only and added the GPLv3
     text referenced by LGPLv3.
   - Updated README and all Fabric metadata to report `LGPL-3.0-only`, matching
     the Modrinth project license selection.
   - Updated jar packaging so public artifacts include both license documents.

-10. Split command root tidy (unreleased):
   - Removed the old combined `ModCommands` aggregator and stopped Core from
     registering `/inventorysort world`.
   - Moved Catalogue commands from `/inventorysort catalog ...` to
     `/inventorycatalogue ...`.
   - Registered shared world-profile commands under feature roots:
     `/inventorycatalogue world ...` and `/inventorysearch world ...`.
   - Updated README command examples and in-game Catalogue prompt/error text to
     use the new command roots.

-9. Release jar runtime crash fix (unreleased):
   - Public Sort/Search/Catalogue jars now embed Core's remapped release jar
     instead of the development-namespaced `-dev` jar.
   - Fixed normal launcher crashes at Core/Search client entrypoint time when
     installing the split public jars.
   - Added `verifyReleaseJars` to `clean build`/`buildAllMods` so future release
     builds fail if a public jar embeds the development Core jar again.
   - Manual launcher validation now passes for Sort-only, Search-only,
     Catalogue-only, Sort+Search, and all-three installs.

-8. One-click release jar collection (unreleased):
   - Added `collectReleaseJars`, which copies only the three public feature jars
     into `build/release/<profile_id>/`.
   - `buildAllMods` now builds Core + Sort/Search/Catalogue and produces a
     publish-ready `build/release/<profile_id>/` folder in one command.
   - CI artifact upload now collects `build/release/` recursively instead of
     module-local libs.

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
     `WorldProfileCommands`.

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
     split and now register through feature-specific roots.
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
   - Current status: DONE for local release builds. `buildAllMods` now leaves the
     three public, publish-ready jars in `build/release/<profile_id>/`.
6. Validate install combinations:
   - Test Sort only, Search only, Catalogue only, pairwise combinations, and all
     three together.
   - Current status: DONE for the `1.21.11` split release jars.
7. Add multi-version support:
   - Once the split is stable on the current target, compile the same modules
     against compatibility-group profiles.
   - Profiles should build one jar for a tested Minecraft version range, not
     automatically one jar per patch version.
   - Planned profile fields:
     - `profile_id`: release/profile folder id such as `1.21.6-1.21.11`.
     - `minecraft_version`: compile anchor used by Loom/mappings.
     - `minecraft_dependency`: Fabric Loader dependency range for metadata.
     - `modrinth_game_versions`: exact game versions to publish after smoke tests.
     - `compat_group`: source overlay group for version-specific APIs.
   - Current status: IN PROGRESS. Gradle now supports compatibility-group profile
     metadata, profile-id release folders, generated Fabric dependency ranges,
     profile metadata verification, and `src/compat/<compat_group>/` source
     overlays. The first non-`1.21.11` candidate profile, `1.21.10`, now
     compiles and builds release jars after small compat adapters, but it is not
     supported/publishable until launcher smoke testing passes. Older `1.21.x`,
     `1.20.x`, and `1.19.x` releases still need source/API porting before
     Modrinth listings. Next blocker for the v26 lane is still
     installing/running a Java 25 toolchain, then compiling a 26.x profile and
     fixing source/API breaks.
8. Add CI validation before publishing:
   - Add automated build verification for every supported compatibility-group
     profile.
   - Run unit tests where possible and keep `verifyReleaseJars` in the CI path.
   - Add launcher smoke tests for every Minecraft version listed by a profile's
     `modrinth_game_versions`; only those passing versions may be published.
   - CI should produce build artifacts grouped by profile/range so manual testing
     and Modrinth upload metadata stay aligned.
   - Current status: NOT STARTED. This should be implemented before automated
     Modrinth upload so publishing cannot get ahead of validation.
9. Configure Modrinth publishing:
   - Give each public feature mod its own Modrinth project id and upload metadata.
   - Publish the correct jar, Minecraft version, loader, dependencies, and
     changelog for each target.
   - Current status: NOT STARTED. This moved after CI validation so Modrinth
     automation only uploads jars and game-version lists that have passed tests.

### Goes in Core (shared by 2+ features)

- Identity & capture: `ContainerIdentity`, `ContainerPositionCapture`,
  `MultiPlayerGameModeMixin`.
- World scoping: `TrackingNamespace`, `ServerWorldProfileManager`,
  `ServerWorldProfileHud`, `ServerWorldProfileScreen`.
- UI primitives: `InventorySortUIUtils`, shared button renderers, and the
  compat-selected concrete button classes.
- Accessor/invoker mixins: `ScreenAccessor`, `AbstractContainerScreenAccessor`,
  `AbstractContainerScreenInvoker`, `AbstractContainerMenuInvoker`.

### Hard coupling points that must be broken first

Current status:
- DONE: `ContainerTrackingMixin` publishes Core snapshot events instead of calling
  Search/Catalogue internals directly.
- DONE: `ServerWorldProfileManager.setActiveProfile` publishes a namespace-change
  event instead of directly reloading Search state.
- DONE: Catalogue and world-profile command implementations are split and
  registered through feature-specific roots.
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
3. DONE: Catalogue and World-profile commands no longer share a combined
   `/inventorysort` root. Catalogue owns `/inventorycatalogue ...`, and shared
   world-profile commands are registered under feature roots.
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
