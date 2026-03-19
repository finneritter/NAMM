# NAMM v2.0 — Hacked Client UI Overhaul

## Overview

Restyle NAMM's entire UI to match the premium hacked-client aesthetic from the jbdsgn reference screenshots. The mod should feel like a polished, high-resolution hacked client with light and dark themes, toast notifications, a top info bar, and seamless integration with Minecraft's controls system.

**Reference:** The jbdsgn "Your Client" HUD design (screenshots provided by user) is the visual spec. All panels, text, spacing, and color choices must match those screenshots as closely as possible.

## Goals

- Premium hacked-client look: semi-transparent panels, clean modern font, subtle borders
- Light and dark theme with toggle
- Toast notification system with granular controls
- Top info bar with status and controls
- Configurable menu keybind via MC's controls UI
- Decompose the 2029-line `NammGuiScreen.java` into focused, maintainable components

## Non-Goals

- Real blur/frosted glass shader effects (using semi-transparent fills instead)
- Changing the 4-window draggable layout (keeping existing UX, reskinning it)
- Adding new macro functionality (this is a visual overhaul only)

---

## Architecture

### File Decomposition

The monolithic `NammGuiScreen.java` (2029 lines) is broken into focused components:

```
src/main/java/com/namm/
    config/
        NammGuiScreen.java              -- Screen lifecycle, window orchestration, event dispatch (~400-500 lines)
        KeyCaptureScreen.java           -- (unchanged)
        NammConfig.java                 -- Add: theme preference, info bar visibility, notification settings
        MacroSerializer.java            -- Add: theme/infobar/notification serialization
        RazerSynapseImporter.java       -- (unchanged)
    ui/
        NammTheme.java                  -- Light/dark color palettes, current theme state
        NammRenderer.java               -- Themed drawing primitives (panels, text, icons, separators)
        NammWindow.java                 -- Reusable draggable window container
        ToastManager.java               -- Notification queue, fade animation, rendering
        InfoBar.java                    -- Top bar HUD (status, theme toggle, bell)
        NotificationSettingsScreen.java -- Notification filter toggles + info bar visibility
    ui/windows/
        MacroWindowRenderer.java        -- Macro list content
        ProfileWindowRenderer.java      -- Profile list content
        EditorWindowRenderer.java       -- Step editor content
        ChatCommandWindowRenderer.java  -- Chat command list content
```

### Key Interfaces

```java
/**
 * Each window's content rendering is encapsulated behind this interface.
 * NammWindow handles the container (drag, collapse, scroll, themed panel);
 * the content renderer handles what goes inside.
 *
 * All input methods return true if the event was consumed.
 */
interface WindowContent {
    int getContentHeight();
    void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY);
    boolean mouseClicked(int x, int y, int button);
    boolean mouseReleased(int x, int y, int button);
    boolean mouseScrolled(int x, int y, double amount);
    boolean mouseDragged(int x, int y, int button, double deltaX, double deltaY);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
    boolean charTyped(char chr, int modifiers);

    /**
     * Called when the window is collapsed/expanded. Content renderers that own
     * EditBox widgets must add/remove them from the parent Screen here.
     * The Screen reference is passed so renderers can call addRenderableWidget()
     * and removeWidget() as needed.
     */
    void onCollapseChanged(Screen parentScreen, boolean collapsed);
}
```

`NammWindow` holds a reference to the parent `Screen` (set at construction) and calls `content.onCollapseChanged(screen, collapsed)` when collapse state toggles. Content renderers that own EditBoxes (EditorWindowRenderer, ChatCommandWindowRenderer) use this callback to manage widget lifecycle. Content renderers without widgets (MacroWindowRenderer, ProfileWindowRenderer) provide no-op implementations.

### HUD Callback Registration

`NammMod.onInitializeClient()` registers `HudRenderCallback.EVENT.register(...)` which delegates to static singleton instances of `ToastManager` and `InfoBar`. These singletons are accessible from both the HUD callback (for in-game overlay rendering) and from `NammGuiScreen` (for rendering within the config screen). The callback checks `InfoBar.isAlwaysVisible()` before rendering the info bar outside the config screen; `ToastManager` always renders via the callback regardless.

