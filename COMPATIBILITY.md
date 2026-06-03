# Minecraft Compatibility

Research date: 2026-06-03

## Recommendation

The only currently supported/publishable profile is **Minecraft 1.21.11**.

Candidate grouped jars now compile, build, and pass automated client launch
smoke testing across **Minecraft 1.20.x and 1.21.x**. They are intentionally
kept in `candidate_minecraft_version_profiles` until we deliberately promote
them to `supported_minecraft_version_profiles` for Modrinth listing.

Do not mark `1.19.x` or older as compatible. Those versions remain out of scope
because the current UI and rendering code depends on newer Minecraft APIs.

For publishing, build a dedicated compatibility-group jar, launch-test that
exact jar on every Minecraft version listed by the profile, then list those game
versions on the Modrinth version entry for that file. Modrinth guidance also
treats separate files for separate game versions as separate project versions
rather than extra files on one upload.

## Compatibility-Group Build Strategy

Multi-version support uses **release compatibility groups** rather than one build
profile for every Minecraft patch version.

A compatibility-group profile means:

- compile one jar against a selected anchor Minecraft version
- use a single compat source group for that API shape
- declare the Fabric `minecraft` dependency range that the jar is intended to
  support
- list the exact Modrinth game versions that the same jar has passed smoke
  testing on
- collect release jars under a profile/range output folder instead of assuming
  the output folder is just the anchor Minecraft version

Gradle now supports these profile fields:

Example:

```properties
profile_id=1.21.6-1.21.11
minecraft_version=1.21.11
minecraft_dependency=>=1.21.6 <=1.21.11
modrinth_game_versions=1.21.6,1.21.7,1.21.8,1.21.9,1.21.10,1.21.11
compat_group=1.21_late
loader_version=0.18.4
loom_version=1.14-SNAPSHOT
fabric_api_version=0.141.4+1.21.11
java_version=21
```

The compile anchor only proves the jar builds against that version's APIs. Before
listing a range on Modrinth, CI or manual validation must launch-test that exact
jar on every Minecraft version listed in `modrinth_game_versions`. The current
CI path launches each public jar standalone plus all public jars together.

If an in-between version fails, split the range into smaller compatibility groups
instead of publishing an untested or partially compatible range.

Compatibility-specific source can live under:

```text
src/compat/<compat_group>/client/java/
src/compat/<compat_group>/client/resources/
```

The current `1.21.11` profile uses `compat_group=1.21.11`. Compatibility source
is already used for small API differences such as custom button render hooks and
dimension id access; shared feature logic should stay in `src/client/java`.

## Current Release Artifacts

The current publish-ready artifacts in `build/release/1.21.11/` are:

| Jar | Mod id | Minecraft dependency | Java dependency |
| --- | --- | --- | --- |
| `inventory-sort-2.6.3.jar` | `inventorysort` | `~1.21.11` | `>=21` |
| `inventory-search-2.6.3.jar` | `inventorysearch` | `~1.21.11` | `>=21` |
| `inventory-catalogue-2.6.3.jar` | `inventorycatalogue` | `~1.21.11` | `>=21` |

These jars have been launch-tested on `1.21.11` in standalone and combined
install combinations.

## Candidate Artifacts

The candidate profiles build public release jars under
`build/release/<profile_id>/`. Each profile contains the three public feature
jars:

- `inventory-sort-2.6.3.jar`
- `inventory-search-2.6.3.jar`
- `inventory-catalogue-2.6.3.jar`

Current candidate profile metadata:

| Profile | Minecraft dependency | Java dependency | Smoke-tested game versions |
| --- | --- | --- | --- |
| `1.21.9-1.21.10` | `>=1.21.9 <=1.21.10` | `>=21` | `1.21.9`, `1.21.10` |
| `1.21.6-1.21.8` | `>=1.21.6 <=1.21.8` | `>=21` | `1.21.6`, `1.21.7`, `1.21.8` |
| `1.21-1.21.5` | `>=1.21 <=1.21.5` | `>=21` | `1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5` |
| `1.20.5-1.20.6` | `>=1.20.5 <=1.20.6` | `>=21` | `1.20.5`, `1.20.6` |
| `1.20-1.20.4` | `>=1.20 <=1.20.4` | `>=17` | `1.20`, `1.20.1`, `1.20.2`, `1.20.3`, `1.20.4` |

