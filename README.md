# Thin Air NeoForge 1.21.1

A faithful NeoForge port of **[Thin Air](https://modrinth.com/mod/thin-air/versions)** by fuzs. The air is not always breathable deep underground and in other dimensions.

The original mod is [archived on Modrinth](https://modrinth.com/mod/thin-air/versions) (last release: Minecraft 1.20.4). This project brings it forward to **Minecraft 1.21.1** on NeoForge while staying close to the upstream design  not a ground-up rewrite.

## About the original

| | |
|---|---|
| **Mod** | [Thin Air on Modrinth](https://modrinth.com/mod/thin-air/versions) |
| **Author** | [fuzs](https://modrinth.com/mod/thin-air) |
| **License** | MIT |

## About this port

This is a **direct port** of fuzs's 1.20.4 codebase and data, updated for NeoForge 1.21.1 APIs and datapack path changes (`recipe/`, `advancement/`, `loot_table/`, and so on).

Design choices for this port:

- **Server-authoritative config** settings live in `thinair-server.toml` under each world's `serverconfig/` folder, not in a client-side menu that can override the server.
- **Optional Curios integration** the respirator uses the Curios head slot when Curios is installed; the mod works without it.
- **No bundled mod compatibility layers** Create and other mods are not baked in; compatibility stays optional and separate.

There is **no in-game config screen**, same as the original mod. Edit the TOML file to change behavior.

## Features

Air quality varies by dimension and height. Four levels [green, yellow, red, and blue] affect how entities breathe and what equipment helps.

**Blocks**

- **Safety Lantern** — shows nearby air quality by color; can be dyed and scraped with an axe
- **Signal Torch** — right-click to emit particles (configurable)

**Items**

- **Respirator** — protects against choking air (Curios head slot when available)
- **Air Bladder** / **Reinforced Air Bladder** — portable air refill
- **Bottle of Soulfire** — emergency air restore in the Nether

Also included: advancements, recipes, loot table injections, air-provider block tags, drowned air-drain behavior, chunk air-quality sync, and English / Russian / Chinese lang files.

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x |
| Java | 21 |
| [Curios](https://modrinth.com/mod/curios) | optional (9.5.1+1.21.1) |

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Download or build `thinair-*.jar` from [Releases](https://github.com/) *(add your repo URL when published)*.
3. Place the JAR in your instance `mods/` folder.
4. Optionally add Curios for respirator slot support.

## Configuration

After launching a world once, edit:

```
<world>/serverconfig/thinair-server.toml
```

On a dedicated server:

```
world/serverconfig/thinair-server.toml
```

Notable options:

- **`dimensions`** — air quality by dimension and Y level (e.g. overworld yellow below Y 128, nether yellow everywhere, end red)
- **`enableSignalTorches`** — toggle signal-torch particle effect
- **`drownedChoking`** — air removed per drowned hit (0 to disable)
- **`Ranges`** — bubble radius for yellow, blue, red, and green air-provider blocks

Changes apply on world reload or server restart depending on your setup.

## Development

Clone the repo and build:

```bash
./gradlew build
```

Output: `build/libs/thinair-1.0.0.jar`

Run the client in the dev environment:

```bash
./gradlew runClient
```

Mod branding assets (launcher icon) live in [`Assets/mod_logo.png`](Assets/mod_logo.png) and are copied into the JAR at build time.

Generate data (when datagen providers are added):

```bash
./gradlew runData
```

## Known limitations

Work still in progress compared to the original 1.20.4 release:

- **Reinforced Air Bladder** crafting recipe not yet ported (requires the custom copy-tag shapeless recipe from upstream)
- **In-game config UI** not planned file-based config only, matching the original
- Full parity testing (multiplayer, loot, advancements, worldgen-placed air providers) is ongoing

Report issues on this repository if something does not match the original mod's behavior.

## Credits

| Role | |
|---|---|
| Original mod | [fuzs](https://modrinth.com/mod/thin-air) |
| 1.21.1 NeoForge port | Marie |

## License

MIT — same as the [original Thin Air project](https://modrinth.com/mod/thin-air). Asset rights follow the upstream [LICENSE-ASSETS](https://github.com/Fuzss/modresources) terms where applicable.
