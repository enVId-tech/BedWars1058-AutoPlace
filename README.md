# BedwarsAutoSetup

A Spigot plugin that drives **Bedwars1058** arena setup in three modes, replacing `basesetup.sk` and
`middlesetup.sk`. It uses Bedwars1058 as a dependency and runs its setup commands for you; it is not
based on BW1058-SetupArena.

> **Build status:** the code was compiled against hand-written API stubs and the position maths was
> unit-tested, but it has **not** been compiled against the real Spigot API or run on a server (the
> authoring environment could not reach any Maven repository). Run `./gradlew build` first and fix anything
> the real API disagrees with. See "Things to verify" below.

## Build and install

```
./gradlew build            # -> build/libs/BedwarsAutoSetup-1.0.0.jar
```

- Needs JDK 17+. Targets Spigot **1.17+** (uses `World#getMinHeight`, `BlockData`, `rayTraceBlocks`).
  Legacy 1.8 material/data-value servers are not supported.
- Drop the jar in `plugins/` next to BedWars1058. **Remove the two `.sk` files**: `/nextgen` exists in both.
- Optional: `./gradlew build -PwithBwApi -PbwApiVersion=<version>` adds the BW1058 API jar to the compile
  classpath. The plugin does not need it - the setup session is not exposed by the API, so everything goes
  through the `/bw ...` setup commands (templates in `config.yml`).

## Using it

Start a BW1058 setup (`/bw create <name>` etc.), then `/bwsetup`.

| Menu | What it does |
|---|---|
| **Main** | Red concrete (left) = auto-create teams. Nether star = gamemode. Comparator = Manual / Semi / Fully Automatic. Compass = lobby. Diamond block = map generators. Green concrete = team & base setup. |
| **Gamemode** | `1v1v1v1 1v1 2v2v2v2 2v2 3v3v3v3 3v3 4v4v4v4 4v4`. Fewer teams than exist: all teams are deleted and recreated. More: the missing ones are created. Same: only players-per-team changes. |
| **Team & Base** | Team tabs, then Spawn / Bed / Shop / Upgrade / Iron / Gold / Drop Area buttons (plus a forge combo). Each button runs the BW command where you stand; in semi/auto the position is also saved as a delta from the bed. |
| **Lobby & Boundary** | Waiting (player) spawn, spectator spawn, region corners; or "detect lobby structure". |
| **Map Generators** | Place diamond/emerald here, auto-place all detected ones, rotate around the centre. |
| **Validation** | Green/red per element per team and for the map. Click a red item to jump to where it is placed. Red concrete goes back. Green concrete saves (twice, within 10 s, if incomplete). |

Commands: `/bwsetup [open|mode|gamemode|status|reset]`, `/bwauto [run|next|confirm|deny]`,
`/nextgen [diamond|emerald] [degrees]` (alias `/bwrotate`). Permission: `bwauto.use` (op).

### The three modes

- **Manual** - every button just runs the BW command at your position (ArenaSetup style). Nothing is saved but "placed" flags.
- **Semi-automatic** (the Skript port) - set up one base by hand; "Apply to next base" copies it to the nearest unassigned bed, then walks you onto the bed after that.
- **Fully automatic** - set up one base by hand; `/bwauto` copies it to every other bed, detects the lobby structure, and places every detectable diamond/emerald generator.

Typing BW commands by hand also works in every mode (the plugin's command sniffer records them, like the
Skript's `on command`).

## INCREDIBLY IMPORTANT:
- This plugin is CODED WITH AI-ASSISTANCE, USE AT YOUR OWN RISK!
- no but really, you should definitely not use this for production.