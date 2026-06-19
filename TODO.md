# Inventory Mods TODO

Current local checkpoint: `3.2.3` 26.x profile consolidation is complete after
the coordinated right-side button placement work. Shared Core now exposes
`tempeststudios.inventorysort.api.InventoryScreenButtonSlots`, which lets mods
reserve priority-ordered `PLAYER_INVENTORY` and `CONTAINER` right-side slots,
read occupied slots, and recalculate placement as vanilla or recipe-book screen
positions move. InvSort now reserves its Sort/transfer slots through that API in
the shared 1.x mixin and the shared 26.x overlay. InvSearch reserves the search
button through the same API, so standalone InvSearch occupies the first
inventory button slot when InvSort is not installed, while combined installs
keep InvSort first and Search underneath. The public release jar size sentinel
is raised from 225,000 to 245,000 bytes for the intentional embedded Core API
footprint while the junk-file and embedded-Core checks stay active. Follow-up
API capacity helpers now report available slot count, remaining slot count,
requested-slot fit, next-slot fit, and placement `fitsInGroup()` status, with
`docs/button-slot-api.md` documenting usage plus the stable InvSort/InvSearch
owner ids, slot ids, priorities, and screen conditions. Verification: PASS
`git diff --check`; PASS
`.\gradlew.bat buildAllMods --no-daemon --console=plain`; PASS
`.\gradlew.bat buildAllVersions --no-daemon --console=plain`. Follow-up docs
hygiene added `docs/README.md`, linked the developer docs from `AGENTS.md` and
`README.md`, and moved the TODO title from the old Search-only name to the
current Inventory Mods suite name. Docs-only verification: PASS
`git diff --check`. The duplicate `26.1.2`/`26.2-pre-3` source overlays were
collapsed into shared `src/compat/26.x`, `26.x.properties` is now the single
supported 26.x release profile (`profile_id=26.1-26.2-pre-3`), and exact
`26.1`, `26.1.1`, `26.1.2`, and `26.2-pre-3` profiles remain runtime-only
smoke targets. Verification: PASS
`.\gradlew.bat printVersionProfile "-Pminecraft_version_profile=26.x" --no-daemon --console=plain`;
PASS
`.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.x" --no-daemon --console=plain`;
PASS
`.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=26.1-26.2-pre-3" --no-daemon --console=plain`.
Release publish run `27806878154` was canceled before upload after a hosted
`1.21.5` all-public smoke launch stopped producing ticks; the same focused
smoke case passed locally. `InventorySortSmokeTest` now has a smoke-only
watchdog that logs `INVENTORYSORT_SMOKE_TEST_TIMEOUT` and exits the JVM if an
armed smoke launch stalls before `INVENTORYSORT_SMOKE_TEST_PASS`. Watchdog
verification: PASS `.\gradlew.bat buildAllVersions --no-daemon --console=plain`;
PASS `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=1.21-1.21.5" "-Pinventorysort_smoke_game_versions=1.21.5" "-Pinventorysort_smoke_install_sets=all-public" --no-daemon --console=plain`.
Follow-up publish run `27808903220` reached the same hosted smoke stall before
upload capture and was also canceled before any Modrinth upload step. The smoke
gate now has an outer Gradle `Exec` timeout
(`inventorysort_smoke_exec_timeout_seconds`, default 300 seconds), the Linux
publish and compatibility workflows install Flite for Minecraft narrator native
libraries, and `compatibility validation` can run targeted hosted
`smokeTestSelectedClients` checks with profile, game-version, and install-set
filters before retrying a full publish. Verification: PASS `git diff --check`;
PASS `.\gradlew.bat smokeTestSelectedClients
"-Pinventorysort_smoke_profiles=1.21-1.21.5"
"-Pinventorysort_smoke_game_versions=1.21.5"
"-Pinventorysort_smoke_install_sets=all-public" --no-daemon --console=plain`;
PASS `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
Hosted validation: PASS GitHub Actions `compatibility validation` run
`27810831395` on commit `a94f536535c140460d002eb1d4f211eeb5b11b4d` for
`smokeTestSelectedClients`, profile `1.21-1.21.5`, game version `1.21.5`,
install set `all-public`, Java 21.

Previous checkpoint: `3.2.2` is published. InvSort now stores player inventory
rules, world-container defaults, screen overrides, and exact-container overrides
under the active `TrackingNamespace.current(...)` entry in
`InvSort/sort_rules.json`. Existing old-shape global rule files migrate once
into the new `worldRules` schema: player/container/screen defaults seed the
current world, and namespace-prefixed exact-container overrides move into their
matching world buckets with the namespace stripped from the container key. The
rules screen labels now say `World player inventory` and `World Containers`
across the shared 1.x UI and then-active 26.x overlays. Local
`git diff --check` and
default-profile `buildAllMods` passed before release; the guarded GitHub Actions
Modrinth publish completed successfully for `3.2.2`, uploaded all 24
compatibility-group versions, and the annotated `v3.2.2` GitHub Release now
points users to the canonical Modrinth downloads.

Maintenance checkpoint: duplicate 1.x `InventorySortDrawContexts`,
`ClientCommandCompat`, and `HudCompat` copies were moved out of every 1.x
compatibility group and into shared client source, while the 26.x profiles now
exclude those shared 1.x wrappers and keep their `GuiGraphicsExtractor`,
`ClientCommands`, and HUD registry overlays. The stale generated
`build/release` folder was deleted and then rebuilt from scratch. Verification:
PASS `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.

Previous `3.2.1` checkpoint: recipe-book button offset regression fixes for
InvSort and InvSearch were published, including the Minecraft 26.x render-state
path. Follow-up `3.2.1` work moved persistent Inventory Mods data to Tempest
Studios app-data roots with instance-scoped single-player namespaces and a
once-per-source migration registry, then hardened multiplayer server namespaces
for account scope. The guarded GitHub Actions Modrinth publish completed
successfully for `3.2.1`, uploaded all 24 compatibility-group versions, and the
annotated `v3.2.1` GitHub Release now points users to the canonical Modrinth
downloads.

Previous `3.2.0` checkpoint: bug-fix hardening is implemented locally for
InvSort, InvSearch, InvCatalogue, and shared Core. InvCatalogue now also has a
local report-history browser for saved catalogue reports, and the full
supported profile build passes. A user-reported Minecraft `1.21.11` launch
crash in the shared Core keybinding shim is fixed locally and covered by a
focused all-public smoke launch. The report browser command now queues the GUI
open so chat closing does not immediately hide it, and the report browser now
uses a darker modern-Minecraft-style layout instead of the old InvSearch-like
grey panel treatment. Follow-up sizing fixes keep the selected-item sidebar
clipped and scrollable, prevent top summary text from running behind the
sidebar, and keep scrolled item tiles inside the grid frame. Item-grid badges
now abbreviate large counts with `K`/`M`/`B`, while the sidebar keeps showing
the exact selected-item count. InvSort now has a revised right-click rules
screen for custom category/item ordering, protected slots, item-specific slots,
and global plus per-container/per-screen rule scopes. The rules screen now fits
inside scaled Minecraft viewports, shows one rule list at a time, and supports
Ctrl-click plus Shift-click slot multi-selection. All three feature screens have
since been unified under a shared modern dark `InvUi` theme with a per-mod
accent: the InvSort rules screen is now tab-based (Slots/Order) with a scope
selector and a selection panel, the InvSearch modal uses card rows with
held-vs-tracked counts plus a restyled tracked-world picker, and the
InvCatalogue browser/detail views are restyled to match. The UI redesign builds
clean on every supported 1.x and 26.x profile. The 26.x screen overlays have
been synced to the shared `InvUi` screens, including the Sort rules screen,
Search modal, tracked-world picker, and Catalogue browser/detail views; the
`26.2-pre-3` all-public install also passes a focused launch smoke test.
InvSearch and the InvCatalogue report-browser filter now accept `:category`
queries such as `:wood`, `:stone`, and `:tools`, backed by the shared Core copy
of the InvSort category vocabulary and aliases. A final pre-CI bug pass tightened
that shared classifier so redstone items and dusts no longer get swallowed by
the broad stone category, delegated the stale InvSort legacy classifier to the
shared helper, and constrained the tracked-world picker mouse wheel to its list
well across shared and 26.x screens. A final packaging sweep found the generated
release jars clean of project docs, source files, source images, and extra public
assets; `verifyReleaseJars` now also fails if those non-runtime files appear in
either a public feature jar or its embedded Core jar. `mod_version` is now
bumped to `3.2.0` for the release jars and the guarded GitHub validation/publish
workflow. Root `gallery/` assets are now managed as Modrinth project-page
gallery sources, with banner and description-image selector folders documented
for each public mod. The live Modrinth project pages were synced from the repo
source copy after uploading 9 InvSort, 5 InvSearch, and 7 InvCatalogue gallery
images. The guarded GitHub Actions Modrinth publish workflow completed
successfully for `3.2.0`, uploaded all 24 compatibility-group versions, and the
annotated `v3.2.0` GitHub Release now points users to the canonical Modrinth
downloads. A post-release metadata-only correction replaced the InvSort
`Rules: Inventory Order` gallery/description screenshot on Modrinth after the
original image included an unwanted nested screenshot icon.

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
- Automated client smoke validation launches the packaged release jars as
  Sort-only, Search-only, Catalogue-only, and all-three installs on supported
  runtimes from Minecraft `1.20` through `1.21.11`, plus `26.1`, `26.1.1`,
  `26.1.2`, and `26.2-pre-3`.

## Current Command Roots

- Inventory Catalogue owns
  `/inventorycatalogue start|stop|status|report|reports|clear`.
- Inventory Catalogue also exposes shared world-profile commands as
  `/inventorycatalogue world list|use|default|current`.
- Inventory Search exposes shared world-profile commands as
  `/inventorysearch world list|use|default|current`.
- Core no longer registers a public `/inventorysort` command root.

## Bug Report Intake (2026-06-12)

### `3.2.2` Publish Evidence

Status: published.

- Release source commit:
  `1729c0135d6f15d7f10b6da9ed7b9990bc368ce4`.
- Guarded Modrinth publish workflow:
  `https://github.com/ThatMasonGuy/minecraft-inventory-sort/actions/runs/27502873676`.
- Publish artifact:
  `https://github.com/ThatMasonGuy/minecraft-inventory-sort/actions/runs/27502873676/artifacts/7622693093`.
- Annotated Git tag: `v3.2.2`, resolving locally to the release source commit.
- GitHub Release:
  `https://github.com/ThatMasonGuy/minecraft-inventory-sort/releases/tag/v3.2.2`.
- GitHub Release assets: none; Modrinth remains the canonical download surface.
- Workflow upload result: `BUILD SUCCESSFUL in 54m 17s`; upload plan plus
  release jars were captured in artifact `7622693093`.
- Workflow log recorded 92 `INVENTORYSORT_SMOKE_TEST_PASS` markers and 24
  successful Modrinth uploads. Public Modrinth API readback confirmed all 8
  InvSort and all 8 InvSearch `3.2.2` versions as `listed`; InvCatalogue version
  URLs are recorded from the successful upload log.

Published Modrinth version ids:

- InvSort: `62ZDg75r` (`1.20-1.20.4`), `KoNqsI1X` (`1.20.5-1.20.6`),
  `dMBExVqg` (`1.21-1.21.5`), `YYerUOjW` (`1.21.6-1.21.8`),
  `wTywmkB3` (`1.21.9-1.21.10`), `eq0emMqR` (`1.21.11`), `K5NgCTWD`
  (`26.1-26.1.2`), and `7OsOWTsH` (`26.2-pre-3`).
- InvSearch: `Ji5DQL4U` (`1.20-1.20.4`), `cEOHysju`
  (`1.20.5-1.20.6`), `q7Qu0bY4` (`1.21-1.21.5`), `STB478sr`
  (`1.21.6-1.21.8`), `mUcmcu6p` (`1.21.9-1.21.10`), `5cH3gk5W`
  (`1.21.11`), `ItL1nbRA` (`26.1-26.1.2`), and `pjMiq3id`
  (`26.2-pre-3`).
- InvCatalogue: `syUYNBBx` (`1.20-1.20.4`), `xr4kwVMT`
  (`1.20.5-1.20.6`), `CeiWkhJf` (`1.21-1.21.5`), `EpGw18oc`
  (`1.21.6-1.21.8`), `cWyq44t3` (`1.21.9-1.21.10`), `alMLgIlB`
  (`1.21.11`), `2UacUu46` (`26.1-26.1.2`), and `dxWyap0B`
  (`26.2-pre-3`).

### `3.2.1` Publish Evidence

Status: published.

- Release source commit:
  `878286ebde96d45d890366c0b2e350f5d080a377`.
- Guarded Modrinth publish workflow:
  `https://github.com/ThatMasonGuy/minecraft-inventory-sort/actions/runs/27409085355`.
- Publish artifact:
  `https://github.com/ThatMasonGuy/minecraft-inventory-sort/actions/runs/27409085355/artifacts/7589794071`.
