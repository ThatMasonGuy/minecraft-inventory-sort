# Minecraft Version Profiles

Build profiles keep one source tree while letting Gradle swap the Minecraft,
Fabric Loader, Fabric API, Loom, and Java target versions.

The default profile is configured in `gradle.properties`:

```properties
minecraft_version_profile=1.21.11
supported_minecraft_version_profiles=1.21.11
```

Useful commands:

```powershell
.\gradlew.bat printVersionProfile
.\gradlew.bat buildAllMods
.\gradlew.bat buildAllMods "-Pminecraft_version_profile=26.1.2"
.\gradlew.bat buildAllVersions
```

Release jars are collected under `build/release/<minecraft_version>/`.

Only add a profile to `supported_minecraft_version_profiles` after it compiles
and launches cleanly. Candidate 26.x profiles are present so migration work can
start without making the default release build depend on Java 25.

The 26.x profiles currently fail during configuration on Java 21 with Minecraft's
Java 25 requirement. Install or select a Java 25 toolchain before using them for
compile migration work.
