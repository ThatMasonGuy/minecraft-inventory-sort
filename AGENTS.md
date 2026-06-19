# AGENTS.md

## Fresh Agent Start Here

Read these files before making changes:

1. `AGENTS.md` for workflow rules and verification expectations.
2. `TODO.md` for the current checkpoint, confirmed behavior, and active backlog.
3. `README.md` for the user-facing mod shape and basic build commands.
4. `COMPATIBILITY.md` for the currently supported Minecraft ranges.
5. `gradle/version-profiles/README.md` for profile metadata, Java toolchains,
   compat overlays, and candidate-vs-supported rules.
6. `gradle/smoke-tests.md` for local spot checks and the full smoke matrix.
7. `gradle/modrinth-publishing.md` for release notes, secrets, dry runs, and
   guarded publishing.
8. `gradle/modrinth-project-pages.md` for Modrinth project summaries and
   description-page source copy.
9. `gallery/README.md` for Modrinth gallery assets, banner selectors, and
   description image selectors.
10. `docs/README.md` for developer-facing docs that do not belong in the
    release/publishing playbooks.
11. `docs/button-slot-api.md` when touching inventory/container button
    placement, companion-mod button interoperability, or right-side slot
    reservations.
12. `gradle/compatibility-release-playbook.md` for the portable compatibility
   and CI plan that can be adapted to other mods.

After reading the docs, run `git status --short` before editing. Preserve any
unrelated user changes.

## Project Workflow

- Keep the repo in a clean checkpoint-driven state while maintaining the split
  mods and multi-version release pipeline.
- After each major change or implementation step:
  1. Update `TODO.md` with the completed work, current state, and next relevant task.
  2. Update `CHANGELOG.md` with the repo-facing engineering history.
  3. Update `gradle/release-notes/<mod_version>.md` when the change is
     user-facing for the release being prepared. Do not put internal-only CI,
     shim, refactor, or docs housekeeping into Modrinth release notes.
  4. Run the appropriate verification command.
  5. Commit the change before starting the next major step.
- Verification command ladder:
  - Docs-only changes: `git diff --check`.
  - Normal local/push sanity check: `.\gradlew.bat buildAllMods --no-daemon --console=plain`.
  - Targeted Minecraft profile build:
    `.\gradlew.bat buildAllMods "-Pminecraft_version_profile=<profile>" --no-daemon --console=plain`.
  - Rebuild every supported publish profile:
    `.\gradlew.bat buildAllVersions --no-daemon --console=plain`.
  - Focused launcher spot check:
    `.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=<profile_id>" "-Pinventorysort_smoke_game_versions=<version>" "-Pinventorysort_smoke_install_sets=<install_set>" --no-daemon --console=plain`.
  - Full local matrix only when explicitly needed:
    `.\gradlew.bat ciValidation --no-daemon --console=plain`.
- Before any Modrinth publish or dry-run publish for a new `mod_version`, add a
  concise per-release note file at `gradle/release-notes/<mod_version>.md`.
  This file is the Modrinth changelog for that version. Do not rely on the full
  `CHANGELOG.md` or the whole `## Unreleased` section for Modrinth uploads.
- Keep the active `gradle/release-notes/<mod_version>.md` file updated as
  user-facing changes accumulate for that Modrinth release. Put internal build,
  shim, CI, docs, and migration details in `CHANGELOG.md` and `TODO.md`
  instead.
- Keep Modrinth project-page copy and gallery assets source-controlled. Long
  descriptions live in `gradle/modrinth-project-pages.md`; uploadable gallery
  images and metadata live under `gallery/`. Root images in each mod folder are
  uploaded to that mod's gallery, while `banner/` and `description_images/`
  contain selector copies only and must not be uploaded as extra images.
- Use `.\scripts\sync-modrinth-project-pages.ps1 -DryRun` before live
  Modrinth project-page or gallery updates. Use `-ReplaceGallery` only when the
  local `gallery/` folders should become the complete live gallery set.
