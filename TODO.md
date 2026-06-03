# Inventory Search TODO

Current checkpoint: Minecraft 26.x Fabric API event/command update complete

## Project Workflow

- After every major change, update `TODO.md`, update `CHANGELOG.md`, verify the
  relevant Gradle build/task, and commit before starting the next major step.
- If a session includes more than one major change, stop between major boundaries
  to update notes and commit each checkpoint separately.

## Confirmed Working

- Fixed block containers track current contents correctly.
- Ender chests track as player-scoped storage.
- Placed shulkers use same-content cleanup when moved, intentionally favoring undercounting over stale duplicate locations.
- Chest minecarts and hopper minecarts track correctly.
- Server/world tracking profiles work for multiplayer HC resets.
- World confirmation HUD blocks writes until confirmed, without blocking gameplay.
- Catalog `includeInventory` uses a stable per-session player-inventory fingerprint.
- Split release jars launch successfully in a normal launcher when installed as
  Sort-only, Search-only, Catalogue-only, Sort+Search, and all three together.
  Search+Catalogue was not separately tested, but is expected to work after the
  standalone and all-three validation passed.
- Automated client smoke validation launches the packaged release jars as
  Sort-only, Search-only, Catalogue-only, and all-three installs on Minecraft
  `1.21.11` plus supported runtimes from `1.20` through `1.21.10`.

## Current Command Roots

- Inventory Catalogue owns `/inventorycatalogue start|stop|status|report|clear`.
- Inventory Catalogue also exposes shared world-profile commands as
  `/inventorycatalogue world list|use|default|current`.
- Inventory Search exposes shared world-profile commands as
  `/inventorysearch world list|use|default|current`.
- Core no longer registers a public `/inventorysort` command root.

## Recently Fixed

-34. Minecraft `26.x` Fabric API event/command update verification (unreleased):
   - Audited direct Fabric command/HUD API usage after the Core helper and
     rendering passes.
   - Confirmed `ClientCommandManager` is now isolated to `1.20.x`/`1.21.x`
     `ClientCommandCompat` overlays, while `26.x` overlays use
     `ClientCommands`.
   - Confirmed `HudRenderCallback` is now isolated to `1.20.x`/`1.21.x`
     `HudCompat` overlays, while `26.x` overlays use the HUD element registry.
   - Confirmed shared `ClientCommandRegistrationCallback` and
     `ClientTickEvents` registrations remain available in the checked `26.x`
     Fabric API jars, so no additional wrapper is required for this step.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes.
   - Verified the `26.x` Core compile boundary remains the planned
     `ClickType` / `ContainerInput` invoker work rather than command, HUD, or
     lifecycle event API drift.
   - Next step is the `26.x` container/mixin input update.

-33. Minecraft `26.x` Core GUI/rendering abstraction (unreleased):
   - Added a shared `InventorySortDrawContext` interface so Core drawing logic no
     longer directly depends on `GuiGraphics`.
   - Added version-selected draw-context wrappers for `1.20.x`/`1.21.x`
     `GuiGraphics` and `26.x` `GuiGraphicsExtractor`.
   - Routed shared panel drawing, icon/text/modal button renderers, the
     world-profile HUD, and Search's shared panel helper calls through the draw
     context.
   - Added `26.1.2` and `26.2-pre-3` button overlays using the new
     `extractContents` lifecycle.
   - Wrapped HUD registration with `HudCompat`, keeping `HudRenderCallback` for
     `1.20.x`/`1.21.x` and using the `26.x` HUD element registry for candidate
     profiles.
   - Added `26.x` profile-screen overlays using `extractRenderState`, with a
     profile-driven shared-source exclusion so the current `GuiGraphics` screen
     remains untouched for `1.20.x`/`1.21.x`.
   - Added Core helpers for GUI hidden state, screen presence, screen switching,
     and singleplayer detection after `26.2-pre-3` moved those APIs again.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes.
   - Verified both `26.1.2` and `26.2-pre-3`
     `:inventorysort-core:compileClientJava` now get past GUI/rendering, HUD,
     screen, and helper API blockers. Both candidate Core probes now stop only
     at the planned `ClickType` / `ContainerInput` invoker work.
   - Search's full `26.x` screen lifecycle is still expected to be handled during
     the Search feature compile pass.
   - Next step is the `26.x` container/mixin input update.

-32. Minecraft `26.x` Core helper compatibility overlay (unreleased):
   - Added `26.1.2` and `26.2-pre-3` Core compatibility overlays for
     Minecraft API helper calls, including dimension ids, connected chest
     lookup, window handles, single-player server directories, HUD pose helpers,
     and player feedback messages.
   - Added a version-selected `ClientCommandCompat` adapter so shared command
     code can use `ClientCommandManager` on `1.20.x`/`1.21.x` and
     `ClientCommands` on `26.x`.
   - Routed shared Core/Search/Catalogue command builders through
     `ClientCommandCompat`.
   - Routed shared player feedback calls through `MinecraftApiCompat` so
     `1.20.x`/`1.21.x` keep `displayClientMessage` while `26.x` uses
     `sendSystemMessage` / `sendOverlayMessage`.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes on the existing release lane.
   - Verified the `26.1.2` Core compile probe now gets past the first helper,
     command, and feedback-message API breaks. The remaining compile blockers
     are the planned GUI/render extraction, HUD registry, and container input
     changes.
   - Next step is the `26.x` GUI/rendering abstraction.

-31. Minecraft `26.x` Java 25 toolchain and CI lane (unreleased):
   - Gradle now requests the active profile's `java_version` as the Java
     toolchain for Java compile and client-run tasks.
   - Added Java toolchain discovery hints for `JAVA_HOME`, `JAVA_HOME_21_X64`,
     and `JAVA_HOME_25_X64`.
   - Added a manual GitHub Actions `compatibility validation` workflow that
     installs Java 21 and Java 25, defaults to running candidate profiles on
     Java 25, and can run focused `printVersionProfile` or `buildAllMods`
     checks for profiles such as `26.1.2`.
   - Updated the manual Modrinth publish workflow to install both Java 21 and
     Java 25 and expose a `gradle_java_version` input. It still defaults to Java
     21 for the current `1.20.x`/`1.21.x` release lane.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes on local Java 21.
   - Verified `.\gradlew.bat printVersionProfile "-Pminecraft_version_profile=26.1.2"`
     still configures and clearly reports Java 25 as the profile target.
   - Local `26.1.2` compile now stops at the expected Java 25 toolchain gate on
     this machine because no JDK 25 is installed locally; GitHub's manual
     compatibility workflow supplies that toolchain for future source/API work.
   - Next step is the `26.x` Core compatibility overlay.

