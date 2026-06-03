# Smoke Test Matrix

`gradle/smoke-tests.json` records launcher smoke-test status for each release
compatibility profile.

Status meanings:

- `pass`: this exact profile jar has launched on this exact Minecraft version.
- `pending`: the profile builds, but this Minecraft version still needs a
  normal launcher smoke test.
- `fail`: this Minecraft version has been tested and currently fails.

Only profiles listed in `supported_minecraft_version_profiles` are publishable.
`verifySmokeTestMatrix` fails if any supported profile/version is missing a
passing smoke record. Profiles listed in `candidate_minecraft_version_profiles`
must be present in the matrix, but may stay `pending` or `fail` while they are
not publishable.

Useful commands:

```powershell
.\gradlew.bat verifySmokeTestMatrix
.\gradlew.bat buildValidationVersions
.\gradlew.bat ciValidation
```

After a candidate profile passes launcher smoke testing on every version in
`modrinth_game_versions`, update its records to `pass`, move the profile from
`candidate_minecraft_version_profiles` to `supported_minecraft_version_profiles`,
then run `.\gradlew.bat ciValidation`.
