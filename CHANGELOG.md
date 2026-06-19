# Changelog

All notable project changes will be documented here.

## Unreleased

### Added

- Added a first-pass InvSort rules screen, opened by right-clicking the Sort
  button, for configuring player-inventory rules and container rules in game.
- Added InvSort custom category ordering, custom item-id ordering, protected
  slots, item-specific slots, and local JSON persistence with world-scoped
  container defaults plus per-container/per-screen overrides.
- Added `/inventorycatalogue reports`, an in-game InvCatalogue report browser
  that groups saved reports by world/profile, opens a visual item-count grid
  with icons and search filtering, and writes structured JSON report snapshots
  alongside the existing plain-text reports.
- Added per-version hitbox-only button shims and 26.x
  `GuiGraphicsExtractor` overlays so the new Catalogue report browser follows
  the existing GUI lifecycle boundaries across supported profile groups.
- Added a shared `InvUi` theme helper (operating on the existing
  `InventorySortDrawContext` abstraction) that centralizes the modern dark
  window, panel, slot, row, field, tab, chip, and scrollbar drawing for all
  three feature screens across the 1.x and 26.x rendering lanes.
- Added `:category` search syntax to InvSearch and the InvCatalogue saved-report
  item filter, so queries such as `:wood`, `:stone`, `:tools`, `:gear`, and
  `:storage` match the corresponding practical item categories.
- Added source-controlled Modrinth gallery assets for InvSort, InvSearch, and
  InvCatalogue, including image metadata, banner selectors, description-image
  selectors, and a repeatable project-page/gallery sync script plus manual
  GitHub workflow.
- Added Tempest Studios app-data roots for persistent InvSort, InvSearch,
  InvCatalogue, and shared Core data, plus a migration registry that records
  each legacy instance-file import with the installed mod versions and import
  timestamp.
- Added the shared Core `InventoryScreenButtonSlots` API for reserving,
  positioning, and inspecting ordered right-side button slots on inventory and
  container screens.
- Added `InventoryScreenButtonSlots` capacity helpers for available slots,
  remaining slots, next-slot fit checks, and placement fit reporting, plus a
  dedicated button-slot API guide that documents first-party owner ids, slot
  ids, priorities, and screen conditions.

### Changed

- Bumped the queued patch lane to `3.2.3` for coordinated right-side inventory
  button placement.
- Bumped the patch lane to `3.2.1` for the recipe-book button offset regression
  fix and launcher/instance-hardened app-data storage migration.
- Bumped the queued patch lane to `3.2.2` for world-scoped InvSort rule
  persistence.
- Scoped InvSort player-inventory rules, container defaults, screen overrides,
  and exact-container overrides to the active tracking world/profile instead of
  keeping new rules global; old-shape rule files are migrated into the
  namespace-keyed `worldRules` schema on first use.
- Published `3.2.2` through guarded GitHub Actions workflow `27502873676`,
  uploaded all 24 Modrinth compatibility-group versions, and created the
  annotated `v3.2.2` GitHub Release on source commit
  `1729c0135d6f15d7f10b6da9ed7b9990bc368ce4` without attaching GitHub jar
  assets.
- Consolidated duplicate 1.x `InventorySortDrawContexts`,
  `ClientCommandCompat`, and `HudCompat` compat wrappers into shared client
  source while the then-current 26.x lane stayed on its overlay path, then
  cleared and rebuilt the generated `build/release` artifacts with
  `buildAllVersions`.
- Moved active persistent data out of `.minecraft/inventorysort` and into
  per-feature app-data folders (`InvSort`, `InvSearch`, `InvCatalogue`, and
  `InvCore`), with first-launch legacy copy/import from the old instance-local
  folder.
- Scoped single-player tracking namespaces by launcher-instance id so same-name
  worlds in different instances do not collide in the shared app-data store,
  while multiplayer server/profile namespaces remain shared across instances
  only for the same Minecraft account.
- Raised the public release jar size sentinel to allow the intentional shared
  Core storage/migration footprint while keeping the release-jar junk-file and
  embedded-Core verification checks in place.
- Unified the InvSort rules, InvSearch results/world, and InvCatalogue report
  screens under one modern dark theme with a per-mod accent (gold for InvSort,
  blue for InvSearch, green for InvCatalogue) so the three menus read as one
  suite instead of the previous mismatched grey and ad-hoc dark panels.
- Restyled the shared text and modal icon button renderers to the new flat
  theme, so every menu in the suite picks up consistent buttons.
