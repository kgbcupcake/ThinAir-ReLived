# Changelog

All notable changes to this **NeoForge 1.21.1 port** of [Thin Air](https://modrinth.com/mod/thin-air) are documented here.

This fork is maintained separately from [fuzs's original mod](https://github.com/Fuzss/thinair) (last release: Minecraft 1.20.4). For history prior to the port, see the [original Modrinth versions page](https://modrinth.com/mod/thin-air/versions).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- In-game config screen (Cloth Config) for server settings from the Mods menu
- Config load/reload binding so `thinair-server.toml` settings apply correctly on world startup and reload
- **Safety Lantern belt slot** via Curios (optional alternative to carrying in inventory)
- Curios belt lantern renderer on the player model (front-mounted, with armor offset)
- Server-authoritative **player air quality sync** (`ClientboundPlayerAirQualityPacket`) for held/belt lantern display in multiplayer
- Client-side `ClientPlayerAirQualityCache` and `LanternDisplayResolver` so item model properties read cached state instead of sampling air quality every render frame
- Reactive placed-lantern updates in `AirBubbleTracker` (dirty-chunk drain when air bubbles change; no scheduled block ticks)
- `minecraft:tags/block/wall_post_override` entry for signal torch (vanilla-compatible wall placement)
- `minecraft:tags/block/mineable/pickaxe` entry for safety lantern (correct break speed and drops)
- Cutout render layers for signal torch, wall signal torch, and safety lantern blocks
- Curios player entity slot assignment (`head` + `belt`) and explicit head/belt slot type registration via InterModComms

### Changed

- Advancement trigger updates for Minecraft 1.21.1 datapack format
- **Respirator advancement** now requires breathing yellow air and equipping the respirator in the Curios head slot (not just receiving the item)
- **Safety Lantern advancement** requires breathing yellow/red air while holding a lantern
- `air_quality_sensitive` entity tag restored to upstream parity (players, villagers, wandering traders, and illagers)
- `yellow_air_providers` block tag emptied to match upstream (torches/campfires/lanterns no longer create yellow air bubbles)
- `heavy_breathing_equipment` item tag emptied to match upstream (respirator protects in yellow air only, not red)
- Air bladder refill logic runs server-side only (fixes hold-to-refill in thin air)
- Reinforced Air Bladder crafting copies durability from the ingredient air bladder
- Chunk air-bubble scanning conditions corrected (removed debug `false ||` / `true ||` overrides)

### Fixed

- Safety Lantern could not be mined with a pickaxe or would not drop as an item when broken
- Curios GUI did not appear when Thin Air was the only mod registering Curios slots
- Held and belt Safety Lantern colors did not update with ambient air quality
- Placed Safety Lantern block colors could stay stale after nearby air-provider blocks changed
- Signal torch wall placement could behave differently from vanilla torches
- Possible transparency/rendering glitches on torches and lanterns without cutout render layers

### Known limitations

- Full parity testing in generated structures (loot injections, worldgen-placed air providers) is ongoing
- Multiplayer edge cases (remote players, chunk unload/reload) have not been exhaustively verified
- Report bugs for **this port only** in [this repository's issue tracker](https://github.com/kgbcupcake/Thin_Air_1.21.1_Port/issues)

## [1.0.0] - 2026-06-08

First release of the NeoForge 1.21.1 port by [Marie (kgbcupcake)](https://github.com/kgbcupcake).

### Added

- **NeoForge 1.21.1 port** of Thin Air, updated from the upstream 1.20.4 codebase and data
- **Air quality system**: green, yellow, red, and blue air by dimension and height, with breathing equipment interactions
- **Safety Lantern**: shows nearby air quality by color; dyeable and color can be scraped off with an axe
- **Signal Torch** and **Wall Signal Torch**: right-click to emit particles (toggleable via config)
- **Respirator**: protects against choking air; uses the Curios head slot when [Curios](https://modrinth.com/mod/curios) is installed
- **Air Bladder** and **Reinforced Air Bladder**: portable air refill items
- **Bottle of Soulfire**: emergency air restore in the Nether
- **Reinforced Air Bladder crafting**: shapeless netherite + air bladder recipe with durability copied from the ingredient bladder
- **Drowned behavior**: drowned can drain player air when attacking (configurable)
- **Chunk air-quality sync**: client receives air quality updates per chunk for air-provider bubbles
- **Air provider bubbles**: configurable radius for green, yellow, red, and blue air sources
- **Advancements**: air bladder, blue air, disco lantern, respirator, safety lantern, signal torch, soulfire bottle, water breathing
- **Recipes**: all core crafting recipes ported to 1.21.1 `recipe/` datapack layout
- **Loot injections**: safety lanterns in dungeon, mineshaft, and stronghold chests; soulfire bottles in buried treasure, shipwreck, and ruin chests
- **Server config**: `thinair-server.toml` under each world's `serverconfig/` folder
- **Localization**: English, Russian, and Chinese lang files
- **Optional Curios integration**: respirator head slot and client rendering when Curios is present

### Changed

- Datapack paths updated for Minecraft 1.21.1 (`recipe/`, `advancement/`, `loot_table/`, and related renames)
- NeoForge-only loader (no dual Forge/NeoForge build)
- Package namespace `dev.maire.thinair` for the port

### Removed

- Bundled compatibility layers for Create and other mods (compatibility remains optional and separate, matching port goals)

[Unreleased]: https://github.com/kgbcupcake/Thin_Air_1.21.1_Port/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/kgbcupcake/Thin_Air_1.21.1_Port/releases/tag/v1.0.0