- Annotated Git tag: `v3.2.1`, resolving locally to the release source commit.
- GitHub Release:
  `https://github.com/ThatMasonGuy/minecraft-inventory-sort/releases/tag/v3.2.1`.
- GitHub Release assets: none; Modrinth remains the canonical download surface.
- Workflow upload result: `BUILD SUCCESSFUL in 53m 51s`; upload plan plus
  release jars were captured in artifact `7589794071`.
- Live Modrinth API readback confirmed all 8 InvSort and all 8 InvSearch
  `3.2.1` versions as `listed`; InvCatalogue version URLs were recorded from
  the successful upload log and public page checks before Modrinth rate limiting
  stopped additional unauthenticated checks.

Published Modrinth version ids:

- InvSort: `PiPhyltA` (`1.20-1.20.4`), `BWm5UAyU` (`1.20.5-1.20.6`),
  `uCSAwAo4` (`1.21-1.21.5`), `w0DSuxSa` (`1.21.6-1.21.8`),
  `nHyLq9Yi` (`1.21.9-1.21.10`), `hDOm8yXL` (`1.21.11`), `Jc8Jyr1e`
  (`26.1-26.1.2`), and `nSHow87k` (`26.2-pre-3`).
- InvSearch: `LFplZX2l` (`1.20-1.20.4`), `oyA5JimT` (`1.20.5-1.20.6`),
  `n8iT4lzh` (`1.21-1.21.5`), `iyuV8YvL` (`1.21.6-1.21.8`),
  `U8wPWCjm` (`1.21.9-1.21.10`), `gvkgnSzg` (`1.21.11`), `mPjljVY5`
  (`26.1-26.1.2`), and `zVi5oW1h` (`26.2-pre-3`).
- InvCatalogue: `rqSTR5f3` (`1.20-1.20.4`), `xK2UviU8`
  (`1.20.5-1.20.6`), `iKL9fLOg` (`1.21-1.21.5`), `YKVqgKgv`
  (`1.21.6-1.21.8`), `sfHh95P5` (`1.21.9-1.21.10`), `yBkX7RdN`
  (`1.21.11`), `2iEZg6L4` (`26.1-26.1.2`), and `7q9J83x8`
  (`26.2-pre-3`).

### Launcher/Instance-Hardened Persistent Data

Status: implemented, verified locally, and published in `3.2.1`.

User-requested storage model:

1. Move persistent cross-instance data into shared Tempest Studios app-data
   folders:
   - Windows: `%APPDATA%\TempestStudios\Inv{Mod}\`
   - macOS: `~/Library/Application Support/TempestStudios/Inv{Mod}/`
   - Linux: `$XDG_DATA_HOME/tempest-studios/inv-{mod}/`, falling back to
     `~/.local/share/tempest-studios/inv-{mod}/`.
2. Keep each public feature mod clearly separated so users can delete one mod's
   data without removing the others.
3. Copy old `.minecraft/inventorysort` data on first launch without repeatedly
   re-reading or double-merging legacy files.
4. Treat single-player worlds as instance-local because two launcher instances
   can both contain a world named `world`; keep multiplayer server/profile data
   shared across instances for the same Minecraft account, while separating
   different accounts on the same computer.
5. Handle InvSearch location files conservatively: prefer leaving ambiguous data
   behind over merging legacy locations into the wrong shared namespace.

Implementation notes:

- Added `TempestStudiosData` in shared Core to centralize OS app-data roots,
  per-mod folder names, legacy source paths, sanitization, and launcher-instance
  ids derived from the canonical Minecraft game directory.
- Active stores now write to app-data:
  - `InvCore/server_world_profiles.json`
  - `InvCore/migration_registry.json`
  - `InvSort/sort_rules.json`
  - `InvSearch/item_locations_<namespace>.json`
  - `InvCatalogue/catalog/catalog_<namespace>.json` plus catalogue reports.
- Single-player namespaces now use
  `singleplayer:instance_<hash>:<world>`. Server namespaces now use
  `server:<server>:account:<hash>` or
  `server:<server>:account:<hash>:world:<profile>`.
- Legacy server namespace imports are mapped into the active Minecraft account
  scope, keeping server data shared across launcher instances for that account
  without sharing it across different accounts on the same OS user.
- Added `LegacyDataMigration`, invoked from Core and defensively from each store
  constructor before loading active data. It copies or transforms legacy files
  only when the app-data target is absent and the source has not already been
  recorded in the migration registry.
- Sort-rule container override keys are rewritten from old single-player
  namespaces to the new instance-scoped namespace during import.
- InvSearch imports parse per-namespace JSON files, reject multi-namespace or
  invalid files, rewrite accepted entries to the target namespace, and skip the
  import if the destination tracking file already exists. It does not merge
  legacy lists into existing shared data.
- InvCatalogue snapshot files and report JSON/text files are copied into
  `InvCatalogue/catalog/`; single-player report ids and namespace labels are
  rewritten to the instance-scoped namespace.
- The migration registry records source, target, result, game directory,
  installed mod versions, epoch millis, and ISO timestamp so future launches do
  not keep revisiting the same legacy source files.
- The public release jar size sentinel was raised from 199 KB to 225 KB because
  the shared Core storage/migration classes intentionally increased the embedded
  Core footprint; the release-jar junk-file and embedded-Core checks remain in
  place.

Verification:

- PASS: `git diff --check`.
- PASS: `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
- PASS: `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.

### Recipe Book Button Offset Regression

Status: fixed, confirmed by user local in-game testing, and published in
`3.2.1`.

User-reported repro:

1. In Minecraft `1.21.11`, InvSort and InvSearch buttons on player inventory
   and crafting screens do not move when the recipe book opens.
2. In Minecraft `26.1.2`, the same buttons also stay anchored to the old GUI
   position instead of following the shifted vanilla screen.

Implementation notes:

- Restored the recipe-book-specific update path because
  `AbstractRecipeBookScreen` overrides the normal `AbstractContainerScreen`
  render/extract method used by the generic button-position refresh.
- Fixed the 1.x `AbstractRecipeBookScreen` hook to match the real
  `render(GuiGraphics, int, int, float)` signature.
- Added 26.x `AbstractRecipeBookScreen` overlays that hook
  `extractRenderState(GuiGraphicsExtractor, int, int, float)` and exclude the
  shared 1.x mixin for 26.x profiles.
- Let the Search button participate in the same recipe-book callback as the
  Sort buttons, while keeping either feature mod installable on its own.

Verification:

- PASS: `git diff --check`.
- PASS: `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
- PASS: `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.1.2" --no-daemon --console=plain`.
- PASS: `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=1.21.11,26.1-26.1.2" "-Pinventorysort_smoke_game_versions=1.21.11,26.1.2" "-Pinventorysort_smoke_install_sets=all-public" --no-daemon --console=plain`.
- PASS: `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=1.21.11,26.1-26.1.2" "-Pinventorysort_smoke_game_versions=1.21.11,26.1.2" "-Pinventorysort_smoke_install_sets=inventorysort-only,inventorysearch-only" --no-daemon --console=plain`.
- PASS: User local in-game recipe-book test confirmed the InvSort and InvSearch
  buttons now move correctly.

## Bug Report Intake (2026-06-08)

Purpose: first-pass bug queue for the current split-mod architecture before
implementing the next fixes or additions. User-reported reproduction notes should
be added here first, then promoted into focused implementation checkpoints.

Architecture checkpoint:

- The repo builds four Gradle modules: private shared `inventorysort-core` plus
  the three public feature mods `inventorysort`, `inventorysearch`, and
  `inventorycatalogue`.
- Each public feature jar embeds the shared Core jar, so players can install
  InvSort, InvSearch, or InvCatalogue on its own without downloading a separate
  Core mod.
- Core owns shared identity capture, namespace/world-profile state, common UI
  widgets, smoke-test startup, and the container/player snapshot event bus.
- InvSort owns sorting behavior plus its sort/transfer screen mixin.
- InvSearch owns the search button, search modal, item-location tracker, and
  inventory sampler.
- InvCatalogue owns catalogue commands, session lifecycle, reports, and the
  catalogue store. It records through Core snapshot events rather than its own
  feature mixin.
- Version shims are selected by `minecraft_version_profile` through
  `compat_group`. Shared code stays in `src/client/java`, while API drift lives
  in `src/compat/<compat_group>/client/java`. The `26.x` lane uses the
  non-remapping Loom path and Java 25, while `1.20.x`/`1.21.x` use remapped
  builds with Java 17 or Java 21 as declared by the profile.

### InvSort Bug Report

Status: fixed locally for the queued `3.2.0` minor release.

User-reported bug:

1. DONE (3.2.0 queued): Bundles in the sortable inventory space can make the rest of the inventory
   sort incorrectly and turn into a larger mess. Bundle handling is intentionally
   avoided because bundle contents are complicated, but plain bundle slots still
   need to stop disrupting non-bundle sorting.
   - Implemented bundle partitioning before the normal sort pass: bundles move
     to the front of the selected sortable space, then ordinary items are
     compacted, restacked, and sorted behind them.
   - Bundle moves use a hotbar-buffer `SWAP` action through the Sort click shim
     instead of primary-click swaps, avoiding vanilla bundle insertion hooks.
   - Moved bundle detection into `ItemStackCompat` profile overlays and added
     `ContainerClickCompat.hotbarSwap` for the 1.x and 26.x click APIs.
   - Removed the old unused `fillPlayerStacksFromContainer` helper and the
     misleading `findFirstEmptyNonBundle` path.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Full smoke/CI validation is intentionally deferred until the end of the
     queued minor-release feature set.

Code-audit leads still worth keeping:

1. DONE (3.2.0 queued): Removed the latent `"Crafting"` name-match trap from
   `InventorySorter` container detection so crafting-like screens are not
   treated as sortable containers if buttons are exposed there later.
2. DONE (3.2.0 queued): Removed the misleading bundle helper code touched by the
   partitioning fix. Remaining cleanup should be filed only if a new repro needs
   it.

Dropped from active bugs by user decision:

- Slot-click burst/rate-limit risk is accepted as a current implementation
  limitation.
- Sort-container hotbar top-up is intentional behavior for now.

### InvSearch Bug Report

Status: fixed locally for the queued `3.2.0` minor release.

Code-audit leads already in or added to the backlog:

1. DONE (3.2.0 queued): `ItemLocationTracker.save()` now writes through a temp
   file and moves it into place, keeping a `.bak` copy of the previous tracker
   file before replacement.
2. DONE (3.2.0 queued): `ItemLocationTracker.load()` now treats malformed JSON
   as recoverable, attempts the `.bak` file, and starts with an empty tracker
   instead of letting parse/runtime failures bubble through client startup.
3. DONE (3.2.0 queued): `InventoryHistorySampler` now debounces inventory-total
   writes, flushes pending state on shutdown, and resets pending state on
   namespace changes so rapid inventory churn no longer saves the whole tracker
   file every tick.
4. DONE (3.2.0 queued): Shared Core world-profile confirmation now registers
   remappable keybindings for confirm/open-profile actions. The compat helper
   bridges the older Fabric `KeyBindingHelper` API and the 26.x
   `KeyMappingHelper` API.

Dropped from active bugs by user decision:

- Portable shulker identity collisions are accepted as a current implementation
  limitation.
- Component/NBT-aware Search identity is accepted as future accuracy work, not a
  bug to fix in the current pass.

### InvCatalogue Bug Report

Status: fixed locally for the queued `3.2.0` minor release.

Code-audit leads already in the backlog:

1. DONE (3.2.0 queued): `CatalogStore.save()` now writes catalogue JSON through
   a temp-file swap, keeps a `.bak` copy of the previous file, and attempts the
   backup before starting fresh when a namespace catalogue cannot be parsed.
2. DONE (3.2.0 queued): Shared Core world-profile confirmation now uses
   remappable keybindings instead of raw Enter/Backspace polling.

Dropped from active bugs by user decision:

- Empty containers counting as zero-item catalogue locations are accepted for
  now.
- Portable shulker identity collisions are accepted as a current implementation
  limitation.

## Feature Requests (2026-06-08)

### InvSearch And InvCatalogue Category Search

Goal: let players search by practical InvSort categories in both InvSearch and
the InvCatalogue report item grid.

Status: implemented locally for the queued `3.2.0` minor release.

Requested capabilities:

1. Category query syntax:
   - DONE (3.2.0 queued): InvSearch treats queries starting with `:` as
     category searches instead of normal fuzzy name/id searches.
   - DONE (3.2.0 queued): InvCatalogue's saved-report item filter accepts the
     same `:category` syntax while keeping existing text/id filtering for
     normal queries and legacy report ids.
2. Category source and aliases:
   - DONE (3.2.0 queued): Added a shared Core category helper based on the
     existing InvSort category order so Search and Catalogue can use categories
     without requiring the public InvSort feature jar.
   - DONE (3.2.0 queued): Added broad and friendly aliases such as `:wood`,
     `:stone`, `:terrain`, `:tools`, `:gear`, `:food`, `:storage`, and the
     individual category names/keys.
   - DONE (3.2.0 queued): InvSort's active comparator now reads category keys
     from the same shared helper.

