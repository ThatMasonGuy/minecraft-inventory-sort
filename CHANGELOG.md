# Changelog

All notable project changes will be documented here.

## Unreleased

### Changed

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
- Added a `1.21.10` candidate compatibility profile that compiles and builds
  release jars, pending launcher smoke testing before Modrinth listing.
- Moved target-specific custom button render hooks into `1.21.10` and `1.21.11`
  compat overlays while keeping the shared drawing logic in Core.
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
- Fixed the first `1.21.10` compile blockers by adapting dimension id access and
  avoiding version-specific minecart entity class imports in shared identity code.

### Removed

- Removed unused Fabric template scaffolding: the unused `InventorySort` initializer, unused `ExampleMixin`, and unused client mixin config referencing the missing `ExampleClientMixin`.