### ModMenu Integration

`NammModMenuIntegration` currently returns the custom `NammGuiScreen` (YACL was removed in a prior version). This is unchanged — the ModMenu settings button continues to open `NammGuiScreen`. No YACL dependency exists.

### Rendering Pipeline

- **Config screen open:** `NammGuiScreen` renders the 4 `NammWindow` instances + `InfoBar` at the top. The HUD callback skips `InfoBar` rendering to avoid double-drawing (checks `Minecraft.getInstance().screen instanceof NammGuiScreen`).
- **Config screen closed:** `HudRenderCallback` renders `InfoBar` (if "always visible") and `ToastManager` toasts.
- All drawing goes through `NammRenderer` which reads colors from `NammTheme`.

---

## Theme System (`NammTheme`)

Two complete color palettes matched to the reference screenshots. A single `isDark` boolean flag switches between them. All rendering code calls theme accessors — zero hardcoded colors anywhere in the rendering path.

### Dark Theme (matches night screenshot)

| Role | Value | Description |
|------|-------|-------------|
| Panel background | `rgba(24, 24, 30, 0.85)` | Near-black, warm dark gray |
| Header background | `rgba(30, 30, 38, 0.95)` | Slightly lighter than panel |
| Text primary | `#E0E0E8` | Soft white |
| Text secondary | `#6E6E78` | Muted gray |
| Accent | `#7C6FE0` | Purple (brand continuity) |
| Hover | `rgba(255, 255, 255, 0.12)` | Subtle white overlay |
| Border | `rgba(255, 255, 255, 0.06)` | Very faint edge |
| Toggle on | `#7C6FE0` (accent) | |
| Toggle off | `#3A3A42` | Muted dark |
| Separator | `rgba(255, 255, 255, 0.07)` | Thin divider |
| Destructive | `#D45555` | Delete/remove actions |
| Toast success | `#55B87A` | Green circle |
| Toast error | `#D45555` | Red circle |
| Toast info | `#5588D4` | Blue circle |

### Light Theme (matches day screenshot)

| Role | Value | Description |
|------|-------|-------------|
| Panel background | `rgba(255, 255, 255, 0.85)` | Clean white |
| Header background | `rgba(245, 245, 248, 0.95)` | Subtle off-white |
| Text primary | `#1A1A2E` | Near-black |
| Text secondary | `#8888A0` | Medium gray |
| Accent | `#7C6FE0` | Same purple |
| Hover | `rgba(0, 0, 0, 0.06)` | Subtle dark overlay |
| Border | `rgba(0, 0, 0, 0.08)` | Faint dark edge |
| Toggle on | `#7C6FE0` (accent) | |
| Toggle off | `#C8C8D0` | Light muted |
| Separator | `rgba(0, 0, 0, 0.06)` | Thin divider |
| Destructive | `#D45555` | |
| Toast success | `#55B87A` | |
| Toast error | `#D45555` | |
| Toast info | `#5588D4` | |

### Theme Toggle

- Sun/moon icon in the top info bar.
- Left-click toggles between light and dark.
- Preference persisted in `namm.json` under `theme: "dark"` or `theme: "light"`.
- Default: dark.

---

## NammRenderer — Drawing Primitives

Static utility class. Every method reads colors from `NammTheme.get()`. No method accepts raw color parameters for themed elements.

### Methods

