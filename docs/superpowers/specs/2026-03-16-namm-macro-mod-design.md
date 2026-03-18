# NAMM (Not Another Macro Mod) - Design Spec

## Context

Build a Minecraft Fabric macro mod for version 1.21.11 that replicates Razer Synapse-style macro functionality. Users create macros via a manual step builder, assign them to trigger keys, and choose playback behavior per macro. Config UI is entirely through YACL integrated with ModMenu. Macros can be imported/exported as JSON.

## Tech Stack

- **Java 21**, Fabric Loader + Fabric API + Fabric Loom (Gradle)
- **Minecraft 1.21.11** (Mounts of Mayhem, Dec 2025)
- **YACL 3.8.1+1.21.11-fabric** for config screens
- **ModMenu** for settings button integration
- **Gson** for JSON serialization
- **LWJGL TinyFileDialogs** for native file picker (import/export)

## Package Structure

```
src/main/java/com/namm/
    NammMod.java                          -- ClientModInitializer entrypoint
    NammModMenuIntegration.java           -- ModMenu entrypoint

    model/
        ActionType.java                   -- Enum: KEY_PRESS, KEY_RELEASE, MOUSE_CLICK, MOUSE_RELEASE
        PlaybackMode.java                 -- Enum: PLAY_ONCE, TOGGLE_LOOP, HOLD_TO_PLAY
        MacroStep.java                    -- Single step: action, key code, isMouse, delayBefore, delayAfter
        Macro.java                        -- Name, steps list, playback mode, trigger key, enabled

    config/
        NammConfig.java                   -- Config holder, JSON load/save, YACL screen builder
        MacroSerializer.java              -- Gson serialization/deserialization

    executor/
        MacroExecutor.java                -- Runnable for threaded playback
        MacroPlaybackState.java           -- Tracks active macros, thread pool, cancel support

    input/
        TriggerKeyHandler.java            -- ClientTickEvents listener for trigger detection
        InputSimulator.java               -- Simulates key/mouse via KeyBinding system

    mixin/
        KeyBindingAccessor.java           -- Accessor mixin for KeyBinding.timesPressed

    util/
        KeyNames.java                     -- GLFW key codes to display names

src/main/resources/
    fabric.mod.json
    namm.mixins.json
    assets/namm/lang/en_us.json
    assets/namm/icon.png
```

## Data Model

### ActionType (enum, implements NameableEnum)
- `KEY_PRESS`, `KEY_RELEASE`, `MOUSE_CLICK`, `MOUSE_RELEASE`

### PlaybackMode (enum, implements NameableEnum)
- `PLAY_ONCE` -- trigger key press plays macro once
- `TOGGLE_LOOP` -- first press starts looping, second press stops
- `HOLD_TO_PLAY` -- loops while trigger held, stops on release

### MacroStep
| Field | Type | Description |
|-------|------|-------------|
| actionType | ActionType | What input event to simulate |
| keyCode | int | GLFW key code or mouse button code |
| isMouse | boolean | true = mouse button, false = keyboard |
| delayBeforeMs | int | Milliseconds to wait before this step (0-10000) |
| delayAfterMs | int | Milliseconds to wait after this step (0-10000) |

### Macro
| Field | Type | Description |
|-------|------|-------------|
| name | String | User-given display name |
| steps | List\<MacroStep\> | Ordered steps |
| playbackMode | PlaybackMode | How this macro plays |
| triggerKeyCode | int | GLFW key/button that activates it |
| triggerIsMouse | boolean | Whether trigger is a mouse button |
| enabled | boolean | Quick toggle without deleting |

## Config & Serialization

### Storage
- Single JSON file at `config/namm.json`
- Loaded on mod init, saved on config screen close and on any change
- Gson with pretty printing
- **Error recovery**: if JSON is malformed on load, log error via SLF4J and fall back to empty macro list

### JSON Format
```json
{
  "macros": [
    {
      "name": "Auto Sprint",
      "playbackMode": "TOGGLE_LOOP",
      "triggerKeyCode": 294,
      "triggerIsMouse": false,
      "enabled": true,
      "steps": [
        {
          "actionType": "KEY_PRESS",
          "keyCode": 87,
          "isMouse": false,
          "delayBeforeMs": 0,
          "delayAfterMs": 50
        }
      ]
    }
  ]
}
```

## YACL Config Screen

### Main Screen (Macro List)
Each macro rendered as an `OptionGroup`:
- **Name**: `Option<String>` with StringController
- **Playback Mode**: `Option<PlaybackMode>` with EnumController
- **Enabled**: `Option<Boolean>` with TickBoxController
- **"Edit Steps..."**: `ButtonOption` that opens step editor sub-screen
- **"Set Trigger Key"**: `ButtonOption` that opens key capture screen
- **"Delete Macro"**: `ButtonOption` with confirmation

Category-level **"Add New Macro"** button appends a default macro and regenerates the screen.

### Step Editor Sub-Screen
Opened per-macro. Each step as an `OptionGroup`:
- **Action Type**: `Option<ActionType>` with EnumController
- **Key**: `ButtonOption` showing current key name, opens key capture on click
- **Delay Before**: `Option<Integer>` with IntegerSliderController (0-10000ms, step 10) — use int, not long, for YACL compatibility
- **Delay After**: `Option<Integer>` with IntegerSliderController (0-10000ms, step 10)
- **"Remove Step"**: `ButtonOption`

Bottom **"Add Step"** button.

