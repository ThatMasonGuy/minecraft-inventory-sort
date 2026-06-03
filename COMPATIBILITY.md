# Minecraft Compatibility

Research date: 2026-06-03

## Recommendation

Publish the current split release jars for **Minecraft 1.21.11 only**.

Do not mark the current `2.6.3` jars as compatible with older `1.21.x`,
`1.20.x`, or `1.19.x` releases on Modrinth until those exact release jars pass
launcher smoke testing. A `1.21.9-1.21.10` candidate profile now compiles and
builds, but it is not yet launch-validated.

For future support, build a dedicated jar for each target Minecraft version,
launch-test that exact jar, then list that Minecraft version on the Modrinth
version entry for that file. Modrinth guidance also treats separate files for
separate game versions as separate project versions rather than extra files on
one upload.

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
jar on every Minecraft version listed in `modrinth_game_versions`.

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

The `1.21.9-1.21.10` profile now compiles and builds release jars in
`build/release/1.21.9-1.21.10/`. Generated metadata declares:

| Jar | Mod id | Minecraft dependency | Java dependency |
| --- | --- | --- | --- |
| `inventory-sort-2.6.3.jar` | `inventorysort` | `>=1.21.9 <=1.21.10` | `>=21` |
| `inventory-search-2.6.3.jar` | `inventorysearch` | `>=1.21.9 <=1.21.10` | `>=21` |
| `inventory-catalogue-2.6.3.jar` | `inventorycatalogue` | `>=1.21.9 <=1.21.10` | `>=21` |

These jars still need normal launcher smoke testing on both `1.21.9` and
`1.21.10` before either version is listed on Modrinth.

## Compile Probe Method

The matrix below was tested in a detached temporary worktree from the current
split-mod source. Each target used:

- Fabric Loader `0.18.4`
- Loom `1.14-SNAPSHOT`
- the latest Fabric API artifact found for that Minecraft version
- the Java major version reported by Mojang version metadata

Probe command:

```powershell
.\gradlew.bat :inventorysort-core:compileClientJava :inventorysort:compileClientJava :inventorysearch:compileClientJava :inventorycatalogue:compileClientJava "-Pminecraft_version_profile=<version>" --no-daemon --console=plain
```

## Results

| Minecraft | Java | Compile result | Notes |
| --- | ---: | --- | --- |
| 1.21.11 | 21 | PASS | Current release target. |
| 1.21.10 | 21 | PASS (candidate) | Covered by grouped `1.21.9-1.21.10` profile. Needs launcher smoke test. |
| 1.21.9 | 21 | PASS (candidate) | Focused compile and grouped release build pass with the `1.21.9-1.21.10` profile. Needs launcher smoke test. |
| 1.21.8 | 21 | FAIL | Adds chest helper/window handle drift. |
| 1.21.7 | 21 | FAIL | Same drift class as 1.21.8. |
| 1.21.6 | 21 | FAIL | Same drift class as 1.21.8. |
| 1.21.5 | 21 | FAIL | Adds HUD pose stack and single-player directory API drift. |
| 1.21.4 | 21 | FAIL | Same drift class as 1.21.5. |
| 1.21.3 | 21 | FAIL | Same drift class as 1.21.5. |
| 1.21.2 | 21 | FAIL | Same drift class as 1.21.5. |
| 1.21.1 | 21 | FAIL | Adds recipe book screen and older GUI API drift. |
| 1.21 | 21 | FAIL | Same drift class as 1.21.1. |
| 1.20.6 | 21 | FAIL | Older 1.21-era API names plus GUI/screen drift. |
| 1.20.5 | 21 | FAIL | Same drift class as 1.20.6. |
| 1.20.4 | 17 | FAIL | Same drift class plus Java 17 target. |
| 1.20.3 | 17 | FAIL | Same drift class as 1.20.4. |
| 1.20.2 | 17 | FAIL | Same drift class as 1.20.4. |
| 1.20.1 | 17 | FAIL | Same drift class as 1.20.4. |
| 1.20 | 17 | FAIL | Same drift class as 1.20.4. |
| 1.19.4 | 17 | FAIL | Missing `GuiGraphics` and newer widget/rendering APIs. |
| 1.19.3 | 17 | FAIL | Same drift class as 1.19.4. |
| 1.19.2 | 17 | FAIL | Same drift class as 1.19.4, with additional older widget API differences. |
| 1.19.1 | 17 | FAIL | Same drift class as 1.19.2. |
| 1.19 | 17 | FAIL | Same drift class as 1.19.2. |

## Porting Implications

- `1.21.9-1.21.10` is the first grouped compile/build candidate and needs
  launcher smoke testing on both game versions before publishing.
- `1.21.8` is the next closest probe and is expected to expose the next API
  boundary around chest/window handling.
- `1.21.8` through `1.21.2` need more compatibility work around chest
  identity, window/profile handling, HUD matrix calls, and screen APIs.
- `1.21.1` and `1.21` need recipe book/screen compatibility work.
- `1.20.x` is a real backport lane. `1.20.5` and `1.20.6` stay on Java 21,
  while `1.20.4` and older need Java 17-compatible builds.
- `1.19.x` is a larger backport because the current UI code depends heavily on
  `GuiGraphics` and newer widget APIs that are not present there.

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
