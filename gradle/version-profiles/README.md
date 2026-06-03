# Minecraft Version Profiles

Build profiles keep one source tree while letting Gradle swap the Minecraft,
Fabric Loader, Fabric API, Loom, and Java target versions.

Profiles are **release compatibility groups**. A profile is not necessarily one
exact Minecraft patch version; it can represent one compiled jar that is tested
and published for several compatible Minecraft versions.

The default profile is configured in `gradle.properties`:

```properties
minecraft_version_profile=1.21.11
supported_minecraft_version_profiles=1.21.11,1.21.9-1.21.10,1.21.6-1.21.8,1.21-1.21.5,1.20.5-1.20.6,1.20-1.20.4
candidate_minecraft_version_profiles=26.1.2,26.2-pre-3
```

Useful commands:

```powershell
.\gradlew.bat printVersionProfile
.\gradlew.bat buildAllMods
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=1.21.9-1.21.10"
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=1.20-1.20.4"
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.1.2"
.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=26.1-26.1.2"
.\gradlew.bat buildAllVersions
.\gradlew.bat ciValidation
```

Gradle uses the active profile's `java_version` as a Java toolchain request for
compile and client-run tasks. The default `1.20.x`/`1.21.x` lane uses Java 21.
Minecraft `26.x` profiles request Java 25, so local compile/run work needs a
Java 25 JDK installed or exposed through one of `JAVA_HOME`, `JAVA_HOME_21_X64`,
or `JAVA_HOME_25_X64`. GitHub Actions installs the needed toolchains for manual
compatibility validation.

Release jars are collected under `build/release/<profile_id>/`.

Compatibility-group profiles support these fields:

```properties
profile_id=1.21.6-1.21.11
minecraft_version=1.21.11
minecraft_dependency=>=1.21.6 <=1.21.11
modrinth_game_versions=1.21.6,1.21.7,1.21.8,1.21.9,1.21.10,1.21.11
compat_group=1.21_late
unobfuscated_minecraft=false
```

- `minecraft_version` is the compile anchor used by Loom and Mojang mappings.
- `minecraft_dependency` is the Fabric Loader dependency range written into
  `fabric.mod.json`.
- `modrinth_game_versions` is the exact set of versions to publish for that jar.
- `compat_group` selects any version-specific source overlay needed for that API
  shape.
- `profile_id` should be used for release output folders when it differs from
  the compile anchor.
- `unobfuscated_minecraft=true` selects the non-remapping Loom plugin, normal
  `implementation` dependencies, and plain `jar` release artifacts. Use this for
  Minecraft 26.x profiles only; 1.20.x and 1.21.x profiles should keep the
  remapped default.

Compatibility-specific code is selected from:

```text
src/compat/<compat_group>/client/java/
src/compat/<compat_group>/client/resources/
```

Only list versions in `modrinth_game_versions` after that exact jar passes launch
smoke testing on those versions.

Exact runtime-only profiles may also exist for smoke testing. For example,
`1.21.9.properties`, `1.21.10.properties`, `1.20.properties`, and
`1.20.4.properties` select exact Minecraft/Fabric runtimes used by
`smokeTestValidationClients`, but they are not release profiles and should not
be added to `supported_minecraft_version_profiles` or
`candidate_minecraft_version_profiles`.

Current 26.x candidate range plan:

- `26.1.2.properties` is the compile-anchor profile for the grouped
  `26.1-26.1.2` candidate jar. Fabric API `0.150.0+26.1.2` declares
  compatibility with Minecraft `26.1`, `26.1.1`, and `26.1.2`, so this is the
  broad candidate range to smoke-test before promotion.
- `26.1.properties` and `26.1.1.properties` are exact runtime-only smoke
  profiles. They use the `26.1.2` compatibility overlay and the same Fabric API
  artifact so the grouped candidate jar can be launched on every listed runtime.
- `26.2-pre-3.properties` stays exact and provisional. Current Fabric API
  `26.2` pre-release artifacts are scoped to individual pre-releases, and
  Minecraft `26.2` final is not available yet.

Only add a profile to `supported_minecraft_version_profiles` after it compiles
and launches cleanly. Current supported groups for `1.20.x` and `1.21.x`
compile, build release jars, and pass automated smoke launches on every listed
game version. `ciValidation` builds supported and candidate profiles, runs
automated client smoke launches, and only allows supported profiles to publish
when their smoke records are `pass`. Candidate 26.x profiles may be added later
so migration work can start without making the default release build depend on
Java 25.

The 26.x profiles now configure through the non-remapping build lane. The manual
GitHub Actions `compatibility validation` workflow defaults to Java 25 and can
run focused candidate builds without changing the fast push/PR workflow.
Compile and smoke-test migration work still requires the version-specific
source/API shims.