- Reworked the InvSort rules screen into `Slots` and `Order` tabs with an
  explicit scope selector, a live slot selection/legend panel, and
  tooltip-labelled `Protect` / `Assign Item` / `Clear` actions.
- Restyled InvSearch results as card rows with colour-coded counts (held in
  your inventory versus tracked elsewhere) and an accent-barred locations
  panel, and matched the tracked-world selector to the same theme. The search
  list redraws its frame over scrolled rows so partial rows stay contained, and
  the world selector sizes its list to the available height (no more overlapping
  controls), marks the active world, and shows each world's last-used time. The
  last-used time is recorded from active play (the shared Core stamps the
  tracked world about once a minute while tracking is confirmed) and on
  selection, so it fills in for each world as you play or switch to it.
- Restyled the InvCatalogue browser report cards and world section bands plus
  the detail item grid and selected-item sidebar, adding a share bar for an
  item's portion of the report total.
- Synchronized the Minecraft 26.x screen overlays with the shared `InvUi`
  redesign so InvSort, InvSearch, the tracked-world picker, and InvCatalogue
  use the same refreshed UI across the 1.x and 26.x rendering lanes.
- Shared the InvSort category vocabulary through Core so standalone InvSearch
  and InvCatalogue jars can use category matching without depending on the
  public InvSort feature jar.
- Moved InvSort and InvSearch screen buttons onto shared priority-ordered
  right-side slot reservations, including the Minecraft 26.x render-state
  overlays, so standalone InvSearch can occupy the top inventory button slot
  when InvSort is not installed.
- Collapsed the duplicated Minecraft 26.x publish lane into one `26.x` build
  profile and one shared `src/compat/26.x` overlay, with exact 26.x runtime
  profiles retained only for smoke testing.
- Hardened automated smoke launches with a smoke-only in-client watchdog so a
  hosted client stall fails with `INVENTORYSORT_SMOKE_TEST_TIMEOUT` instead of
  leaving the guarded publish workflow running indefinitely.
- Raised the public release jar size sentinel to allow the intentional embedded
  Core footprint from the shared button-slot API while keeping the existing
  junk-file and embedded-Core verification checks active.
- Added a `docs/README.md` developer-doc index, linked the button-slot API guide
  from `AGENTS.md` and `README.md`, and renamed the TODO heading to the current
  Inventory Mods suite name.
- Bumped the queued minor release to `3.2.0` for validation and publishing.
- Refreshed the source-of-truth Modrinth project descriptions to explain the
  new InvSort rules screen, InvSearch category search, and InvCatalogue report
  browser, with selected gallery images embedded as separate page sections.
- Synced the live Modrinth project pages and galleries from the repo copy,
  uploading 9 InvSort images, 5 InvSearch images, and 7 InvCatalogue images.
- Published `3.2.0` through guarded GitHub Actions workflow `27131728812`,
  uploaded all 24 Modrinth compatibility-group versions, and created the
  annotated `v3.2.0` GitHub Release on the exact source commit used for the
  jars.
- Corrected the live InvSort `Rules: Inventory Order` Modrinth gallery and
  description screenshot after replacing the source image in `gallery/`.
- Published `3.2.1` through guarded GitHub Actions workflow `27409085355`,
  uploaded all 24 Modrinth compatibility-group versions, and created the
  annotated `v3.2.1` GitHub Release on source commit
  `878286ebde96d45d890366c0b2e350f5d080a377` without attaching GitHub jar
  assets.

### Fixed

- Fixed the recipe-book button offset regression on player inventory and
  crafting screens so the InvSort and InvSearch buttons recalculate against the
  shifted vanilla GUI position when the recipe book opens, including the
  Minecraft 26.x `GuiGraphicsExtractor` render-state path.
- Hardened `verifyReleaseJars` so public feature jars and their embedded Core
  jars fail verification if project docs, source files, build scripts, or
  source-art formats are accidentally packaged.
- Fixed shared category classification for the new `:category` searches so
  redstone items and redstone dust are matched by `:redstone` instead of being
  swallowed by the broad stone-name terrain rule, while dusts such as
  glowstone dust no longer appear under `:stone`.
- Fixed the tracked-world picker mouse-wheel handler so it only scrolls the
  saved-world list when the cursor is over that list, including the Minecraft
  26.x screen overlays.
