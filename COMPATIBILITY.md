# Minecraft Compatibility

Research date: 2026-06-03

## Recommendation

The current supported/publishable profiles cover **Minecraft 1.20.x,
1.21.x, and 26.x** through smoke-tested compatibility-group jars:

- `1.20` through `1.20.4` are covered by the grouped `1.20-1.20.4` jar.
- `1.20.5` and `1.20.6` are covered by the grouped `1.20.5-1.20.6` jar.
- `1.21` through `1.21.5` are covered by the grouped `1.21-1.21.5` jar.
- `1.21.6` through `1.21.8` are covered by the grouped `1.21.6-1.21.8` jar.
- `1.21.9` and `1.21.10` are covered by the grouped `1.21.9-1.21.10` jar.
- `1.21.11` is covered by the exact `1.21.11` jar.
- `26.1`, `26.1.1`, `26.1.2`, and `26.2-pre-3` are covered by the grouped
  `26.1-26.2-pre-3` jar.

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

## Default Development Profile Artifacts

The default development profile remains `1.21.11` so local/push builds stay
fast on Java 21. These artifacts can be built under `build/release/1.21.11/`
and are one of the `3.1.1` publish lane groups:

| Jar | Mod id | Minecraft dependency | Java dependency |
| --- | --- | --- | --- |
| `inventory-sort-<mod_version>.jar` | `inventorysort` | `~1.21.11` | `>=21` |
| `inventory-search-<mod_version>.jar` | `inventorysearch` | `~1.21.11` | `>=21` |
| `inventory-catalogue-<mod_version>.jar` | `inventorycatalogue` | `~1.21.11` | `>=21` |

These jars have been launch-tested on `1.21.11` in standalone and combined
install combinations.

## Current Supported Compatibility Artifacts

The supported profiles build public release jars under
`build/release/<profile_id>/`. Each profile contains the three public feature
jars:

- `inventory-sort-<mod_version>.jar`
- `inventory-search-<mod_version>.jar`
- `inventory-catalogue-<mod_version>.jar`

Current supported profile metadata:

| Profile | Minecraft dependency | Java dependency | Smoke-tested game versions |
| --- | --- | --- | --- |
| `1.20-1.20.4` | `>=1.20 <=1.20.4` | `>=17` | `1.20`, `1.20.1`, `1.20.2`, `1.20.3`, `1.20.4` |
| `1.20.5-1.20.6` | `>=1.20.5 <=1.20.6` | `>=21` | `1.20.5`, `1.20.6` |
| `1.21-1.21.5` | `>=1.21 <=1.21.5` | `>=21` | `1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5` |
| `1.21.6-1.21.8` | `>=1.21.6 <=1.21.8` | `>=21` | `1.21.6`, `1.21.7`, `1.21.8` |
| `1.21.9-1.21.10` | `>=1.21.9 <=1.21.10` | `>=21` | `1.21.9`, `1.21.10` |
| `1.21.11` | `~1.21.11` | `>=21` | `1.21.11` |
| `26.1-26.2-pre-3` | `>=26.1 <26.3` | `>=25` | `26.1`, `26.1.1`, `26.1.2`, `26.2-pre-3` |

All current supported groups passed automated client smoke launches as
Sort-only, Search-only, Catalogue-only, and all-three installs.

## Previously Published 3.0.0 Compatibility Artifacts

The `3.0.0` release lane previously covered Minecraft `1.20.x` and `1.21.x`.
Those same compatibility groups are now also part of the unified `3.1.1`
publish lane:

