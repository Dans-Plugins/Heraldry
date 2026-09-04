# Heraldry — MVP

## Problem

Players on a medieval roleplay server want coats of arms — for themselves, for their houses, and for their nations. Minecraft's banner system cannot express real heraldry: the pattern set is a fixed few dozen layers, and no combination of them produces a charge like a rampant lion or a division like a per-saltire field.

Note that Medieval Factions already uses the word "flag" for something else entirely — `FACTION_FLAGS.md` documents configuration toggles, not heraldic devices. Anyone searching for existing coat-of-arms support will find that and be misled.

## Target environment

Spigot, Minecraft 26.1, **vanilla clients**. The decisive constraint: a filled map is the only vanilla surface that will display an arbitrary server-supplied image. Everything below follows from that.

## The smallest shippable slice

### v1 does

1. **Import arms as an image.** Accept a coat of arms as a PNG or SVG — including one exported from [Armoria](https://azgaar.github.io/Armoria/) — and rasterise it to the 128×128 palette a Minecraft map uses.
2. **Issue it as a map item.** A named, lore-tagged filled map bearing the arms, which a player can hang in an item frame. Vanilla clients render it with no pack and no mod.
3. **Ownership.** Arms belong to a player, or — when [Medieval Factions](https://github.com/Dans-Plugins/Medieval-Factions) is present — to a faction. Faction arms can be reissued to any member, so a whole army can carry the same device.
4. **Persist arms across restarts**, keyed to the owner rather than to the map item, so a lost map is reissuable and a stolen one is not authoritative.
5. **`/heraldry`** to view, set and issue.

### v1 does not

- **Convert arms into a vanilla banner.** It cannot be done faithfully, and a bad approximation is worse than a map. If a "close enough banner" mode is ever wanted it is a separate feature with its own expectations.
- **Generate arms.** v1 imports; it does not have its own heraldry generator. Armoria already does that job well and players can use it directly.
- **Call the Armoria API at runtime.** See the licensing question below. v1 takes an image the player already has.
- **Put arms on capes, elytra, shields or armour.** All of those need a resource pack.
- **Auto-place faction arms at claim borders or on the BlueMap.** Tempting, and a good v2 — BlueMap markers are already in play via [Bluemap_MedievalFactions](https://github.com/Dans-Plugins/Bluemap_MedievalFactions).
- **Moderate submitted images.** An import-any-image feature on an unwhitelisted server is a content-moderation surface. v1 restricts issuing to operators, or to faction leaders, rather than pretending otherwise.

## Why these non-goals

The whole value of this plugin is one insight — that maps, not banners, are how you show arbitrary heraldry to a vanilla client. Everything else is decoration on that. Shipping the map path alone is a small plugin; the banner-approximation path is an unbounded argument about which lion is close enough.

## Open questions

- **Licensing.** Armoria's code is MIT and its self-made shapes are CC0, but a large portion of its complex charge renders come from WappenWiki and are **non-commercial use only**. That does not stop a player generating arms and importing the image themselves, but it does affect whether this plugin may bundle, redistribute, or server-side render those charges. Resolve before any runtime Armoria integration.
- Map colour depth. The Minecraft map palette is limited and heraldic tinctures are flat and saturated, which should dither well — but this needs a real test before promising fidelity.
- Should arms be issuable as a *banner* item that merely carries the map as lore, so it feels like a standard rather than a picture? Cosmetic, but it changes how the thing reads in-world.
- Does an arms-bearing map need to be protected from being copied in a cartography table?

## Dependencies

None required. Soft dependency on [Medieval Factions](https://github.com/Dans-Plugins/Medieval-Factions) for faction-owned arms.

## References

- [Armoria](https://github.com/Azgaar/Armoria) — heraldry generator and editor (MIT)
- [armoria-api](https://github.com/Azgaar/armoria-api) — the server-side API