- Delegated the legacy `InventorySorter.categoryKey` entry point to the shared
  `InventorySortCategories` helper so custom sorting, InvSearch, and
  InvCatalogue category filters cannot drift onto separate classification
  rules.
- Fixed the regenerated 26.x InvSearch screen overlay to use
  `MinecraftApiCompat.setScreen`, preserving the `26.2-pre-3`
  `client.gui.setScreen(...)` path after Minecraft removed `Minecraft#setScreen`.
- Fixed InvCatalogue report-browser layout overflow by clipping and scrolling
  the selected-item detail pane, reserving more top summary space, and keeping
  scrolled item-grid tiles inside the grid frame.
- Fixed a Minecraft `1.21.11` launch crash in the shared Core world-profile
  keybinding shim by removing named Minecraft class-string reflection from the
  `KeyMapping.Category` path and discovering the live runtime constructor
  signatures instead.
- Fixed the InvCatalogue report browser command so opening it from chat is
  queued after the chat screen closes, and `/inventorycatalogue report` with no
  active session now opens saved reports instead of dead-ending on an error.
- Hardened InvSearch and InvCatalogue persistence by writing tracker/catalogue
  JSON through temp-file swaps, keeping `.bak` copies, and recovering from
  malformed primary files without crashing client startup.
- Reduced InvSearch inventory-tracker disk churn by debouncing player-inventory
  snapshot writes, periodically flushing sustained changes, and flushing pending
  inventory history during the Search shutdown hook.
- Replaced shared world-profile raw Enter/Backspace polling with registered
  remappable keybindings, using a Core compat bridge for the older Fabric
  `KeyBindingHelper` API and the 26.x `KeyMappingHelper` API.
- Removed the latent InvSort `"Crafting"` screen-name match so crafting-like
  screens are not treated as sortable containers if buttons are exposed there.
- Fixed InvSort sorting when bundles are present by moving bundle stacks to the
  front of the selected sortable region with a hotbar-buffer swap before sorting
  the non-bundle items behind them.
- Reduced public release jar sizes by packaging only each module's declared icon
  asset, shrinking the per-mod icons to metadata-sized JPGs, and adding a
  release-jar size guard so public jars must stay below `199000` bytes.
- Fixed the Minecraft `26.x` Core entity-interaction mixin for
  `MultiPlayerGameMode`: `26.x` exposes a single `interact(Player, Entity,
  EntityHitResult, InteractionHand)` method instead of the older separate
  `interact`/`interactAt` methods, which could crash on launch when another mod
  eagerly loaded that client class.
- Fixed generated Mixin configs to use each compatibility profile's Java
  target instead of always declaring `JAVA_21`, allowing Java 17 Minecraft
  `1.20.x` smoke launches to initialize correctly.
- Hardened automated smoke testing by force-loading
  `net.minecraft.client.multiplayer.MultiPlayerGameMode` during smoke startup,
  so lazy mixin target failures are caught by the launcher gate.

### Changed

- Revised the InvSort rules screen to fit scaled Minecraft viewports, show one
  category/item-order list at a time, use clearer `Protect`/`Item Slot` slot
  actions, and support Ctrl-click plus Shift-click slot multi-selection.
- Extended the versioned icon-button shims with an optional secondary-click
  action so the InvSort rules menu can respect the mouse-input API changes from
  older `double,double,int` clicks to newer `MouseButtonEvent` clicks and the
  26.x render/input lane.
- Changed InvCatalogue report-browser item-grid count badges to use rounded
  `K`/`M`/`B` abbreviations from `1K` upward while keeping exact selected-item
  totals in the sidebar.
- Restyled the InvCatalogue report browser with darker modern-Minecraft-style
  chrome, slot-style item tiles, and local Catalogue back/close controls so it
  no longer visually follows the older InvSearch modal theme.
- Added Sort compatibility shims for hotbar swap clicks and moved bundle
  detection behind per-profile `ItemStackCompat` overlays, including the 26.x
  `ContainerInput.SWAP` path.
- Pruned the split-mod bug intake based on user decisions, added the InvSort
  bundle sorting bug, and captured the requested InvSort custom sorting/slot
  rules plus InvCatalogue GUI/snapshot-comparison feature tracks.
- Added a split-mod bug report intake checkpoint to `TODO.md`, covering
  InvSort, InvSearch, and InvCatalogue ownership, Core/shim boundaries, and
  first-pass code-audit leads for the next user-reported fixes.
- Created the GitHub `v3.1.3` tag and release for the icon refresh, pointing
  downloads to the published Modrinth versions.
