# Inventory Search TODO

Current checkpoint: `2.4.16`

## Confirmed Working

- Fixed block containers track current contents correctly.
- Ender chests track as player-scoped storage.
- Placed shulkers use same-content cleanup when moved, intentionally favoring undercounting over stale duplicate locations.
- Chest minecarts and hopper minecarts track correctly.
- Server/world tracking profiles work for multiplayer HC resets.
- World confirmation HUD blocks writes until confirmed, without blocking gameplay.
- Catalog `includeInventory` uses a stable per-session player-inventory fingerprint.

## Recently Fixed

1. Placed shulker identical-content collision:
   - Restored same-content cleanup for placed shulkers.
   - This can undercount separate identical shulkers, but avoids permanent stale duplicates when a shulker is broken and placed elsewhere.

2. Catalog `includeInventory` double-counting:
   - Player inventory fingerprint no longer varies by player position.
   - Moving and reopening inventory during one catalog session should not count it again.

3. Search tracked count semantics:
   - Collapsed search rows now show total known tracked storage count when the item is not in the live inventory.

4. Routine log noise:
   - Normal container open/close tracking, save/update messages, and search result diagnostics moved from `info` to `debug`.

## P2 Accuracy And Data Hardening

1. Potion/component-aware tracking:
   - Search aliases let `water bottle` find `minecraft:potion`, but item identity is still base item only.
   - Catalog and tracking currently merge component variants such as potions, enchanted books, named items, and filled containers.
   - True variant tracking needs component-aware keys and display names.

2. Custom/modded dimensions:
   - Unknown dimensions still deserialize poorly and can fall back to overworld.
   - Store and restore dimension ids generically instead of hard-coding overworld/nether/end.

3. Corrupt JSON hardening:
   - Bad tracking/profile JSON should not crash or poison load.
   - Catch parse/runtime exceptions, skip bad entries, and consider writing a `.bak` before overwriting.

4. Catalog mode cleanup:
   - Review catalog mode against the newer tracking namespace/profile model.
   - Consider component-aware totals and export/paging for large reports.

5. Old data cleanup/migration:
   - Earlier dev builds may have stale locations such as crafting tables or old fake inventory coordinates.
   - Add a cleanup/migration command if old data becomes annoying.

## UX Polish

1. Portable shulkers:
   - Shulkers opened from inventory are still weak/history-ish because identity is based on contents.
   - Decide whether to skip them or build a deliberate portable-container model.

2. Search default view:
   - Empty search only shows in-session recent inventory items.
   - Consider recent tracked storage hits or a more useful default summary after restart.

3. Component-specific search:
   - Search cannot query potion type, enchantment, custom name, shulker contents, or similar stack details yet.

4. Server world profile UI:
   - Profile selector only shows the first few profiles.
   - Add scrolling/search if HC world count keeps climbing.

5. HUD/profile display:
   - Make HUD prompt position configurable if it clashes with other HUD mods.
   - Consider shortening profile names in HUD if they are long.

## Performance And Logging

1. Inventory sampler save churn:
   - Inventory sampler saves whenever inventory signature changes.
   - Watch long sessions and busy servers for save churn.

2. Log/chat noise:
   - Watch for remaining chat/log spam during manual testing.
   - Keep important warnings visible, move routine diagnostics to `debug`.

## Release Process

1. Run `./gradlew.bat compileJava compileClientJava` after each patch chunk.
2. Run full `./gradlew.bat build` before release.
3. Push/verify release build after each patch chunk.
