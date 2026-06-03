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
`MODRINTH_TOKEN`.

## API References

- Modrinth API overview: `https://docs.modrinth.com/api/`
- Create version endpoint: `https://docs.modrinth.com/api/operations/createversion/`
