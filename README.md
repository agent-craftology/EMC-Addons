# EMC Addons

Free public **Fabric client** mod for **EnchantedMC** (not EarthMC). It adds a click GUI, HUD stat cards driven by EnchantedMC sidebar currencies, and named config profiles.

**Author:** Ben (Discord: `agent_.`). Co-contributor: Craftology Inc. Licensed under [MIT](LICENSE).

Current release: **1.0**.

## Features

- Click GUI with **HOME** (Modules) and **CONFIGURATION** (Settings, Config)
- HUD cards for **Souls**, **Essence**, **Shards**, **Credits**, **Money**, **Swings**, **Rebirth**, **Grind Time**, plus a sparkline **Graph**, and a **Zone** card that shows Zone and Stage from nearby dungeon mob custom names and floating text displays
- Named config profiles under `.minecraft/config/emcaddons/`
- Client-only `/config` commands that are never sent to the server
- Shareable `.cbshare` packs (ZIP + manifest) for the same Minecraft version

## Install

1. Install **Fabric Loader** and **Fabric API** for the same Minecraft version as the jar.
2. Drop the jar into your `mods/` folder.
3. Launch with a Java version that matches the target Minecraft version (see the table below).

Release jars look like `EMC Addons 1.0-1.20.6.jar` (mod version, then Minecraft version).

| Minecraft | Java |
|-----------|------|
| 1.18.2, 1.19.4 | 17 |
| 1.20.6, 1.21, 1.21.11 | 21 |
| 26.2 | 25 |

Use one EMC Addons jar that matches your game version. Mixing a 1.20.6 jar with a 1.21 client (or the wrong Java runtime) will not work.

## Open the menu

Default keybind: **Right Alt**.

Rebind it in **Options → Controls → EMC Addons → Open menu**, or from the Settings page inside the GUI.

Pages:

- **HOME**
  - **Modules** — 3-column title + description grid (no icons). Dungeons has a short explanation; Gens, Factories, Skyblock, and Prisons show **Coming Soon!** (default page)
- **CONFIGURATION**
  - **Settings** — appearance, the Open menu key, and HUD layout
  - **Config** — named profiles, create/import, and search

Search is on the **Config** page and filters the profile list. Show HUD, EMC Stats card visible, Advanced stats, and EMC Stats rows live on **Modules → Dungeons**. Edit layout and Reset positions stay on **Settings**.

## Settings

Appearance and keybinds:

| Setting | What it does |
|---------|----------------|
| **Theme** | **Emerald**, **Midnight**, **Ember**, **Ocean**, or **Amethyst** |
| **GUI opacity** | How transparent the click GUI is |
| **GUI scale** | 50–150% (click GUI only; does not change vanilla GUI scale) |
| **Window icon** | Toggle the custom Minecraft window icon |
| **Open menu** | Rebind the menu key (same as Controls → EMC Addons) |

## HUD

HUD tracker controls live on **Modules → Dungeons**. Stats are read from the EnchantedMC sidebar. Cards can show:

- **Souls**
- **Essence**
- **Shards**
- **Credits**
- **Money**
- **Swings**
- **Rebirth**
- **Grind Time** — how long you have been grinding this session (`45s`, `12m 05s`, or `1h 23m 45s`)
- **Graph** — a sparkline of session progress for one of the five currencies
- **Zone** — a separate card for Zone and Stage inferred from nearby dungeon mob custom names and floating text displays (for example `[RARE] LVL1 Chicken`)

Tracker controls on **Modules → Dungeons**:

- **Show HUD** — master toggle for all cards
- **EMC Stats card visible** — show or hide the stats card
- **Advanced stats** — extra detail on the card
- **EMC Stats rows** — per-row toggles for Souls, Essence, Shards, Credits, Money, Swings, Rebirth, Graph, and Grind Time
- **Graph currency** — which of the five currencies (Souls, Essence, Shards, Credits, Money) the sparkline tracks (on the rows page)

Settings HUD keeps layout tools:

- **Edit layout…** — drag cards on screen; they snap to corners; **Esc** saves and exits
- **Reset positions** — restore default card placement

On **1.18.2** and **1.19.4**, Settings HUD also has **HUD opacity** and **HUD scale**.

**Grind Time** pauses in the EnchantedMC Hub and while disconnected. Session earned, `/hr` rates, and the sparkline also do not count while the Hub (or an unknown sidebar) is showing. Frozen last values can still render; they just do not increment. Switching Hub ↔ dungeon does not reset stats.

Each Modules card has **Reset statistics**. Dungeons clears session earned, rates, sparkline, and grind time. Gens, Factories, Skyblock, and Prisons are **Coming Soon!** with empty per-mode buckets until those stats exist.

Cards only populate while EnchantedMC is showing the matching sidebar lines. If a currency is missing from the sidebar, that row stays empty until it appears.

## Config

Profiles live in:

```text
.minecraft/config/emcaddons/profiles/<name>/settings.cbcfg
```

On Windows that is typically `%appdata%\.minecraft\config\emcaddons\`.

The Config page lists profiles, shows the active name, and can create or import packs. Use search to filter the list.

### Commands

These are **client-only**. They are handled in the client and are **never sent to the server**.

| Command | Action |
|---------|--------|
| `/config list` | List saved profiles |
| `/config create <name>` | Create a new profile |
| `/config delete <name>` | Delete a profile |
| `/config load <name>` | Switch to a profile |
| `/config export <name>` | Export a `.cbshare` pack |
| `/config import` | Import a `.cbshare` pack |

Profile names use letters, digits, `_`, and `-`.

### Sharing packs

A `.cbshare` file is a ZIP of one profile directory plus a `manifest.txt`. Import and export use a file dialog; the default folder is `config/emcaddons/exports/`.

- Packs work only with the **same Minecraft version** they were exported from (a 1.20.6 pack will not import on 1.21).
- **License and token files are never packed.** Do not rely on a share file to move those.

After import, load the profile (`/config load <name>` or the Config page) to use it.

## Developers

Each Minecraft version is a separate Gradle project:

| Folder | Minecraft |
|--------|-----------|
| `emcaddons-1.18.2` | 1.18.2 |
| `emcaddons-1.19.4` | 1.19.4 |
| `emcaddons-1.20.6` | 1.20.6 |
| `emcaddons-1.21` | 1.21 |
| `emcaddons-1.21.11` | 1.21.11 |
| `emcaddons-26.2` | 26.2 |

Build from inside the version folder:

```bash
./gradlew build
```

On Windows you can use `gradlew.bat` instead of `./gradlew`.

The output jar is:

```text
build/libs/EMC Addons 1.0-<minecraft_version>.jar
```

Example: `emcaddons-1.20.6` produces `EMC Addons 1.0-1.20.6.jar`.

Use the JDK that matches the table in [Install](#install). **26.2** requires **JDK 25** and **non-remap Loom**. The older folders use **Yarn** mappings and **remap** Loom.

Run the game with Fabric Loader and Fabric API for that Minecraft version, then copy the jar into `mods/`.

## License

[MIT](LICENSE).