### InvSort Custom Sorting And Slot Rules

Goal: add an in-game configuration menu for user-defined sorting behavior across
player inventories and containers.

Status: implementation plus the first usability revision are complete locally
for the queued `3.2.0` minor release.

Requested capabilities:

1. Custom sort order:
   - DONE (3.2.0 queued): Let players define their own sorting order for
     inventories and chests.
   - DONE (3.2.0 queued): First version supports both category ordering and
     optional specific item-id ordering. Category order remains the broader
     preference, while exact item order can be enabled for full custom control.
   - DONE (3.2.0 queued): Default behavior remains available when no custom
     order is configured.
2. Protected slots:
   - DONE (3.2.0 queued): Let players mark specific inventory/container slots
     as protected.
   - DONE (3.2.0 queued): Protected slots do not receive sorted items.
   - DONE (3.2.0 queued): Protected slots are fully protected by sorting; the
     normal sort pass does not move items out of them or into them.
3. Item-specific slots:
   - DONE (3.2.0 queued): Let players assign specific items to specific slots,
     such as logs in the bottom-right inventory slot, planks in the middle-right
     slot, and sticks in the top-right slot.
   - DONE (3.2.0 queued): Sorting fills item-specific slots with matching items when
     possible and avoids putting other item types there.
4. Scope and persistence decisions:
   - DONE (3.2.0 queued): Rules support global player inventory behavior,
     global container defaults, and per-container overrides when Core has a
     concrete identity. Unsupported/transient containers fall back to a
     per-screen override.
   - DONE (3.2.0 queued): Rules are stored client-side under the existing
     `inventorysort` game directory in `sort_rules.json`, using temp-file writes
     and `.bak` recovery.
   - DONE (3.2.0 queued): Rules compile across the supported profile matrix and
     compose with the bundle partitioning behavior: bundles are still moved to
     the front of the movable sortable area, while protected/item slots are
     withheld from that movable area.
5. Rules-screen usability:
   - DONE (3.2.0 queued): Revised the rules screen so the panel and controls
     stay inside the scaled Minecraft viewport instead of using fixed interior
     columns.
   - DONE (3.2.0 queued): Replaced the simultaneous category/item columns with
     a single right-side list switched by `Category` and `Items` buttons.
   - DONE (3.2.0 queued): Renamed slot actions to `Protect`, `Item Slot`, and
     `Clear` so the rule semantics are visible in the UI.
   - DONE (3.2.0 queued): Added Ctrl-click slot toggling and Shift-click range
     selection; slot actions now apply to the full current selection.

### InvCatalogue In-Game GUI And Snapshot Comparison

Goal: add an in-game Catalogue GUI for browsing item totals visually and
comparing current totals against older catalogue snapshots.

Requested capabilities:

1. Catalogue browser modal:
   - DONE (3.2.0 queued): `/inventorycatalogue reports` opens an in-game
     browser for saved catalogue reports.
   - DONE (3.2.0 queued): selected reports show catalogued item counts in a
     scrollable grid with item icons and readable counts.
   - DONE (3.2.0 queued): selected reports include a search filter for large
     grids.
   - DONE (3.2.0 queued): the report browser and selected-report detail screen
     now use darker modern-Minecraft-style chrome, slot-style item tiles, and
     local Catalogue controls instead of the older InvSearch-like bevel theme.
   - DONE (3.2.0 queued): selected-report layout now reserves enough top space,
     clips and scrolls the selected-item sidebar, and redraws grid/detail
     borders above their clipped contents so oversized report details and
     scrolled items stay visually contained.
   - DONE (3.2.0 queued): item-grid count badges now abbreviate values from
     `1K` upward with rounded `K`/`M`/`B` units, while selected-item details
     continue to show the exact comma-formatted total.
2. Snapshot history:
   - DONE (3.2.0 queued): stopping a session now writes a timestamped
     structured report snapshot alongside the existing plain-text report.
   - Keep the existing persistent current catalogue behavior while adding
     historical snapshots for comparison.
3. Snapshot comparison:
   - Let players select an old snapshot and compare it against the current
     catalogue or another snapshot.
   - Show increases, decreases, new items, and missing items clearly.
4. Reporting compatibility:
   - Keep the existing command-based status/report flow working.
   - Reuse the current catalogue store/session model where possible, but add any
     history data model needed for comparisons.

## Recently Fixed

-62. InvSort gallery screenshot correction (metadata-only, released):
   - Replaced `gallery/InvSort/02_InvSort_Rules_Inventory_Order.png` and the
     matching `description_images` selector copy with the cleaned screenshot.
   - Verified the two local copies have the same SHA-256 hash.
   - Ran `.\scripts\sync-modrinth-project-pages.ps1 -DryRun`.
   - Ran `.\scripts\sync-modrinth-project-pages.ps1 -ReplaceGallery` to refresh
     the live Modrinth project-page/gallery metadata.
   - Public Modrinth readback confirmed the InvSort gallery still has 9 images
     and the long description references the newly uploaded `Rules: Inventory
     Order` CDN image.
   - This did not touch mod jars or published version files.

-61. `3.2.0` minor release publish (released):
   - Release source commit: `8bed48a786f89a74d9c912ea55c78ff9f1adf96c`
     (`8bed48a`, `Bump inventory mods to 3.2.0`).
   - Guarded GitHub Actions Modrinth publish workflow `27131728812` completed
     successfully on 2026-06-08, after running the supported smoke matrix and
     uploading all 24 listed compatibility-group versions.
   - Created annotated tag `v3.2.0` on the release source commit and pushed it
     to GitHub without moving the tag onto later gallery/page-sync docs work.
   - Created GitHub Release `v3.2.0`:
     `https://github.com/ThatMasonGuy/minecraft-inventory-sort/releases/tag/v3.2.0`.
   - Download surface remains Modrinth only; no GitHub jar assets were attached.
   - Uploaded Modrinth versions:
     - InvSort:
       - `1.20-1.20.4`: `dQ6JHDcw`
       - `1.20.5-1.20.6`: `oK3sV7gr`
       - `1.21-1.21.5`: `mmJAaa63`
       - `1.21.6-1.21.8`: `HI972pdE`
       - `1.21.9-1.21.10`: `HBCeGqks`
       - `1.21.11`: `2BnAOcIz`
       - `26.1-26.1.2`: `gcc89gCe`
       - `26.2-pre-3`: `UfCp6WSQ`
     - InvSearch:
       - `1.20-1.20.4`: `BlAL0YJf`
       - `1.20.5-1.20.6`: `QwJiqhJR`
       - `1.21-1.21.5`: `ame5KYab`
       - `1.21.6-1.21.8`: `qhDIPdtI`
       - `1.21.9-1.21.10`: `MH1vnbi9`
       - `1.21.11`: `EpRVszCi`
       - `26.1-26.1.2`: `cBjqkdhr`
       - `26.2-pre-3`: `Ceic8pnh`
     - InvCatalogue:
       - `1.20-1.20.4`: `mFBjMLi9`
       - `1.20.5-1.20.6`: `UmiPhyDj`
       - `1.21-1.21.5`: `q3CXaaPQ`
       - `1.21.6-1.21.8`: `q1PJJd6Z`
       - `1.21.9-1.21.10`: `6iHqgAKS`
       - `1.21.11`: `NHoMRjpi`
       - `26.1-26.1.2`: `dnNmDDO1`
       - `26.2-pre-3`: `q5wDMTLj`
   - Captured the workflow artifact locally with the Modrinth upload plan and
     published release jars under `build/github-artifacts/27131728812/`.
   - While the workflow was running, repeated focused local launch checks for
     `1.21-1.21.5` on Minecraft `1.21.5` and `26.1.2` on Minecraft `26.1` with
     the `all-public` install set; both passed.

-60. Modrinth gallery and project-page source sync for `3.2.0` minor release
     (released):
   - Added root `gallery/` source folders for InvSort, InvSearch, and
     InvCatalogue, with ordered root gallery images plus selector-only
     `banner/` and `description_images/` folders.
   - Added `gallery/metadata.json` for gallery image titles/descriptions and
     `gallery/README.md` for the maintained folder convention.
   - Updated `gradle/modrinth-project-pages.md` so each public mod description
     mentions its new in-game systems and embeds selected gallery images as
     separate Markdown sections instead of side-by-side stacks.
   - Added `scripts/sync-modrinth-project-pages.ps1` plus the manual
     `modrinth project pages` GitHub workflow for repeatable project summary,
     long-description, and gallery syncing.
   - Updated `AGENTS.md` and `gradle/modrinth-publishing.md` so future agents
     manage gallery assets and page-level Modrinth syncs separately from jar
     publishing.
   - Verified `.\scripts\sync-modrinth-project-pages.ps1 -DryRun`.
   - Synced the live Modrinth pages with `.\scripts\sync-modrinth-project-pages.ps1 -ReplaceGallery`;
     readback confirmed InvSort has 9 gallery images with `Rules: Inventory
     Slots` featured, InvSearch has 5 with `Search UI` featured, and
     InvCatalogue has 7 with `Catalogue Report` featured.