-30. Minecraft `26.x` build-system foundation (unreleased):
   - Added `unobfuscated_minecraft=true` profile support for `26.x` profiles.
   - Gradle now selects `net.fabricmc.fabric-loom-remap`,
     `modImplementation`, and `remapJar` for the existing `1.20.x`/`1.21.x`
     lane, while `26.x` selects `net.fabricmc.fabric-loom`, `implementation`,
     and plain `jar` release artifacts.
   - Feature modules now use the Core `namedElements` dependency only for
     remapped profiles; unobfuscated profiles use the normal Core project
     dependency.
   - `collectReleaseJars`, `verifyReleaseJars`, and `printVersionProfile` now
     report/use the profile-selected release jar task.
   - Verified default `.\gradlew.bat buildAllMods --no-daemon --console=plain`
     still passes on `1.21.11`.
   - Verified `26.1.2` and `26.2-pre-3` both configure with the non-remap
     build lane via `printVersionProfile`.
   - Verified a `26.1.2` `buildAllMods --dry-run` uses `jar` tasks instead of
     `remapJar` tasks.
   - Next step is Java 25 toolchain/CI handling before real `26.x` compile and
     source/API shim work.

-29. `3.1.0` release-notes lane for Minecraft `26.x` (unreleased):
   - Added `gradle/release-notes/3.1.0.md` as the running Modrinth-facing,
     user-focused changelog for the planned official `26.x` support release.
   - Clarified in `AGENTS.md` that `CHANGELOG.md` remains the repo-facing
     engineering history, while `gradle/release-notes/3.1.0.md` should only
     collect user-visible `3.1.0` release notes.
   - Do not bump `mod_version` to `3.1.0` until the `26.x` compile, smoke, and
     supported-profile promotion gates pass.

-28. Minecraft 26.x forward-development roadmap (unreleased):
   - Decided to keep shared source anchored to the proven `1.20.x`/`1.21.x`
     compatibility baseline and build `26.x` support forward as candidate
     profiles.
   - Recorded the `26.x` roadmap: Java 25/build-system support, non-remap Loom,
     Core API compatibility, GUI/render extraction compatibility, Fabric command
     and HUD API changes, container/mixin input changes, feature compile passes,
     automated smoke testing, and the promotion/publish gate.
   - Branching remains the fallback only if `26.x` forces duplicated feature
     logic instead of isolated build config, adapters, and mixin surfaces.

-27. Fast push/PR build workflow (unreleased):
   - Changed the automatic GitHub `build` workflow from full `ciValidation` to
     the faster default-profile `buildAllMods` task.
   - Removed the push/PR workflow's `xvfb` install because the fast build does
     not launch Minecraft clients.
   - Kept the manual `modrinth publish` workflow as the expensive release gate:
     it still runs `publishModrinth`/`publishValidation`, builds supported
     profiles, smoke-launches the supported matrix, and only uploads after that
     passes.
   - Updated README and Gradle docs so the intended workflow is local fast
     build -> commit/push -> manual Modrinth workflow validates and publishes.

-26. Final `3.0.0` broad Modrinth release sweep (unreleased):
   - Bumped `mod_version` to `3.0.0`.
   - Added `gradle/release-notes/3.0.0.md` for the public Modrinth release.
   - Promoted the smoke-passed compatibility groups into
     `supported_minecraft_version_profiles`: `1.21.11`,
     `1.21.9-1.21.10`, `1.21.6-1.21.8`, `1.21-1.21.5`,
     `1.20.5-1.20.6`, and `1.20-1.20.4`.
   - Cleared `candidate_minecraft_version_profiles` so the public publish set
     is exactly the supported `1.20.x` and `1.21.x` release matrix.
   - Updated release-facing compatibility docs and README metadata for the
     `3.0.0` supported profile set.
   - Verified `.\gradlew.bat ciValidation --no-daemon --console=plain`; the
     full supported matrix passed in 43m 17s across all six compatibility
     groups and all automated standalone/all-public smoke launches.
   - Committed and pushed the release sweep as `933925b`.
   - Triggered GitHub Actions run `26875020709` (`modrinth publish`) with
     `dry_run=false`, `version_type=release`, and `requested_status=listed`.
     The workflow passed in 49m52s and uploaded all 18 compatibility-group
     versions:
     - InvSort:
       - `3.0.0+mc1.21.11`: `v5sFbix1`
       - `3.0.0+mc1.21.9-1.21.10`: `jXDZPzIX`
       - `3.0.0+mc1.21.6-1.21.8`: `og828QKS`
       - `3.0.0+mc1.21-1.21.5`: `JJsp2SDN`
       - `3.0.0+mc1.20.5-1.20.6`: `6QWi2YRO`
       - `3.0.0+mc1.20-1.20.4`: `JtpNe6aB`
     - InvSearch:
       - `3.0.0+mc1.21.11`: `3a2ShEXs`
       - `3.0.0+mc1.21.9-1.21.10`: `wl0CFxZw`
       - `3.0.0+mc1.21.6-1.21.8`: `CJq6pDbY`
       - `3.0.0+mc1.21-1.21.5`: `8ylLOamo`
       - `3.0.0+mc1.20.5-1.20.6`: `ZK09KjA9`
       - `3.0.0+mc1.20-1.20.4`: `So21qEo1`
     - InvCatalogue:
       - `3.0.0+mc1.21.11`: `oWpB46Ap`
       - `3.0.0+mc1.21.9-1.21.10`: `DMUYgcx2`
       - `3.0.0+mc1.21.6-1.21.8`: `vG5mdvQA`
       - `3.0.0+mc1.21-1.21.5`: `i8J2pIat`
       - `3.0.0+mc1.20.5-1.20.6`: `XhGUQKCH`
       - `3.0.0+mc1.20-1.20.4`: `w8j4OR6w`
   - Public unauthenticated Modrinth API lookups for the uploaded project/version
     ids returned 404 immediately after upload, consistent with projects still
     awaiting Modrinth review/public visibility rather than a workflow upload
     failure.
   - Next: watch Modrinth review/public visibility, then begin the v26 migration
     lane when ready.

-25. Per-release Modrinth notes (unreleased):
   - Changed Modrinth publish automation to read release notes from
     `gradle/release-notes/<mod_version>.md` by default instead of pulling the
     whole `## Unreleased` section from `CHANGELOG.md`.
   - Publish tasks now fail if the per-version release note file is missing or
     blank. A custom notes file can still be supplied with
     `-Pmodrinth_changelog_file=<path>`.
   - Added `gradle/release-notes/2.6.4.md` for the validation release.
   - Updated `AGENTS.md`, `README.md`, and `gradle/modrinth-publishing.md` so
     future work keeps broad repo history in `CHANGELOG.md` and concise
     Modrinth-facing notes under `gradle/release-notes/`.
   - Completed by item 26, which creates the `3.0.0` release notes, bumps the
     mod version, promotes the smoke-passed compatibility groups, and starts the
     final publish sweep after user approval.

