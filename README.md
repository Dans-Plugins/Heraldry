# Heraldry

## Description

Heraldry is a Minecraft plugin that gives players and factions a coat of arms, and puts it in the world where people can see it.

Vanilla banners can express a few dozen layered patterns. Real heraldry cannot be expressed that way — a proper coat of arms has charges, divisions and tinctures that no banner pattern set covers. Heraldry solves this by rendering arms onto Minecraft maps, which are the one surface a vanilla client will display an arbitrary image on. Arms can be imported from [Armoria](https://azgaar.github.io/Armoria/), the open coat-of-arms generator, or from any image.

It is designed to sit alongside [Medieval Factions](https://github.com/Dans-Plugins/Medieval-Factions), so a nation's arms belong to the nation.

This plugin was proposed from a Kingdom: First Era player request on 2026-09-04 ("Needs patterns coat of arms flags for medieval Minecraft" / "We can use Armoria flag for coat of arms").

## Project Status

**In development; not yet released.** A player's arms can be set from a PNG in the plugin's `import` folder and issued as a named, lore-tagged filled map with `/heraldry` (see [Commands](#commands)). The arms are stored against the player, and each issued map is re-bound to them when the server restarts. Faction-owned arms are not available yet, and only PNG images can be imported. The scope of the first release is defined in [MVP.md](MVP.md).

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

A vanilla client cannot upload a file, so an image becomes arms by being placed in `plugins/Heraldry/import` on the server, for example a PNG exported from Armoria. The folder is created when the plugin is enabled.

| Command | Permission | Default | What it does |
|---|---|---|---|
| `/heraldry view [player]` | `heraldry.view` | everyone | Says whether a player has arms |
| `/heraldry set <file> [player]` | `heraldry.set` | operators | Sets a player's arms from a PNG in the import folder, replacing any they had |
| `/heraldry issue [player]` | `heraldry.issue` | operators | Gives a player a map of their own arms, dropped at their feet if their inventory is full |

Each command acts on the sender unless a player is named. A named player must be online, and the console must always name one.

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
