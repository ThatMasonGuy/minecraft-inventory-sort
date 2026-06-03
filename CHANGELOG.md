# Changelog

All notable project changes will be documented here.

## Unreleased

### Changed

- Added Java toolchain handling and manual CI support for Minecraft `26.x`
  migration work: compile/client-run tasks now request the active profile's
  `java_version`, GitHub Actions has a focused Java 25 compatibility-validation
  workflow, and the Modrinth publish workflow can select Java 21 or Java 25.
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