-59. Category search for InvSearch and InvCatalogue for `3.2.0` minor release
     (unreleased):
   - Added `InventorySortCategories` to shared Core with the existing InvSort
     category order, category-key matcher, and friendly aliases for broad
     queries such as `:wood`, `:stone`, `:tools`, `:gear`, `:food`, and
     `:storage`.
   - Updated InvSearch so leading-colon queries use category matching across
     live registry results, with category results grouped by category order and
     then item name.
   - Updated the InvCatalogue report-browser item-grid filter to use the same
     category queries for known registry item ids while preserving existing
     name/id filtering for normal queries and legacy report entries.
   - Regenerated the `26.1.2` and `26.2-pre-3` InvSearch and InvCatalogue
     screen overlays from the shared screens, keeping the 26.x
     `GuiGraphicsExtractor` lifecycle and `MinecraftApiCompat.setScreen`
     shims intact.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-58. Minecraft 26.x UI shim sync for `3.2.0` minor release (unreleased):
   - Regenerated the `26.1.2` and `26.2-pre-3` screen overlays for the InvSort
     rules screen, InvSearch modal, tracked-world picker, and InvCatalogue
     report browser from the shared `InvUi` redesign.
   - Kept the 26.x overlays on the `GuiGraphicsExtractor` render-state
     lifecycle while matching the shared modern dark theme and per-mod accents.
   - Fixed the regenerated 26.x Search modal to route screen changes through
     `MinecraftApiCompat.setScreen`, preserving the `26.2-pre-3`
     `client.gui.setScreen(...)` API path.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.1.2" --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.2-pre-3" --no-daemon --console=plain`.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=26.2-pre-3" "-Pinventorysort_smoke_game_versions=26.2-pre-3" "-Pinventorysort_smoke_install_sets=all-public" --no-daemon --console=plain`.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-57. Cohesive in-game UI redesign for `3.2.0` minor release (unreleased):
   - Added a shared `InvUi` theme helper in `src/client/java` (registered in the
     Core client source set) that draws the modern dark window, panels, slot
     tiles, list rows, fields, segmented tabs, chips, count badges, and
     scrollbars through the existing `InventorySortDrawContext` abstraction, so
     the look is reusable on both the 1.x and 26.x rendering lanes.
   - Restyled `InventorySortTextButtonRenderer` and
     `InventorySortModalIconButtonRenderer` to the new flat button theme, which
     restyles buttons across all three feature screens at once.
   - Rebuilt `InventorySortConfigScreen` into `Slots` and `Order` tabs with a
     scope segmented control, a responsive slot grid, a live selection/legend
     panel, and tooltip-labelled slot actions. Chrome controls use invisible
     hitbox buttons (not a `mouseClicked` override) to stay compatible across the
     1.20-1.21.11 mouse-event API drift.
   - Rebuilt `SearchModalScreen` with card result rows, held-versus-tracked
     count colours, an accent-barred locations panel, a focus-highlighted search
     field, and a right rail scrollbar; restyled `ServerWorldProfileScreen` to
     match with an active-profile indicator.
   - Rebuilt `CatalogReportBrowserScreen` browser and detail views with world
     section bands, report cards, slot-tile item grid with count badges, and a
     selected-item sidebar with a report-share bar.
   - Used a Python (PIL) replica of the `InvUi` pixel math to preview each screen
     layout at min/max sizes before writing the Java; preview tooling lives only
     under the ignored `build/ui-preview/` directory.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain` on the
     default `1.21.11` profile.
   - Verified `buildAllMods` directly on every supported 1.x profile:
     `1.20-1.20.4`, `1.20.5-1.20.6`, `1.21-1.21.5`, `1.21.6-1.21.8`,
     `1.21.9-1.21.10`, and `1.21.11`. The 26.x profile verification is
     recorded in the follow-up shim-sync checkpoint above.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-56. InvSort rules-screen usability revision for `3.2.0` minor release
     (unreleased):
   - Reworked the rules screen layout to use a responsive panel that keeps
     buttons, slot actions, and the right-side rule list inside scaled
     Minecraft viewports.
   - Replaced the fixed simultaneous category/item columns with one right-side
     list switched between `Category` and `Items` modes.
   - Renamed unclear slot actions from lock/reserve terminology to `Protect`,
     `Item Slot`, and `Clear`.
   - Added Ctrl-click slot toggling and Shift-click range selection so protected
     slots, item slots, and slot clearing can be applied to multiple slots at
     once.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-55. InvSort custom sorting and slot-rule menu for `3.2.0` minor release
     (unreleased):
   - Added a local `SortRuleStore` persisted at `inventorysort/sort_rules.json`
     with temp-file saves and `.bak` recovery.
   - Added a right-click Sort-button rules screen for player inventory rules and
     container rules.
   - Added custom category order, optional specific item-id order, protected
     slots, and item-specific slots.
   - Sorting now enforces item-specific slots before normal sorting, excludes
     protected and item-specific slots from the movable region, and still
     partitions bundles to the front of the remaining movable area before
     sorting ordinary items.
   - Added global player rules, global container defaults, concrete
     per-container overrides, and per-screen fallback overrides when a concrete
     container identity is unavailable.
   - Moved right-click handling into versioned icon-button shims so the entry
     point works across older `double,double,int` mouse APIs, newer
     `MouseButtonEvent` APIs, and the 26.x render/input lane.
   - Added 26.x `GuiGraphicsExtractor` overlays for the InvSort rules screen.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=1.21.11" "-Pinventorysort_smoke_game_versions=1.21.11" "-Pinventorysort_smoke_install_sets=inventorysort-only" --no-daemon --console=plain`.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-54. InvCatalogue report browser command follow-up for `3.2.0` minor release
     (unreleased):
   - Confirmed backwards compatibility for existing plain-text catalogue
     reports: the browser loads legacy `report_*.txt` files by parsing the
     existing `World:`, `Generated:`, totals, and tab-separated item rows, while
     newer JSON snapshots win when both files exist for the same report id.
   - Fixed the real-client command flow where opening the browser directly from
     chat could be immediately overwritten by the chat screen closing.
     `/inventorycatalogue reports` now queues the screen open on the client
     task queue.
   - Made `/inventorycatalogue report` with no active session open the saved
     report browser instead of returning `No active catalog session`, so users
     who hit the singular command or a command matching edge case still land in
     the useful GUI.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=1.21.11" "-Pinventorysort_smoke_game_versions=1.21.11" "-Pinventorysort_smoke_install_sets=all-public" --no-daemon --console=plain`.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-53. Shared Core `1.21.11` keybinding launch crash for `3.2.0` minor release
     (unreleased):
   - Fixed a user-reported Minecraft `1.21.11` launch crash:
     `Failed to create keybinding` from `KeyBindingCompat` during
     `InventorySortCoreClient` startup.
   - Cause: the remappable world-profile keybinding shim looked up
     `KeyMapping.Category` and `ResourceLocation` by named string reflection.
     Those strings are not remapped inside normal `1.x` release jars, so the
     all-public `1.21.11` launch fell through to an obsolete string-category
     constructor that no longer exists.
   - `KeyBindingCompat` now discovers the live `KeyMapping` category
     constructor from `KeyMapping.class`, creates/registers the category through
     signature-based reflection, and avoids named Minecraft class or method
     strings on the runtime path.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=1.21.11" "-Pinventorysort_smoke_game_versions=1.21.11" "-Pinventorysort_smoke_install_sets=all-public" --no-daemon --console=plain`.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-52. InvCatalogue report-history browser for `3.2.0` minor release
     (unreleased):
   - Added `/inventorycatalogue reports`, opening a Catalogue-styled in-game
     modal for saved reports grouped by world/profile.
   - Added structured JSON report snapshots beside the existing plain-text
     report files, and kept old text reports visible in the browser through a
     fallback parser.
   - Added a scrollable item-icon grid with readable count badges, a search
     filter, and a selected-item detail panel.
   - Kept the new browser behind version shims: shared `GuiGraphics` rendering
     remains in the 1.x lane, 26.x uses `GuiGraphicsExtractor` overlays, and
     hitbox-only buttons avoid mouse-click signature drift.
   - Updated the user-facing README and `3.2.0` release notes.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain` after
     the report browser, release-doc, and shim-overlay updates.
   - Verified
     `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=1.20-1.20.4" --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.1.2" --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.2-pre-3" --no-daemon --console=plain`.
   - `.\gradlew.bat buildAllVersions --no-daemon --console=plain` initially
     caught a `1.21.9-1.21.10` hitbox-button render override mismatch; the
     shim was corrected to follow that profile's `renderWidget` API.
   - Verified
     `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=1.21.9-1.21.10" --no-daemon --console=plain`.
   - Verified
     `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-51. InvSort bundle partitioning fix for `3.2.0` minor release (unreleased):
   - Fixed the user-reported case where bundles in a sortable inventory or
     container region could disrupt the rest of the sort and leave ordinary
     items in a worse order.
   - Added a pre-sort bundle partition step that moves bundles to the front of
     the selected sortable region, then sorts only the non-bundle tail behind
     them.
   - Used a hotbar-buffer `SWAP` action for bundle partitioning so bundle stacks
     move as opaque items without primary-clicking items into bundle slots.
   - Added `ContainerClickCompat.hotbarSwap` for the normal `ClickType.SWAP`
     path and the 26.x `ContainerInput.SWAP` path.
   - Moved bundle detection behind the per-profile `ItemStackCompat` shim.
   - Removed the unused `fillPlayerStacksFromContainer` helper and the
     misleading `findFirstEmptyNonBundle` helper.
   - Added draft user-facing release notes in `gradle/release-notes/3.2.0.md`.
   - Verified `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
   - Full smoke/CI validation remains deferred until the rest of the `3.2.0`
     minor-release work is complete.

-50. `3.1.3` icon refresh publish (unreleased):
   - Consumed root source images `InvSort.jpg`, `InvSearch.jpg`, and
     `InvCatalogue.jpg`.
   - Resized the new icons to `256x256` JPG assets:
     `invsort.jpg` `25305` bytes, `invsearch.jpg` `23529` bytes, and
     `invcatalogue.jpg` `23498` bytes.
   - Updated the live Modrinth project icons for InvSort, InvSearch, and
     InvCatalogue through the Modrinth project icon API, with changed
     `icon_url` readbacks saved under ignored `build/modrinth/` snapshots.
   - Bumped `mod_version` from `3.1.2` to `3.1.3`.
   - Updated README to identify `3.1.3` as the current release lane.
   - Added `gradle/release-notes/3.1.3.md` for the user-facing Modrinth
     changelog.
   - Verified `git diff --check` and
     `.\gradlew.bat buildAllMods --no-daemon --console=plain`; default
     `1.21.11` jars rebuilt at `140059-161210` bytes.
   - Pushed release source commit `c3346e6`; fast GitHub build `26945586568`
     passed.
   - First guarded GitHub Actions Modrinth publish workflow `26945681058`
     failed before any uploads after `76` smoke pass lines due to a transient
     `:smokelaunch:downloadAssets` executor shutdown during the first `26.1`
     smoke runtime.
   - Retried the guarded GitHub Actions Modrinth publish workflow as
     `26947958432`; it completed in `44m55s`, recorded `92` smoke pass lines,
     prepared `24` upload-plan entries, and uploaded all `24` supported public
     versions.
   - Refreshed local ignored `build/release/` from the successful GitHub
     workflow artifact so all eight local release folders contain the published
     `3.1.3` jars and no stale `3.1.2` or `3.1.1` jars. The largest local
     published jar is `162197` bytes.
   - Uploaded Modrinth versions:
     - InvSort:
       `3.1.3+mc1.20-1.20.4` `LcViD2sA`,
       `3.1.3+mc1.20.5-1.20.6` `TuYY3CLN`,
       `3.1.3+mc1.21-1.21.5` `f7F0JajY`,
       `3.1.3+mc1.21.6-1.21.8` `bLxDJ2VH`,
       `3.1.3+mc1.21.9-1.21.10` `IEIWP9ev`,
       `3.1.3+mc1.21.11` `LTqSbpWh`,
       `3.1.3+mc26.1-26.1.2` `zdmT23QI`,
       `3.1.3+mc26.2-pre-3` `aFOwnFrf`.
     - InvSearch:
       `3.1.3+mc1.20-1.20.4` `p3W0xqZC`,
       `3.1.3+mc1.20.5-1.20.6` `zqq1fBON`,
       `3.1.3+mc1.21-1.21.5` `jnIZO8AK`,
       `3.1.3+mc1.21.6-1.21.8` `RwS16UPT`,
       `3.1.3+mc1.21.9-1.21.10` `2fTru9ay`,
       `3.1.3+mc1.21.11` `O2GfI17p`,
       `3.1.3+mc26.1-26.1.2` `1nshovgG`,
       `3.1.3+mc26.2-pre-3` `tWwyp90c`.
     - InvCatalogue:
       `3.1.3+mc1.20-1.20.4` `2D2Lz7Wx`,
       `3.1.3+mc1.20.5-1.20.6` `OnippIT6`,
       `3.1.3+mc1.21-1.21.5` `ns9pYQ26`,
       `3.1.3+mc1.21.6-1.21.8` `6Z6f79v2`,
       `3.1.3+mc1.21.9-1.21.10` `T9VBdK1L`,
       `3.1.3+mc1.21.11` `7f81Mm2H`,
       `3.1.3+mc26.1-26.1.2` `hETjLZkK`,
       `3.1.3+mc26.2-pre-3` `E3CFq9g4`.
   - Created annotated Git tag `v3.1.3` on release source commit `c3346e6`.
   - Created GitHub release `Inventory Mods 3.1.3`:
     `https://github.com/ThatMasonGuy/minecraft-inventory-sort/releases/tag/v3.1.3`.
   - The GitHub release does not attach jar assets; it points users to the
     canonical Modrinth project pages and the 24 uploaded Modrinth version URLs.

-49. GitHub repository About metadata refresh (unreleased):
   - Updated the live GitHub repository description to describe InvSort,
     InvSearch, and InvCatalogue as separate client-side Fabric inventory mods
     across Minecraft `1.20.x`, `1.21.x`, and `26.x`.
   - Set the repository homepage URL to the InvSort Modrinth page:
     `https://modrinth.com/mod/invsort`.
   - Added GitHub topics:
     `minecraft`, `minecraft-mod`, `fabric`, `fabricmc`, `modrinth`,
     `inventory`, `inventory-management`, `inventory-sort`,
     `inventory-search`, `inventory-catalogue`, `storage-management`,
     `client-side`, and `java`.

-48. GitHub release tagging for Modrinth publishes (unreleased):
   - Created annotated Git tag `v3.1.2` on release source commit `713b9ec`.
   - Created GitHub release `Inventory Mods 3.1.2`:
     `https://github.com/ThatMasonGuy/minecraft-inventory-sort/releases/tag/v3.1.2`.
   - The GitHub release does not attach jar assets; it points users to the
     canonical Modrinth project pages and the 24 uploaded Modrinth version URLs.
   - Updated `AGENTS.md` so future agents know every successful real Modrinth
     publish must be followed by an annotated `v<mod_version>` tag and GitHub
     release.
   - Updated `gradle/modrinth-publishing.md` with the ongoing GitHub
     tag/release procedure and release body expectations.