-24. Focused `2.6.4` Modrinth pipeline validation (unreleased):
   - Bumped `mod_version` from `2.6.3` to `2.6.4` for a focused validation
     upload on the currently supported `1.21.11` profile only.
   - Corrected the InvSearch Modrinth project id to `wIOLlhbN` from the latest
     project-id list.
   - Plan: run `publishModrinthDryRun`, commit and push the metadata change,
     then trigger the manual GitHub Actions Modrinth workflow using the
     repository `MODRINTH_TOKEN` secret.
   - Verified `.\gradlew.bat publishModrinthDryRun
     "-Pmodrinth_requested_status=unlisted" --no-daemon --console=plain`; it
     built `2.6.4`, smoke-launched the supported `1.21.11` profile, and
     prepared dry-run upload entries for the three public Modrinth projects.
   - Pushed commit `f42122c` and triggered GitHub Actions run
     `26871634493` (`modrinth publish`) with `dry_run=false`,
     `version_type=release`, and `requested_status=unlisted`.
   - GitHub Actions used the repository `MODRINTH_TOKEN` secret successfully,
     smoke-launched all four supported `1.21.11` install combinations, and
     uploaded:
     - InvSort `2.6.4`: Modrinth version `l8tVjMEf`
     - InvSearch `2.6.4`: Modrinth version `ITIdiJHe`
     - InvCatalogue `2.6.4`: Modrinth version `k3rNn4zA`
   - Kept `supported_minecraft_version_profiles=1.21.11` for this validation
     run. The broad `1.20.x`/`1.21.x` groups stayed candidates until the later
     `3.0.0` promotion in item 26.

-23. Modrinth publishing automation (unreleased):
   - Added Modrinth project IDs for InvSort, InvSearch, and InvCatalogue in
     non-secret Gradle properties.
   - Added `prepareModrinthUploads`, which runs the supported-only publish gate,
     validates supported-profile upload metadata, and writes
     `build/modrinth/upload-plan.json`.
   - Added `publishModrinthDryRun` for safe local/CI pipeline testing without
     calling the Modrinth API.
   - Added `publishModrinth`, which uploads to Modrinth only after
     `-Pmodrinth_confirm_publish=true` and a token from `MODRINTH_TOKEN` or a
     user-level Gradle property.
   - Added a manual GitHub Actions `modrinth publish` workflow with dry-run and
     real-publish modes.
   - Documented project IDs, token handling, version-number suffix behavior, and
     publishing options in `gradle/modrinth-publishing.md`.
   - Verified `.\gradlew.bat publishModrinthDryRun --no-daemon
     --console=plain`; it built and smoke-launched the supported `1.21.11`
     profile, then prepared dry-run upload entries for InvSort, InvSearch, and
     InvCatalogue without calling the Modrinth API.
   - Important: publishing reads `supported_minecraft_version_profiles` only.
     Candidate compatibility groups remain invisible to Modrinth automation
     until promoted.

-22. Fast supported-only publish validation gate (unreleased):
   - Added `smokeTestSupportedClients`, which launches the same Sort-only,
     Search-only, Catalogue-only, and all-public install combinations as the
     full smoke matrix, but only for supported/publishable profiles.
   - Added `publishValidation`, the fast pre-publish gate that builds supported
     profiles, checks smoke records, and smoke-launches supported profiles only.
   - Added `smokeTestSelectedClients` for local spot checks using
     `inventorysort_smoke_profiles`, `inventorysort_smoke_game_versions`, and
     `inventorysort_smoke_install_sets` filters.
   - Added `inventorysort_smoke_nested_no_daemon=false` as an optional local
     timing experiment for nested smoke Gradle launches. The default remains
     `--no-daemon` for predictable CI behavior.
   - `ciValidation` remains the exhaustive supported + candidate compatibility
     matrix and should still run before broad Modrinth compatibility promotion.

-21. Broad `1.20.x` and `1.21.x` compatibility groups (unreleased):
   - Added candidate release compatibility groups for:
     `1.21.6-1.21.8`, `1.21-1.21.5`, `1.20.5-1.20.6`, and
     `1.20-1.20.4`.
   - Added exact runtime-only smoke profiles for every `1.20.x` and `1.21.x`
     version covered by those grouped jars.
   - Added version shims for chest neighbor lookup, window handle access, HUD
     pose/matrix calls, single-player server directory return type, item stack
     identity/components vs tags, recipe book screen presence, mouse wheel
     method signatures, and older public `renderWidget` button overrides.
   - Updated automated smoke records after `ciValidation` launched Sort-only,
     Search-only, Catalogue-only, and all-three installs across the full
     candidate matrix.
   - Verified `.\gradlew.bat buildValidationVersions --no-daemon
     --console=plain`; all supported and candidate release profiles built.
   - Verified `.\gradlew.bat ciValidation --no-daemon --console=plain`; the
     full launch matrix passed across `1.21.11`, `1.21.9-1.21.10`,
     `1.21.6-1.21.8`, `1.21-1.21.5`, `1.20.5-1.20.6`, and `1.20-1.20.4`.
   - These broad groups were later promoted to supported/publishable Modrinth
     targets by item 26 for the `3.0.0` release.

-20. Automated client smoke validation (unreleased):
   - Added a no-mod `smokelaunch` Loom project that launches exact Minecraft
     runtimes with packaged release jars injected through Fabric Loader.
   - Added `InventorySortSmokeTest`, which arms only when
     `-Dinventorysort.smokeTest=true`, waits for the client tick loop, logs
     `INVENTORYSORT_SMOKE_TEST_PASS`, and closes the client cleanly.
   - `smokeTestValidationClients` now launches every validation profile/game
     version as Sort-only, Search-only, Catalogue-only, and all-three installs.
   - Added smoke runtime profiles for exact `1.21.9` and `1.21.10` launches so
     the grouped `1.21.9-1.21.10` release jars can be tested against both game
     versions without producing separate release jars.
   - Updated GitHub Actions to install `xvfb` and run
     `./gradlew ciValidation -Pinventorysort_smoke_xvfb=true` for headless
     client smoke launches.
   - Updated `gradle/smoke-tests.json` with passing automated smoke records for
     `1.21.11`, `1.21.9`, and `1.21.10`.
   - Verified `.\gradlew.bat smokeTestValidationClients`; all twelve automated
     client launches passed locally.
   - Superseded by item 21, which adds broad `1.20.x` and `1.21.x` candidate
     coverage and passes the full automated launch matrix.

-19. CI validation and smoke-test matrix foundation (unreleased):
   - Added `candidate_minecraft_version_profiles` so CI can build candidate
     profiles without marking them publishable.
   - Added `buildValidationVersions`, which builds every supported and candidate
     profile sequentially to avoid Loom cache/output races.
   - Added `ciValidation`, the GitHub Actions entrypoint for Step 8 validation.
   - Added `verifySmokeTestMatrix`, which blocks supported/publishable profiles
     unless every listed `modrinth_game_versions` entry has a passing smoke
     record.
   - Added `gradle/smoke-tests.json` with the tested `1.21.11` release target
     recorded as `pass`, and the `1.21.9-1.21.10` candidate versions recorded
     as `pending`.
   - Added `gradle/smoke-tests.md` documenting how smoke statuses promote a
     candidate profile into a supported profile.
   - Updated `.github/workflows/build.yml` to run `./gradlew ciValidation`
     instead of the single-profile `build` task.
   - This gives Step 9 a gate: Modrinth automation should only read/publish
     profiles from `supported_minecraft_version_profiles`, which must pass the
     smoke matrix check.
   - Verified `.\gradlew.bat ciValidation`; it builds both `1.21.11` and
     `1.21.9-1.21.10` release folders and reports the grouped candidate as
     pending/non-publishable.
   - Superseded by item 20, which turns this foundation into automated client
     smoke launches.

