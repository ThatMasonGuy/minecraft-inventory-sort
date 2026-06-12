# Inventory Sort (Fabric)

A lightweight **client-side** Minecraft mod that adds robust sorting capabilities and powerful inventory tracking to container screens. Intelligently organize your items, find misplaced gear, and keep tabs on your storage.

- **Mod ID:** `inventorysort`
- **Minecraft:** current `3.2.1` patch lane targets `1.20.x`, `1.21.x`, and
  `26.x` through compatibility-group releases
- **Loader:** Fabric
- **Java:** 17+ for `1.20-1.20.4`, 21+ for `1.20.5+`, 25+ for `26.x`
- **License:** LGPL-3.0-only

## Features

### 1. Sorting Capabilities

- **Inventory & Container Sorting:** Adds a **Sort** button to supported container screens, letting you organize any supported GUI instantly. Works on chests, barrels, shulker boxes, droppers, and dispensers (hoppers, furnaces, and brewing stands are skipped - their slots are functional).
- **Hotbar-friendly Top-up Behavior:** Intelligently tops up partial hotbar stacks from your main inventory *before* sorting, ensuring your tools and blocks are ready to use.
- **Restacking and Compaction:** Performs restacking and stable compaction so full stacks and empties are arranged cleanly, leaving no awkward gaps.
- **Practical Category-based Ordering:** Sorts items by practical priorities:
  1. Higher max stack size first (e.g., 64-stack items before low-stack items)
  2. Category grouping (wood, stone/terrain, ores, redstone, building blocks, tools/combat, misc)
  3. Alphabetical item ID ordering
- Leaves hotbar organization intentional when sorting the player inventory.

### 2. Inventory Search

- **Searchable Inventory:** Quickly search for items across your immediate inventory and previously tracked containers.
- **Known-current Item Location Tracking:** Remembers where you last saw an item, acting as an intelligent memory aid for your storage systems.
- **Per-location Counts:** Displays exact quantities of searched items at each specific location.
- **Live Inventory Results:** Real-time search updating as you type.
- **Category Shortcuts:** Prefix a query with `:` to search practical groups
  such as `:wood`, `:stone`, `:tools`, `:gear`, or `:storage`.
- **Expanded Location/Details View:** View detailed context about where your items are stored to easily locate them in a massive base.

### 3. Tracked Storage & Known Locations

- **Fixed Block Containers:** Automatically tracks the contents of standard chests, barrels, and other block-based storage.
- **Ender Chest Tracking:** Treats Ender Chests as player-scoped storage, tracking them properly across dimensions.
- **Placed Shulkers:** Dynamically updates contents when you interact with placed shulker boxes.
- **Minecart Storage:** Properly tracks Chest Minecarts and Hopper Minecarts.
- **Multiplayer Server & World Profiles:** Maintains separate tracking databases per server or single-player world. Essential for multiplayer hardcore resets!
- **World Confirmation HUD:** Uses a non-intrusive HUD prompt to confirm the world/profile context, preventing writes to the wrong database until confirmed without blocking your gameplay.

### 4. Catalogue Mode

- **Cataloguing Sessions:** Start a session with `/inventorycatalogue start` (use `/inventorycatalogue start includeInventory` to also count your own inventory), then walk your base opening every chest, shulker, ender chest, minecart, and other storage. Finish with `/inventorycatalogue stop` to get a deduplicated tally of everything you own.
- **Identity-Based Deduplication:** Built on the same container-identity system as Tracked Storage, so single vs. double chests, individual shulkers, per-player ender chests, and minecarts are each counted once. Reopening a container refreshes its snapshot instead of double-counting it.
- **Per-World & Persistent:** Catalogue data is scoped per server/world profile and saved to disk, so a tally accumulates across play sessions and survives restarts - perfect for seeing exactly how much of everything you hoarded by the end of a world. Reset a world's catalogue with `/inventorycatalogue clear`.
- **Reports:** `/inventorycatalogue status` and `/inventorycatalogue report` show running totals in chat; `/inventorycatalogue reports` opens an in-game browser for saved reports with item icons, counts, and `:category` filters; `/inventorycatalogue stop` also writes a full plain-text report to `.minecraft/inventorysort/catalog/`.

## Compatibility & Scope

- **Client-Side Only:** This mod operates entirely on the client. It adds no new blocks, items, or server-side mechanics, making it usable on vanilla servers where client-side utility mods are allowed.
- **Framework:** Requires **Fabric Loader** and **Fabric API** matching the target Minecraft version.
- **Current Release Target:** The split release jars are supported/publishable
  for Minecraft `1.20` through `1.21.11`, plus `26.1`, `26.1.1`, `26.1.2`,
  and `26.2-pre-3` through smoke-tested compatibility-group builds. See
  `COMPATIBILITY.md` for the exact profile ranges.
