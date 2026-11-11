# Heraldry

## Description

Heraldry is a Minecraft plugin that gives players and factions a coat of arms, and puts it in the world where people can see it.

Vanilla banners can express a few dozen layered patterns. Real heraldry cannot be expressed that way — a proper coat of arms has charges, divisions and tinctures that no banner pattern set covers. Heraldry solves this by rendering arms onto Minecraft maps, which are the one surface a vanilla client will display an arbitrary image on. Arms can be imported from [Armoria](https://azgaar.github.io/Armoria/), the open coat-of-arms generator, or from any image.

It is designed to sit alongside [Medieval Factions](https://github.com/Dans-Plugins/Medieval-Factions), so a nation's arms belong to the nation.

This plugin was proposed from a Kingdom: First Era player request on 2026-09-04 ("Needs patterns coat of arms flags for medieval Minecraft" / "We can use Armoria flag for coat of arms").

## Project Status

**In development; not yet released.** A player's arms can be set from a PNG in the plugin's `import` folder and issued as a named, lore-tagged filled map with `/heraldry` (see [Commands](#commands)). The arms are stored against the player, so changing them redraws every map already issued, and each issued map is re-bound to them when the server restarts. Faction-owned arms are not available yet, and only PNG images can be imported. The scope of the first release is defined in [MVP.md](MVP.md).

## Planned Features

- Import a coat of arms from an image, including one exported from Armoria
- Render it to a Minecraft map item that any vanilla client can see
- Assign arms to a player or to a Medieval Factions faction
- Reissue a faction's arms to any member

## Installation

Not yet released. Once there is a release, installation will follow the usual DPC pattern: drop the jar in `plugins/` and restart. It will also be available through [Dan's Plugin Manager](https://github.com/Dans-Plugins/Dans-Plugin-Manager).

### Building from source

Builds run on JDK 21, as CI does. The bytecode targets Java 17 so the jar loads on every version in `minecraft-versions.json`.

```bash
./gradlew clean build
```

The jar is written to `build/libs/Heraldry-<version>.jar`.

### Optional Companion Plugins

- [Medieval Factions](https://github.com/Dans-Plugins/Medieval-Factions) — lets arms belong to a faction rather than only to a player.

## Usage

### Commands

A vanilla client cannot upload a file, so an image becomes arms by being placed in `plugins/Heraldry/import` on the server, for example a PNG exported from Armoria. The folder is created when the plugin is enabled. An image can be at most 2048 pixels wide and 2048 pixels tall.

The file name must end in `.png`, in lower case (`arms.PNG` is not accepted), and otherwise use only letters, digits, `.`, `-` and `_`, starting with a letter, digit or `_`, with at most 64 characters before `.png` and no folder in the name. Files with any other name are left out of tab completion for `/heraldry set`. Setting arms stores a copy of the image, so the file can be removed from the import folder afterwards.

| Command | Permission | Default | What it does |
|---|---|---|---|
| `/heraldry view [player]` | `heraldry.view` | everyone | Says whether a player has arms |
| `/heraldry set <file> [player]` | `heraldry.set` | operators | Sets a player's arms from a PNG in the import folder, replacing any they had and redrawing the maps already issued to them |
| `/heraldry issue [player]` | `heraldry.issue` | operators | Gives a player a map of their own arms, dropped at their feet if their inventory is full |

Each command acts on the sender unless a player is named. A named player must be online, and the console must always name one.

### Issued maps

An issued map is named `Arms of <player>`, and its lore reads `Arms of a player` followed by `Issued by Heraldry`. The name and lore are only a label: which player a map belongs to is recorded against the map's id, so renaming the map in an anvil does not change whose arms it shows.

The arms are scaled to fit the 128×128 map with their proportions kept, centred, and padded with transparent pixels. Each pixel takes the nearest map colour, with no dithering, and pixels that are mostly transparent let the item frame show through.

When the server starts, and when `/heraldry set` changes a player's arms, a map that no longer exists, or whose owner has no readable arms, is left as it is and a warning naming it is written to the server log.

### Data files

Everything Heraldry keeps is in `plugins/Heraldry`:

| Path | What it holds |
|---|---|
| `import/` | Images waiting to be set as arms with `/heraldry set` |
| `arms/player/<uuid>.png` | Each player's arms, as the imported image saved again as a PNG, at its original size rather than as map pixels |
| `issued-maps.txt` | One `<map id> <owner>` line per issued map, so each map is re-bound to its owner's arms on restart |

If `issued-maps.txt` cannot be read, or holds a line that is not a valid `<map id> <owner>` entry (or repeats a map id), Heraldry logs why and disables itself instead of overwriting it.

### Documentation

- [MVP](MVP.md) – Scope of the first release, and its deliberate non-goals

`USER_GUIDE.md`, `COMMANDS.md`, `CONFIG.md` and `CHANGELOG.md` land with the first release, per [DOCUMENTATION_PRACTICES.md](https://github.com/Dans-Plugins/dpc-conventions/blob/main/docs/DOCUMENTATION_PRACTICES.md).

## Support

You can find the support Discord server [here](https://discord.gg/xXtuAQ2).

### Experiencing a bug?

Please fill out a bug report [here](https://github.com/Dans-Plugins/Heraldry/issues/new).

## Contributing

This repository follows the standards in [dpc-conventions](https://github.com/Dans-Plugins/dpc-conventions). `CONTRIBUTING.md` has not been written yet; until it is, build and test instructions are in [CLAUDE.md](CLAUDE.md).

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

See the [LICENSE](LICENSE) file for full details.
