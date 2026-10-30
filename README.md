# BedWars1058-EasySetup

A Spigot plugin that drives **Bedwars1058** arena setup in three modes, replacing `basesetup.sk` and
`middlesetup.sk`. It uses Bedwars1058 as a dependency and runs its setup commands for you; it is not
based on BW1058-SetupArena.

> **Build status:** the code was compiled against hand-written API stubs and the position maths was
> unit-tested, but it has **not** been compiled against the real Spigot API or run on a server (the
> authoring environment could not reach any Maven repository). Run `./gradlew build` first and fix anything
> the real API disagrees with. The 1.8 / 1.12 / 1.16 / 1.20 / latest paths below are written to be correct
> but are untested on real servers. See "Things to verify".

## Build and install

```
./gradlew build            # -> build/libs/BedWars1058-EasySetup-v1.0.0.jar
```

- One jar for every version. It is compiled as **Java 8 bytecode** against the **Spigot 1.8.8 API**, so it
  cannot call anything newer by accident, and it loads on any server JVM (8 through 25). The build itself
  needs JDK 9+ (for `--release 8`); JDK 17 or 21 is fine.
- Drop the jar in `plugins/` next to BedWars1058. **Remove the two `.sk` files**: `/nextgen` exists in both.
- Optional: `./gradlew build -PwithBwApi -PbwApiVersion=<version>` adds the BW1058 API jar to the compile
  classpath. The plugin does not need it - the setup session is not exposed by the API, so everything goes
  through the `/bw ...` setup commands (templates in `config.yml`).

## Language

The whole plugin and its checks are Kotlin (`src/main/kotlin`, `src/test/kotlin`), compiled to Java 8 bytecode. The Kotlin
standard library is bundled into the jar, because servers before 1.16.5 cannot download libraries. Nothing relocates it; each
plugin has its own class loader, so it does not clash with other plugins' Kotlin.

## Version compatibility

| Server | Colours / items | Beds | Notes |
|---|---|---|---|
| 1.8 - 1.12 | `WOOL`/`STAINED_CLAY`/... + data value | data bits (facing, head/foot) | `api-version` ignored |
| 1.13 - 1.16 | flattened names (`RED_WOOL`, ...) | `Bed` BlockData | world height 0-256 |
| 1.17 - latest | flattened names | `Bed` BlockData | negative min height handled via `getMinHeight` |

How: everything version-specific lives in `dev.etran.compat` (`Compat`, `Dyes`, `BedCompat`) and
`scan/WorldSnap`. Menu items are specs like `RED_CONCRETE|CONCRETE:14|STAINED_CLAY:14` (first material
that exists wins). Post-1.8 APIs (`getMinHeight`, `BlockData`, `ChunkSnapshot#getBlockType`) are reached by
reflection / MethodHandles, `Sound` is looked up by field name (it is not an enum on the newest versions),
and map-wide scans stream one chunk snapshot at a time within `beds.scan-y-range`. Known limit: 1.8.0-1.8.2 lack `World#getNearbyEntities`.

## Using it

Start a BW1058 setup (`/bw create <name>` etc.), then `/bwsetup`. Opening it also gives you the **setup hotbar**
(your normal inventory is saved and restored on `/bwsetup exit`, quit, death-safe, and after a crash/restart).

### Hotbar tools
Page 1 (team): team wool (right-click = pick team) - Spawn - Bed - Shop - Upgrade - Iron - Gold - Drop area - Menu.
Page 2 (map): Waiting spawn - Spectator spawn - Lobby corner 1/2 - Diamond - Emerald - page switch - Menu.
`/bwsetup page <team|map>` or the page item switches; the Menu item opens the full GUI (advanced options, team selection).

- **Place at the clicked block**: right-click a block with a tool and the element goes there (the cell next to the face you
  clicked), whether or not you stand there. The plugin briefly moves you to that spot to run the BW command, then back.
- **Shop/upgrade facing**: pitch 0, yaw snapped to 45 degrees, and the villager looks *back at you* (opposite of your facing).
  Change with `/bwsetup facing <toward|same|degrees|reset>`, `/bwsetup snap <deg|off|reset>`, `/bwsetup pitch <deg|reset>`,
  or `facing.*` in `config.yml`. Spawns snap to `facing.spawn-snap-degrees`.
- **Ghost villagers** mark existing and placed shops/upgrades (looking the way the shop faces). They are real entities with AI
  off, removed when you exit/quit, when the world saves, on chunk unload and on disable, and swept again on chunk load, so they
  never end up in a real round. Elements already in `plugins/BedWars1058/Arenas/<world>.yml` are imported (`/bwsetup import`).
- **Deleting**: punch/right-click a ghost villager or generator hologram once; block-located things (spawn, bed, lobby
  points) need a second hit within `delete.confirm-seconds`. Bedwars1058 only has a remove command for generators
  (`remove-*` templates in `config.yml`), so other removals just make the plugin forget until you place them again.
- **Menus**: back/forward still work; the green "forward" concrete was moved one slot away from the save/finish slot.
  Save stays on the Validation screen only.

Other menus: Gamemode (incl. solos/doubles), Team & Base, Lobby & Boundary, Map Generators, Validation.

Commands: `/bwsetup [open|exit|tools|page|facing|snap|pitch|import|mode|gamemode|status|reset]`,
`/bwauto [run|next|confirm|deny|gens]`, `/nextgen [diamond|emerald] [degrees]` (alias `/bwrotate`). Permission: `bwauto.use`.

**Positions are used exactly where you place them** - no wall relocation.

### Generator detection (`/bwauto gens`)

Every diamond/emerald block (grouped with touching blocks) is a candidate and gets a probability from independent
clues, combined logistically: free **headroom** above, a **pedestal** (stairs/slabs around it or lower ground at its
sides), **support** below, small **size** (big diamond/emerald structures are decoration), **map symmetry** (partners
under 90/180/270 degree rotation or an axis mirror about `center`; in a clearly symmetric map a block with no partner
loses probability), and **distance from beds**. Blocks at or above `generator-scan.min-probability` (0.6) are placed.
`/bwauto gens` lists every candidate with its probability and the clues, so you can tune the threshold.

`./gradlew placementCheck` checks facing maths, arena-file parsing and deletion; `./gradlew inferenceCheck` runs the generator detector against a synthetic map (pedestals, flush blocks, decoys) without a server.

### The three modes

- **Manual** - every button just runs the BW command at your position (ArenaSetup style). Nothing is saved but "placed" flags.
- **Semi-automatic** (the Skript port) - set up one base by hand; "Apply to next base" copies it to the nearest unassigned bed, then walks you onto the bed after that.
- **Fully automatic** - set up one base by hand; `/bwauto` copies it to every other bed, detects the lobby structure, and places every detectable diamond/emerald generator.

Typing BW commands by hand also works in every mode (the plugin's command sniffer records them, like the
Skript's `on command`).

## Developer note
This plugin is coded with AI assistance and architected completely by me. Regardless, use at your own risk.

## License
This plugin is under the MIT License, check the [License](LICENSE) file for more details.