-18. Split module icon assets (unreleased):
   - Moved the dropped public mod icon images into
     `src/main/resources/assets/inventory-sort/`.
   - Inventory Sort now uses `assets/inventory-sort/invsort.jpg`.
   - Inventory Search now uses `assets/inventory-sort/invsearch.jpg`.
   - Inventory Catalogue now uses `assets/inventory-sort/invcatalogue.jpg`.
   - The legacy root metadata follows the Sort icon; Core keeps the generic
     shared icon because it is not a public standalone download.
   - `verifyReleaseJars` now fails if a public jar declares an icon path that is
     not packaged in the jar.
   - Verified `.\gradlew.bat clean build` and inspected the `1.21.11` release
     jars to confirm all public jars carry their own icon paths.

-17. `1.21.9-1.21.10` compatibility candidate group (unreleased):
   - Re-probed `1.21.9` with the current adapter shape and confirmed the focused
     compile passes.
   - Replaced the exact `1.21.10` candidate profile with
     `gradle/version-profiles/1.21.9-1.21.10.properties`.
   - The grouped candidate is anchored on `minecraft_version=1.21.9`, uses
     `compat_group=1.21.9-1.21.10`, and declares
     `minecraft_dependency=>=1.21.9 <=1.21.10`.
   - Renamed the shared API-shape overlay from `src/compat/1.21.10` to
     `src/compat/1.21.9-1.21.10`.
   - Verified `.\gradlew.bat printVersionProfile
     "-Pminecraft_version_profile=1.21.9-1.21.10"` and
     `.\gradlew.bat clean buildAllMods
     "-Pminecraft_version_profile=1.21.9-1.21.10"`.
   - Verified grouped release jars collect under
     `build/release/1.21.9-1.21.10/` and declare
     `minecraft >=1.21.9 <=1.21.10`, `java >=21`, and
     `fabricloader >=0.18.4`.
   - Important: this grouped profile is still not supported/publishable until
     the exact grouped jars pass launcher smoke testing on both `1.21.9` and
     `1.21.10`.
   - Next Step 7 slice: launcher smoke-test the grouped jars on `1.21.9` and
     `1.21.10`, then probe `1.21.8` to find the next API boundary.

-16. `1.21.10` compatibility candidate (unreleased):
   - Added `gradle/version-profiles/1.21.10.properties` as a candidate
     compatibility-group profile.
   - Added `src/compat/1.21.10` and `src/compat/1.21.11` source overlays for
     Minecraft API differences that cannot compile from one shared source file.
   - Moved custom button render hooks into version-specific wrapper classes:
     `1.21.10` uses `renderWidget`, while `1.21.11` uses `renderContents`.
   - Added `MinecraftApiCompat.dimensionId(...)` adapters for the
     `ResourceKey.location()` / `ResourceKey.identifier()` API rename.
   - Removed direct Chest/Hopper Minecart class imports from shared identity code
     and now recognizes those containers by stable entity type id.
   - Verified sequential profile builds:
     `.\gradlew.bat clean build` for `1.21.11`, and
     `.\gradlew.bat clean buildAllMods "-Pminecraft_version_profile=1.21.10"`.
   - Verified the `1.21.10` release jars collect under `build/release/1.21.10/`
     and declare `minecraft ~1.21.10`, `java >=21`, and
     `fabricloader >=0.18.4`.
   - Important: `1.21.10` is still not a supported Modrinth listing until the
     release jars pass normal launcher smoke testing.
   - Superseded by item 17 after `1.21.9` compiled with the same adapter shape,
     allowing a grouped `1.21.9-1.21.10` candidate.

-15. Compatibility-group build profile foundation (unreleased):
   - Extended Gradle profiles with `profile_id`, `minecraft_dependency`,
     `modrinth_game_versions`, and `compat_group`.
   - Generated Fabric metadata now uses `minecraft_dependency` instead of always
     deriving `~<minecraft_version>`.
   - Release jars now collect under `build/release/<profile_id>/`.
   - `printVersionProfile` reports compatibility-group metadata and
     `verifyReleaseJars` checks generated Minecraft/Java dependency metadata.
   - Added `src/compat/<compat_group>/` wiring and documentation for
     version-specific API adapters.
   - Verified `.\gradlew.bat printVersionProfile`, `.\gradlew.bat clean build`,
     and `.\gradlew.bat buildAllVersions` on the current `1.21.11` profile.
   - First non-`1.21.11` compatibility-group profile is now tracked in item 16.

-14. Compatibility-group version strategy (unreleased):
   - Documented that version profiles should become release compatibility groups,
     not necessarily one profile per Minecraft patch version.
   - A profile should compile one jar against an anchor Minecraft version, select
     the appropriate compat source group, declare a Fabric Minecraft dependency
     range, and list only the exact Modrinth game versions that jar has passed
     smoke testing on.
   - Planned release outputs should move toward profile/range folders such as
     `build/release/1.21.6-1.21.11/` instead of assuming the output folder is
     always the compile anchor version.
   - Inserted CI validation as roadmap step 8 and moved Modrinth publishing
     automation to step 9.

-13. Compatibility matrix research (unreleased):
   - Added `COMPATIBILITY.md` with the current Minecraft compatibility probe
     matrix and source links.
   - Confirmed the current split `2.6.3` release jars should be listed on
     Modrinth for Minecraft `1.21.11` only.
   - Compile probes against every `1.21.x`, `1.20.x`, and `1.19.x` release show
     the current source only compiles as-is on `1.21.11`.
   - Next compatibility work should produce dedicated version-profile builds and
     launcher smoke tests before marking additional Modrinth game versions.

-12. Multi-version build profile foundation (unreleased):
   - Added Gradle Minecraft version profiles under `gradle/version-profiles/`.
   - Default and supported release builds remain on the tested `1.21.11` profile.
   - Added candidate `26.1.2` and `26.2-pre-3` profiles for migration work; both
     require Java 25 before source/API compile validation.
   - Upgraded the Gradle wrapper to 9.4.0 for Loom 1.16 candidate profile support.
   - Release jars now collect under `build/release/<profile_id>/`.

-11. License alignment (unreleased):
   - Replaced the old repo license text with LGPL-3.0-only and added the GPLv3
     text referenced by LGPLv3.
   - Updated README and all Fabric metadata to report `LGPL-3.0-only`, matching
     the Modrinth project license selection.
   - Updated jar packaging so public artifacts include both license documents.

-10. Split command root tidy (unreleased):
   - Removed the old combined `ModCommands` aggregator and stopped Core from
     registering `/inventorysort world`.
   - Moved Catalogue commands from `/inventorysort catalog ...` to
     `/inventorycatalogue ...`.
   - Registered shared world-profile commands under feature roots:
     `/inventorycatalogue world ...` and `/inventorysearch world ...`.
   - Updated README command examples and in-game Catalogue prompt/error text to
     use the new command roots.

