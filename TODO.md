# Inventory Search TODO

Current checkpoint: `2.4.16`

Last confirmed working:
- Fixed block containers track current contents correctly.
- Ender chests track as player-scoped storage.
- Placed shulkers update correctly when moved.
- Chest minecarts and hopper minecarts track correctly.
- Server/world tracking profiles work for multiplayer HC resets.
- World confirmation HUD blocks writes until confirmed, without blocking gameplay.

## Remaining TODO

1. Push/verify release build after each next patch chunk.

2. Custom/modded dimensions:
   - Unknown dimensions still deserialize poorly and can fall back to overworld.
   - Store and restore dimension ids generically instead of hard-coding overworld/nether/end.

3. Corrupt JSON hardening:
   - Bad tracking/profile JSON should not crash or poison load.
   - Catch parse/runtime exceptions, skip bad entries, and consider writing a `.bak` before overwriting.

4. Potion/component-aware tracking:
   - Search aliases let `water bottle` find `minecraft:potion`, but item identity is still base item only.
   - True variant tracking would need components/NBT-aware keys and display names.

5. Portable shulkers:
   - Placed shulkers are good.
   - Shulkers opened from inventory are still weak/history-ish because identity is based on contents.
   - Need a deliberate portable-container model or skip them.

6. Search count semantics:
   - Row count is live inventory if present, otherwise most recent tracked location.
   - It is not a total across all locations.
   - Decide whether to label this better or implement safe per-location summary UI.

7. Catalog mode cleanup:
   - Inventory catalog fingerprint can double-count if opened from different player positions.
   - Review catalog mode against the newer tracking namespace/profile model.

8. Old data cleanup/migration:
   - Earlier dev builds may have stale locations such as crafting tables or old fake inventory coordinates.
   - Add a cleanup/migration command if old data becomes annoying.

9. Performance polish:
   - Inventory sampler saves whenever inventory signature changes.
   - Probably fine, but watch long sessions and busy servers for chat/log/save churn.

10. Server world profile UI polish:
    - Profile selector only shows the first few profiles.
    - Add scrolling/search if HC world count keeps climbing.

11. Optional UX polish:
    - Make HUD prompt position configurable if it clashes with other HUD mods.
    - Consider shortening profile names in HUD if they are long.