- Published the `3.1.3` icon refresh release through the guarded GitHub
  Actions Modrinth workflow, uploading all 24 listed compatibility-group
  versions after the full smoke matrix passed.
- Bumped the icon refresh release to `3.1.3`, replaced the packaged public mod
  icons, and updated the live Modrinth project icons for all three public mods.
- Updated the GitHub repository About metadata with the current split-mod
  summary, InvSort Modrinth homepage, and repository topics.
- Created the GitHub `v3.1.2` tag and release that points downloads to the
  published Modrinth versions, and documented GitHub release maintenance for
  future Modrinth publishes.
- Published the `3.1.2` packaging cleanup release through the guarded GitHub
  Actions Modrinth workflow, uploading all 24 listed compatibility-group
  versions after the full smoke matrix passed.
- Bumped the queued packaging cleanup release to `3.1.2` so the smaller jars
  and refreshed metadata summaries can be published across all supported
  Minecraft compatibility groups.
- Added source-of-truth Modrinth project page copy for all three public mods
  and refreshed their metadata summaries used by loaders and mod lists.
- Updated project onboarding and release-process documentation so a fresh agent
  can find the right workflow, verification commands, release-note rules, and
  reusable compatibility-release plan without reconstructing the chat history.
- Promoted all smoke-passed Minecraft `1.20.x`, `1.21.x`, and `26.x`
  compatibility groups into one unified `3.1.1` supported publish lane,
  rebuilt the local release artifacts for all eight groups, and verified the
  Modrinth dry-run plan contains 24 uploads across InvSort, InvSearch, and
  InvCatalogue.
- Published the unified `3.1.1` release through the guarded GitHub Actions
  Modrinth workflow, uploading all 24 listed versions after the full smoke
  matrix passed.
- Added Java 17 toolchain discovery and GitHub Actions setup alongside Java 21
  and Java 25 so the full `3.1.1` publish matrix can build locally and in the
  guarded Modrinth workflow.
- Bumped the Minecraft `26.x` hotfix lane to `3.1.1` and added focused
  Modrinth-facing release notes for the launch-crash fix.
- Updated README, compatibility, and version-profile documentation to point at
  the active `3.1.1` 26.x publish lane after the hotfix.
- Researched and configured the Minecraft `26.x` candidate range plan before
  smoke testing: `26.1.2` now builds one grouped `26.1-26.1.2` candidate jar
  with exact `26.1`, `26.1.1`, and `26.1.2` smoke runtime profiles, while
  `26.2-pre-3` remains an exact provisional candidate until later 26.2 metadata
  supports a wider range.
- Completed the Minecraft `26.x` automated smoke-test step for candidate
  profiles: the grouped `26.1-26.1.2` jar now smoke-launches on exact runtimes
  `26.1`, `26.1.1`, and `26.1.2`, and the exact `26.2-pre-3` jar
  smoke-launches after matching Fabric API's prerelease `minecraft` dependency
  range.
- Promoted the smoke-passed Minecraft `26.x` profiles into the `3.1.0`
  supported publish lane: `26.1.2` now publishes the grouped
  `26.1-26.1.2` jar, `26.2-pre-3` publishes the exact pre-release jar,
  candidate profiles are cleared, and the public release notes now describe the
  validated 26.x versions.
- Published all six `3.1.0` Minecraft `26.x` versions through the guarded
  GitHub Actions Modrinth workflow with `requested_status=listed`.
- Added Java toolchain handling and manual CI support for Minecraft `26.x`
  migration work: compile/client-run tasks now request the active profile's
  `java_version`, GitHub Actions has a focused Java 25 compatibility-validation
  workflow, and the Modrinth publish workflow can select Java 21 or Java 25.
- Added Minecraft `26.x` Core helper compatibility overlays for Minecraft API
  helper calls and the Fabric command builder rename, and routed shared command
  builders/player feedback through version-selected adapters. The focused
  Core compile probe now gets past those helper breaks.
- Added the Minecraft `26.x` Core GUI/rendering abstraction: shared renderers now
  draw through `InventorySortDrawContext`, version overlays wrap `GuiGraphics`
  or `GuiGraphicsExtractor`, HUD registration is versioned, and 26.x profile
  screens/buttons use the extraction lifecycle. Focused `26.1.2` and
  `26.2-pre-3` Core compile probes now stop only at the planned
  `ClickType`/`ContainerInput` invoker work.