All candidate groups passed automated client smoke launches as Sort-only,
Search-only, Catalogue-only, and all-three installs. They still need the profile
promotion decision before any of these versions are listed on Modrinth.

Smoke-test records live in `gradle/smoke-tests.json`. CI runs
`verifySmokeTestMatrix` and `smokeTestValidationClients`: supported profiles
must have passing records and exact-runtime launches, while candidate profiles
can be tracked and tested without making them publishable.

## Compile Probe Method

The matrix below combines the original compile probes with the final
compatibility-group validation. Each target used:

- Fabric Loader `0.18.4`
- Loom `1.14-SNAPSHOT`
- the latest Fabric API artifact found for that Minecraft version
- the Java major version reported by Mojang version metadata

Probe command:

```powershell
.\gradlew.bat :inventorysort-core:compileClientJava :inventorysort:compileClientJava :inventorysearch:compileClientJava :inventorycatalogue:compileClientJava "-Pminecraft_version_profile=<version>" --no-daemon --console=plain
```

Final validation commands:

```powershell
.\gradlew.bat buildValidationVersions --no-daemon --console=plain
.\gradlew.bat ciValidation --no-daemon --console=plain
```

## Results

| Minecraft | Java | Compile result | Notes |
| --- | ---: | --- | --- |
| 1.21.11 | 21 | PASS | Current release target. |
| 1.21.10 | 21 | PASS (candidate) | Covered by grouped `1.21.9-1.21.10` profile. Automated client smoke launch passed. |
| 1.21.9 | 21 | PASS (candidate) | Covered by grouped `1.21.9-1.21.10` profile. Automated client smoke launch passed. |
| 1.21.8 | 21 | PASS (candidate) | Covered by grouped `1.21.6-1.21.8` profile. Automated client smoke launch passed. |
| 1.21.7 | 21 | PASS (candidate) | Covered by grouped `1.21.6-1.21.8` profile. Automated client smoke launch passed. |
| 1.21.6 | 21 | PASS (candidate) | Covered by grouped `1.21.6-1.21.8` profile. Automated client smoke launch passed. |
| 1.21.5 | 21 | PASS (candidate) | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21.4 | 21 | PASS (candidate) | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21.3 | 21 | PASS (candidate) | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21.2 | 21 | PASS (candidate) | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21.1 | 21 | PASS (candidate) | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21 | 21 | PASS (candidate) | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.20.6 | 21 | PASS (candidate) | Covered by grouped `1.20.5-1.20.6` profile. Automated client smoke launch passed. |
| 1.20.5 | 21 | PASS (candidate) | Covered by grouped `1.20.5-1.20.6` profile. Automated client smoke launch passed. |
| 1.20.4 | 17 | PASS (candidate) | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.20.3 | 17 | PASS (candidate) | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.20.2 | 17 | PASS (candidate) | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.20.1 | 17 | PASS (candidate) | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.20 | 17 | PASS (candidate) | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.19.4 | 17 | FAIL | Missing `GuiGraphics` and newer widget/rendering APIs. |
| 1.19.3 | 17 | FAIL | Same drift class as 1.19.4. |
| 1.19.2 | 17 | FAIL | Same drift class as 1.19.4, with additional older widget API differences. |
| 1.19.1 | 17 | FAIL | Same drift class as 1.19.2. |
| 1.19 | 17 | FAIL | Same drift class as 1.19.2. |

## Porting Implications

- `1.20.x` and `1.21.x` are now covered by smoke-passed candidate
  compatibility groups.
- Publishing any candidate group is now a promotion decision: move the profile
  from `candidate_minecraft_version_profiles` to
  `supported_minecraft_version_profiles`, then rerun `ciValidation`.
- `1.20.5` and newer use Java 21. `1.20` through `1.20.4` use Java 17.
- `1.19.x` remains a larger backport because the current UI code depends
  heavily on newer rendering and widget APIs.

## Sources

- Mojang/Piston version metadata:
  `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`
- Fabric API Maven metadata:
  `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml`
- Fabric Loader/Fabric Loom metadata:
  `https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml`
  and `https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml`
- Fabric `fabric.mod.json` dependency range documentation:
  `https://docs.fabricmc.net/develop/loader/fabric-mod-json`
- Modrinth guidance for separate files per game version:
  `https://support.modrinth.com/en/articles/8793363-additional-files`