-9. Release jar runtime crash fix (unreleased):
   - Public Sort/Search/Catalogue jars now embed Core's remapped release jar
     instead of the development-namespaced `-dev` jar.
   - Fixed normal launcher crashes at Core/Search client entrypoint time when
     installing the split public jars.
   - Added `verifyReleaseJars` to `clean build`/`buildAllMods` so future release
     builds fail if a public jar embeds the development Core jar again.
   - Manual launcher validation now passes for Sort-only, Search-only,
     Catalogue-only, Sort+Search, and all-three installs.

-8. One-click release jar collection (unreleased):
   - Added `collectReleaseJars`, which copies only the three public feature jars
     into `build/release/<profile_id>/`.
   - `buildAllMods` now builds Core + Sort/Search/Catalogue and produces a
     publish-ready `build/release/<profile_id>/` folder in one command.
   - CI artifact upload now collects `build/release/` recursively instead of
     module-local libs.

-7. Public module structure (unreleased):
   - Added Gradle subprojects for `inventorysort-core`, `inventorysort`,
     `inventorysearch`, and `inventorycatalogue`.
   - Public feature jars now build separately as `inventory-sort`,
     `inventory-search`, and `inventory-catalogue`.
   - Each public feature jar nests the shared Core jar, so users do not need to
     manually download a separate Core mod.
   - Module source ownership is selected from the existing shared `src/client/java`
     tree, preserving one code location while producing separate artifacts.
   - Added module-specific `fabric.mod.json` and mixin configs for Core, Sort, and
     Search; Catalogue has no mixins.

-6. Module-readiness split prep (unreleased):
   - Split the old combined `HandledScreenMixin` so Sort owns sort/transfer buttons
     and Search owns its inventory search button through `SearchButtonMixin`.
   - Added separate client entrypoints for Core, Search, and Catalogue while keeping
     the existing Sort client entrypoint.
   - Moved feature/shared logging onto `InventorySortCore.LOGGER` so Search and
     Catalogue no longer depend on the Sort client class.
   - Updated the temporary combined `fabric.mod.json` entrypoint list so local
     single-project runs still initialize all features before the Gradle module
     split lands.

-5. Core event wiring / hard-coupling break (unreleased):
   - `ContainerTrackingMixin` now publishes Core inventory/container snapshot events
     instead of directly calling Search (`ItemLocationTracker`) and Catalogue
     (`CatalogSession`) internals.
   - Added `InventorySearchFeature` and `InventoryCatalogueFeature` as event
     subscribers around the existing Search and Catalogue behavior.
   - `ServerWorldProfileManager` now publishes a Core namespace-change event instead
     of directly reloading Search tracker state.
   - `InventorySortClient` delegates Search/Catalogue startup to feature bridge
     classes.
   - Split command implementation into `InventoryCatalogueCommands` and
     `WorldProfileCommands`.

-4. Core foundation / split prep (unreleased):
   - Added `tempeststudios.inventorysort.core.InventorySortCore` for shared mod id
     and logger ownership.
   - Added `tempeststudios.inventorysort.core.InventorySortEvents` with Core event
     contracts for namespace changes, container snapshots, and inventory snapshots.
   - Switched `InventorySortClient` to read its active mod id/logger from Core.
   - Behavior is intentionally unchanged; the next checkpoint wires Search and
     Catalogue through these events to remove direct coupling.

-3. Baseline cleanup / split prep (unreleased):
   - Added `AGENTS.md` so future work keeps `TODO.md`, `CHANGELOG.md`, verification,
     and commits synchronized after every major step.
   - Added `CHANGELOG.md` and recorded the current baseline cleanup.
   - Removed unused Fabric template scaffolding: `InventorySort.java`,
     `ExampleMixin.java`, and the unused client mixin config referencing the missing
     `ExampleClientMixin`.
   - Fixed `fabric.mod.json` icon path to match `assets/inventory-sort/icon.png`.

-2. Tracker/search hardening (2.6.3):
   - Modded/custom dimensions now round-trip: `LocationEntry` stores the dimension as
     a generic id string and deserialization keeps it verbatim (no more overworld
     collapse). Legacy entries self-heal on next container visit.
   - Search results compute tracker data lazily: `buildRowForEntry` does cheap work
     only; tracked counts/locations are queried + formatted on demand and cached, so
     a 400-result query no longer hits the tracker for off-screen/collapsed rows.
   - Wheel/▲▼ scroll snaps to row boundaries (one notch = one row) regardless of
     whether the top row is expanded.

-1. Small grid containers (2.6.2):
   - Sort + transfer buttons now appear on droppers and dispensers via a
     `DispenserMenu` gate in `HandledScreenMixin`; dispenser counts as a 3-row grid.
   - Hoppers, furnaces, and brewing stands stay excluded (functional / not wanted).

0. Quick-win patch (2.6.1) — from the deep-dive audit:
   - Legible input text: world-selector and search boxes now use light text on the
     dark recessed field (was black-on-near-black, invisible).
   - Search columns: count column position scales from modal width and the item
     name truncates before it, so long names no longer overlap the count.
   - Esc returns to parent: `SearchModalScreen` and `ServerWorldProfileScreen`
     override `onClose()` to go back to the screen that opened them.
   - Shulker tracking now flushed to disk on container close instead of waiting for
     the next unrelated save / shutdown hook.

1. Placed shulker identical-content collision:
   - Restored same-content cleanup for placed shulkers.
   - This can undercount separate identical shulkers, but avoids permanent stale duplicates when a shulker is broken and placed elsewhere.

2. Catalog `includeInventory` double-counting:
   - Player inventory fingerprint no longer varies by player position.
   - Moving and reopening inventory during one catalog session should not count it again.

3. Search tracked count semantics:
   - Collapsed search rows now show total known tracked storage count when the item is not in the live inventory.

4. Routine log noise:
   - Normal container open/close tracking, save/update messages, and search result diagnostics moved from `info` to `debug`.

## P2 Accuracy And Data Hardening

1. Potion/component-aware tracking:
   - Search aliases let `water bottle` find `minecraft:potion`, but item identity is still base item only.
   - Catalog and tracking currently merge component variants such as potions, enchanted books, named items, and filled containers.
   - True variant tracking needs component-aware keys and display names.

2. Custom/modded dimensions:
   - DONE (2.6.3): dimension ids are now stored/restored generically in the tracker
     (`LocationEntry`) and were already generic in `CatalogStore`; no more overworld
     fallback for custom dimensions.

3. Corrupt JSON hardening:
   - Bad tracking/profile JSON should not crash or poison load.
   - Catch parse/runtime exceptions, skip bad entries, and consider writing a `.bak` before overwriting.

4. Catalog mode cleanup:
   - DONE: Rebuilt catalogue on the ContainerIdentity/namespace model (`CatalogStore` + `CatalogSession`).
   - Now per-world, persistent, identity-keyed (single vs double chest, per shulker/ender/minecart), and reopening refreshes instead of double-counting.
   - Added `catalog report`/`catalog clear` commands and a full plain-text report file under `inventorysort/catalog/`.
   - Remaining: catalogue totals are still item-id based, so they merge component variants (potions, enchanted books, named items) — needs the shared component-aware key work.

