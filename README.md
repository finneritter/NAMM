# NAMM — Not Another Macro Mod

A client-side Fabric macro mod for Minecraft 1.21.11. Create, edit, and trigger macros with a Razer Synapse-style step builder and an in-game UI.

## Features

- **Manual step builder** — add key presses, mouse clicks, and delays with precise control
- **Playback modes** — Play Once, Toggle Loop, and Hold to Play
- **Profiles** — group macros into switchable profiles (only active profile's macros fire)
- **Chat Commands** — bind chat messages and `/slash` commands to a single keypress
- **Draggable UI** — four collapsible, repositionable windows (Macros, Profiles, Editor, Chat Commands)
- **Import/Export** — save and load macros as JSON, or import from Razer Synapse XML
- **Config persistence** — macros, profiles, chat commands, and window positions saved to `config/namm.json`

## Requirements

- Minecraft 1.21.11
- [Fabric Loader](https://fabricmc.net/) >= 0.18.2
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 21+

**Optional:** [Mod Menu](https://modrinth.com/mod/modmenu) for settings integration

## Installation

1. Download `namm-x.x.x.jar` from the [latest release](https://github.com/finneritter/NAMM/releases/latest)
2. Drop it into your `.minecraft/mods/` folder
3. Launch Minecraft with Fabric

## Usage

Open the NAMM UI from Mod Menu, or bind it to a key in Minecraft's controls settings.

### Macros
- **Left-click** a macro to toggle it on/off (or toggle its profile state if a profile is active)
- **Right-click** a macro to edit or delete it
- In the editor: set a name, playback mode, trigger key, and build your step sequence
- Steps support key press/release, mouse click/release, and configurable delays

### Profiles
- Create profiles to organize which macros are active
- Click a profile to activate/deactivate it
- Expand a profile to toggle individual macros within it

### Chat Commands
- Bind any chat message or `/command` to a key
- Commands starting with `/` are sent as slash commands; everything else is sent as chat
- Not filtered by profiles — always available when enabled

## Building from Source

```bash
git clone https://github.com/finneritter/NAMM.git
cd NAMM
./gradlew build
```

The built jar will be at `build/libs/namm-<version>.jar`.

To launch Minecraft with the mod for testing:

```bash
./gradlew runClient
```

## License

[MIT](LICENSE)
