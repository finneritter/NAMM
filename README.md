# NAMM — Not Another Macro Mod

A client-side Fabric macro mod for Minecraft 1.21.11 with a hacked-client-style UI. Create, edit, and trigger macros with a Razer Synapse-style step builder, draggable windows, and an in-game HUD.

## Features

- **Manual step builder** — add key presses, mouse clicks, and delays with precise control
- **Playback modes** — Play Once, Toggle Loop, and Hold to Play
- **Profiles** — group macros into switchable profiles with keybind cycling
- **Chat Commands** — bind chat messages and `/slash` commands to a single keypress
- **Array List HUD** — always-on-screen display of enabled macros/commands in accent color, sorted by width
- **Draggable windows** — five collapsible windows (Macros, Profiles, Editor, Chat Commands, Settings) with snap-to-column alignment
- **Settings window** — customizable accent color, array list position/visibility, info bar options
- **Toast notifications** — configurable per-category notifications with an "All" toggle
- **Info bar** — compact HUD pill showing player head, name, active profile, and time
- **Import/Export** — save and load macros as JSON, or import from Razer Synapse XML
- **Custom font** — bundled Inter TTF for high-resolution text rendering
- **Config persistence** — all settings, macros, profiles, and window positions saved to `config/namm.json`

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

Press **Right Shift** (default, rebindable in Controls) to open the NAMM menu.

### Macros
- **Left-click** a macro to toggle it on/off (or toggle its profile state if a profile is active)
- **Right-click** a macro to edit or delete it
- In the editor: set a name, playback mode, trigger key, and build your step sequence
- Steps support key press/release, mouse click/release, and configurable delays

### Profiles
- Create profiles to organize which macros are active
- Click a profile to activate/deactivate it
- Expand a profile to toggle individual macros within it
- **Cycle profiles** via configurable keybinds (set in Minecraft Controls > NAMM)

### Chat Commands
- Bind any chat message or `/command` to a key
- Commands starting with `/` are sent as slash commands; everything else is sent as chat

### Array List
- Displays enabled macros and/or chat commands on the HUD at all times
- Position configurable (any corner) via the Settings window
- Toggle visibility and content filters in Settings

### Settings
- Click "Settings" in the bottom bar to open the settings window
- **Accent Color** — choose from Purple, Blue, Green, Red, Orange, or White
- **Array List** — toggle on/off, change position, filter macros/commands
- **Info Bar** — always visible or menu-only

### Notifications
- Click "Notifications" in the bottom bar to configure toast notifications
- **All** toggle enables/disables all categories at once
- Individual toggles for: Macro Toggled, Chat Command, Profile Switched, Import/Export, Errors

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