5. Old data cleanup/migration:
   - Earlier dev builds may have stale locations such as crafting tables or old fake inventory coordinates.
   - Add a cleanup/migration command if old data becomes annoying.

## UX Polish

1. Portable shulkers:
   - Shulkers opened from inventory are still weak/history-ish because identity is based on contents.
   - Decide whether to skip them or build a deliberate portable-container model.

2. Search default view:
   - Empty search only shows in-session recent inventory items.
   - Consider recent tracked storage hits or a more useful default summary after restart.

3. Component-specific search:
   - Search cannot query potion type, enchantment, custom name, shulker contents, or similar stack details yet.

4. Server world profile UI:
   - Profile selector only shows the first few profiles.
   - Add scrolling/search if HC world count keeps climbing.

5. HUD/profile display:
   - Make HUD prompt position configurable if it clashes with other HUD mods.
   - Consider shortening profile names in HUD if they are long.

## Performance And Logging

1. Inventory sampler save churn:
   - Inventory sampler saves whenever inventory signature changes.
   - Watch long sessions and busy servers for save churn.

2. Log/chat noise:
   - Watch for remaining chat/log spam during manual testing.
   - Keep important warnings visible, move routine diagnostics to `debug`.

## Deep-Dive Audit (2026-05-30)

Full read-through of all three segments (Sort / Search / Catalogue) plus infra,
mixins, and config. Severity: 🔴 high, 🟠 medium, 🟡 low, 🟢 nit. Nothing here is
implemented yet — this is the backlog backup.

### Confirmed UI bugs

1. 🔴 (DONE 2.6.1) Black text on near-black input background (both text inputs):
   - `ServerWorldProfileScreen` world-name box sets `setTextColor(0xFF000000)`
     (`ServerWorldProfileScreen.java:44`) over `drawRecessedPanel` fill
     `COLOR_RECESSED_BACKGROUND = 0xFF121212` → typed text is invisible. This is
     the reported world-selector bug.
   - Same root cause in the Search modal search box (`SearchModalScreen.java:120`
     + recessed panel at `:222`). Fix both: use a light text color (or lighten
     the recessed fill) so input is legible. Consider a shared helper so all mod
     inputs stay consistent.

### Other UI issues

1. 🔴 (DONE 2.6.1) Search results: name column overlaps the count column. Count
   column now scales from content width and the name truncates before it.
2. 🟠 (DONE 2.6.1) Count column position no longer hardcoded — derived from
   `listContentW` with reserved room on the right.
3. 🟠 (DONE 2.6.1) Esc abandons the parent screen — `SearchModalScreen` and
   `ServerWorldProfileScreen` now override `onClose()` to route back to parent.
4. 🟡 (DONE 2.6.3) Wheel scroll always moves `ROW_H+4` even past expanded rows —
   scroll now snaps to row boundaries so one notch advances exactly one row.
5. 🟡 Bottom-most visible row often can't be expanded: `updateLayout` `withinClip`
   check (`SearchModalScreen.java:367`) hides a ▶ button if partially clipped even
   though the row text is on screen.
6. 🟡 Sort buttons vs recipe book: `calcButtonX` (`HandledScreenMixin.java:44`)
   anchors right then falls back to left edge; when the recipe book shifts `leftPos`
   on narrow screens the buttons can float over the GUI.

### Dead scaffolding / consistency

1. DONE (baseline cleanup): Dead template code removed:
   - Deleted unused `InventorySort.java` (`ModInitializer`).
   - Deleted unused `ExampleMixin.java`.
   - Deleted unused `inventory-sort.client.mixins.json` that referenced the missing
     `ExampleClientMixin`.
2. DONE (baseline cleanup): `MOD_ID` inconsistency removed with the unused
   `InventorySort` initializer. Active client id remains `inventorysort`, matching
   `fabric.mod.json`.
3. DONE (baseline cleanup): `fabric.mod.json` icon path now matches the checked-in
   `assets/inventory-sort/icon.png` asset.

### Sort segment

1. 🟠 (DONE 2.6.2) Small containers get no sort/transfer buttons — gate now also
   accepts `DispenserMenu` (dropper/dispenser). Hoppers, furnaces, and brewing
   stands remain excluded on purpose. `InventorySorter`'s existing name-based slot
   detection already handled these once buttons were wired.
2. 🟠 Click-storm desync risk: sorting/transfers drive hundreds of synchronous
   `slotClicked` packets (compact→restack→apply→restack→compact; shift-all quick-
   moves every slot). Risk of ghost items / rollback / kicks on rate-limited
   servers. Consider throttling/batching or a known-limitation note.
3. 🟡 `"Crafting"` name match (`InventorySorter.java:257`, `:834`) would include the
   crafting result slot if ever reached (`containerSize = total-36`). Currently
   unreachable but a latent trap.
4. 🟡 Dead/misleading code:
   - `fillPlayerStacksFromContainer` (`:199`) never called.
   - `findFirstEmptyNonBundle` (`:722`) is identical to `findFirstEmpty` — does not
     actually skip bundles despite the name.
   - Bundle-skip branches in `ensureCursorEmpty` (`:749`, `:759`) are unreachable
     (sit after an `isEmpty()` early-return).
5. 🟢 "Sort container" also tops up the hotbar from main inventory first (`:34-40`)
   — a container action silently reshuffles player inventory.

### Search segment

1. 🟠 (DONE 2.6.1) Shulker tracking never explicitly saved — `ContainerTrackingMixin`
   now flushes `tracker.save()` after tracking a portable shulker's contents.
2. 🔴 (DONE 2.6.3) Modded dimensions corrupt on reload — `LocationEntry` now keeps a
   generic dimension id string and deserialization preserves it verbatim instead of
   round-tripping through `Level.OVERWORLD`.
3. 🟠 (DONE 2.6.3) Per-keystroke eager work — tracker queries and location-string
   formatting moved to lazy, cached accessors on `ResultRow`; `buildRowForEntry` now
   does only cheap snapshot work.
4. 🟡 Dead branch: `formatLocation` INVENTORY case checks `loc.getPos() != null`
   (`SearchModalScreen.java:515`) but inventory entries always have `pos == null`
   and are filtered out before formatting anyway.
5. 🟡 World-confirmation uses global GLFW ENTER/BACKSPACE polling
   (`ServerWorldProfileManager.java:91`); ignores keybind remaps and hijacks those
   keys while active (does correctly bail when a screen is open). A real keybinding
   would be cleaner.

### Catalogue segment (new code, self-review)

1. 🟡 Empty containers are recorded as zero-item snapshots, inflating "locations
   catalogued" without adding items. Conscious decision needed.
2. 🟡 Inherits identical-shulker collision: portable shulkers keyed by a 5-slot
   content hash (`ContainerTrackingMixin.generateContainerHash`), so two identically
   filled shulkers collide and count once.
3. 🟢 No atomic/`.bak` write on `CatalogStore.save()`; a crash mid-write loses that
   namespace's catalogue (loader catches and starts fresh — no crash).
4. 🟢 No in-game catalogue GUI yet (command-only). Natural future feature reusing
   the search modal styling.

### Performance

