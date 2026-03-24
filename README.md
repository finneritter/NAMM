# NAMM — Not Another Macro Mod

A client-side Fabric utility mod for Minecraft 1.21.11 PvP and general gameplay. Features a hacked-client-style UI with macros, a target HUD, draggable windows, and an always-on-screen array list.

## Features

### Target HUD
- **On-screen opponent info panel** — shows health bar, armor slots, totem indicator, and active potion effects for the player you're looking at or recently hit
- **Crosshair + sticky targeting** — locks onto players in your crosshair; attacking a player resets the 3-second timeout
- **Draggable positioning** — open the NAMM menu and drag the Target HUD anywhere on screen; position persists across sessions
- **Toggle on/off** in the Settings window

### Macros
- **Manual step builder** — add key presses, mouse clicks, and delays with precise control
- **Playback modes** — Play Once, Toggle Loop, and Hold to Play
- **Drag-and-drop step reordering** with auto-paired press/release inputs

### Profiles
- **Switchable macro groups** — organize which macros are active per profile
- **Keybind cycling** — cycle through profiles with configurable keybinds

### Chat Commands
- Bind any chat message or `/slash` command to a single keypress

### HUD Elements
- **Array List** — always-on-screen display of enabled macros/commands in accent color, sorted by width; position configurable to any corner
- **Info Bar** — compact pill showing player head, name, active profile, and time; always-visible or menu-only
- **Toast notifications** — configurable per-category (macro toggled, chat command, profile switched, import/export, errors)

### UI
- **Draggable windows** — five collapsible windows (Macros, Profiles, Editor, Chat Commands, Settings) with snap alignment
- **Accent colors** — Purple, Blue, Green, Red, Orange, or White
- **Custom font** — bundled Inter TTF for crisp text rendering
- **Import/Export** — save and load macros as JSON, or import from Razer Synapse XML

### Config
- All settings, macros, profiles, window positions, and HUD positions saved to `config/namm.json`

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

- **Left-click** a macro to toggle it on/off
- **Right-click** a macro to edit or delete it
- In the editor: set a name, playback mode, trigger key, and build your step sequence
- Open **Settings** from the bottom bar to configure accent color, array list, info bar, and Target HUD
- Open **Notifications** from the bottom bar to configure toast categories

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
