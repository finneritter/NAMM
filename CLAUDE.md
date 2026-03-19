# NAMM - Not Another Macro Mod

## Project Overview
Minecraft Fabric macro mod for version 1.21.11. Replicates Razer Synapse-style macro functionality with a manual step builder, per-macro playback modes, and custom config UI.

## Tech Stack
- **Java 21** with Fabric Loader + Fabric API + Fabric Loom (Gradle)
- **Minecraft 1.21.11** (Mounts of Mayhem)
- **ModMenu** for settings integration
- **Gson** for JSON config serialization
- **Inter TTF font** for high-resolution UI text

## Project Structure
```
src/main/java/com/namm/
    NammMod.java                  -- Client mod initializer
    NammModMenuIntegration.java   -- ModMenu entrypoint
    model/                        -- Data classes (Macro, MacroStep, enums)
    config/                       -- Config management + main GUI screen
    executor/                     -- Threaded macro playback
    input/                        -- Trigger detection + input simulation
    mixin/                        -- KeyBinding accessor
    util/                         -- Key name utilities
    ui/                           -- Theme, renderer, window container, HUD components
    ui/windows/                   -- Per-window content renderers
```

## Build Commands
- `./gradlew build` — compile the mod
- `./gradlew runClient` — launch Minecraft with the mod

## Key Design Decisions
- All UI through custom Screen subclasses (NammGuiScreen, KeyCaptureScreen)
- Themed rendering via NammRenderer + NammTheme (light/dark modes)
- Macros stored as JSON in `config/namm.json`
- Macro executor runs on daemon threads; input simulation marshalled to render thread
- ConcurrentHashMap for active macro tracking (thread safety)
- TinyFileDialogs for native file picker (import/export) — must run off render thread
- Disconnect cleanup via ClientPlayConnectionEvents.DISCONNECT

## Design Spec
Full spec at `docs/superpowers/specs/2026-03-16-namm-macro-mod-design.md`
