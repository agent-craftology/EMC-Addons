# EMC Addons

Free public **Fabric client** mod for **EnchantedMC** (not EarthMC). It adds a click GUI, HUD stat cards driven by EnchantedMC sidebar currencies, and named config profiles.

**Author:** Ben (Discord: `agent_.`). Co-contributor: Craftology Inc. Licensed under [MIT](LICENSE).

This is **not** the paid EMC Bundle. There is **no automation**.

Current release: **1.0**.

## Features

- Click GUI with **Settings**, **Config**, and **HUD** pages
- HUD cards for **Souls**, **Essence**, **Shards**, **Credits**, **Money**, **Swings**, **Rebirth**, plus a sparkline **Graph**
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

- **Settings** — appearance and the Open menu key
- **Config** — named profiles, create/import, and search
- **HUD** — visibility, rows, graph currency, and layout editing

Search is on the **Config** page and filters the profile list.

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

Stats are read from the EnchantedMC sidebar. Cards can show:

- **Souls**
- **Essence**
- **Shards**
- **Credits**
- **Money**
- **Swings**
- **Rebirth**
- **Graph** — a sparkline of session progress for one of the five currencies

HUD page controls:

- **Show HUD** — master toggle for all cards
- **EMC Stats card visible** — show or hide the stats card
- **Advanced stats** — extra detail on the card
- **EMC Stats rows** — per-row toggles for Souls, Essence, Shards, Credits, Money, Swings, Rebirth, and Graph
- **Graph currency** — which of the five currencies (Souls, Essence, Shards, Credits, Money) the sparkline tracks
- **Edit layout…** — drag cards on screen; they snap to corners; **Esc** saves and exits
- **Reset positions** — restore default card placement

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
