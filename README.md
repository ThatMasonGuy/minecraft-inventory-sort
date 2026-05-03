# Inventory Sort (Fabric)

A lightweight **client-side** Minecraft mod that adds robust sorting capabilities and powerful inventory tracking to container screens. Intelligently organize your items, find misplaced gear, and keep tabs on your storage.

- **Mod ID:** `inventorysort`
- **Minecraft:** `1.21.11`
- **Loader:** Fabric
- **Java:** 21+
- **License:** MIT

## Features

### 1. Sorting Capabilities
- **Inventory & Container Sorting:** Adds a **Sort** button to supported container screens, letting you organize any supported GUI instantly.
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
- **Expanded Location/Details View:** View detailed context about where your items are stored to easily locate them in a massive base.

### 3. Tracked Storage & Known Locations
- **Fixed Block Containers:** Automatically tracks the contents of standard chests, barrels, and other block-based storage.
- **Ender Chest Tracking:** Treats Ender Chests as player-scoped storage, tracking them properly across dimensions.
- **Placed Shulkers:** Dynamically updates contents when you interact with placed shulker boxes.
- **Minecart Storage:** Properly tracks Chest Minecarts and Hopper Minecarts.
- **Multiplayer Server & World Profiles:** Maintains separate tracking databases per server or single-player world. Essential for multiplayer hardcore resets!
- **World Confirmation HUD:** Uses a non-intrusive HUD prompt to confirm the world/profile context, preventing writes to the wrong database until confirmed without blocking your gameplay.

### 4. Catalogue Mode
- **Global Item Catalogue:** Browse a consolidated view of all items across all tracked containers.
- **Relates Tracked Items to Containers:** Acts as a holistic view of your wealth and resources. Instead of searching for a specific item, you can review everything you own across your known storage network.
- *Note: This feature is currently experimental and may occasionally reflect older container snapshots if they have been altered by other players.*

## Compatibility & Scope

- **Client-Side Only:** This mod operates entirely on the client. It adds no new blocks, items, or server-side mechanics, making it usable on vanilla servers where client-side utility mods are allowed.
- **Framework:** Requires **Fabric Loader** and **Fabric API** for Minecraft `1.21.11`.
- **"Known Current Locations":** The tracking features rely on what your client has *seen*. It provides "known current locations," not guaranteed live server truth. If another player empties a chest while you are away, your client will still remember the old contents until you reopen and rescan that container.

## Future Plans

- **Custom Sorting Rules:** Define your own sorting priorities and categories.
- **Custom Sorting per Chest:** Save specific sorting configurations for individual containers.
- **Lockable Inventory Slots:** Prevent specific slots in your inventory from being sorted or moved.
- **Portable Shulker Improvements:** Better tracking for shulker boxes opened directly from your inventory.
- **Component/NBT-aware Tracking:** Better distinguish item variants such as potion types, custom names, enchantments, and other component-backed data.
- **Profile Selector Polish:** Improve the UI for managing numerous server/world profiles.

## Installation

1. Install **Fabric Loader** for Minecraft `1.21.11`.
2. Install **Fabric API** matching your game version.
3. Place the built mod JAR into your `.minecraft/mods` folder.
4. Launch the game with the Fabric profile.

## Building from Source

```bash
./gradlew build
```

Built artifacts are generated in `build/libs/`.

## Development

Run the client for local testing:

```bash
./gradlew runClient
```

## Project Structure

- `src/client/java/.../InventorySorter.java` – Core logic for sorting, restacking, hotbar top-ups, and layout organization.
- `src/client/java/.../ItemLocationTracker.java` – The engine powering known-current item location tracking and container snapshots.
- `src/client/java/.../SearchModalScreen.java` – UI implementation for the live inventory and container search feature.
- `src/client/java/.../CatalogSession.java` – Logic for the global tracked items catalogue.
- `src/client/java/.../ServerWorldProfileManager.java` – Manages different tracking databases across multiplayer servers and single-player worlds.
- `src/client/java/.../mixin/HandledScreenMixin.java` – Injects and renders the **Sort** button and search integration into existing container screens.

## Credits

Created by **Tempest Studios**.
