# Changelog

All notable project changes will be documented here.

## Unreleased

### Changed

- Added `AGENTS.md` with the project workflow for updating `TODO.md`, updating this changelog, verifying, and committing after each major change.
- Recorded the 8-step split and multi-version migration roadmap in `TODO.md` so it survives future context compaction.
- Added the initial Core foundation with shared mod id/logger ownership and event contracts for namespace changes, container snapshots, and inventory snapshots.
- Routed container/inventory snapshot handling and namespace-change handling through Core events, with Search and Catalogue now subscribed via feature bridge classes.
- Split catalogue and world-profile command implementation into separate command classes.
- Moved split-mod commands to feature-specific roots: `/inventorycatalogue ...`, `/inventorycatalogue world ...`, and `/inventorysearch world ...`.
- Split sort/search screen-button ownership and added separate Core/Search/Catalogue client entrypoints as preparation for public modules.
- Added Gradle subprojects for Core, Sort, Search, and Catalogue. The three public feature jars now build separately and each nests the shared Core jar.
- Added `collectReleaseJars` and updated `buildAllMods` so publish-ready Sort, Search, and Catalogue jars are collected in `build/release/`.
- Confirmed the rebuilt split release jars launch in normal launcher installs individually and together.

### Fixed

- Fixed split release jars embedding the development-namespaced Core jar, which could crash client entrypoints in normal launcher installs. Public jars now embed the remapped Core jar and `clean build` verifies this.
- Corrected the Fabric metadata icon path to use the checked-in `assets/inventory-sort/icon.png` asset.

### Removed

- Removed unused Fabric template scaffolding: the unused `InventorySort` initializer, unused `ExampleMixin`, and unused client mixin config referencing the missing `ExampleClientMixin`.
