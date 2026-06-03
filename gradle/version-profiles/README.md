# Minecraft Version Profiles

Build profiles keep one source tree while letting Gradle swap the Minecraft,
Fabric Loader, Fabric API, Loom, and Java target versions.

Profiles are **release compatibility groups**. A profile is not necessarily one
exact Minecraft patch version; it can represent one compiled jar that is tested
and published for several compatible Minecraft versions.

The default profile is configured in `gradle.properties`:

```properties
minecraft_version_profile=1.21.11
supported_minecraft_version_profiles=1.21.11
```

Useful commands:

```powershell
.\gradlew.bat printVersionProfile
.\gradlew.bat buildAllMods
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=1.21.9-1.21.10"
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.1.2"
.\gradlew.bat buildAllVersions
```

Release jars are collected under `build/release/<profile_id>/`.

Compatibility-group profiles support these fields:

```properties
profile_id=1.21.6-1.21.11
minecraft_version=1.21.11
minecraft_dependency=>=1.21.6 <=1.21.11
modrinth_game_versions=1.21.6,1.21.7,1.21.8,1.21.9,1.21.10,1.21.11
compat_group=1.21_late
```

- `minecraft_version` is the compile anchor used by Loom and Mojang mappings.
- `minecraft_dependency` is the Fabric Loader dependency range written into
  `fabric.mod.json`.
- `modrinth_game_versions` is the exact set of versions to publish for that jar.
- `compat_group` selects any version-specific source overlay needed for that API
  shape.
- `profile_id` should be used for release output folders when it differs from
  the compile anchor.

Compatibility-specific code is selected from:

```text
src/compat/<compat_group>/client/java/
src/compat/<compat_group>/client/resources/
```

Only list versions in `modrinth_game_versions` after that exact jar passes launch
smoke testing on those versions.

Only add a profile to `supported_minecraft_version_profiles` after it compiles
and launches cleanly. The `1.21.9-1.21.10` profile currently compiles and builds
release jars, but stays a candidate until launcher smoke testing passes on every
listed game version. Candidate 26.x profiles are present so migration work can
start without making the default release build depend on Java 25.

The 26.x profiles currently fail during configuration on Java 21 with Minecraft's
Java 25 requirement. Install or select a Java 25 toolchain before using them for
compile migration work.