1. 🟠 Inventory sampler disk churn: `InventoryHistorySampler.sample` runs every tick
   and any inventory-total change triggers a full-file `save()`
   (`ItemLocationTracker.java:141`). Mining/combat → many whole-JSON rewrites/sec.
   Debounce / dirty-flag with periodic flush. (Overlaps "Performance And Logging" #1.)

## Mod Split Plan: InvSort / InvSearch / InvCatalogue

A clean flat 3-way split is not possible without extracting a shared **Core**.
Recommended shape: **Core + Sort + Search + Catalogue** (Sort/Search/Catalogue all
depend on Core; Catalogue and Search both need Core's identity/namespace/event layer).

### Implementation Roadmap

1. Baseline cleanup:
   - Remove dead template scaffolding and fix metadata drift while preserving the
     current single-mod behavior.
2. Extract Core:
   - Create the shared Core layer for identity/capture, world scoping, common UI,
     accessor/invoker mixins, and feature events.
   - Current status: Core constants/logger and event contracts are in place.
     Remaining extraction work is moving/wiring shared identity, world, UI, and
     mixin code as modules are split.
3. Break hard coupling:
   - Replace direct Search/Catalogue calls in shared code with Core events.
   - Replace direct namespace reload calls with a namespace-change event.
   - Split feature command registration and client startup responsibilities.
   - Current status: snapshot events, namespace-change event wiring, and feature
     startup bridges are in place. Catalogue/world command implementations are
     split and now register through feature-specific roots.
4. Create public modules:
   - Build separate Sort, Search, and Catalogue modules from the shared Core.
   - Keep user installation simple by packaging Core so users do not install it
     manually.
   - Current status: DONE for the current Minecraft target. Gradle now builds
     Core + Sort/Search/Catalogue modules, with Core nested inside each public
     feature jar.
5. Add one-click local build:
   - Add root Gradle tasks such as `buildAllMods` and later `publishAllModrinth`.
   - Collect public release jars in a predictable output folder.
   - Current status: DONE for local release builds. `buildAllMods` now leaves the
     three public, publish-ready jars in `build/release/<profile_id>/`.
6. Validate install combinations:
   - Test Sort only, Search only, Catalogue only, pairwise combinations, and all
     three together.
   - Current status: DONE for the `1.21.11` split release jars.
7. Add multi-version support:
   - Once the split is stable on the current target, compile the same modules
     against compatibility-group profiles.
   - Profiles should build one jar for a tested Minecraft version range, not
     automatically one jar per patch version.
   - Planned profile fields:
     - `profile_id`: release/profile folder id such as `1.21.6-1.21.11`.
     - `minecraft_version`: compile anchor used by Loom/mappings.
     - `minecraft_dependency`: Fabric Loader dependency range for metadata.
     - `modrinth_game_versions`: exact game versions to publish after smoke tests.
     - `compat_group`: source overlay group for version-specific APIs.
   - Current status: DONE for broad `1.20.x` and `1.21.x` supported coverage.
     Gradle supports compatibility-group profile metadata, profile-id release
     folders, generated Fabric dependency ranges, profile metadata verification,
     and `src/compat/<compat_group>/` source overlays. Grouped jars now compile,
     build, and pass automated client smoke launches for
     `1.21.9-1.21.10`, `1.21.6-1.21.8`, `1.21-1.21.5`,
     `1.20.5-1.20.6`, and `1.20-1.20.4`. They were promoted into
     `supported_minecraft_version_profiles` for the `3.0.0` release.
     `1.19.x` and older are intentionally out of scope for now.
     The next compatibility lane is `26.x`; use the dedicated forward roadmap
     below rather than moving the shared baseline to `26.x`.
8. Add CI validation before publishing:
   - Add automated build verification for every supported compatibility-group
     profile.
   - Run unit tests where possible and keep `verifyReleaseJars` in the CI path.
   - Add launcher smoke tests for every Minecraft version listed by a profile's
     `modrinth_game_versions`; only those passing versions may be published.
   - CI should produce build artifacts grouped by profile/range so manual testing
     and Modrinth upload metadata stay aligned.
   - Current status: DONE for the current validation matrix. `ciValidation` now
     builds supported profiles and any configured candidate profiles, verifies
     release jar metadata and nested Core jars, checks smoke-test records, and
     launches exact Minecraft runtimes with the packaged release jars as
     standalone and all-three installs. `publishValidation` is now available as
     the fast supported-only gate before Modrinth publishing. Candidate profiles
     are tracked separately from publishable supported profiles.
9. Configure Modrinth publishing:
   - Give each public feature mod its own Modrinth project id and upload metadata.
   - Publish the correct jar, Minecraft version, loader, dependencies, and
     changelog for each target.
   - Current status: DONE for the `3.0.0` public release. Gradle prepares and
     dry-runs Modrinth uploads for supported profiles only, with a guarded real
     upload task and manual GitHub Actions workflow. Candidate profiles are
     ignored by publishing until promoted into
     `supported_minecraft_version_profiles`.

## Minecraft 26.x Forward Compatibility Roadmap

Goal: keep one repository and one shared Core/feature codebase. The current
shared source remains anchored to the smoke-tested `1.20.x`/`1.21.x` baseline;
`26.x` is added as forward candidate compatibility profiles. Do not move the
shared baseline to `26.x` unless forward overlays prove too fragile.

Known `26.x` constraints:

- Minecraft `26.x` requires Java 25.
- Mojang removed the obfuscation/mapping layer for `26.x`, so `26.x` profiles
  need non-remapping Fabric Loom (`net.fabricmc.fabric-loom`) while `1.20`/`1.21`
  profiles still use `net.fabricmc.fabric-loom-remap`.
- `26.x` profiles should use normal `implementation` dependencies and `jar`
  release artifacts instead of remapped `modImplementation` and `remapJar`
  artifacts.
- The current compile probe reached source/API errors after a temporary
  non-remap build-path change, which means one-repo forward support still looks
  plausible.
- Representative `26.x` API shifts observed in the probe:
  - `GuiGraphics` render methods become `GuiGraphicsExtractor` / render-state
    extraction methods.
  - `HudRenderCallback` moves to the Fabric HUD element registry API.
  - `ClientCommandManager` becomes `ClientCommands`.
  - `ClickType` becomes `ContainerInput`.
  - Player chat feedback uses `sendSystemMessage` / `sendOverlayMessage` instead
    of `displayClientMessage`.

Forward tasks:

1. Build-system foundation:
   - Add a profile flag such as `unobfuscated_minecraft=true` for `26.x`
     profiles.
   - Load both Loom plugin ids in `settings.gradle`, then select remap vs
     non-remap in root/subprojects by profile.
   - Skip `loom.officialMojangMappings()` for `26.x` profiles.
   - Select `modImplementation`/`remapJar` for remapped profiles and
     `implementation`/`jar` for unobfuscated profiles.
   - Replace `namedElements` project dependencies with normal/client output
     dependencies when running `26.x` profiles.
   - Verify default `1.21.11` `buildAllMods` still passes and a `26.x`
     `printVersionProfile` configures under Java 25.
   - Current status: DONE. Profile metadata now selects remap vs non-remap Loom,
     Fabric dependency configuration, release artifact task, and Core project
     dependency shape. Default `1.21.11` still builds, and both `26.x` candidate
     profiles configure through the non-remap lane.
2. Java 25 toolchain and CI:
   - Decide whether Gradle should always run on Java 25 or whether workflows
     should select Java by profile/job.
   - Update GitHub Actions/manual validation so `26.x` candidate builds can run
     without breaking `1.20`/`1.21` release builds.
   - Keep normal push/PR builds fast; use full `26.x` validation only in
     candidate validation/publish workflows.
   - Current status: DONE. Compile/run tasks now request the active profile's
     Java toolchain. Normal push/PR builds remain on Java 21/default profile,
     the manual compatibility-validation workflow defaults to Java 25 for
     focused `26.x` candidate builds, and Modrinth publishing can be switched to
     Java 25 when a `26.x` profile is promoted.
3. `26.x` Core compatibility overlay:
   - Add `src/compat/26.1.2/.../MinecraftApiCompat.java`.
   - Port dimension id, chest neighbor lookup, window handle, single-player
     directory, HUD pose, and player feedback helpers to `26.x` names.
   - Add Core compile probing as the first validation target.
   - Current status: DONE for the helper/adapter layer. `26.1.2` and
     `26.2-pre-3` now have Core compatibility overlays for Minecraft helper
     calls and the Fabric command builder rename, and shared player feedback
     now goes through `MinecraftApiCompat`. The `26.1.2` Core compile probe
     then stopped at the expected GUI/render extraction, HUD registry, and
     container input blockers. GUI/HUD/screen blockers were resolved by item 33.
4. GUI/rendering abstraction:
   - Refactor shared drawing helpers so shared logic does not directly depend on
     `GuiGraphics`.
   - Add version-specific wrappers/adapters for `1.20`/`1.21` `GuiGraphics` and
     `26.x` `GuiGraphicsExtractor`.
   - Port text buttons, icon buttons, modal buttons, HUD, and profile/search
     screens through those adapters.
   - Current status: DONE for Core and shared drawing helpers. `InventorySortDrawContext`
     now isolates shared renderers from Minecraft's render object, Core HUD
     registration is versioned, and `26.x` profile-screen/button overlays use
     the new extraction lifecycle. Search's full `26.x` modal-screen lifecycle
     remains for the Search feature compile pass.
5. Fabric API event/command updates:
   - Replace or wrap `HudRenderCallback` with the `26.x` HUD element registry.
   - Replace or wrap `ClientCommandManager` with `26.x` `ClientCommands`.
   - Keep command bodies/shared handlers version-independent.
   - Current status: DONE. Command builder calls are wrapped with
     `ClientCommandCompat`, HUD registration is wrapped with `HudCompat`, and
     shared command-registration/tick-event callbacks still exist in the checked
     `26.x` Fabric API jars.
6. Container/mixin input updates:
   - Port `ClickType` invokers and slot-click mixins to `26.x`
     `ContainerInput`.
   - Re-check `AbstractContainerScreen` render/extract method names and
     recipe-book integration.
   - Keep sort/search/catalogue behavior code out of version-specific mixins.
7. Feature compile passes:
   - After Core compiles, compile Sort.
   - Then compile Search.
   - Then compile Catalogue.
   - Treat each feature pass as its own checkpoint with TODO/CHANGELOG/commit.
8. `26.x` smoke testing:
   - Add pending/pass smoke records for exact `26.x` runtimes.
   - Extend the smoke launcher if `26.x` client launch semantics differ.
   - Run selected smoke tests first, then full candidate `ciValidation`.
9. Publication promotion:
   - Keep `26.x` in `candidate_minecraft_version_profiles` until compile and
     smoke tests pass.
   - Once passing, promote it to `supported_minecraft_version_profiles`.
   - Add `gradle/release-notes/<version>.md`, run the Modrinth publish workflow,
     and list the smoke-passed `26.x` versions.
10. Branch fallback trigger:
   - Branch only if `26.x` forces duplicated feature logic rather than isolated
     build config, rendering adapters, and mixin surfaces.
   - If branching becomes necessary, keep `main` as the current `1.20`/`1.21`
     release lane and create an explicit `26.x` development branch with a
     patch-forward process.

### Goes in Core (shared by 2+ features)

- Identity & capture: `ContainerIdentity`, `ContainerPositionCapture`,
  `MultiPlayerGameModeMixin`.
- World scoping: `TrackingNamespace`, `ServerWorldProfileManager`,
  `ServerWorldProfileHud`, `ServerWorldProfileScreen`.
- UI primitives: `InventorySortUIUtils`, shared button renderers, and the
  compat-selected concrete button classes.
- Accessor/invoker mixins: `ScreenAccessor`, `AbstractContainerScreenAccessor`,
  `AbstractContainerScreenInvoker`, `AbstractContainerMenuInvoker`.

### Hard coupling points that must be broken first

Current status:
- DONE: `ContainerTrackingMixin` publishes Core snapshot events instead of calling
  Search/Catalogue internals directly.
- DONE: `ServerWorldProfileManager.setActiveProfile` publishes a namespace-change
  event instead of directly reloading Search state.
- DONE: Catalogue and world-profile command implementations are split and
  registered through feature-specific roots.
- PARTIAL: `InventorySortClient` delegates Search/Catalogue startup to bridge
  classes. Full completion waits for separate modules, entrypoints, metadata, and
  mixin configs.

Original audit bullets, retained for context:

1. `ContainerTrackingMixin` calls BOTH `ItemLocationTracker` (Search) and
   `CatalogSession` (Catalogue) directly. Core should own this mixin and fire events
   (`onContainerClosed(identity, items)`, `onInventorySnapshot(items)`); Search and
   Catalogue subscribe. Today Catalogue cannot exist without Search's mixin.
2. `ServerWorldProfileManager.setActiveProfile` reaches into Search
   (`ItemLocationTracker.reloadForCurrentNamespace`, `InventoryHistorySampler.reset`).
   Replace with a "namespace changed" event each store subscribes to (incl.
   `CatalogStore`).
3. DONE: Catalogue and World-profile commands no longer share a combined
   `/inventorysort` root. Catalogue owns `/inventorycatalogue ...`, and shared
   world-profile commands are registered under feature roots.
4. `InventorySortClient` is one entrypoint doing everything (tracker init, commands,
   tick sampler + confirmation, HUD, shutdown save). Each mod needs its own
   `ClientModInitializer`, `fabric.mod.json`, mixin json + package, partitioned
   registrations.

### Notes

- Sort is the most independent (only Core invoker/accessor mixins + UI).
- The event-bus refactor (points 1–2) is the keystone; without it Search and
  Catalogue stay welded together through `ContainerTrackingMixin`.
- Repackage `tempeststudios.inventorysort.*` into `…core` / `…invsort` /
  `…invsearch` / `…invcatalogue`; split the single mixin json accordingly.

## Release Process

1. Run `./gradlew.bat compileJava compileClientJava` after each patch chunk.
2. Run full `./gradlew.bat build` before release.
3. Push/verify release build after each patch chunk.