- Verified the Minecraft `26.x` Fabric API event/command update step:
  command-builder calls are behind `ClientCommandCompat`, HUD registration is
  behind `HudCompat`, and shared command-registration/tick-event callbacks still
  exist in the checked `26.x` Fabric API jars.
- Added Minecraft `26.x` container/mixin input adapters: shared Sort logic now
  calls a version-selected `ContainerClickCompat`, existing `1.20.x`/`1.21.x`
  builds keep `ClickType`, and `26.1.2`/`26.2-pre-3` overlays use
  `ContainerInput`. The 26.x Sort button mixin now hooks `extractRenderState`,
  and feature modules compile against Core's client jar in the non-remap lane.
  Focused `26.1.2` and `26.2-pre-3` Core plus Sort compile probes now pass.
- Added Minecraft `26.x` Search overlays for the modal screen and screen-button
  mixin. Search rendering now compiles against the `GuiGraphicsExtractor`
  `extractRenderState` lifecycle on `26.1.2` and `26.2-pre-3`, while the shared
  `GuiGraphics` implementation remains in place for `1.20.x`/`1.21.x`.
- Completed the Minecraft `26.x` feature compile pass across Core, Sort, Search,
  and Catalogue. Candidate `26.1.2` and `26.2-pre-3` profiles now pass
  `buildAllMods`, including public jar collection and metadata verification; the
  source-jar task now applies the same profile-driven shared-source exclusions as
  client compilation.
- Added profile-selected remap vs non-remap build plumbing for Minecraft
  `26.x`: `unobfuscated_minecraft=true` profiles now use
  `net.fabricmc.fabric-loom`, normal `implementation` dependencies, plain `jar`
  release artifacts, and normal Core project dependencies while the existing
  `1.20.x`/`1.21.x` profiles keep the remapped Loom path.
- Added `gradle/release-notes/3.1.0.md` as the running user-facing Modrinth
  changelog for the planned Minecraft `26.x` support release, and clarified in
  `AGENTS.md` that `CHANGELOG.md` remains the repo-facing engineering history.
- Recorded the Minecraft `26.x` forward-compatibility roadmap in `TODO.md`,
  keeping shared source anchored to the proven `1.20.x`/`1.21.x` baseline while
  adding `26.x` as forward candidate profiles with Java 25, non-remap Loom,
  Core API, GUI/rendering, command/HUD, container/mixin, smoke-test, and
  promotion tasks.
- Changed the repo and Fabric metadata license to LGPL-3.0-only to match the Modrinth projects.
- Added `AGENTS.md` with the project workflow for updating `TODO.md`, updating this changelog, verifying, and committing after each major change.
- Recorded the split and multi-version migration roadmap in `TODO.md` so it survives future context compaction.
- Added the initial Core foundation with shared mod id/logger ownership and event contracts for namespace changes, container snapshots, and inventory snapshots.
- Routed container/inventory snapshot handling and namespace-change handling through Core events, with Search and Catalogue now subscribed via feature bridge classes.
- Split catalogue and world-profile command implementation into separate command classes.
- Moved split-mod commands to feature-specific roots: `/inventorycatalogue ...`, `/inventorycatalogue world ...`, and `/inventorysearch world ...`.
- Split sort/search screen-button ownership and added separate Core/Search/Catalogue client entrypoints as preparation for public modules.
- Added Gradle subprojects for Core, Sort, Search, and Catalogue. The three public feature jars now build separately and each nests the shared Core jar.
- Added `collectReleaseJars` and updated `buildAllMods` so publish-ready Sort, Search, and Catalogue jars are collected under `build/release/<profile_id>/`.
- Added Minecraft version profiles and upgraded the Gradle wrapper to 9.4.0 so 26.x migration profiles can be configured separately from the current release target.
- Extended Minecraft version profiles into compatibility-group build profiles with
  `profile_id`, `minecraft_dependency`, `modrinth_game_versions`, and
  `compat_group` metadata.
- Release jars now collect under `build/release/<profile_id>/`, and
  `verifyReleaseJars` checks generated Fabric metadata against the active profile.
- Added `src/compat/<compat_group>/` overlay wiring for version-specific API
  adapter sources.
- Added separate public mod icon assets for Inventory Sort, Inventory Search,
  and Inventory Catalogue, and wired each module's Fabric metadata to its own
  icon.
- Added CI validation tasks for supported and candidate Minecraft profiles,
  including a smoke-test matrix gate for publishable profiles.
