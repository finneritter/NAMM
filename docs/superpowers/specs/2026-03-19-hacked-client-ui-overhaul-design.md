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
 */
interface WindowContent {
    int getContentHeight();
    void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY);
    boolean mouseClicked(int x, int y, int button);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
}
```

### Rendering Pipeline

- **Config screen open:** `NammGuiScreen` renders the 4 `NammWindow` instances + `InfoBar` at the top.
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
        inter.ttf               -- Inter font file (or similar clean sans-serif)
        default.json            -- Font provider definition
```

The `default.json` font provider registers the TTF under a custom font ID (`namm:default`). All text rendering in `NammRenderer` uses this font ID instead of Minecraft's default.

### Font Choice

Inter (or Roboto) — clean, modern sans-serif that matches the reference screenshots. Open source, freely redistributable. Multiple weights not needed; regular weight at the sizes we're rendering (8-12px) is sufficient.

---

## Icon Sprite Atlas

A single PNG texture (`assets/namm/textures/gui/icons.png`) containing all UI icons at native resolution:

- Bell (normal + muted state)
- Sun and moon (theme toggle)
- Macro status dot (green/red/blue)
- Toggle pill shapes (on/off)
- Collapse dots (`...`)

Rendered at 1:1 pixel ratio via `GuiGraphics.blit()` for pixel-perfect crispness. Atlas size: 128x128 with 16x16 icon slots.

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
4. **Ping** — from `PlayerInfo.getLatency()`, e.g., "24ms"
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
- Max visible: 4 toasts. When a 5th arrives, the oldest fades out immediately.

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

Opened by right-clicking the bell icon in the info bar. A simple themed panel (not a full Minecraft Screen — rendered as an overlay within the NAMM GUI) containing:

- **Info bar visibility** toggle: "Menu only" / "Always visible"
- **Category toggles** — one row per notification category with a toggle switch
- **Close button** or click-outside-to-close

All settings persisted in `namm.json`.

---

## Configurable Menu Keybind

### Current State

The menu toggle is hardcoded to Right Shift in `TriggerKeyHandler`.

### New Approach

Register a Fabric `KeyMapping` in `NammMod.onInitializeClient()`:

```java
KeyMapping openMenu = KeyBindingHelper.registerKeyBinding(
    new KeyMapping("key.namm.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.namm")
);
```

This places the keybind in Minecraft's Options → Controls → Key Binds under a "NAMM" category. Users can rebind it to any key through MC's standard UI.

`TriggerKeyHandler` checks this `KeyMapping` instead of hardcoding `GLFW_KEY_RIGHT_SHIFT`.

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
