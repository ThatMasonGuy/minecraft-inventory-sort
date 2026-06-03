# AGENTS.md

## Project Workflow

- Keep the repo in a clean checkpoint-driven state while splitting the mod and preparing multi-version releases.
- After each major change or implementation step:
  1. Update `TODO.md` with the completed work, current state, and next relevant task.
  2. Update `CHANGELOG.md` with the user-facing or engineering changes.
  3. Run the appropriate verification command, usually `.\gradlew.bat clean build` for baseline/build changes.
  4. Commit the change before starting the next major step.
- Before any Modrinth publish or dry-run publish for a new `mod_version`, add a
  concise per-release note file at `gradle/release-notes/<mod_version>.md`.
  This file is the Modrinth changelog for that version. Do not rely on the full
  `CHANGELOG.md` or the whole `## Unreleased` section for Modrinth uploads.
- If multiple major changes happen in one session, stop between each major boundary to update `TODO.md`, update `CHANGELOG.md`, verify, and commit.
- Keep commits focused. Do not bundle unrelated split, cleanup, publishing, or version-migration work into one commit.
- Before editing or committing, check `git status --short` and preserve any user changes that are unrelated to the current task.

## Major Change Boundaries

Examples of major boundaries for this project:

- Baseline cleanup or metadata correction.
- Core extraction.
- Event bus or namespace-change refactor.
- Splitting Sort, Search, or Catalogue into separate modules.
- Gradle build/publish task changes.
- Modrinth publishing configuration.
- Minecraft/Fabric/Loom version migration.

## Current Direction

- Keep user installation simple: each public feature mod should be installable on its own.
- Shared Core code should be packaged so users do not need to download a separate Core mod manually.
- Split the current single mod on the existing Minecraft target first, then port the split modules to newer Minecraft versions.
- Treat Minecraft version profiles as release compatibility groups, not necessarily
  one profile per exact patch version. A profile should compile one jar from one
  anchor Minecraft version, list every Minecraft version that exact jar has passed
  smoke testing on, and publish only those tested game versions to Modrinth.
- Add automated CI validation before Modrinth automation: compile/build checks,
  release jar metadata checks, and launcher smoke tests for every Minecraft version
  claimed by a compatibility-group profile.
- Keep `CHANGELOG.md` as the broad project history. Keep Modrinth-facing release
  notes focused and version-specific in `gradle/release-notes/`.
