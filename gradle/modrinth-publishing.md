# Modrinth Publishing

Modrinth publishing is driven by supported Minecraft version profiles only.
Profiles in `candidate_minecraft_version_profiles` are ignored until they are
promoted to `supported_minecraft_version_profiles`.

## Tasks

```powershell
.\gradlew.bat publishValidation
.\gradlew.bat prepareModrinthUploads
.\gradlew.bat publishModrinthDryRun
.\gradlew.bat publishModrinth -Pmodrinth_confirm_publish=true
```

- `publishValidation` builds and smoke-tests supported profiles only.
- `prepareModrinthUploads` runs `publishValidation`, validates upload metadata,
  and writes `build/modrinth/upload-plan.json`.
- `publishModrinthDryRun` performs the full local validation path without
  calling the Modrinth API.
- `publishModrinth` performs the real upload and requires
  `-Pmodrinth_confirm_publish=true`.

## Secrets

Real uploads require a Modrinth personal access token with `VERSION_CREATE`
scope. Provide it through one of these non-repo locations:

```powershell
$env:MODRINTH_TOKEN="..."
.\gradlew.bat publishModrinth -Pmodrinth_confirm_publish=true
```

or a user-level Gradle property such as `%USERPROFILE%\.gradle\gradle.properties`:

```properties
modrinth_token=...
```

Do not store tokens in this repository.

## Release Notes

Modrinth changelogs come from a concise per-version release note file:

```text
gradle/release-notes/<mod_version>.md
```

For example, `mod_version=3.0.0` requires:

```text
gradle/release-notes/3.0.0.md
```

The publish tasks fail if the release note file is missing or blank. This keeps
Modrinth uploads focused on what changed in that release instead of reposting
the entire project changelog.

Use `CHANGELOG.md` for the broad repo history, and use
`gradle/release-notes/<version>.md` for the exact Modrinth-facing notes. To test
or publish with a different notes file, pass:

```powershell
.\gradlew.bat publishModrinthDryRun "-Pmodrinth_changelog_file=gradle/release-notes/3.0.0.md"
```

## Project IDs

The public Modrinth project IDs are stored in `gradle.properties`:

- Inventory Sort: `modrinth_inventorysort_project_id`
- Inventory Search: `modrinth_inventorysearch_project_id`
- Inventory Catalogue: `modrinth_inventorycatalogue_project_id`

Fabric API is added as a required project dependency through
`modrinth_fabric_api_project_id`.

## Version Numbers

When only one supported profile exists, Modrinth `version_number` is the mod
version, such as `2.6.3`.

When multiple supported profiles exist, the default `auto` behavior appends the
profile id to keep Modrinth version entries unique per project, such as:

```text
3.0.0+mc1.21.11
3.0.0+mc1.21.9-1.21.10
3.0.0+mc1.20-1.20.4
```

Override with `-Pmodrinth_profile_version_suffix=always` or
`-Pmodrinth_profile_version_suffix=never` if needed.

## Useful Options

```powershell
.\gradlew.bat publishModrinthDryRun "-Pmodrinth_requested_status=unlisted"
.\gradlew.bat publishModrinthDryRun "-Pmodrinth_version_type=beta"
.\gradlew.bat publishModrinthDryRun "-Pmodrinth_featured=true"
```

GitHub Actions has a manual `modrinth publish` workflow. Its dry-run mode does
not require `MODRINTH_TOKEN`; real publishing requires the repository secret
`MODRINTH_TOKEN`. The workflow installs Java 17, Java 21, and Java 25 toolchains
before running the full supported-profile publish gate. The `gradle_java_version`
input controls only the Java runtime used to run Gradle itself; toolchain
selection still follows each Minecraft profile's `java_version`.

For normal development, run local fast builds such as `buildAllMods`, commit,
push, and then use the manual `modrinth publish` workflow when ready to release.
The regular push/PR workflow is intentionally quick; the Modrinth workflow runs
the expensive supported-profile build and smoke gate before upload.

## API References

- Modrinth API overview: `https://docs.modrinth.com/api/`
- Create version endpoint: `https://docs.modrinth.com/api/operations/createversion/`
