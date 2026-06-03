# Changelog

All notable project changes will be documented here.

## Unreleased

### Fixed

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

- Promoted all smoke-passed Minecraft `1.20.x`, `1.21.x`, and `26.x`
  compatibility groups into one unified `3.1.1` supported publish lane,
  rebuilt the local release artifacts for all eight groups, and verified the
  Modrinth dry-run plan contains 24 uploads across InvSort, InvSearch, and
  InvCatalogue.
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