-47. `3.1.2` packaging cleanup publish (unreleased):
   - Bumped `mod_version` from `3.1.1` to `3.1.2`.
   - Updated README to identify `3.1.2` as the current release lane.
   - Updated `gradle/release-notes/3.1.2.md` to call out that current release
     jars are roughly 5-6x smaller, dropping from about `862-888 KB` to about
     `131-156 KB` each.
   - Verified `git diff --check` and
     `.\gradlew.bat buildAllMods --no-daemon --console=plain`; default
     `1.21.11` jars rebuilt at `134099-155263` bytes.
   - Pushed commit `9740215` and the fast GitHub build `26939731373` passed.
   - Ran the guarded GitHub Actions Modrinth publish workflow
     `26939817894` as a listed release. It completed in `45m6s`, recorded
     `92` smoke pass lines, prepared `24` upload-plan entries, and uploaded all
     `24` supported public versions.
   - Refreshed local ignored `build/release/` from the GitHub workflow artifact
     so all eight local release folders contain the published `3.1.2` jars and
     no stale `3.1.1` jars. The largest local published jar is `156249` bytes.
   - Uploaded Modrinth versions:
     - InvSort:
       `3.1.2+mc1.20-1.20.4` `hZ2Nin6T`,
       `3.1.2+mc1.20.5-1.20.6` `BbB72m5g`,
       `3.1.2+mc1.21-1.21.5` `9rgZ15Gi`,
       `3.1.2+mc1.21.6-1.21.8` `7BTWmrgp`,
       `3.1.2+mc1.21.9-1.21.10` `Ob9P3g7O`,
       `3.1.2+mc1.21.11` `gahcvA21`,
       `3.1.2+mc26.1-26.1.2` `mpLwcxBU`,
       `3.1.2+mc26.2-pre-3` `jCKYi5Me`.
     - InvSearch:
       `3.1.2+mc1.20-1.20.4` `oS9hgFqE`,
       `3.1.2+mc1.20.5-1.20.6` `UQrTErEN`,
       `3.1.2+mc1.21-1.21.5` `912yIhwu`,
       `3.1.2+mc1.21.6-1.21.8` `1mkkgJZV`,
       `3.1.2+mc1.21.9-1.21.10` `lTwu813j`,
       `3.1.2+mc1.21.11` `bavd4UJL`,
       `3.1.2+mc26.1-26.1.2` `IIozutcI`,
       `3.1.2+mc26.2-pre-3` `2EQEowJX`.
     - InvCatalogue:
       `3.1.2+mc1.20-1.20.4` `oQK5d8sc`,
       `3.1.2+mc1.20.5-1.20.6` `l2uq6quh`,
       `3.1.2+mc1.21-1.21.5` `YviEKx4t`,
       `3.1.2+mc1.21.6-1.21.8` `heQSwYks`,
       `3.1.2+mc1.21.9-1.21.10` `uapVbqgB`,
       `3.1.2+mc1.21.11` `1tBzoWW5`,
       `3.1.2+mc26.1-26.1.2` `VFXtd0OC`,
       `3.1.2+mc26.2-pre-3` `aPvh0lV7`.

-46. InvSearch and InvCatalogue Modrinth page copy refresh (unreleased):
   - Added source-of-truth Modrinth page summaries and description Markdown for
     InvSearch and InvCatalogue in `gradle/modrinth-project-pages.md`.
   - Updated Inventory Search and Inventory Catalogue Fabric metadata
     descriptions to match their new Modrinth summaries.
   - Updated draft `gradle/release-notes/3.1.2.md` so the queued patch release
     mentions all three refreshed metadata descriptions.
   - Verified `git diff --check` and
     `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
   - Updated the live InvSearch and InvCatalogue Modrinth project summaries and
     description pages through the Modrinth API, with readback verification.
   - Saved before/after API snapshots under ignored `build/modrinth/` artifacts.

-45. InvSort Modrinth page copy refresh (unreleased):
   - Added `gradle/modrinth-project-pages.md` as the source-of-truth file for
     Modrinth project summaries and description-page copy.
   - Drafted a cleaner InvSort project summary and description in the same
     style as the Lifetime Stat Tracker page: short intro, feature list,
     good-for list, and install note.
   - Updated `AGENTS.md` and `gradle/modrinth-publishing.md` so future agents
     know where Modrinth page-level copy lives and that publish tasks upload
     versions only, not project page metadata.
   - Updated the Inventory Sort Fabric metadata description to match the new
     short Modrinth summary.
   - Updated draft `gradle/release-notes/3.1.2.md` with the user-visible
     metadata refresh.
   - Updated the live InvSort Modrinth project summary and description page
     through the Modrinth API, with readback verification.

-44. Release jar size cleanup (unreleased):
   - Investigated the post-split jar size jump from roughly low hundreds of KB
     to roughly `862-888 KB` per public jar.
   - Root cause: every module packaged all shared assets, including all three
     large per-mod JPG icons, and each public feature jar also embedded Core
     with the same full asset set.
   - Changed Gradle resource packaging so Core includes only
     `assets/inventory-sort/icon.png`, Sort includes only `invsort.jpg`, Search
     includes only `invsearch.jpg`, and Catalogue includes only
     `invcatalogue.jpg`.
   - Resized the three per-mod JPG metadata icons to `256x256`, reducing them
     from `114-146 KB` each to roughly `18-19 KB` each.
   - Added a `verifyReleaseJars` guard that fails public release jars above
     `199000` bytes and verifies public jars only package their declared icon
     asset.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`;
     all 24 public `3.1.1` release jars rebuilt successfully.
   - Confirmed the rebuilt public release jars are `131457-156225` bytes, with
     the largest jar below the `199000` byte target.
   - Added draft `gradle/release-notes/3.1.2.md` for this user-facing packaging
     improvement if it is published as the next patch.
   - Verified final diff checks and committed the cleanup.

-43. Documentation handoff cleanup (unreleased):
   - Updated `AGENTS.md` with a fresh-agent reading order, the actual
     verification command ladder, local-vs-GitHub validation guidance, and the
     rule for updating Modrinth-facing release notes only for user-facing
     release changes.
   - Added `gradle/compatibility-release-playbook.md` as a portable plan for
     adapting the compatibility-group, smoke-test, and guarded publish strategy
     to other single-mod projects.
   - Clarified in `gradle/version-profiles/README.md` that supported/candidate
     profile lists use profile file names while release folders and Modrinth
     suffixes use `profile_id`.
   - Expanded `gradle/modrinth-publishing.md` with when to update the active
     user-facing release note file as work progresses.
   - Rebuilt `README.md` with clean ASCII punctuation and a link to the
     compatibility-release playbook.
   - Verified `git diff --check` for the documentation cleanup.

-42. Unified `3.1.1` release matrix (unreleased):
   - Promoted all smoke-passed compatibility groups into the active
     `3.1.1` supported publish lane:
     `1.20-1.20.4`, `1.20.5-1.20.6`, `1.21-1.21.5`,
     `1.21.6-1.21.8`, `1.21.9-1.21.10`, `1.21.11`,
     `26.1-26.1.2`, and `26.2-pre-3`.
   - Added `JAVA_HOME_17_X64` to Gradle toolchain environment discovery so the
     Java 17 `1.20-1.20.4` lane can be rebuilt locally without changing the
     active Java 21 install.
   - Updated the Modrinth publish and compatibility-validation workflows to
     install Java 17 alongside Java 21 and Java 25.
   - Updated `gradle/release-notes/3.1.1.md` so the Modrinth changelog matches
     a unified release across Minecraft `1.20.x`, `1.21.x`, and `26.x`.
   - Updated README, compatibility, version-profile, smoke-test, and Modrinth
     publishing docs to describe the full `3.1.1` release matrix.
   - Verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`;
     it rebuilt all eight local release folders with 24 total `3.1.1` jars.
   - Verified `.\gradlew.bat publishModrinthDryRun -x smokeTestSupportedClients
     --no-daemon --console=plain`; it wrote a 24-upload Modrinth plan covering
     all eight compatibility groups for InvSort, InvSearch, and InvCatalogue.
   - Cancelled the first guarded GitHub publish attempt after the
     `1.20-1.20.4` Java 17 smoke launch failed to initialize Mixin because the
     packaged configs still declared `JAVA_21`.
   - Made `*.mixins.json` resources expand the active profile Java target
     (`JAVA_17`, `JAVA_21`, or `JAVA_25`) at build time.
   - Verified the exact failing slice with
     `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=1.20-1.20.4" "-Pinventorysort_smoke_game_versions=1.20" "-Pinventorysort_smoke_install_sets=inventorysort-only" --no-daemon --console=plain`.
   - Re-verified `.\gradlew.bat buildAllVersions --no-daemon --console=plain`;
     it rebuilt all eight local release folders with 24 total `3.1.1` jars.
   - Re-verified `.\gradlew.bat publishModrinthDryRun -x smokeTestSupportedClients
     --no-daemon --console=plain`; it wrote the 24-upload Modrinth plan after
     the Mixin compatibility-level fix.
   - Inspected representative packaged release jars and confirmed Mixin configs
     now emit `JAVA_17` for `1.20-1.20.4`, `JAVA_21` for the later
     `1.20.x`/`1.21.x` lanes, and `JAVA_25` for the `26.x` lanes.
   - Pushed commit `74ebd4a` and reran the guarded GitHub Actions Modrinth
     publish workflow with `dry_run=false`, `version_type=release`,
     `requested_status=listed`, and `gradle_java_version=25`.
   - Workflow run `26896650032` passed in GitHub Actions after the full smoke
     matrix and uploaded all 24 listed `3.1.1` Modrinth versions.

-41. Minecraft `26.x` launch-crash hotfix (unreleased):
   - Investigated a real Minecraft `26.1.2` launch crash from the published
     `3.1.0+mc26.1-26.1.2` jars.
   - Root cause: the shared `MultiPlayerGameModeMixin` still targeted the
     older `interactAt` method used by the `1.20.x`/`1.21.x` lanes, while
     checked `26.x` runtimes expose one `interact(Player, Entity,
     EntityHitResult, InteractionHand)` method.
   - Added `26.1.2` and `26.2-pre-3` compatibility overlays for
     `MultiPlayerGameModeMixin`, and excluded the shared mixin source from all
     `26.x` release/smoke profiles that use those overlays.
   - Hardened `InventorySortSmokeTest` to force-load
     `net.minecraft.client.multiplayer.MultiPlayerGameMode` during smoke
     startup, so this class of lazy mixin-target failure is caught before a
     smoke run can pass.
   - Bumped the hotfix lane to `3.1.1` and added
     `gradle/release-notes/3.1.1.md` for the Modrinth-facing changelog.
   - Verified `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.1.2"
     --no-daemon --console=plain`.
   - Verified selected all-public smoke launches for the fixed `26.1-26.1.2`
     jar on exact runtimes `26.1`, `26.1.1`, and `26.1.2`, plus the sibling
     `26.2-pre-3` all-public smoke launch.
   - Verified `.\gradlew.bat publishModrinthDryRun -x smokeTestSupportedClients
     --no-daemon --console=plain`; it wrote the expected six-upload `3.1.1`
     Modrinth plan without performing an API upload.
   - Cleared stale generated `build/release` and `build/modrinth` output, then
     regenerated the dry-run output so only `26.1-26.1.2` and `26.2-pre-3`
     `3.1.1` release folders remain locally.
   - Updated README, compatibility, and version-profile docs so the active
     Minecraft `26.x` lane points to `3.1.1` instead of the superseded
     `3.1.0` release.
   - Next step: run the guarded GitHub Actions Modrinth publish workflow for
     `3.1.1` with Java 25 after release approval.

-40. Minecraft `26.x` `3.1.0` release promotion (unreleased):
   - Bumped `mod_version` to `3.1.0`.
   - Promoted the smoke-passed `26.x` release profiles into
     `supported_minecraft_version_profiles`: `26.1.2` and `26.2-pre-3`.
   - Cleared `candidate_minecraft_version_profiles` so the publish automation
     only targets the `26.x` release lane for `3.1.0`.
   - Kept the existing `3.0.0` Modrinth release lane as the public
     compatibility set for Minecraft `1.20.x` and `1.21.x`.
   - Updated `gradle/release-notes/3.1.0.md` with user-facing Modrinth notes
     for the validated `26.x` versions.
   - Verified `verifySmokeTestMatrix`, both `26.x` profile summaries, the
     default fast `buildAllMods`, and both promoted `26.x` `buildAllMods`
     profiles.
   - Verified `publishModrinthDryRun -x smokeTestSupportedClients` writes the
     expected six-upload `3.1.0` Modrinth plan: Sort/Search/Catalogue for
     `26.1-26.1.2`, plus Sort/Search/Catalogue for `26.2-pre-3`.
   - Pushed commit `4c4cc13` and triggered GitHub Actions run
     `26887305101` (`modrinth publish`) with `dry_run=false`,
     `version_type=release`, `requested_status=listed`, and
     `gradle_java_version=25`.
   - The workflow passed in 9m51s, ran the supported `26.x` smoke matrix under
     `xvfb`, and uploaded:
     - InvSort:
       - `3.1.0+mc26.1-26.1.2`: `eCQRy8yN`
       - `3.1.0+mc26.2-pre-3`: `5SYieSUN`
     - InvSearch:
       - `3.1.0+mc26.1-26.1.2`: `D3qSEGTH`
       - `3.1.0+mc26.2-pre-3`: `u09BCKUg`
     - InvCatalogue:
       - `3.1.0+mc26.1-26.1.2`: `YP0PHkOr`
       - `3.1.0+mc26.2-pre-3`: `Hc3QRfTb`
   - Public unauthenticated Modrinth API lookups for the uploaded version ids
     returned 404 immediately after upload, consistent with project/version
     review or public-visibility delay rather than a workflow upload failure.
   - Step 9 is complete for the current `26.x` publish lane.

