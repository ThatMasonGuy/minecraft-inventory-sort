# Compatibility Release Playbook

This playbook captures the portable compatibility, CI, and publishing strategy
used here. It is meant to be adapted to other Fabric mods without copying this
repo's exact Gradle code or Inventory Sort module layout.

## Core Idea

Build one jar per **compatibility group**, not one jar per Minecraft patch. A
compatibility group compiles against one anchor Minecraft version, then only
claims the exact Minecraft versions that same jar has passed launcher smoke
testing on.

This keeps publishing honest while avoiding needless duplicate builds for patch
versions that share the same API shape.

Use the fewest supported build profiles that can honestly pass validation. Add
or keep a separate supported profile only when the same compiled jar cannot
cover the target runtimes because of source, dependency, metadata, or smoke-test
drift.

## Profile Model

Each profile should describe:

- `minecraft_version`: the compile anchor used by Loom and mappings.
- `profile_id`: the release folder and Modrinth suffix, such as
  `1.21.9-1.21.10`.
- `minecraft_dependency`: the loader dependency range written into mod metadata.
- `modrinth_game_versions`: exact game versions to list after smoke testing.
- `compat_group`: optional source overlay folder for version-specific APIs.
- `java_version`: the Java toolchain required by that Minecraft version.
- Loader/build-tool versions, such as Fabric Loader, Fabric API, and Loom.

Keep two profile lists:

- `candidate_minecraft_version_profiles`: builds or experiments that are not
  publishable yet.
- `supported_minecraft_version_profiles`: profiles that have compiled, passed
  launcher smoke testing for every listed game version, and can be published.

The profile lists can reference profile file names, while `profile_id` can be a
broader release range. For example, `26.1.2.properties` can publish to
`26.1-26.1.2` after exact runtimes `26.1`, `26.1.1`, and `26.1.2` pass.

## Source Layout

Keep shared behavior in the normal source tree. Add compatibility overlays only
for API drift that cannot compile across the whole range.

Suggested layout:

```text
src/client/java/
src/compat/<compat_group>/client/java/
src/compat/<compat_group>/client/resources/
```

Prefer small adapters, wrappers, or replacement mixins over copying whole
feature classes into compat groups.

## Verification Ladder

Use a fast local loop and move expensive proof to CI:

1. Local default-profile build for normal development.
2. Targeted profile build when touching a specific Minecraft version.
3. Build all supported profiles when changing metadata, overlays, packaging, or
   shared code with cross-version risk.
4. Focused launcher smoke test for suspected runtime issues.
5. Full smoke matrix in GitHub Actions before publishing.

For single-mod repos, the smoke matrix can be simpler than Inventory Sort's
four install sets. The important invariant is that the packaged release jar
launches under every Minecraft version listed in `modrinth_game_versions`.

## Publishing Gate

Publishing should only target supported profiles.

Before publishing:

1. Bump `mod_version`.
2. Create or update `gradle/release-notes/<mod_version>.md` with user-facing
   changes for that release.
3. Build every supported profile.
4. Verify release jar metadata and Modrinth upload metadata.
5. Run launcher smoke tests for every listed game version.
6. Dry-run the Modrinth upload plan.
7. Publish through a guarded manual workflow after review.

Do not list a Minecraft version on Modrinth because it "probably works". List it
only after the exact packaged jar has launched on that version.

## Local vs GitHub

Local development should stay fast. The normal push/PR workflow should run a
default-profile build and metadata checks. The manual publish workflow should
install all required Java toolchains, run the expensive smoke matrix under a
virtual display, and publish only after that gate passes.

This keeps the developer machine free for quick iteration while still making the
public release path strict.

## Release Notes

Keep two changelog tracks:

- `CHANGELOG.md`: broad repo history, including internal build, CI, migration,
  and implementation details.
- `gradle/release-notes/<mod_version>.md`: concise Modrinth-facing notes for
  users installing that specific version.

Update release notes as user-facing changes happen. Skip internal-only docs,
CI, or refactor details unless they affect install, compatibility, commands, or
visible behavior.