| Profile | Minecraft dependency | Java dependency | Smoke-tested game versions |
| --- | --- | --- | --- |
| `1.21.11` | `~1.21.11` | `>=21` | `1.21.11` |
| `1.21.9-1.21.10` | `>=1.21.9 <=1.21.10` | `>=21` | `1.21.9`, `1.21.10` |
| `1.21.6-1.21.8` | `>=1.21.6 <=1.21.8` | `>=21` | `1.21.6`, `1.21.7`, `1.21.8` |
| `1.21-1.21.5` | `>=1.21 <=1.21.5` | `>=21` | `1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5` |
| `1.20.5-1.20.6` | `>=1.20.5 <=1.20.6` | `>=21` | `1.20.5`, `1.20.6` |
| `1.20-1.20.4` | `>=1.20 <=1.20.4` | `>=17` | `1.20`, `1.20.1`, `1.20.2`, `1.20.3`, `1.20.4` |

All `3.0.0` compatibility groups passed automated client smoke launches as
Sort-only, Search-only, Catalogue-only, and all-three installs before being
published.

Smoke-test records live in `gradle/smoke-tests.json`. CI runs
`verifySmokeTestMatrix` and `smokeTestValidationClients`: supported profiles
must have passing records and exact-runtime launches before publishing.

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
| 26.2-pre-3 | 25 | PASS | Covered by grouped `26.1-26.2-pre-3` profile. Automated client smoke launch passed. |
| 26.1.2 | 25 | PASS | Covered by grouped `26.1-26.2-pre-3` profile. Automated client smoke launch passed. |
| 26.1.1 | 25 | PASS | Covered by grouped `26.1-26.2-pre-3` profile. Automated client smoke launch passed. |
| 26.1 | 25 | PASS | Covered by grouped `26.1-26.2-pre-3` profile. Automated client smoke launch passed. |
| 1.21.11 | 21 | PASS | Covered by default `1.21.11` profile. Automated client smoke launch passed. |
| 1.21.10 | 21 | PASS | Covered by grouped `1.21.9-1.21.10` profile. Automated client smoke launch passed. |
| 1.21.9 | 21 | PASS | Covered by grouped `1.21.9-1.21.10` profile. Automated client smoke launch passed. |
| 1.21.8 | 21 | PASS | Covered by grouped `1.21.6-1.21.8` profile. Automated client smoke launch passed. |
| 1.21.7 | 21 | PASS | Covered by grouped `1.21.6-1.21.8` profile. Automated client smoke launch passed. |
| 1.21.6 | 21 | PASS | Covered by grouped `1.21.6-1.21.8` profile. Automated client smoke launch passed. |
| 1.21.5 | 21 | PASS | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21.4 | 21 | PASS | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21.3 | 21 | PASS | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21.2 | 21 | PASS | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21.1 | 21 | PASS | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.21 | 21 | PASS | Covered by grouped `1.21-1.21.5` profile. Automated client smoke launch passed. |
| 1.20.6 | 21 | PASS | Covered by grouped `1.20.5-1.20.6` profile. Automated client smoke launch passed. |
| 1.20.5 | 21 | PASS | Covered by grouped `1.20.5-1.20.6` profile. Automated client smoke launch passed. |
| 1.20.4 | 17 | PASS | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.20.3 | 17 | PASS | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.20.2 | 17 | PASS | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.20.1 | 17 | PASS | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.20 | 17 | PASS | Covered by grouped `1.20-1.20.4` profile. Automated client smoke launch passed. |
| 1.19.4 | 17 | FAIL | Missing `GuiGraphics` and newer widget/rendering APIs. |
| 1.19.3 | 17 | FAIL | Same drift class as 1.19.4. |
| 1.19.2 | 17 | FAIL | Same drift class as 1.19.4, with additional older widget API differences. |
| 1.19.1 | 17 | FAIL | Same drift class as 1.19.2. |
| 1.19 | 17 | FAIL | Same drift class as 1.19.2. |

## Porting Implications

- `1.20.x`, `1.21.x`, and `26.x` are now covered by smoke-passed supported
  compatibility groups for the unified `3.1.1` publish lane.
- Future candidate groups should stay in `candidate_minecraft_version_profiles`
  until their exact runtime smoke tests pass, then move to
  `supported_minecraft_version_profiles` before publishing.
- `26.x` uses Java 25 and the non-remapping Fabric Loom lane.
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
