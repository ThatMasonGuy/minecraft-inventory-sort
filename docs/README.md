# Inventory Mods Docs

This folder holds developer-facing docs that are not part of the public
Modrinth project copy or Gradle release/publish playbooks.

## Index

- [`button-slot-api.md`](button-slot-api.md): shared Core API for right-side
  inventory/container button reservations, capacity checks, fit checks, and the
  stable first-party InvSort/InvSearch reservation order for companion mods.

Release and publishing docs live under `gradle/`. Modrinth gallery source docs
live in `gallery/README.md`. The root `README.md` remains the user-facing mod
overview and build entrypoint, while `TODO.md` records current checkpoints and
validation evidence.
