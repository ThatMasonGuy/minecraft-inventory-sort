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
release jars for every supported and candidate profile. Each profile/game version
is launched as:

- Inventory Sort only
- Inventory Search only
- Inventory Catalogue only
- all public feature jars together

The smoke launcher arms `InventorySortSmokeTest`, waits until the Minecraft
client reaches the tick loop, logs `INVENTORYSORT_SMOKE_TEST_PASS`, and closes
the client. On GitHub Actions, `ciValidation` runs these launches under `xvfb`.

Useful commands:

```powershell
.\gradlew.bat verifySmokeTestMatrix
.\gradlew.bat buildValidationVersions
.\gradlew.bat smokeTestValidationClients
.\gradlew.bat ciValidation
```

After a candidate profile passes client smoke testing on every version in
`modrinth_game_versions`, update its records to `pass`, move the profile from
`candidate_minecraft_version_profiles` to `supported_minecraft_version_profiles`,
then run `.\gradlew.bat ciValidation`.

For Linux/headless CI:

```bash
./gradlew ciValidation -Pinventorysort_smoke_xvfb=true --no-daemon --console=plain
```
