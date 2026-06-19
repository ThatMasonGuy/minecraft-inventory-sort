# Minecraft Version Profiles

Build profiles keep one source tree while letting Gradle swap the Minecraft,
Fabric Loader, Fabric API, Loom, and Java target versions.

Profiles are **release compatibility groups**. A profile is not necessarily one
exact Minecraft patch version; it can represent one compiled jar that is tested
and published for several compatible Minecraft versions.

The default profile is configured in `gradle.properties`:

```properties
minecraft_version_profile=1.21.11
supported_minecraft_version_profiles=1.20-1.20.4,1.20.5-1.20.6,1.21-1.21.5,1.21.6-1.21.8,1.21.9-1.21.10,1.21.11,26.x
candidate_minecraft_version_profiles=
```

The supported/candidate lists contain profile file names without the
`.properties` extension. Release output folders and Modrinth version suffixes
use the profile's `profile_id`, so a file such as `26.x.properties` can build
and publish under `build/release/26.1-26.2-pre-3/`.

Use the fewest supported build profiles that can honestly cover the tested
runtime set. Prefer broadening a compatibility group when one compiled jar can
pass every exact runtime smoke test; keep exact per-version profiles as
runtime-only smoke profiles unless their jar must be published separately.

Useful commands:

```powershell
.\gradlew.bat printVersionProfile
.\gradlew.bat buildAllMods
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=1.21.9-1.21.10"
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=1.20-1.20.4"
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.x"
.\gradlew.bat smokeTestSelectedClients "-Pinventorysort_smoke_profiles=26.1-26.2-pre-3"
.\gradlew.bat buildAllVersions
.\gradlew.bat ciValidation
```

Gradle uses the active profile's `java_version` as a Java toolchain request for
compile and client-run tasks. The `1.20-1.20.4` lane uses Java 17,
`1.20.5+`/`1.21.x` lanes use Java 21, and Minecraft `26.x` profiles request
Java 25. Local compile/run work needs the relevant JDKs installed or exposed
through one of `JAVA_HOME`, `JAVA_HOME_17_X64`, `JAVA_HOME_21_X64`, or
`JAVA_HOME_25_X64`. GitHub Actions installs the needed toolchains for manual
compatibility validation and publishing.

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

Current 26.x release range plan:

- `26.x.properties` is the single supported release profile for the checked
  26.x lane. It compiles from the newest checked anchor, `26.2-pre-3`, uses
  the shared `26.x` compatibility overlay, and publishes under profile id
  `26.1-26.2-pre-3`.
- `26.1.properties`, `26.1.1.properties`, `26.1.2.properties`, and
  `26.2-pre-3.properties` are exact runtime-only smoke profiles. Keep them out
  of `supported_minecraft_version_profiles` and
  `candidate_minecraft_version_profiles`.
- The release profile lists Modrinth game versions `26.1`, `26.1.1`,
  `26.1.2`, and `26.2-pre-3`. The exact `26.2-pre-3` runtime profile still
  uses Fabric API's `~26.2-` dependency because Fabric Loader reports the
  runtime version as `26.2-pre.3`, while `modrinth_game_versions` keeps the
  public Modrinth label `26.2-pre-3`.

Only add a profile to `supported_minecraft_version_profiles` after it compiles
and launches cleanly. The current `3.1.1` supported publish lane includes the
smoke-passed `1.20.x`, `1.21.x`, and `26.x` compatibility groups.
`ciValidation` builds supported and candidate profiles, runs automated client
smoke launches, and only allows supported profiles to publish when their smoke
records are `pass`.

The 26.x profiles now configure through the non-remapping build lane. The manual
GitHub Actions Modrinth workflow installs Java 17, Java 21, and Java 25 before
the `3.1.1` publish gate without changing the fast push/PR workflow.
