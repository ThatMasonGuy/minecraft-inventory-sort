# Compatibility Source Groups

Compatibility groups hold source files that only compile for a specific
Minecraft API shape. The active group is selected by the `compat_group` property
in `gradle/version-profiles/*.properties`.

Expected layout:

```text
src/compat/<compat_group>/client/java/
src/compat/<compat_group>/client/resources/
```

Keep shared logic in `src/client/java`. Add compatibility-group code only when a
Minecraft version range needs different class names, method signatures, mixins,
or small API adapters.

Prefer the broadest compatibility source group that still compiles and passes
the exact runtime smoke matrix. Add a new compat group only when the API shape
really diverges; use exact runtime profiles for smoke testing without creating
extra publish profiles.

Recommended package ownership:

```text
tempeststudios.inventorysort.compat.core      -> inventorysort-core
tempeststudios.inventorysort.compat.sort      -> inventorysort
tempeststudios.inventorysort.compat.search    -> inventorysearch
tempeststudios.inventorysort.compat.catalogue -> inventorycatalogue
```

Avoid compiling duplicate fully-qualified class names from shared and compat
source folders. If a Minecraft API requires the same public class name to have a
different method signature, remove that concrete class from shared source and
keep only the target-specific wrappers in compat groups. Prefer shared code
calling small compat adapter or renderer classes over copying whole feature
classes into a compat group.