### Key Capture Screen
Small custom `Screen` subclass:
- Displays "Press any key or mouse button..."
- Overrides `keyPressed()` and `mouseClicked()` to capture the input
- Records GLFW code and isMouse flag, returns to previous screen
- Override `shouldCloseOnEsc()` to return false so Escape can be captured as a valid key
- Uses a `Consumer<CapturedKey>` callback to pass the result back to the calling screen

### Import/Export Category
- **"Export Macros"**: `ButtonOption` → TinyFileDialogs save dialog → writes JSON
- **"Import Macros"**: `ButtonOption` → TinyFileDialogs open dialog → reads JSON, appends to config
- **Threading**: TinyFileDialogs calls are blocking — must run on a separate thread, then marshal results back to render thread via `MinecraftClient.execute()`
- **Name conflicts on import**: If an imported macro has the same name as an existing one, append " (imported)" suffix

## Input Handling

### Trigger Detection (TriggerKeyHandler)
- Registered via `ClientTickEvents.END_CLIENT_TICK`
- **In-game guard**: skip if `client.currentScreen != null || client.player == null`
- For each enabled macro, poll trigger key state:
  - Keyboard: `InputUtil.isKeyPressed(windowHandle, keyCode)`
  - Mouse: `GLFW.glfwGetMouseButton(windowHandle, keyCode) == GLFW.GLFW_PRESS`
- Edge detection via `Map<String, Boolean> previousKeyState`:
  - **PLAY_ONCE**: rising edge → start executor (ignore if already running)
  - **TOGGLE_LOOP**: rising edge → toggle (start if stopped, stop if running)
  - **HOLD_TO_PLAY**: rising edge → start looping; falling edge → cancel

### Input Simulation (InputSimulator)
- Maps GLFW key code to `InputUtil.Key` via `InputUtil.fromKeyCode(keyCode, 0)` (keyboard) or `InputUtil.Type.MOUSE.createFromCode(keyCode)` (mouse)
- Press: `KeyBinding.setKeyPressed(key, true)` + increment `timesPressed` via mixin
- Release: `KeyBinding.setKeyPressed(key, false)`
- **Must run on render thread** via `MinecraftClient.getInstance().execute()`

## Macro Executor

### MacroPlaybackState (singleton)
- `ConcurrentHashMap<String, Future<?>> activeMacros` — macro name → running Future (must be concurrent for thread safety between tick thread and executor threads)
- `ExecutorService` — cached thread pool with daemon threads
- Methods: `startMacro()`, `stopMacro()`, `isRunning()`, `stopAll()`
- **Unique key requirement**: Use macro name as key; enforce unique names in config UI (append suffix on duplicate)

### MacroExecutor (Runnable)
```
do {
    for each step in macro.steps:
        if interrupted: return
        sleep(step.delayBeforeMs)
        if interrupted: return
        schedule InputSimulator.simulate(step) on render thread
        sleep(step.delayAfterMs)
        if interrupted: return
} while (loop && !interrupted)
```

- Cancellation via `Future.cancel(true)` + interrupt checks
- `stopAll()` called on disconnect via `ClientPlayConnectionEvents.DISCONNECT` callback
- **Empty macro guard**: skip execution if `macro.steps` is empty
- **Self-trigger guard**: ignore trigger key events that originate from InputSimulator to prevent infinite recursion

## Mixin

### KeyBindingAccessor
```java
@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("timesPressed")
    int getTimesPressed();

    @Accessor("timesPressed")
    void setTimesPressed(int timesPressed);
}
```
Needed because `timesPressed` is private with no public setter.

## Build Dependencies

### gradle.properties
```properties
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.1
loader_version=0.18.1
fabric_loom_version=1.14-SNAPSHOT
fabric_api_version=0.119.2+1.21.11
yacl_version=3.8.1+1.21.11-fabric
modmenu_version=13.0.0
```

### build.gradle repositories
```groovy
maven { url 'https://maven.isxander.dev/releases' }      // YACL
maven { url 'https://maven.terraformersmc.com/' }          // ModMenu
```

## Implementation Order

1. Scaffold Fabric mod project (build.gradle, fabric.mod.json, gradle wrapper)
2. Model classes (ActionType, PlaybackMode, MacroStep, Macro)
3. Serialization (MacroSerializer with Gson)
4. Config load/save (NammConfig, no UI yet)
5. Mixin (KeyBindingAccessor + namm.mixins.json)
6. Input simulation (InputSimulator)
7. Executor (MacroExecutor + MacroPlaybackState)
8. Trigger detection (TriggerKeyHandler)
9. YACL config screen (main screen + step editor + key capture)
10. ModMenu integration (NammModMenuIntegration)
11. Import/export (TinyFileDialogs + ButtonOptions)
12. Polish (translations, edge cases, disconnect cleanup)

## Verification Plan

1. **Build**: `./gradlew build` compiles without errors
2. **Launch**: `./gradlew runClient` starts Minecraft with the mod loaded
3. **ModMenu**: Mod appears in ModMenu list with config button
4. **Config screen**: Can create a macro, add steps, set trigger key, choose playback mode
5. **Playback**: Create a simple macro (e.g., press W for 50ms), assign to F6, verify it fires in-game
6. **All playback modes**: Test play-once, toggle-loop, and hold-to-play
7. **In-game guard**: Verify macros don't fire on title screen or in menus
8. **Import/export**: Export macros, delete them, re-import, verify they restore
9. **Cancellation**: Verify toggle-loop stops cleanly, hold-to-play stops on release
10. **Disconnect cleanup**: All macros stop when leaving a server/world