- Added automated client smoke launches through a dedicated `smokelaunch`
  project. CI now launches packaged release jars as Sort-only, Search-only,
  Catalogue-only, and all-three installs across the validation runtimes.
- Added a `1.21.9-1.21.10` candidate compatibility profile that compiles and
  builds release jars with range metadata, and now passes automated client
  smoke launches on both `1.21.9` and `1.21.10`.
- Added broad candidate compatibility groups for `1.21.6-1.21.8`,
  `1.21-1.21.5`, `1.20.5-1.20.6`, and `1.20-1.20.4`, with exact runtime smoke
  profiles for every covered `1.20.x` and `1.21.x` version.
- Added compatibility shims for older Minecraft API shapes, including chest
  neighbor lookup, window handle access, HUD pose/matrix calls, single-player
  server directory paths, item stack components vs tags, recipe book screen
  presence, mouse wheel method signatures, and older button render overrides.
- Recorded passing automated smoke results for the full `1.20.x` and `1.21.x`
  candidate matrix, then promoted those smoke-passed profiles to supported for
  the `3.0.0` release.
- Added `publishValidation` and `smokeTestSupportedClients` as a fast
  supported-only pre-publish gate, plus `smokeTestSelectedClients` filters for
  local smoke-test spot checks.
- Added guarded Modrinth publishing automation for the three public projects,
  including dry-run upload planning, supported-profile-only publishing, token
  handling through `MODRINTH_TOKEN`, and a manual GitHub Actions workflow.
- Bumped the focused Modrinth validation release to `2.6.4` and corrected the
  InvSearch project id used by publishing automation.
- Validated the guarded GitHub Actions Modrinth publish workflow by uploading
  unlisted `2.6.4` versions for InvSort, InvSearch, and InvCatalogue on
  Minecraft `1.21.11`.
- Changed Modrinth publishing to use concise per-version release notes from
  `gradle/release-notes/<mod_version>.md` instead of reposting the whole
  project changelog.
- Bumped the public release target to `3.0.0`, added
  `gradle/release-notes/3.0.0.md`, and promoted the supported Modrinth publish
  matrix to cover Minecraft `1.20.x` and `1.21.x` compatibility groups.
- Verified the full `3.0.0` supported matrix with `ciValidation`, including all
  six compatibility groups and automated standalone/all-public smoke launches.
- Published all 18 `3.0.0` compatibility-group versions through the guarded
  GitHub Actions Modrinth workflow with `requested_status=listed`.
- Changed the automatic GitHub push/PR build to run the fast default-profile
  `buildAllMods` task, leaving the expensive smoke matrix in the manual
  Modrinth publish workflow before upload.
- Moved target-specific custom button render hooks into `1.21.9-1.21.10` and
  `1.21.11` compat overlays while keeping the shared drawing logic in Core.
- Added `COMPATIBILITY.md` with the Minecraft version probe matrix and Modrinth listing recommendation.
- Confirmed the current split release jars should be published for Minecraft `1.21.11` only until version-specific builds pass compile and launch testing.
- Documented the compatibility-group profile strategy: one build profile may produce
  one jar for a tested range of Minecraft versions, with CI smoke testing required
  before Modrinth publishing.
- Expanded the roadmap so CI validation becomes step 8 and Modrinth publishing
  automation moves to step 9.
- Confirmed the rebuilt split release jars launch in normal launcher installs individually and together.

### Fixed

- Fixed split release jars embedding the development-namespaced Core jar, which could crash client entrypoints in normal launcher installs. Public jars now embed the remapped Core jar and `clean build` verifies this.
- Corrected the Fabric metadata icon path to use the checked-in `assets/inventory-sort/icon.png` asset.
- `verifyReleaseJars` now checks that each public jar's declared icon path is
  present in the packaged jar.
- GitHub Actions publish validation still gates Modrinth uploads with supported
  profile builds and smoke launches while normal push/PR builds stay fast.
- Fixed the first `1.21.9-1.21.10` compile blockers by adapting dimension id
  access and avoiding version-specific minecart entity class imports in shared
  identity code.
- Fixed broad `1.20.x` and earlier `1.21.x` compile/runtime launch blockers by
  moving API-sensitive calls behind version-selected compat overlays.

### Removed

- Removed unused Fabric template scaffolding: the unused `InventorySort` initializer, unused `ExampleMixin`, and unused client mixin config referencing the missing `ExampleClientMixin`.