-39. Minecraft `26.x` automated smoke testing (unreleased):
   - Ran the selected `26.x` candidate smoke matrix with packaged release jars.
   - The grouped `26.1-26.1.2` candidate jar launched successfully on exact
     runtimes `26.1`, `26.1.1`, and `26.1.2`.
   - Each `26.1.x` runtime passed as Sort-only, Search-only, Catalogue-only,
     and all-three public installs.
   - The first `26.2-pre-3` launch failed before mod initialization because
     Fabric Loader reports the runtime as `26.2-pre.3`, while the profile had
     generated a `~26.2-pre-3` Minecraft dependency.
   - Matched Fabric API's prerelease metadata by changing the `26.2-pre-3`
     profile's `minecraft_dependency` to `~26.2-`, while keeping
     `modrinth_game_versions=26.2-pre-3` for Modrinth upload metadata.
   - Re-ran `26.2-pre-3`; it launched successfully as Sort-only, Search-only,
     Catalogue-only, and all-three public installs.
   - Updated the smoke-test matrix so `26.1`, `26.1.1`, `26.1.2`, and
     `26.2-pre-3` are all recorded as passing candidate smoke tests.
   - Did not rerun the old full `1.20.x`/`1.21.x` smoke matrix for this
     checkpoint because only the `26.x` candidate lane changed; the guarded
     promotion/publish workflow remains responsible for the full release gate.
   - Step 8 is complete. Next step is publication promotion: decide whether to
     promote the smoke-passed `26.x` profiles into the supported publish lane,
     bump to `3.1.0`, and run the guarded Modrinth workflow when ready.

-38. Minecraft `26.x` candidate range research and smoke plan (unreleased):
   - Checked current Modrinth/Fabric metadata before starting the `26.x` smoke
     step.
   - Found that Fabric API `0.150.0+26.1.2` declares Minecraft `26.1`,
     `26.1.1`, and `26.1.2` support, so the `26.1.2` compile-anchor profile
     now builds one grouped `26.1-26.1.2` candidate jar.
   - Added exact runtime-only smoke profiles for `26.1` and `26.1.1`, both
     using the `26.1.2` compat group and Fabric API artifact, so the grouped
     candidate jar can be launch-tested on every listed runtime.
   - Kept `26.2-pre-3` as an exact provisional candidate because current
     Fabric API `26.2` pre-release artifacts are scoped to individual
     pre-releases and Minecraft `26.2` final is not available yet.
   - Added pending smoke records for `26.1`, `26.1.1`, `26.1.2`, and
     `26.2-pre-3`, and added `26.1.2` plus `26.2-pre-3` to
     `candidate_minecraft_version_profiles`.
   - Next step is running and repairing the automated `26.x` smoke-test matrix.

-37. Minecraft `26.x` Catalogue feature compile and candidate jar build pass (unreleased):
   - Verified Inventory Catalogue compiles without additional source shims on
     both checked `26.x` candidate profiles.
   - Extended profile-driven shared-source exclusions to source jar tasks so
     26.x overlays such as `SearchModalScreen`, `SearchButtonMixin`, and
     `ContainerClickCompat` do not create duplicate source-jar entries during
     full candidate builds.
   - Verified `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.1.2" --no-daemon --console=plain`
     now builds all public candidate jars and verifies their metadata.
   - Verified `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.2-pre-3" --no-daemon --console=plain`
     now builds all public candidate jars and verifies their metadata.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes.
   - Step 7 feature compile passes are complete for Core, Sort, Search, and
     Catalogue on the checked `26.x` profiles.
   - Next step is `26.x` smoke testing.

-36. Minecraft `26.x` Search feature compile pass (unreleased):
   - Added `26.1.2` and `26.2-pre-3` Search overlays for
     `SearchModalScreen` and `SearchButtonMixin`.
   - Ported Search rendering to the `26.x` `GuiGraphicsExtractor`
     `extractRenderState` lifecycle while preserving the shared
     `1.20.x`/`1.21.x` `GuiGraphics` screen.
   - Routed Search's screen transitions through `MinecraftApiCompat` in the
     `26.x` overlays so both checked `26.x` API shapes compile.
   - Updated `26.x` profile exclusions so old shared Search render classes do
     not collide with the candidate overlays.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes.
   - Verified both `26.1.2` and `26.2-pre-3`
     `:inventorysearch:compileClientJava` pass.
   - Next step is the `26.x` Catalogue feature compile pass.

-35. Minecraft `26.x` container/mixin input update (unreleased):
   - Added a shared `ContainerClickCompat` adapter so Sort behavior asks for
     pickup/quick-move actions without importing Minecraft's click enum directly.
   - Kept `1.20.x`/`1.21.x` on `ClickType` while adding `26.1.2` and
     `26.2-pre-3` overlays that translate the same Sort actions to
     `ContainerInput.PICKUP` and `ContainerInput.QUICK_MOVE`.
   - Added `26.x` `AbstractContainerScreenInvoker` and
     `AbstractContainerMenuInvoker` overlays so Core no longer compiles the old
     `ClickType` invoker signatures for candidate profiles.
   - Added `26.x` Sort `HandledScreenMixin` overlays for the
     `extractRenderState` lifecycle after `render(GuiGraphics, ...)` moved out
     of the container-screen API.
   - Added `26.x` Sort item-stack compatibility overlays and fixed the
     non-remap Core dependency shape so feature modules compile against Core's
     actual client jar instead of its empty main classes directory.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes.
   - Verified both `26.1.2` and `26.2-pre-3`
     `:inventorysort-core:compileClientJava` pass.
   - Verified both `26.1.2` and `26.2-pre-3`
     `:inventorysort:compileClientJava` pass.
   - Next step is the `26.x` feature compile pass, starting with Search and then
     Catalogue.

-34. Minecraft `26.x` Fabric API event/command update verification (unreleased):
   - Audited direct Fabric command/HUD API usage after the Core helper and
     rendering passes.
   - Confirmed `ClientCommandManager` is now isolated to `1.20.x`/`1.21.x`
     `ClientCommandCompat` overlays, while `26.x` overlays use
     `ClientCommands`.
   - Confirmed `HudRenderCallback` is now isolated to `1.20.x`/`1.21.x`
     `HudCompat` overlays, while `26.x` overlays use the HUD element registry.
   - Confirmed shared `ClientCommandRegistrationCallback` and
     `ClientTickEvents` registrations remain available in the checked `26.x`
     Fabric API jars, so no additional wrapper is required for this step.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes.
   - Verified the `26.x` Core compile boundary remains the planned
     `ClickType` / `ContainerInput` invoker work rather than command, HUD, or
     lifecycle event API drift.
   - Next step is the `26.x` container/mixin input update.

-33. Minecraft `26.x` Core GUI/rendering abstraction (unreleased):
   - Added a shared `InventorySortDrawContext` interface so Core drawing logic no
     longer directly depends on `GuiGraphics`.
   - Added version-selected draw-context wrappers for `1.20.x`/`1.21.x`
     `GuiGraphics` and `26.x` `GuiGraphicsExtractor`.
   - Routed shared panel drawing, icon/text/modal button renderers, the
     world-profile HUD, and Search's shared panel helper calls through the draw
     context.
   - Added `26.1.2` and `26.2-pre-3` button overlays using the new
     `extractContents` lifecycle.
   - Wrapped HUD registration with `HudCompat`, keeping `HudRenderCallback` for
     `1.20.x`/`1.21.x` and using the `26.x` HUD element registry for candidate
     profiles.
   - Added `26.x` profile-screen overlays using `extractRenderState`, with a
     profile-driven shared-source exclusion so the current `GuiGraphics` screen
     remains untouched for `1.20.x`/`1.21.x`.
   - Added Core helpers for GUI hidden state, screen presence, screen switching,
     and singleplayer detection after `26.2-pre-3` moved those APIs again.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes.
   - Verified both `26.1.2` and `26.2-pre-3`
     `:inventorysort-core:compileClientJava` now get past GUI/rendering, HUD,
     screen, and helper API blockers. Both candidate Core probes now stop only
     at the planned `ClickType` / `ContainerInput` invoker work.
   - Search's full `26.x` screen lifecycle is still expected to be handled during
     the Search feature compile pass.
   - Next step is the `26.x` container/mixin input update.

-32. Minecraft `26.x` Core helper compatibility overlay (unreleased):
   - Added `26.1.2` and `26.2-pre-3` Core compatibility overlays for
     Minecraft API helper calls, including dimension ids, connected chest
     lookup, window handles, single-player server directories, HUD pose helpers,
     and player feedback messages.
   - Added a version-selected `ClientCommandCompat` adapter so shared command
     code can use `ClientCommandManager` on `1.20.x`/`1.21.x` and
     `ClientCommands` on `26.x`.
   - Routed shared Core/Search/Catalogue command builders through
     `ClientCommandCompat`.
   - Routed shared player feedback calls through `MinecraftApiCompat` so
     `1.20.x`/`1.21.x` keep `displayClientMessage` while `26.x` uses
     `sendSystemMessage` / `sendOverlayMessage`.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes on the existing release lane.
   - Verified the `26.1.2` Core compile probe now gets past the first helper,
     command, and feedback-message API breaks. The remaining compile blockers
     are the planned GUI/render extraction, HUD registry, and container input
     changes.
   - Next step is the `26.x` GUI/rendering abstraction.

-31. Minecraft `26.x` Java 25 toolchain and CI lane (unreleased):
   - Gradle now requests the active profile's `java_version` as the Java
     toolchain for Java compile and client-run tasks.
   - Added Java toolchain discovery hints for `JAVA_HOME`, `JAVA_HOME_21_X64`,
     and `JAVA_HOME_25_X64`.
   - Added a manual GitHub Actions `compatibility validation` workflow that
     installs Java 21 and Java 25, defaults to running candidate profiles on
     Java 25, and can run focused `printVersionProfile` or `buildAllMods`
     checks for profiles such as `26.1.2`.
   - Updated the manual Modrinth publish workflow to install both Java 21 and
     Java 25 and expose a `gradle_java_version` input. It still defaults to Java
     21 for the current `1.20.x`/`1.21.x` release lane.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes on local Java 21.
   - Verified `.\gradlew.bat printVersionProfile "-Pminecraft_version_profile=26.1.2"`
     still configures and clearly reports Java 25 as the profile target.
   - Local `26.1.2` compile now stops at the expected Java 25 toolchain gate on
     this machine because no JDK 25 is installed locally; GitHub's manual
     compatibility workflow supplies that toolchain for future source/API work.
   - Next step is the `26.x` Core compatibility overlay.

-30. Minecraft `26.x` build-system foundation (unreleased):
   - Added `unobfuscated_minecraft=true` profile support for `26.x` profiles.
   - Gradle now selects `net.fabricmc.fabric-loom-remap`,
     `modImplementation`, and `remapJar` for the existing `1.20.x`/`1.21.x`
     lane, while `26.x` selects `net.fabricmc.fabric-loom`, `implementation`,
     and plain `jar` release artifacts.
   - Feature modules now use the Core `namedElements` dependency only for
     remapped profiles; unobfuscated profiles use the normal Core project
     dependency.
   - `collectReleaseJars`, `verifyReleaseJars`, and `printVersionProfile` now
     report/use the profile-selected release jar task.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes on `1.21.11`.
   - Verified `26.1.2` and `26.2-pre-3` both configure with the non-remap
     build lane via `printVersionProfile`.
   - Verified a `26.1.2` `buildAllMods --dry-run` uses `jar` tasks instead of
     `remapJar` tasks.
   - Next step is Java 25 toolchain/CI handling before real `26.x` compile and
     source/API shim work.

-29. `3.1.0` release-notes lane for Minecraft `26.x` (unreleased):
   - Added `gradle/release-notes/3.1.0.md` as the running Modrinth-facing,
     user-focused changelog for the planned official `26.x` support release.
   - Clarified in `AGENTS.md` that `CHANGELOG.md` remains the repo-facing
     engineering history, while `gradle/release-notes/3.1.0.md` should only
     collect user-visible `3.1.0` release notes.
   - Completed by item 40 after the `26.x` compile, smoke, and
     supported-profile promotion gates passed.

-28. Minecraft 26.x forward-development roadmap (unreleased):
   - Decided to keep shared source anchored to the proven `1.20.x`/`1.21.x`
     compatibility baseline and build `26.x` support forward as candidate
     profiles.
   - Recorded the `26.x` roadmap: Java 25/build-system support, non-remap Loom,
     Core API compatibility, GUI/render extraction compatibility, Fabric command
     and HUD API changes, container/mixin input changes, feature compile passes,
     automated smoke testing, and the promotion/publish gate.
   - Branching remains the fallback only if `26.x` forces duplicated feature
     logic instead of isolated build config, adapters, and mixin surfaces.

-27. Fast push/PR build workflow (unreleased):
   - Changed the automatic GitHub `build` workflow from full `ciValidation` to
     the faster default-profile `buildAllMods` task.
   - Removed the push/PR workflow's `xvfb` install because the fast build does
     not launch Minecraft clients.
   - Kept the manual `modrinth publish` workflow as the expensive release gate:
     it still runs `publishModrinth`/`publishValidation`, builds supported
     profiles, smoke-launches the supported matrix, and only uploads after that
     passes.
   - Updated README and Gradle docs so the intended workflow is local fast
     build -> commit/push -> manual Modrinth workflow validates and publishes.

