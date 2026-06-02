# Changelog

All notable project changes will be documented here.

## Unreleased

### Changed

- Added `AGENTS.md` with the project workflow for updating `TODO.md`, updating this changelog, verifying, and committing after each major change.
- Recorded the 8-step split and multi-version migration roadmap in `TODO.md` so it survives future context compaction.
- Added the initial Core foundation with shared mod id/logger ownership and event contracts for namespace changes, container snapshots, and inventory snapshots.
- Routed container/inventory snapshot handling and namespace-change handling through Core events, with Search and Catalogue now subscribed via feature bridge classes.

### Fixed

- Corrected the Fabric metadata icon path to use the checked-in `assets/inventory-sort/icon.png` asset.

### Removed

- Removed unused Fabric template scaffolding: the unused `InventorySort` initializer, unused `ExampleMixin`, and unused client mixin config referencing the missing `ExampleClientMixin`.