- **`drawPanel(GuiGraphics g, int x, int y, int w, int h)`** — Semi-transparent filled rectangle with theme panel background. 1px border in theme border color. 1px chamfer at corners (inset fill by 1px at each corner pixel) to approximate rounded corners.
- **`drawHeader(GuiGraphics g, int x, int y, int w, int h, String title)`** — Header bar with theme header background. Collapse indicator dots (`...`) on the left. Title centered. Slightly different background shade from the panel body.
- **`drawRow(GuiGraphics g, int x, int y, int w, int h, boolean hovered)`** — If hovered, fills with theme hover color. Otherwise transparent.
- **`drawToggle(GuiGraphics g, int x, int y, boolean on)`** — Small pill-shaped toggle. Uses accent color when on, muted when off. Rendered via sprite atlas texture.
- **`drawSeparator(GuiGraphics g, int x, int y, int w)`** — 1px horizontal line in theme separator color.
- **`drawIcon(GuiGraphics g, int x, int y, IconType icon)`** — Draws from the sprite atlas. Icons: bell, bell-muted, sun, moon, macro, chat, profile, enabled-dot, disabled-dot, info-dot.
- **`drawText(GuiGraphics g, int x, int y, String text, boolean primary)`** — Draws text using the custom TTF font in theme primary or secondary color.
- **`drawTextRight(GuiGraphics g, int x, int y, String text, boolean primary)`** — Right-aligned variant.

---

## Custom Font

### Approach

Bundle a TrueType font via Minecraft's resource pack font system. The mod's assets include:

```
src/main/resources/assets/namm/
    font/
        inter.ttf               -- Inter font file (bundled in mod JAR)
    assets/minecraft/font/
        default.json            -- Overrides MC's default font provider
```

**Font plumbing:** Minecraft 1.21's font system resolves fonts by resource location. The global default font used by `GuiGraphics.drawString()` is `minecraft:default`, loaded from `assets/minecraft/font/default.json`. To use a custom TTF everywhere in our UI without requiring a separate `Font` object, we override `minecraft:default` by placing our font provider JSON at `assets/minecraft/font/default.json` within the mod's resources. This adds our TTF as an additional font provider to MC's default font — MC merges providers from all resource packs/mods, so our TTF takes precedence for the character ranges it covers while MC's built-in font handles anything we don't (e.g., special symbols).

The font provider JSON:
```json
{
  "providers": [
    {
      "type": "ttf",
      "file": "namm:inter.ttf",
      "shift": [0, 1],
      "size": 10.0,
      "oversample": 4.0
    }
  ]
}
```

The `oversample: 4.0` ensures high-resolution glyph rendering. The `shift` value may need fine-tuning to align with MC's text baseline.

With this approach, `NammRenderer.drawText()` simply calls `GuiGraphics.drawString()` with the standard `Minecraft.getInstance().font` — no special Font object needed. The TTF applies only when NAMM is installed; uninstalling the mod restores MC's default font.

**Caveat:** This overrides the default font globally while NAMM is loaded. If this causes issues with other mods' text rendering, an alternative is to register a namespaced font (`namm:ui`) via `assets/namm/font/ui.json` and obtain a `Font` instance from `Minecraft.getInstance().fontManager.createFont(ResourceLocation.fromNamespaceAndPath("namm", "ui"))` to pass explicitly to draw calls. The global override is simpler and is the initial approach; we fall back to the namespaced approach if compatibility issues arise during testing.

### Font Choice

Inter — clean, modern sans-serif that matches the reference screenshots. Open source (SIL Open Font License), freely redistributable. Regular weight only; at the sizes we render (8-12px scaled) this is sufficient.

---

## Icon Sprite Atlas

A single PNG texture (`assets/namm/textures/gui/icons.png`) containing all UI icons at native resolution:

- Bell (normal + muted state)
- Sun and moon (theme toggle)
- Macro status dot (green/red/blue)
- Toggle pill shapes (on/off)
- Collapse dots (`...`)

Rendered at 1:1 pixel ratio via `GuiGraphics.blit()` for pixel-perfect crispness. Atlas size: 128x128 with 16x16 icon slots.

### Sprite Layout

The atlas is a 128x128 PNG organized as an 8x8 grid of 16x16 slots:

