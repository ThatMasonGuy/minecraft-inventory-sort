# Modrinth Project Pages

This file is the source-of-truth copy for public Modrinth project summaries and
description pages. Update it before changing live Modrinth project metadata.

## Inventory Sort

- Modrinth project name: `InvSort`
- Project ID: `V38RsqtI`
- Summary:

Quickly sort Minecraft inventories and containers into a clean, predictable order.

### Description Markdown

```markdown
InvSort is a lightweight Fabric client mod that adds fast, practical inventory
sorting to Minecraft. It cleans up messy inventories and containers without
making you manually drag every stack around.

It focuses on predictable survival-friendly sorting: restacking partial piles,
compacting empty gaps, and ordering items in a way that keeps common storage
readable. Use it on its own, or alongside InvSearch and InvCatalogue as part of
the wider inventory management suite.

## Features

- Sort button for supported inventory and container screens
- Restacks partial piles before laying items out cleanly
- Compacts empty gaps so storage is easier to scan
- Practical ordering by stack size, item category, and item id
- Hotbar-friendly player inventory behavior
- Skips slot-sensitive screens such as hoppers, furnaces, and brewing stands
- Works client-side in singleplayer, LAN, Realms, and multiplayer where
  client-side utility mods are allowed

## Good for

- Cleaning up messy chests, barrels, and shulker boxes
- Keeping survival storage readable
- Resetting dump chests after a mining or building session
- Players who want predictable sorting without server-side changes
- Using alongside InvSearch and InvCatalogue for broader inventory management

## Install note

InvSort does not require a server install. Install Fabric Loader, Fabric API,
and the InvSort jar for your Minecraft version.

It does not add blocks, items, or server mechanics. Server rules still apply,
so only use it where client-side utility mods are allowed.
```

## Inventory Search

- Modrinth project name: `InvSearch`
- Project ID: `wIOLlhbN`
- Summary:

Find items across your inventory and known storage locations with a clean in-game search.

### Description Markdown

```markdown
InvSearch is a lightweight Fabric client mod that adds fast item search to
Minecraft. It helps you find what your client has seen in your inventory and
tracked storage, so you can stop opening every chest in the room just to find
one missing stack.

It works as a practical storage memory aid: open containers as you play, then
search later to see known-current item locations and counts. Use it on its own,
or alongside InvSort and InvCatalogue as part of the wider inventory management
suite.

## Features

- In-game search UI for inventory and known storage
- Live results while you type
- Known-current item location tracking
- Per-location item counts for searched stacks
- Tracks standard containers, placed shulkers, ender chests, and storage
  minecarts through the shared inventory tracking system
- Keeps tracking data separated by singleplayer world or multiplayer server
- Works client-side in singleplayer, LAN, Realms, and multiplayer where
  client-side utility mods are allowed

## Good for

- Finding misplaced tools, blocks, and resources
- Large survival bases with too many storage chests
- Hardcore or seasonal worlds where storage changes often
- Players who want item search without server-side changes
- Using alongside InvSort and InvCatalogue for broader inventory management

## Install note

InvSearch does not require a server install. Install Fabric Loader, Fabric API,
and the InvSearch jar for your Minecraft version.

It remembers what your client has seen. If another player moves items while you
are away, reopen that storage so InvSearch can refresh its known location data.
```

## Inventory Catalogue

- Modrinth project name: `InvCatalogue`
- Project ID: `rV7fRk8Z`
- Summary:

Run storage catalogue sessions that count your Minecraft base and write clear item reports.

### Description Markdown

```markdown
InvCatalogue is a lightweight Fabric client mod for counting what is stored
across your Minecraft base. Start a catalogue session, open the storage you want
counted, then generate a clear report of the items your client saw.

It is built for end-of-world audits, hardcore resets, big storage rooms, and
those moments where you genuinely need to know how much stuff you have. Use it
on its own, or alongside InvSort and InvSearch as part of the wider inventory
management suite.

## Features

- Start and stop catalogue sessions with `/inventorycatalogue start` and
  `/inventorycatalogue stop`
- Optional `includeInventory` mode for counting your own inventory too
- Counts standard containers, placed shulkers, ender chests, and storage
  minecarts through the shared inventory tracking system
- Deduplicates known container identities so reopening the same storage refreshes
  it instead of double-counting it
- Chat status and report commands for quick totals
- Plain-text reports written locally for later review
- Keeps catalogue data separated by singleplayer world or multiplayer server

## Good for

- End-of-world storage audits
- Hardcore base exports and reset planning
- Big farms, warehouses, and shared storage systems
- Checking resource totals before large builds
- Players who want local reports without server-side changes

## Install note

InvCatalogue does not require a server install. Install Fabric Loader, Fabric
API, and the InvCatalogue jar for your Minecraft version.

It counts storage your client opens during catalogue sessions. For accurate
reports, walk through the storage you care about and open each container once.
```
