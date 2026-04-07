# Inventory Sort (Fabric)

A lightweight **client-side** Minecraft mod that adds a **Sort** button to container screens and intelligently organizes your items.

- **Mod ID:** `inventorysort`
- **Minecraft:** `1.21.11`
- **Loader:** Fabric
- **Java:** 21+
- **License:** MIT

## Features

- Adds a **Sort** button to supported container screens.
- **Hotbar-friendly behavior:** tops up partial hotbar stacks from your main inventory before sorting.
- Sorts items by practical priorities:
  1. Higher max stack size first (e.g., 64-stack items before low-stack items)
  2. Category grouping (wood, stone/terrain, ores, redstone, building blocks, tools/combat, misc)
  3. Alphabetical item ID ordering
- Performs restacking and stable compaction so full stacks and empties are arranged cleanly.
- Leaves hotbar organization intentional when sorting player inventory.

## How it works in-game

1. Open a container/inventory screen.
2. Click the **Sort** button (shown near the top-right side of the GUI).
3. The mod:
   - tops up partial hotbar stacks (when possible),
   - sorts the target region,
   - restacks similar items,
   - compacts empty slots.

## Compatibility & scope

- This is a **client-only** mod.
- It focuses on inventory/container UX and does not add new blocks, items, or server-side mechanics.
- Requires Fabric Loader and Fabric API.

## Installation

1. Install **Fabric Loader** for Minecraft `1.21.11`.
2. Install **Fabric API** matching your game version.
3. Place the built mod JAR into your `.minecraft/mods` folder.
4. Launch the game with the Fabric profile.

## Building from source

```bash
./gradlew build
```

Built artifacts are generated in `build/libs/`.

## Development

Run the client for local testing:

```bash
./gradlew runClient
```

## Project structure

- `src/client/java/.../HandledScreenMixin.java` – injects and renders the **Sort** button.
- `src/client/java/.../InventorySorter.java` – sorting, restacking, hotbar top-up, and layout logic.
- `src/main/resources/fabric.mod.json` – mod metadata and dependencies.

## Credits

Created by **Tempest Studios**.