| Slot (col, row) | Icon | Notes |
|-----------------|------|-------|
| (0, 0) | Bell | Normal state |
| (1, 0) | Bell muted | Slash overlay |
| (2, 0) | Sun | Light theme indicator |
| (3, 0) | Moon | Dark theme indicator |
| (0, 1) | Dot green | Enabled/success |
| (1, 1) | Dot red | Disabled/error |
| (2, 1) | Dot blue | Info |
| (0, 2) | Toggle on | Pill shape, accent fill |
| (1, 2) | Toggle off | Pill shape, muted fill |
| (0, 3) | Collapse dots | `...` pattern |

Remaining slots are reserved. The atlas is created as part of the implementation — a simple pixel art PNG. Both light and dark themes use the same atlas; icon colors are baked into the texture (status dots) or tinted at render time via vertex color (bell, sun/moon).

---

## NammWindow — Reusable Window Container

### State

```java
class NammWindow {
    String title;
    int x, y, width;
    boolean collapsed;
    double scrollOffset;
    WindowContent content;
}
```

### Behavior

- **Rendering:** Calls `NammRenderer.drawPanel()` for the body, `NammRenderer.drawHeader()` for the title bar. When collapsed, only the header renders.
- **Dragging:** Mouse down on header starts drag. Tracks offset from click point. Position clamped to screen bounds.
- **Collapsing:** Click on the collapse dots (`...`) in the header toggles collapsed state. When expanding, re-adds any child widgets (EditBoxes) to the screen.
- **Scrolling:** Mouse wheel within the content area adjusts `scrollOffset`. Content is scissor-clipped to the panel bounds.
- **Hit testing:** `isMouseOver(mouseX, mouseY)` checks panel bounds.
- **Max height:** 300px (unchanged from current). Content scrolls within this.

### Window Instances

`NammGuiScreen` creates 4 windows:

| Window | Width | Content Renderer |
|--------|-------|-----------------|
| Macros | 160 | `MacroWindowRenderer` |
| Profiles | 160 | `ProfileWindowRenderer` |
| Editor | 220 | `EditorWindowRenderer` |
| Chat Commands | 180 | `ChatCommandWindowRenderer` |

Position persistence unchanged — saved in `namm.json`.

---

## Top Info Bar (`InfoBar`)

### Layout

Slim horizontal panel at the top-left of the screen. Same themed panel style as windows.

**Left to right:**
1. **"NAMM"** — label in accent color
2. **Active profile** — profile name or "None"
3. **Active macros** — e.g., "3 active"
4. **Ping** — from `PlayerInfo.getLatency()`, e.g., "24ms". Displays "N/A" in single-player (where latency returns 0)
5. **Clock** — real-world time in `HH:mm` format

**Right side (top-right of screen):**
6. **Sun/moon icon** — left-click toggles light/dark theme
7. **Bell icon** — left-click mutes/unmutes all notifications; right-click opens `NotificationSettingsScreen`

### Visibility

Configurable in `NotificationSettingsScreen`:
- **"Menu only"** (default) — renders as part of `NammGuiScreen` only
- **"Always visible"** — renders via `HudRenderCallback` as a persistent HUD overlay

Persisted in `namm.json` as `infoBarVisibility: "menu_only"` or `"always"`.

---

## Toast Notifications (`ToastManager`)

### Appearance

Small themed panels in the bottom-right corner, stacking upward. Each toast contains:
- **Status icon** — colored circle (green = enabled/success, red = disabled/error, blue = info)
- **Message text** — e.g., "Sprint Macro was enabled"

### Animation

- Fade in: 200ms (alpha 0 → 1)
- Hold: 3 seconds
- Fade out: 300ms (alpha 1 → 0)
- Max visible: 4 toasts. When a 5th arrives, the oldest is instantly removed (no fade) to make room.

### Notification Categories

Each independently toggleable in `NotificationSettingsScreen`:

| Category | Example | Default |
|----------|---------|---------|
| Macro toggled | "Sprint was enabled" | On |
| Chat command executed | "Sent /home" | On |
| Profile switched | "Switched to PvP" | On |
| Import/export | "Exported 5 macros" | On |
| Errors | "Failed to import file" | On |