- **"Known Current Locations":** The tracking features rely on what your client has *seen*. It provides "known current locations," not guaranteed live server truth. If another player empties a chest while you are away, your client will still remember the old contents until you reopen and rescan that container.

## Future Plans

- **Custom Sorting Rules:** Define your own sorting priorities and categories.
- **Custom Sorting per Chest:** Save specific sorting configurations for individual containers.
- **Lockable Inventory Slots:** Prevent specific slots in your inventory from being sorted or moved.
- **Portable Shulker Improvements:** Better tracking for shulker boxes opened directly from your inventory.
- **Component/NBT-aware Tracking:** Better distinguish item variants such as potion types, custom names, enchantments, and other component-backed data.
- **Profile Selector Polish:** Improve the UI for managing numerous server/world profiles.

## Installation

1. Install **Fabric Loader** for the Minecraft version targeted by the jar.
2. Install **Fabric API** matching your game version.
3. Place the built mod JAR into your `.minecraft/mods` folder.
4. Launch the game with the Fabric profile.

## Building from Source

```bash
./gradlew buildAllMods
```

Publish-ready feature artifacts are collected in:

- `build/release/<profile_id>/`

The default Minecraft version profile is `1.21.11`. To inspect or switch build
profiles:

```bash
./gradlew printVersionProfile
./gradlew buildAllMods -Pminecraft_version_profile=1.21.11
./gradlew buildAllVersions
./gradlew publishValidation
./gradlew publishModrinthDryRun
./gradlew ciValidation
```

`buildAllMods` is the normal local and push/PR sanity check. It builds the
default profile, collects the public jars, and verifies release jar metadata.
`buildAllVersions` builds supported/publishable profiles. `ciValidation` also
builds candidate profiles, verifies release metadata, checks
`gradle/smoke-tests.json`, and launches packaged release jars through the
automated smoke matrix. The manual Modrinth publish workflow runs the expensive
publish gate before upload, so local development can stay on faster builds.

Minecraft `1.20-1.20.4` builds require Java 17, `1.20.5+` builds require Java
21, and `26.x` builds require Java 25. The normal push/PR workflow stays on the
fast Java 21 default-profile build, while the manual GitHub Actions Modrinth
workflow installs all three toolchains before the full publish gate. Local full
matrix compile work also needs Java 17, Java 21, and Java 25 JDKs installed or
exposed through Gradle toolchain detection.

Modrinth publishing is configured through supported profiles only:

```bash
./gradlew publishModrinthDryRun
./gradlew publishModrinth -Pmodrinth_confirm_publish=true
```

Real uploads require `MODRINTH_TOKEN` outside the repo. See
`gradle/modrinth-publishing.md`. Each published `mod_version` also needs a
focused Modrinth changelog at `gradle/release-notes/<mod_version>.md`.
For the reusable compatibility-group strategy behind the build, smoke-test, and
publish pipeline, see `gradle/compatibility-release-playbook.md`.

Module-local build artifacts are also generated in:

- `modules/inventorysort/build/libs/`
- `modules/inventorysearch/build/libs/`
- `modules/inventorycatalogue/build/libs/`

Each public feature JAR includes the shared Core JAR internally.

## Development

Run the client for local testing:

```bash
./gradlew :inventorysort:runClient
```

## Project Structure

- `src/client/java/.../InventorySorter.java` - Core logic for sorting, restacking, hotbar top-ups, and layout organization.
- `src/client/java/.../ItemLocationTracker.java` - The engine powering known-current item location tracking and container snapshots.
- `src/client/java/.../SearchModalScreen.java` - UI implementation for the live inventory and container search feature.
- `src/client/java/.../CatalogSession.java` - Cataloguing session lifecycle (start/stop/status/report) and report generation.
- `src/client/java/.../CatalogStore.java` - Persistent, per-world catalogue store keyed by container identity.
- `src/client/java/.../ServerWorldProfileManager.java` - Manages different tracking databases across multiplayer servers and single-player worlds.
- `src/client/java/.../mixin/HandledScreenMixin.java` - Injects and renders the **Sort** and transfer buttons into existing container screens.
- `src/client/java/.../mixin/SearchButtonMixin.java` - Injects the inventory search button independently from the sorting feature.
- `src/compat/<compat_group>/` - Optional compatibility overlays selected by Minecraft version profile.
- `modules/` - Gradle subprojects for Core, Sort, Search, and Catalogue release artifacts.

## Credits

Created by **Tempest Studios**.