-26. Final `3.0.0` broad Modrinth release sweep (unreleased):
   - Bumped `mod_version` to `3.0.0`.
   - Added `gradle/release-notes/3.0.0.md` for the public Modrinth release.
   - Promoted the smoke-passed compatibility groups into
     `supported_minecraft_version_profiles`: `1.21.11`,
     `1.21.9-1.21.10`, `1.21.6-1.21.8`, `1.21-1.21.5`,
     `1.20.5-1.20.6`, and `1.20-1.20.4`.
   - Cleared `candidate_minecraft_version_profiles` so the public publish set
     is exactly the supported `1.20.x` and `1.21.x` release matrix.
   - Updated release-facing compatibility docs and README metadata for the
     `3.0.0` supported profile set.
   - Verified `.\gradlew.bat ciValidation --no-daemon --console=plain`; the
     full supported matrix passed in 43m 17s across all six compatibility
     groups and all automated standalone/all-public smoke launches.
   - Committed and pushed the release sweep as `933925b`.
   - Triggered GitHub Actions run `26875020709` (`modrinth publish`) with
     `dry_run=false`, `version_type=release`, and `requested_status=listed`.
     The workflow passed in 49m52s and uploaded all 18 compatibility-group
     versions:
     - InvSort:
       - `3.0.0+mc1.21.11`: `v5sFbix1`
       - `3.0.0+mc1.21.9-1.21.10`: `jXDZPzIX`
       - `3.0.0+mc1.21.6-1.21.8`: `og828QKS`
       - `3.0.0+mc1.21-1.21.5`: `JJsp2SDN`
       - `3.0.0+mc1.20.5-1.20.6`: `6QWi2YRO`
       - `3.0.0+mc1.20-1.20.4`: `JtpNe6aB`
     - InvSearch:
       - `3.0.0+mc1.21.11`: `3a2ShEXs`
       - `3.0.0+mc1.21.9-1.21.10`: `wl0CFxZw`
       - `3.0.0+mc1.21.6-1.21.8`: `CJq6pDbY`
       - `3.0.0+mc1.21-1.21.5`: `8ylLOamo`
       - `3.0.0+mc1.20.5-1.20.6`: `ZK09KjA9`
       - `3.0.0+mc1.20-1.20.4`: `So21qEo1`
     - InvCatalogue:
       - `3.0.0+mc1.21.11`: `oWpB46Ap`
       - `3.0.0+mc1.21.9-1.21.10`: `DMUYgcx2`
       - `3.0.0+mc1.21.6-1.21.8`: `vG5mdvQA`
       - `3.0.0+mc1.21-1.21.5`: `i8J2pIat`
       - `3.0.0+mc1.20.5-1.20.6`: `XhGUQKCH`
       - `3.0.0+mc1.20-1.20.4`: `w8j4OR6w`
   - Public unauthenticated Modrinth API lookups for the uploaded project/version
     ids returned 404 immediately after upload, consistent with projects still
     awaiting Modrinth review/public visibility rather than a workflow upload
     failure.
   - Next: watch Modrinth review/public visibility, then begin the v26 migration
     lane when ready.

-25. Per-release Modrinth notes (unreleased):
   - Changed Modrinth publish automation to read release notes from
     `gradle/release-notes/<mod_version>.md` by default instead of pulling the
     whole `## Unreleased` section from `CHANGELOG.md`.
   - Publish tasks now fail if the per-version release note file is missing or
     blank. A custom notes file can still be supplied with
     `-Pmodrinth_changelog_file=<path>`.
   - Added `gradle/release-notes/2.6.4.md` for the validation release.
   - Updated `AGENTS.md`, `README.md`, and `gradle/modrinth-publishing.md` so
     future work keeps broad repo history in `CHANGELOG.md` and concise
     Modrinth-facing notes under `gradle/release-notes/`.
   - Completed by item 26, which creates the `3.0.0` release notes, bumps the
     mod version, promotes the smoke-passed compatibility groups, and starts the
     final publish sweep after user approval.

-24. Focused `2.6.4` Modrinth pipeline validation (unreleased):
   - Bumped `mod_version` from `2.6.3` to `2.6.4` for a focused validation
     upload on the currently supported `1.21.11` profile only.
   - Corrected the InvSearch Modrinth project id to `wIOLlhbN` from the latest
     project-id list.
   - Plan: run `publishModrinthDryRun`, commit and push the metadata change,
     then trigger the manual GitHub Actions Modrinth workflow using the
     repository `MODRINTH_TOKEN` secret.
   - Verified `.\gradlew.bat publishModrinthDryRun
     "-Pmodrinth_requested_status=unlisted" --no-daemon --console=plain`; it
     built `2.6.4`, smoke-launched the supported `1.21.11` profile, and
     prepared dry-run upload entries for the three public Modrinth projects.
   - Pushed commit `f42122c` and triggered GitHub Actions run
     `26871634493` (`modrinth publish`) with `dry_run=false`,
     `version_type=release`, and `requested_status=unlisted`.
   - GitHub Actions used the repository `MODRINTH_TOKEN` secret successfully,
     smoke-launched all four supported `1.21.11` install combinations, and
     uploaded:
     - InvSort `2.6.4`: Modrinth version `l8tVjMEf`
     - InvSearch `2.6.4`: Modrinth version `ITIdiJHe`
     - InvCatalogue `2.6.4`: Modrinth version `k3rNn4zA`
   - Kept `supported_minecraft_version_profiles=1.21.11` for this validation
     run. The broad `1.20.x`/`1.21.x` groups stayed candidates until the later
     `3.0.0` promotion in item 26.

-23. Modrinth publishing automation (unreleased):
   - Added Modrinth project IDs for InvSort, InvSearch, and InvCatalogue in
     non-secret Gradle properties.
   - Added `prepareModrinthUploads`, which runs the supported-only publish gate,
     validates supported-profile upload metadata, and writes
     `build/modrinth/upload-plan.json`.
   - Added `publishModrinthDryRun` for safe local/CI pipeline testing without
     calling the Modrinth API.
   - Added `publishModrinth`, which uploads to Modrinth only after
     `-Pmodrinth_confirm_publish=true` and a token from `MODRINTH_TOKEN` or a
     user-level Gradle property.
   - Added a manual GitHub Actions `modrinth publish` workflow with dry-run and
     real-publish modes.
   - Documented project IDs, token handling, version-number suffix behavior, and
     publishing options in `gradle/modrinth-publishing.md`.
   - Verified `.\gradlew.bat publishModrinthDryRun --no-daemon
     --console=plain`; it built and smoke-launched the supported `1.21.11`
     profile, then prepared dry-run upload entries for InvSort, InvSearch, and
     InvCatalogue without calling the Modrinth API.
   - Important: publishing reads `supported_minecraft_version_profiles` only.
     Candidate compatibility groups remain invisible to Modrinth automation
     until promoted.

-22. Fast supported-only publish validation gate (unreleased):
   - Added `smokeTestSupportedClients`, which launches the same Sort-only,
     Search-only, Catalogue-only, and all-public install combinations as the
     full smoke matrix, but only for supported/publishable profiles.
   - Added `publishValidation`, the fast pre-publish gate that builds supported
     profiles, checks smoke records, and smoke-launches supported profiles only.
   - Added `smokeTestSelectedClients` for local spot checks using
     `inventorysort_smoke_profiles`, `inventorysort_smoke_game_versions`, and
     `inventorysort_smoke_install_sets` filters.
   - Added `inventorysort_smoke_nested_no_daemon=false` as an optional local
     timing experiment for nested smoke Gradle launches. The default remains
     `--no-daemon` for predictable CI behavior.
   - `ciValidation` remains the exhaustive supported + candidate compatibility
     matrix and should still run before broad Modrinth compatibility promotion.

-21. Broad `1.20.x` and `1.21.x` compatibility groups (unreleased):
   - Added candidate release compatibility groups for:
     `1.21.6-1.21.8`, `1.21-1.21.5`, `1.20.5-1.20.6`, and
     `1.20-1.20.4`.
   - Added exact runtime-only smoke profiles for every `1.20.x` and `1.21.x`
     version covered by those grouped jars.
   - Added version shims for chest neighbor lookup, window handle access, HUD
     pose/matrix calls, single-player server directory return type, item stack
     identity/components vs tags, recipe book screen presence, mouse wheel
     method signatures, and older public `renderWidget` button overrides.
   - Updated automated smoke records after `ciValidation` launched Sort-only,
     Search-only, Catalogue-only, and all-three installs across the full
     candidate matrix.
   - Verified `.\gradlew.bat buildValidationVersions --no-daemon
     --console=plain`; all supported and candidate release profiles built.
   - Verified `.\gradlew.bat ciValidation --no-daemon --console=plain`; the
     full launch matrix passed across `1.21.11`, `1.21.9-1.21.10`,
     `1.21.6-1.21.8`, `1.21-1.21.5`, `1.20.5-1.20.6`, and `1.20-1.20.4`.
   - These broad groups were later promoted to supported/publishable Modrinth
     targets by item 26 for the `3.0.0` release.

-20. Automated client smoke validation (unreleased):
   - Added a no-mod `smokelaunch` Loom project that launches exact Minecraft
     runtimes with packaged release jars injected through Fabric Loader.
   - Added `InventorySortSmokeTest`, which arms only when
     `-Dinventorysort.smokeTest=true`, waits for the client tick loop, logs
     `INVENTORYSORT_SMOKE_TEST_PASS`, and closes the client cleanly.
   - `smokeTestValidationClients` now launches every validation profile/game
     version as Sort-only, Search-only, Catalogue-only, and all-three installs.
   - Added smoke runtime profiles for exact `1.21.9` and `1.21.10` launches so
     the grouped `1.21.9-1.21.10` release jars can be tested against both game
     versions without producing separate release jars.
   - Updated GitHub Actions to install `xvfb` and run
     `./gradlew ciValidation -Pinventorysort_smoke_xvfb=true` for headless
     client smoke launches.
   - Updated `gradle/smoke-tests.json` with passing automated smoke records for
     `1.21.11`, `1.21.9`, and `1.21.10`.
   - Verified `.\gradlew.bat smokeTestValidationClients`; all twelve automated
     client launches passed locally.
   - Superseded by item 21, which adds broad `1.20.x` and `1.21.x` candidate
     coverage and passes the full automated launch matrix.

-19. CI validation and smoke-test matrix foundation (unreleased):
   - Added `candidate_minecraft_version_profiles` so CI can build candidate
     profiles without marking them publishable.
   - Added `buildValidationVersions`, which builds every supported and candidate
     profile sequentially to avoid Loom cache/output races.
   - Added `ciValidation`, the GitHub Actions entrypoint for Step 8 validation.
   - Added `verifySmokeTestMatrix`, which blocks supported/publishable profiles
     unless every listed `modrinth_game_versions` entry has a passing smoke
     record.
   - Added `gradle/smoke-tests.json` with the tested `1.21.11` release target
     recorded as `pass`, and the `1.21.9-1.21.10` candidate versions recorded
     as `pending`.
   - Added `gradle/smoke-tests.md` documenting how smoke statuses promote a
     candidate profile into a supported profile.
   - Updated `.github/workflows/build.yml` to run `./gradlew ciValidation`
     instead of the single-profile `build` task.
   - This gives Step 9 a gate: Modrinth automation should only read/publish
     profiles from `supported_minecraft_version_profiles`, which must pass the
     smoke matrix check.
   - Verified `.\gradlew.bat ciValidation`; it builds both `1.21.11` and
     `1.21.9-1.21.10` release folders and reports the grouped candidate as
     pending/non-publishable.
   - Superseded by item 20, which turns this foundation into automated client
     smoke launches.

-18. Split module icon assets (unreleased):
   - Moved the dropped public mod icon images into
     `src/main/resources/assets/inventory-sort/`.
   - Inventory Sort now uses `assets/inventory-sort/invsort.jpg`.
   - Inventory Search now uses `assets/inventory-sort/invsearch.jpg`.
   - Inventory Catalogue now uses `assets/inventory-sort/invcatalogue.jpg`.
   - The legacy root metadata follows the Sort icon; Core keeps the generic
     shared icon because it is not a public standalone download.
   - `verifyReleaseJars` now fails if a public jar declares an icon path that is
     not packaged in the jar.
   - Verified `.\gradlew.bat clean build` and inspected the `1.21.11` release
     jars to confirm all public jars carry their own icon paths.

