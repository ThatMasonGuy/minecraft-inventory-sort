# Minecraft Compatibility

Research date: 2026-06-03

## Recommendation

Publish the current split release jars for **Minecraft 1.21.11 only**.

Do not mark the current `2.6.3` jars as compatible with older `1.21.x`,
`1.20.x`, or `1.19.x` releases on Modrinth. The current source only compiled
successfully against `1.21.11` during compatibility probes.

For future support, build a dedicated jar for each target Minecraft version,
launch-test that exact jar, then list that Minecraft version on the Modrinth
version entry for that file. Modrinth guidance also treats separate files for
separate game versions as separate project versions rather than extra files on
one upload.

## Current Release Artifacts

The current publish-ready artifacts in `build/release/1.21.11/` are:

| Jar | Mod id | Minecraft dependency | Java dependency |
| --- | --- | --- | --- |
| `inventory-sort-2.6.3.jar` | `inventorysort` | `~1.21.11` | `>=21` |
| `inventory-search-2.6.3.jar` | `inventorysearch` | `~1.21.11` | `>=21` |
| `inventory-catalogue-2.6.3.jar` | `inventorycatalogue` | `~1.21.11` | `>=21` |

These jars have been launch-tested on `1.21.11` in standalone and combined
install combinations.

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
| 1.21.10 | 21 | FAIL | Minecart entity package/name, dimension id, and button render API drift. |
| 1.21.9 | 21 | FAIL | Same core API drift as 1.21.10. |
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

- `1.21.10` and `1.21.9` are the closest ports. They mostly fail on a small
  set of renamed or moved APIs.
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