### Mute

Left-clicking the bell icon in the info bar mutes ALL toasts regardless of category settings. A muted bell shows a different icon (bell with slash). State persisted in config.

### Rendering

Via `HudRenderCallback` — toasts are visible whether the config screen is open or not.

---

## Notification Settings Screen (`NotificationSettingsScreen`)

Opened by right-clicking the bell icon in the info bar. Rendered as a modal overlay within `NammGuiScreen` (not a separate Minecraft `Screen`).

### Rendering & Input Model

`NammGuiScreen` maintains a `boolean showingNotificationSettings` flag. When true:
- The `NotificationSettingsScreen` overlay renders on top of everything else (after windows, after info bar).
- A semi-transparent scrim covers the background to visually separate the overlay.
- All mouse/key input is intercepted by the overlay first. Clicks outside the overlay panel close it (the click is consumed and does NOT propagate to windows underneath).
- While the overlay is open, the 4 draggable windows do not receive input events.

### Contents

- **Info bar visibility** toggle: "Menu only" / "Always visible"
- **Category toggles** — one row per notification category with a toggle switch
- Themed panel with same visual style as the windows

All settings persisted in `namm.json`.

---

## Configurable Menu Keybind

### Current State

The menu toggle is hardcoded to Right Shift via raw `GLFW.glfwGetKey()` polling in `NammMod.onInitializeClient()` (not in `TriggerKeyHandler`). The current code polls every client tick and opens the screen on key press.

### New Approach

Register a Fabric `KeyMapping` in `NammMod.onInitializeClient()`:

```java
KeyMapping openMenu = KeyBindingHelper.registerKeyBinding(
    new KeyMapping("key.namm.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.namm")
);
```

This places the keybind in Minecraft's Options → Controls → Key Binds under a "NAMM" category. Users can rebind it to any key through MC's standard UI.

The tick handler in `NammMod` changes from raw GLFW polling to `openMenu.consumeClick()`. Note: `consumeClick()` returns true once per press and auto-debounces, which is the correct behavior for a toggle (the current code manually tracks previous key state for edge detection — `consumeClick()` replaces that logic). The `NammMod` tick handler is updated, not `TriggerKeyHandler` (which handles macro/chat command triggers and remains unchanged).

---

## Config Persistence Changes

`namm.json` gains new fields in the config wrapper:

```json
{
  "macros": [...],
  "profiles": [...],
  "chatCommands": [...],
  "theme": "dark",
  "infoBarVisibility": "menu_only",
  "notificationsMuted": false,
  "notificationCategories": {
    "macroToggled": true,
    "chatCommandExecuted": true,
    "profileSwitched": true,
    "importExport": true,
    "errors": true
  },
  "macroWinX": 10,
  "macroWinY": 30,
  ...
}
```

`MacroSerializer` handles backwards compatibility — missing fields default to the values shown above.

---

## Migration Path

This is a visual overhaul, not a data model change. Existing `namm.json` files continue to work — new config fields are added with sensible defaults. No data migration needed beyond what `MacroSerializer` already handles for missing fields.

---

## Testing Strategy

All changes are client-side rendering. Testing approach:

1. **Manual in-game testing** — verify each window renders correctly in both themes
2. **Theme toggle** — switch between light/dark, verify all elements update
3. **Toast notifications** — trigger each category, verify appearance and fade animation
4. **Info bar** — verify all status fields update correctly, both visibility modes
5. **Keybind** — rebind in MC controls, verify menu opens with new key
6. **Backwards compatibility** — load an existing `namm.json` from v1.5.0, verify no data loss
7. **Mod compatibility** — test with Sodium and ModMenu installed

---

## Post-Implementation

- Update `CLAUDE.md` to reflect the new `ui/` package structure and architectural patterns.
- Update `README.md` if user-facing features change (keybind configuration, theme toggle).
- Bump version to 2.0.0 in `gradle.properties`.
