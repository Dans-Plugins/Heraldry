# Heraldry

## Description

Heraldry is a Minecraft plugin that gives players and factions a coat of arms, and puts it in the world where people can see it.

Vanilla banners can express a few dozen layered patterns. Real heraldry cannot be expressed that way — a proper coat of arms has charges, divisions and tinctures that no banner pattern set covers. Heraldry solves this by rendering arms onto Minecraft maps, which are the one surface a vanilla client will display an arbitrary image on. Arms can be imported from [Armoria](https://azgaar.github.io/Armoria/), the open coat-of-arms generator, or from any image.

It is designed to sit alongside [Medieval Factions](https://github.com/Dans-Plugins/Medieval-Factions), so a nation's arms belong to the nation.

This plugin was proposed from a Kingdom: First Era player request on 2026-09-04 ("Needs patterns coat of arms flags for medieval Minecraft" / "We can use Armoria flag for coat of arms").

## Project Status

**In development; not yet usable on a server.** The building blocks exist as library code: rasterising a PNG to the map palette, storing each owner's arms, drawing rasterised arms onto a map, and remembering which owner each issued map belongs to. On enable the plugin loads the issued maps and re-binds each one to its owner's stored arms, but nothing can issue a map or set arms yet, because there are no commands. The scope of the first release is defined in [MVP.md](MVP.md).

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