-17. `1.21.9-1.21.10` compatibility candidate group (unreleased):
   - Re-probed `1.21.9` with the current adapter shape and confirmed the focused
     compile passes.
   - Replaced the exact `1.21.10` candidate profile with
     `gradle/version-profiles/1.21.9-1.21.10.properties`.
   - The grouped candidate is anchored on `minecraft_version=1.21.9`, uses
     `compat_group=1.21.9-1.21.10`, and declares
     `minecraft_dependency=>=1.21.9 <=1.21.10`.
   - Renamed the shared API-shape overlay from `src/compat/1.21.10` to
     `src/compat/1.21.9-1.21.10`.
   - Verified `.\gradlew.bat printVersionProfile
     "-Pminecraft_version_profile=1.21.9-1.21.10"` and
     `.\gradlew.bat clean buildAllMods
     "-Pminecraft_version_profile=1.21.9-1.21.10"`.
   - Verified grouped release jars collect under
     `build/release/1.21.9-1.21.10/` and declare
     `minecraft >=1.21.9 <=1.21.10`, `java >=21`, and
     `fabricloader >=0.18.4`.
   - Important: this grouped profile is still not supported/publishable until
     the exact grouped jars pass launcher smoke testing on both `1.21.9` and
     `1.21.10`.
   - Next Step 7 slice: launcher smoke-test the grouped jars on `1.21.9` and
     `1.21.10`, then probe `1.21.8` to find the next API boundary.

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
   - Superseded by item 17 after `1.21.9` compiled with the same adapter shape,
     allowing a grouped `1.21.9-1.21.10` candidate.

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
   - DROPPED FROM ACTIVE BUGS (2026-06-08): accepted as future accuracy work,
     not a current bug-fix target.
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
   - DROPPED FROM ACTIVE BUGS (2026-06-08): accepted as a current implementation
     limitation until a deliberate portable-container model exists.
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
2. 🟠 DROPPED FROM ACTIVE BUGS (2026-06-08): Click-storm desync risk is an
   accepted current implementation limitation. Sorting/transfers drive hundreds of synchronous
   `slotClicked` packets (compact→restack→apply→restack→compact; shift-all quick-
   moves every slot). Risk of ghost items / rollback / kicks on rate-limited
   servers. Consider throttling/batching or a known-limitation note.
3. 🟡 DONE (3.2.0 queued): Removed the `"Crafting"` name match from
   `InventorySorter` container detection, closing the latent crafting-result
   slot trap if buttons are ever exposed on a crafting-like screen.
4. 🟡 DONE (3.2.0 queued): Dead/misleading bundle helper code:
   - Removed unused `fillPlayerStacksFromContainer`.
   - Removed misleading `findFirstEmptyNonBundle`.
   - Removed redundant bundle branches in `ensureCursorEmpty`.
5. 🟢 DROPPED FROM ACTIVE BUGS (2026-06-08): "Sort container" also tops up the
   hotbar from main inventory first (`:34-40`) as intentional behavior.

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
5. 🟡 DONE (3.2.0 queued): World-confirmation now uses registered keybindings
   for the confirm/open-profile actions instead of raw GLFW ENTER/BACKSPACE
   polling. Core uses a compat bridge for the 1.x `KeyBindingHelper` API and
   the 26.x `KeyMappingHelper` API.

### Catalogue segment (new code, self-review)

1. 🟡 DROPPED FROM ACTIVE BUGS (2026-06-08): Empty containers are recorded as
   zero-item snapshots, inflating "locations catalogued" without adding items.
   This is accepted for now.
2. 🟡 DROPPED FROM ACTIVE BUGS (2026-06-08): Inherits identical-shulker
   collision: portable shulkers keyed by a 5-slot
   content hash (`ContainerTrackingMixin.generateContainerHash`), so two identically
   filled shulkers collide and count once. This is accepted as a current
   implementation limitation.
3. 🟢 DONE (3.2.0 queued): `CatalogStore.save()` now writes through a temp-file
   swap, keeps a `.bak` copy, and attempts the backup before starting fresh on
   parse/load failure.
4. 🟢 No in-game catalogue GUI yet (command-only). Natural future feature reusing
   the search modal styling.

### Performance

1. 🟠 DONE (3.2.0 queued): Inventory sampler disk churn is reduced by debouncing
   inventory-total snapshot writes, periodically flushing sustained changes, and
   flushing pending state during the Search shutdown hook.

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
   - Current status: DONE for broad `1.20.x` and `1.21.x` supported coverage.
     Gradle supports compatibility-group profile metadata, profile-id release
     folders, generated Fabric dependency ranges, profile metadata verification,
     and `src/compat/<compat_group>/` source overlays. Grouped jars now compile,
     build, and pass automated client smoke launches for
     `1.21.9-1.21.10`, `1.21.6-1.21.8`, `1.21-1.21.5`,
     `1.20.5-1.20.6`, and `1.20-1.20.4`. They were promoted into
     `supported_minecraft_version_profiles` for the `3.0.0` release.
     `1.19.x` and older are intentionally out of scope for now.
     The next compatibility lane is `26.x`; use the dedicated forward roadmap
     below rather than moving the shared baseline to `26.x`.
8. Add CI validation before publishing:
   - Add automated build verification for every supported compatibility-group
     profile.
   - Run unit tests where possible and keep `verifyReleaseJars` in the CI path.
   - Add launcher smoke tests for every Minecraft version listed by a profile's
     `modrinth_game_versions`; only those passing versions may be published.
   - CI should produce build artifacts grouped by profile/range so manual testing
     and Modrinth upload metadata stay aligned.
   - Current status: DONE for the current validation matrix. `ciValidation` now
     builds supported profiles and any configured candidate profiles, verifies
     release jar metadata and nested Core jars, checks smoke-test records, and
     launches exact Minecraft runtimes with the packaged release jars as
     standalone and all-three installs. `publishValidation` is now available as
     the fast supported-only gate before Modrinth publishing. Candidate profiles
     are tracked separately from publishable supported profiles.
9. Configure Modrinth publishing:
   - Give each public feature mod its own Modrinth project id and upload metadata.
   - Publish the correct jar, Minecraft version, loader, dependencies, and
     changelog for each target.
   - Current status: DONE for the `3.0.0` public release. Gradle prepares and
     dry-runs Modrinth uploads for supported profiles only, with a guarded real
     upload task and manual GitHub Actions workflow. Candidate profiles are
     ignored by publishing until promoted into
     `supported_minecraft_version_profiles`.

## Minecraft 26.x Forward Compatibility Roadmap

Goal: keep one repository and one shared Core/feature codebase. The current
shared source remains anchored to the smoke-tested `1.20.x`/`1.21.x` baseline;
`26.x` is added as forward candidate compatibility profiles. Do not move the
shared baseline to `26.x` unless forward overlays prove too fragile.

Known `26.x` constraints:

- Minecraft `26.x` requires Java 25.
- Mojang removed the obfuscation/mapping layer for `26.x`, so `26.x` profiles
  need non-remapping Fabric Loom (`net.fabricmc.fabric-loom`) while `1.20`/`1.21`
  profiles still use `net.fabricmc.fabric-loom-remap`.
- `26.x` profiles should use normal `implementation` dependencies and `jar`
  release artifacts instead of remapped `modImplementation` and `remapJar`
  artifacts.
- The current compile probe reached source/API errors after a temporary
  non-remap build-path change, which means one-repo forward support still looks
  plausible.
- Representative `26.x` API shifts observed in the probe:
  - `GuiGraphics` render methods become `GuiGraphicsExtractor` / render-state
    extraction methods.
  - `HudRenderCallback` moves to the Fabric HUD element registry API.
  - `ClientCommandManager` becomes `ClientCommands`.
  - `ClickType` becomes `ContainerInput`.
  - Player chat feedback uses `sendSystemMessage` / `sendOverlayMessage` instead
    of `displayClientMessage`.

Forward tasks:

1. Build-system foundation:
   - Add a profile flag such as `unobfuscated_minecraft=true` for `26.x`
     profiles.
   - Load both Loom plugin ids in `settings.gradle`, then select remap vs
     non-remap in root/subprojects by profile.
   - Skip `loom.officialMojangMappings()` for `26.x` profiles.
   - Select `modImplementation`/`remapJar` for remapped profiles and
     `implementation`/`jar` for unobfuscated profiles.
   - Replace `namedElements` project dependencies with normal/client output
     dependencies when running `26.x` profiles.
   - Verify default `1.21.11` `buildAllMods` still passes and a `26.x`
     `printVersionProfile` configures under Java 25.
   - Current status: DONE. Profile metadata now selects remap vs non-remap Loom,
     Fabric dependency configuration, release artifact task, and Core project
     dependency shape. Default `1.21.11` still builds, and both `26.x` candidate
     profiles configure through the non-remap lane.
2. Java 25 toolchain and CI:
   - Decide whether Gradle should always run on Java 25 or whether workflows
     should select Java by profile/job.
   - Update GitHub Actions/manual validation so `26.x` candidate builds can run
     without breaking `1.20`/`1.21` release builds.
   - Keep normal push/PR builds fast; use full `26.x` validation only in
     candidate validation/publish workflows.
   - Current status: DONE. Compile/run tasks now request the active profile's
     Java toolchain. Normal push/PR builds remain on Java 21/default profile,
     the manual compatibility-validation workflow defaults to Java 25 for
     focused `26.x` candidate builds, and Modrinth publishing can be switched to
     Java 25 when a `26.x` profile is promoted.
3. `26.x` Core compatibility overlay:
   - Add `src/compat/26.1.2/.../MinecraftApiCompat.java`.
   - Port dimension id, chest neighbor lookup, window handle, single-player
     directory, HUD pose, and player feedback helpers to `26.x` names.
   - Add Core compile probing as the first validation target.
   - Current status: DONE for the helper/adapter layer. `26.1.2` and
     `26.2-pre-3` now have Core compatibility overlays for Minecraft helper
     calls and the Fabric command builder rename, and shared player feedback
     now goes through `MinecraftApiCompat`. The `26.1.2` Core compile probe
     then stopped at the expected GUI/render extraction, HUD registry, and
     container input blockers. GUI/HUD/screen blockers were resolved by item 33.
4. GUI/rendering abstraction:
   - Refactor shared drawing helpers so shared logic does not directly depend on
     `GuiGraphics`.
   - Add version-specific wrappers/adapters for `1.20`/`1.21` `GuiGraphics` and
     `26.x` `GuiGraphicsExtractor`.
   - Port text buttons, icon buttons, modal buttons, HUD, and profile/search
     screens through those adapters.
   - Current status: DONE for Core and shared drawing helpers. `InventorySortDrawContext`
     now isolates shared renderers from Minecraft's render object, Core HUD
     registration is versioned, and `26.x` profile-screen/button overlays use
     the new extraction lifecycle. Search's full `26.x` modal-screen lifecycle
     remains for the Search feature compile pass.
5. Fabric API event/command updates:
   - Replace or wrap `HudRenderCallback` with the `26.x` HUD element registry.
   - Replace or wrap `ClientCommandManager` with `26.x` `ClientCommands`.
   - Keep command bodies/shared handlers version-independent.
   - Current status: DONE. Command builder calls are wrapped with
     `ClientCommandCompat`, HUD registration is wrapped with `HudCompat`, and
     shared command-registration/tick-event callbacks still exist in the checked
     `26.x` Fabric API jars.
6. Container/mixin input updates:
   - Port `ClickType` invokers and slot-click mixins to `26.x`
     `ContainerInput`.
   - Re-check `AbstractContainerScreen` render/extract method names and
     recipe-book integration.
   - Keep sort/search/catalogue behavior code out of version-specific mixins.
   - Current status: DONE for Core and Sort compile. Sort click behavior now
     calls a version-selected `ContainerClickCompat`, `26.x` invokers use
     `ContainerInput`, and `26.x` Sort button mixins hook
     `extractRenderState`. Default `buildAllMods` still passes, and
     `26.1.2`/`26.2-pre-3` Core plus Sort compile probes pass.
7. Feature compile passes:
   - After Core compiles, compile Sort.
   - Then compile Search.
   - Then compile Catalogue.
   - Treat each feature pass as its own checkpoint with TODO/CHANGELOG/commit.
   - Current status: DONE. Core, Sort, Search, and Catalogue compile on the
     shared `26.x` release profile, which now replaces the old duplicated
     `26.1.2` and `26.2-pre-3` publish profiles.
8. `26.x` smoke testing:
   - Add pending/pass smoke records for exact `26.x` runtimes.
   - Extend the smoke launcher if `26.x` client launch semantics differ.
   - Run selected `26.x` smoke tests first; run the full release validation
     matrix during promotion/publish once the supported publish lane changes.
   - Current status: DONE. The grouped `26.1-26.2-pre-3` release jar passed
     automated smoke launches on exact runtimes `26.1`, `26.1.1`, `26.1.2`,
     and `26.2-pre-3`. Each runtime passed Sort-only, Search-only,
     Catalogue-only, and all-three public install sets.
9. Publication promotion:
   - Keep `26.x` in `candidate_minecraft_version_profiles` until compile and
     smoke tests pass.
   - Once passing, promote it to `supported_minecraft_version_profiles`.
   - Add `gradle/release-notes/<version>.md`, run the Modrinth publish workflow,
     and list the smoke-passed `26.x` versions.
   - Current status: DONE. The smoke-passed `26.x` profiles were promoted for
     `3.1.0`, and the guarded GitHub Actions Modrinth workflow passed with
     listed uploads for all six public `26.x` versions.
10. Branch fallback trigger:
   - Branch only if `26.x` forces duplicated feature logic rather than isolated
     build config, rendering adapters, and mixin surfaces.
   - If branching becomes necessary, keep `main` as the current `1.20`/`1.21`
     release lane and create an explicit `26.x` development branch with a
     patch-forward process.

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
