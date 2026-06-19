# Smoke Test Matrix

`gradle/smoke-tests.json` records client launch smoke-test status for each
release compatibility profile.

Status meanings:

- `pass`: this exact profile jar has launched on this exact Minecraft version.
- `pending`: the profile builds, but this Minecraft version still needs a
  client launch smoke test.
- `fail`: this Minecraft version has been tested and currently fails.

Only profiles listed in `supported_minecraft_version_profiles` are publishable.
`verifySmokeTestMatrix` fails if any supported profile/version is missing a
passing smoke record. Profiles listed in `candidate_minecraft_version_profiles`
must be present in the matrix, but may stay `pending` or `fail` while they are
not publishable.

`smokeTestValidationClients` launches exact Minecraft runtimes with the packaged
release jars for every supported and candidate profile. `smokeTestSupportedClients`
launches the same install combinations for supported/publishable profiles only,
which is the gate used before publishing. Each profile/game version is launched
as:

- Inventory Sort only
- Inventory Search only
- Inventory Catalogue only
- all public feature jars together

The smoke launcher arms `InventorySortSmokeTest`, waits until the Minecraft
client reaches the tick loop, force-loads key mixin targets, logs
`INVENTORYSORT_SMOKE_TEST_PASS`, and closes the client. On GitHub Actions, the
manual Modrinth publish workflow runs these launches under `xvfb` before
upload. Broad supported matrices can take close to an hour on a local machine,
so the manual Modrinth publish workflow is the normal place to run the full
matrix.

Smoke-test mode also starts an in-client watchdog after the harness is armed.
If the client stops producing ticks before the pass marker, it logs
`INVENTORYSORT_SMOKE_TEST_TIMEOUT` and exits the JVM instead of leaving the
outer Gradle smoke task running indefinitely. The default watchdog window is
180 seconds and can be overridden with
`-Dinventorysort.smokeTimeoutSeconds=<seconds>` when debugging timing issues.
The outer Gradle `Exec` smoke task also has a 300-second timeout, configurable
with `-Pinventorysort_smoke_exec_timeout_seconds=<seconds>`, so a frozen client
process cannot hold a GitHub runner until the workflow-level timeout.

Normal push/PR builds intentionally run only `buildAllMods` for the default
profile. Use local full smoke testing only when you specifically need it; the
manual Modrinth publish workflow is the normal expensive release gate. For
focused hosted smoke checks, the manual GitHub Actions `compatibility
validation` workflow can run `smokeTestSelectedClients` with specific
`inventorysort_smoke_profiles`, `inventorysort_smoke_game_versions`, and
`inventorysort_smoke_install_sets` filters under `xvfb`.

Useful commands:

```powershell
.\gradlew.bat verifySmokeTestMatrix
.\gradlew.bat smokeTestSupportedClients
.\gradlew.bat publishValidation
.\gradlew.bat buildValidationVersions
.\gradlew.bat smokeTestValidationClients
.\gradlew.bat ciValidation
```

For local spot checks, use `smokeTestSelectedClients` with one or more filters:

```powershell
.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=1.21.9-1.21.10" "-Pinventorysort_smoke_game_versions=1.21.10" "-Pinventorysort_smoke_install_sets=all-public"
```

Accepted install set ids are `inventorysort-only`, `inventorysearch-only`,
`inventorycatalogue-only`, and `all-public`. Nested smoke Gradle launches still
use `--no-daemon` by default; for local timing experiments, pass
`-Pinventorysort_smoke_nested_no_daemon=false`.

After a candidate profile passes client smoke testing on every version in
`modrinth_game_versions`, update its records to `pass`. To make that profile
publishable, move it from `candidate_minecraft_version_profiles` to
`supported_minecraft_version_profiles`, then run the manual Modrinth publish
workflow or `.\gradlew.bat ciValidation` if you explicitly want local proof.

For Linux/headless CI:

```bash
./gradlew ciValidation -Pinventorysort_smoke_xvfb=true --no-daemon --console=plain
```