- If multiple major changes happen in one session, stop between each major boundary to update `TODO.md`, update `CHANGELOG.md`, verify, and commit.
- Keep commits focused. Do not bundle unrelated split, cleanup, publishing, or version-migration work into one commit.
- Before editing or committing, check `git status --short` and preserve any user changes that are unrelated to the current task.

## Local vs GitHub Validation

- Use local `buildAllMods` for rapid development.
- Use `buildAllVersions` when changing profile metadata, compat overlays,
  release packaging, or anything that should compile across every supported
  publish profile.
- Use `smokeTestSelectedClients` for focused launcher checks around a suspected
  runtime issue.
- Prefer the manual GitHub Actions `modrinth publish` workflow for the full
  supported smoke matrix. It installs Java 17, Java 21, and Java 25, runs under
  `xvfb`, and avoids locking the local machine for close to an hour.
- Real Modrinth uploads should go through the guarded GitHub workflow unless
  the user explicitly asks for a local `publishModrinth`.
- After every successful real Modrinth publish, create an annotated Git tag
  named `v<mod_version>` on the released source commit and create a GitHub
  release for that tag. The GitHub release should not attach jar assets;
  Modrinth is the canonical download surface. Link the public Modrinth projects,
  the uploaded Modrinth version URLs, the release source commit, and the
  successful publish workflow run.

## Major Change Boundaries

Examples of major boundaries for this project:

- Baseline cleanup or metadata correction.
- Core extraction.
- Event bus or namespace-change refactor.
- Splitting Sort, Search, or Catalogue into separate modules.
- Gradle build/publish task changes.
- Modrinth publishing configuration.
- GitHub release/tag publishing configuration.
- Minecraft/Fabric/Loom version migration.

## Current Direction

- Keep user installation simple: each public feature mod should be installable on its own.
- Shared Core code should be packaged so users do not need to download a separate Core mod manually.
- Keep Sort, Search, and Catalogue as separate public mods while packaging the
  shared Core internally so users can install any one feature mod by itself.
- Treat Minecraft version profiles as release compatibility groups, not necessarily
  one profile per exact patch version. A profile should compile one jar from one
  anchor Minecraft version, list every Minecraft version that exact jar has passed
  smoke testing on, and publish only those tested game versions to Modrinth.
- Prefer as few supported build/release profiles as possible. Broaden an
  existing compatibility-group profile when one compiled jar can cover multiple
  exact smoke-tested runtimes; add a new supported profile only when source,
  dependency, metadata, or runtime validation proves the jar must split.
- `supported_minecraft_version_profiles` and
  `candidate_minecraft_version_profiles` list profile file names. Release
  folders and Modrinth version suffixes use each profile's `profile_id`, which
  can be broader than the file name.
- For the Minecraft 26.x lane, keep `26.x.properties` as the single supported
  release profile unless real API/dependency drift proves a split is necessary.
  It compiles from the newest checked 26.x anchor, publishes with profile id
  `26.1-26.2-pre-3`, and uses the shared `src/compat/26.x` overlay.
  `26.1.properties`, `26.1.1.properties`, `26.1.2.properties`, and
  `26.2-pre-3.properties` are exact runtime-only smoke profiles. For prerelease
  Fabric metadata, remember that Modrinth uses labels like `26.2-pre-3` while
  Fabric Loader reports/compares runtime versions like `26.2-pre.3`; follow
  Fabric API's `minecraft` dependency string for exact runtime profiles and
  keep `modrinth_game_versions` as the Modrinth label.
- Keep automated validation ahead of Modrinth publishing: compile/build checks,
  release jar metadata checks, and launcher smoke tests must pass for every
  Minecraft version claimed by a compatibility-group profile.
- Keep `CHANGELOG.md` as the broad repo history. Keep Modrinth-facing release
  notes focused, version-specific, and user-facing in `gradle/release-notes/`.
