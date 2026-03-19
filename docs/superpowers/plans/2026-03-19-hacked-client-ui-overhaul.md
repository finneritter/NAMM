# Hacked Client UI Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle NAMM's UI to match the jbdsgn "Your Client" hacked-client aesthetic with light/dark themes, toast notifications, info bar, and configurable keybind.

**Architecture:** Break the 2029-line `NammGuiScreen.java` into a theme system, renderer, reusable window container, and per-window content renderers. Add HUD overlay components (info bar, toasts) via Fabric's `HudRenderCallback`. Bundle a TTF font for high-res text.

**Tech Stack:** Java 21, Fabric API (HudRenderCallback, KeyBindingHelper), Minecraft 1.21.11 GuiGraphics API, Gson, Inter TTF font.

**Spec:** `docs/superpowers/specs/2026-03-19-hacked-client-ui-overhaul-design.md`

---

## File Map

### New Files

| File | Responsibility |
|------|---------------|
| `src/main/java/com/namm/ui/NammTheme.java` | Light/dark color palettes, theme state singleton, color accessors |
| `src/main/java/com/namm/ui/NammRenderer.java` | Themed drawing primitives (panels, headers, rows, toggles, text, icons) |
| `src/main/java/com/namm/ui/WindowContent.java` | Interface for window content renderers |
| `src/main/java/com/namm/ui/NammWindow.java` | Reusable draggable/collapsible/scrollable window container |
| `src/main/java/com/namm/ui/ToastManager.java` | Toast notification queue, fade animation, HUD rendering |
| `src/main/java/com/namm/ui/InfoBar.java` | Top info bar HUD (status, theme toggle, bell icon) |
| `src/main/java/com/namm/ui/NotificationSettingsScreen.java` | Modal overlay for notification category toggles |
| `src/main/java/com/namm/ui/IconType.java` | Enum for sprite atlas icon types with UV coordinates |
| `src/main/java/com/namm/ui/windows/MacroWindowRenderer.java` | Macro list content rendering + click handling |
| `src/main/java/com/namm/ui/windows/ProfileWindowRenderer.java` | Profile list content rendering + click handling |
| `src/main/java/com/namm/ui/windows/EditorWindowRenderer.java` | Step editor content rendering + click/key handling |
| `src/main/java/com/namm/ui/windows/ChatCommandWindowRenderer.java` | Chat command list content rendering + click/key handling |
| `src/main/resources/assets/namm/font/inter.ttf` | Inter TTF font file |
| `src/main/resources/assets/minecraft/font/default.json` | Font provider override for TTF |
| `src/main/resources/assets/namm/textures/gui/icons.png` | 128x128 sprite atlas for UI icons |

### Modified Files

| File | Changes |
|------|---------|
| `src/main/java/com/namm/config/NammGuiScreen.java` | Gut and rewrite — slim orchestrator using NammWindow + content renderers (~400-500 lines) |
| `src/main/java/com/namm/config/NammConfig.java` | Add theme, infoBarVisibility, notificationsMuted, notificationCategories fields + getters/setters |
| `src/main/java/com/namm/config/MacroSerializer.java` | Add new fields to ConfigWrapper with defaults for backwards compatibility |
| `src/main/java/com/namm/NammMod.java` | Register KeyMapping, HudRenderCallback, initialize ToastManager/InfoBar singletons |
| `src/main/java/com/namm/input/TriggerKeyHandler.java` | Fire toast events on macro toggle and chat command execution |
| `src/main/java/com/namm/executor/MacroPlaybackState.java` | Fire toast events on macro start/stop |
| `src/main/resources/assets/namm/lang/en_us.json` | Add keybind, info bar, and notification translation keys |
| `CLAUDE.md` | Remove stale YACL references, add `ui/` package to structure |
| `gradle.properties` | Bump version to 2.0.0 |

---

## Task 1: Pre-Implementation — Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

The spec notes that CLAUDE.md has stale YACL references. Fix this first so implementation agents have accurate context.

- [ ] **Step 1: Update CLAUDE.md**

Remove "YACL 3.8.1+1.21.11-fabric" from the Tech Stack section. Change "All UI through YACL (no custom screens except key capture)" to "All UI through custom Screen subclasses (NammGuiScreen, KeyCaptureScreen)". Add `ui/` and `ui/windows/` to the Project Structure section. These will be populated in subsequent tasks.

```markdown
## Tech Stack
- **Java 21** with Fabric Loader + Fabric API + Fabric Loom (Gradle)
- **Minecraft 1.21.11** (Mounts of Mayhem)
- **ModMenu** for settings integration
- **Gson** for JSON config serialization
- **Inter TTF font** for high-resolution UI text

## Project Structure
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

## Key Design Decisions
- All UI through custom Screen subclasses (NammGuiScreen, KeyCaptureScreen)
- Themed rendering via NammRenderer + NammTheme (light/dark modes)
- ...rest unchanged...
```

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md — remove stale YACL refs, add ui/ package structure"
```

---

## Task 2: Theme System (`NammTheme`)

**Files:**
- Create: `src/main/java/com/namm/ui/NammTheme.java`
- Modify: `src/main/java/com/namm/config/NammConfig.java`
- Modify: `src/main/java/com/namm/config/MacroSerializer.java`

Build the theme singleton that provides all colors for both light and dark modes. Also extend `NammConfig` and `MacroSerializer.ConfigWrapper` with all new config fields needed by the overhaul (theme, notifications, info bar visibility).

- [ ] **Step 1: Create NammTheme.java**

```java
package com.namm.ui;

import com.namm.config.NammConfig;

/**
 * Provides light/dark color palettes. Singleton — call NammTheme.get().
 * All colors returned as ARGB ints for use with GuiGraphics.fill() / drawString().
 */
public class NammTheme {
    private static final NammTheme INSTANCE = new NammTheme();

    public static NammTheme get() { return INSTANCE; }

    public boolean isDark() {
        return "dark".equals(NammConfig.getInstance().getTheme());
    }

    public void toggle() {
        NammConfig cfg = NammConfig.getInstance();
        cfg.setTheme(isDark() ? "light" : "dark");
        cfg.save();
    }

    // --- Colors (ARGB int format) ---
    // Dark palette matched to jbdsgn night screenshot
    // Light palette matched to jbdsgn day screenshot

    public int panelBg() {
        return isDark() ? 0xD9181820 : 0xD9FFFFFF;
    }

    public int headerBg() {
        return isDark() ? 0xF21E1E26 : 0xF2F5F5F8;
    }

    public int textPrimary() {
        return isDark() ? 0xFFE0E0E8 : 0xFF1A1A2E;
    }

    public int textSecondary() {
        return isDark() ? 0xFF6E6E78 : 0xFF8888A0;
    }

    public int accent() {
        return 0xFF7C6FE0;
    }

    public int hover() {
        return isDark() ? 0x1FFFFFFF : 0x0F000000;
    }

    public int border() {
        return isDark() ? 0x0FFFFFFF : 0x14000000;
    }

    public int toggleOn() {
        return 0xFF7C6FE0;
    }

    public int toggleOff() {
        return isDark() ? 0xFF3A3A42 : 0xFFC8C8D0;
    }

    public int separator() {
        return isDark() ? 0x12FFFFFF : 0x0F000000;
    }

    public int destructive() {
        return 0xFFD45555;
    }

    public int toastSuccess() {
        return 0xFF55B87A;
    }

    public int toastError() {
        return 0xFFD45555;
    }

    public int toastInfo() {
        return 0xFF5588D4;
    }

    public int screenOverlay() {
        return isDark() ? 0x90000000 : 0x60000000;
    }

    public int scrim() {
        return 0x80000000;
    }
}
```

- [ ] **Step 2: Add new fields to NammConfig**

Add these fields and getters/setters to `NammConfig.java`:

```java
// Theme
private String theme = "dark";

// Info bar
private String infoBarVisibility = "menu_only"; // "menu_only" or "always"

// Notifications
private boolean notificationsMuted = false;
private boolean notifMacroToggled = true;
private boolean notifChatCommand = true;
private boolean notifProfileSwitched = true;
private boolean notifImportExport = true;
private boolean notifErrors = true;
```

Add corresponding getters/setters. Update `load()` to read these from the wrapper and `save()` to write them.

- [ ] **Step 3: Add new fields to MacroSerializer.ConfigWrapper**

Add to `ConfigWrapper`:

```java
public String theme = "dark";
public String infoBarVisibility = "menu_only";
public boolean notificationsMuted = false;
public boolean notifMacroToggled = true;
public boolean notifChatCommand = true;
public boolean notifProfileSwitched = true;
public boolean notifImportExport = true;
public boolean notifErrors = true;
```

In the `load()` method, add null-guards after deserialization:

```java
if (wrapper.theme == null) wrapper.theme = "dark";
if (wrapper.infoBarVisibility == null) wrapper.infoBarVisibility = "menu_only";
```

- [ ] **Step 4: Verify build compiles**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/namm/ui/NammTheme.java src/main/java/com/namm/config/NammConfig.java src/main/java/com/namm/config/MacroSerializer.java
git commit -m "feat: add NammTheme with light/dark palettes and config persistence"
```

---

## Task 3: Icon Sprite Atlas and Font Assets

**Files:**
- Create: `src/main/resources/assets/namm/textures/gui/icons.png`
- Create: `src/main/resources/assets/namm/font/inter.ttf`
- Create: `src/main/resources/assets/minecraft/font/default.json`
- Create: `src/main/java/com/namm/ui/IconType.java`

- [ ] **Step 1: Create IconType enum**

```java
package com.namm.ui;

/**
 * Icons in the sprite atlas (assets/namm/textures/gui/icons.png).
 * Atlas is 128x128 with 16x16 slots in an 8x8 grid.
 */
public enum IconType {
    BELL(0, 0),
    BELL_MUTED(1, 0),
    SUN(2, 0),
    MOON(3, 0),
    DOT_GREEN(0, 1),
    DOT_RED(1, 1),
    DOT_BLUE(2, 1),
    TOGGLE_ON(0, 2),
    TOGGLE_OFF(1, 2),
    COLLAPSE_DOTS(0, 3);

    public final int u; // pixel x in atlas
    public final int v; // pixel y in atlas
    public static final int SIZE = 16;
    public static final int ATLAS_SIZE = 128;

    IconType(int col, int row) {
        this.u = col * SIZE;
        this.v = row * SIZE;
    }
}
```

- [ ] **Step 2: Create sprite atlas PNG**

Create a 128x128 PNG at `src/main/resources/assets/namm/textures/gui/icons.png`. This is pixel art — use simple geometric shapes:

- **(0,0) Bell:** 16x16, white bell silhouette on transparent background. Simple bell shape: rounded top, flared bottom, small clapper dot.
- **(1,0) Bell muted:** Same bell with a diagonal slash line through it.
- **(2,0) Sun:** White circle with 8 short rays radiating outward.
- **(3,0) Moon:** White crescent moon shape.
- **(0,1) Dot green:** Centered 6x6 filled circle, `#55B87A`.
- **(1,1) Dot red:** Centered 6x6 filled circle, `#D45555`.
- **(2,1) Dot blue:** Centered 6x6 filled circle, `#5588D4`.
- **(0,2) Toggle on:** 12x8 pill shape, filled with `#7C6FE0`, white circle on right.
- **(1,2) Toggle off:** 12x8 pill shape, filled with `#3A3A42`, white circle on left.
- **(0,3) Collapse dots:** Three small white dots arranged horizontally (`...`).

Use any image editor or programmatic generation. The atlas must be exactly 128x128px with transparency.

- [ ] **Step 3: Download Inter font**

Download Inter Regular TTF from the Inter font project (SIL Open Font License). Place at `src/main/resources/assets/namm/font/inter.ttf`.

**Important:** Minecraft's TTF font provider requires an actual TrueType (.ttf) file, not WOFF2. Download the `.ttf` from the Inter GitHub releases page:
1. Go to https://github.com/rsms/inter/releases
2. Download the latest release ZIP
3. Extract `Inter-Regular.ttf` from the `extras/ttf/` directory
4. Copy it to `src/main/resources/assets/namm/font/inter.ttf`

Verify the file is a valid TTF (not WOFF2) before proceeding.

- [ ] **Step 4: Create font provider JSON**

Create `src/main/resources/assets/minecraft/font/default.json`:

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

- [ ] **Step 5: Verify build compiles**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL (font/atlas are just resources, no Java changes needed to verify they load — that's tested in-game).

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/assets/ src/main/java/com/namm/ui/IconType.java
git commit -m "feat: add icon sprite atlas, Inter TTF font, and font provider"
```

---

## Task 4: NammRenderer — Drawing Primitives

**Files:**
- Create: `src/main/java/com/namm/ui/NammRenderer.java`

This is the central rendering utility. All themed drawing goes through here.

- [ ] **Step 1: Create NammRenderer.java**

```java
package com.namm.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Static themed drawing primitives. All colors come from NammTheme.
 * Reference: jbdsgn "Your Client" screenshots.
 */
public class NammRenderer {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath("namm", "textures/gui/icons.png");
    private static final int CORNER = 3; // corner chamfer size in pixels

    /** Semi-transparent panel with 1px border and chamfered corners. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        NammTheme t = NammTheme.get();
        int bg = t.panelBg();
        int border = t.border();

        // Fill with chamfered corners (inset corners by 1px)
        g.fill(x + CORNER, y, x + w - CORNER, y + CORNER, bg);      // top edge
        g.fill(x, y + CORNER, x + w, y + h - CORNER, bg);            // middle
        g.fill(x + CORNER, y + h - CORNER, x + w - CORNER, y + h, bg); // bottom edge
        // Corner fills (1px inset)
        g.fill(x + 1, y + 1, x + CORNER, y + CORNER, bg);
        g.fill(x + w - CORNER, y + 1, x + w - 1, y + CORNER, bg);
        g.fill(x + 1, y + h - CORNER, x + CORNER, y + h - 1, bg);
        g.fill(x + w - CORNER, y + h - CORNER, x + w - 1, y + h - 1, bg);

        // Border (1px lines along edges, skipping corner pixels)
        g.fill(x + CORNER, y, x + w - CORNER, y + 1, border);            // top
        g.fill(x + CORNER, y + h - 1, x + w - CORNER, y + h, border);    // bottom
        g.fill(x, y + CORNER, x + 1, y + h - CORNER, border);            // left
        g.fill(x + w - 1, y + CORNER, x + w, y + h - CORNER, border);    // right
    }

    /** Header bar with slightly different bg shade. Title centered, collapse dots on left. */
    public static void drawHeader(GuiGraphics g, int x, int y, int w, int h, String title, boolean collapsed) {
        NammTheme t = NammTheme.get();
        int bg = t.headerBg();

        if (collapsed) {
            // Full rounded rect when collapsed (header is entire window)
            drawPanel(g, x, y, w, h);
            // Override the fill with header bg
            g.fill(x + CORNER, y + 1, x + w - CORNER, y + h - 1, bg);
            g.fill(x + 1, y + CORNER, x + w - 1, y + h - CORNER, bg);
        } else {
            // Top-only rounded: chamfer top corners only
            g.fill(x + CORNER, y, x + w - CORNER, y + CORNER, bg);
            g.fill(x, y + CORNER, x + w, y + h, bg);
            g.fill(x + 1, y + 1, x + CORNER, y + CORNER, bg);
            g.fill(x + w - CORNER, y + 1, x + w - 1, y + CORNER, bg);
        }

        // Collapse dots (same icon for both states — always shows "...")
        drawIcon(g, x + 2, y + (h - IconType.SIZE) / 2, IconType.COLLAPSE_DOTS);

        // Centered title
        drawTextCentered(g, x + w / 2, y + (h - 8) / 2, title, true);
    }

    /** Hover highlight row. */
    public static void drawRow(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        if (hovered) {
            g.fill(x + 1, y, x + w - 1, y + h, NammTheme.get().hover());
        }
    }

    /** Separator line. */
    public static void drawSeparator(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, NammTheme.get().separator());
    }

    /** Draw icon from sprite atlas. */
    public static void drawIcon(GuiGraphics g, int x, int y, IconType icon) {
        g.blit(ICONS, x, y, icon.u, icon.v, IconType.SIZE, IconType.SIZE, IconType.ATLAS_SIZE, IconType.ATLAS_SIZE);
    }

    /** Draw text in theme primary or secondary color. */
    public static void drawText(GuiGraphics g, int x, int y, String text, boolean primary) {
        NammTheme t = NammTheme.get();
        g.drawString(Minecraft.getInstance().font, text, x, y, primary ? t.textPrimary() : t.textSecondary(), false);
    }

    /** Draw right-aligned text. */
    public static void drawTextRight(GuiGraphics g, int x, int y, String text, boolean primary) {
        int w = Minecraft.getInstance().font.width(text);
        drawText(g, x - w, y, text, primary);
    }

    /** Draw centered text. */
    public static void drawTextCentered(GuiGraphics g, int centerX, int y, String text, boolean primary) {
        NammTheme t = NammTheme.get();
        g.drawCenteredString(Minecraft.getInstance().font, text, centerX, y, primary ? t.textPrimary() : t.textSecondary());
    }

    /** Draw text in accent color. */
    public static void drawTextAccent(GuiGraphics g, int x, int y, String text) {
        g.drawString(Minecraft.getInstance().font, text, x, y, NammTheme.get().accent(), false);
    }

    /** Draw text in a specific color (for non-themed elements like status dots). */
    public static void drawTextColored(GuiGraphics g, int x, int y, String text, int color) {
        g.drawString(Minecraft.getInstance().font, text, x, y, color, false);
    }

    /** Small toggle indicator (filled rect). */
    public static void drawToggleIndicator(GuiGraphics g, int x, int y, int h, boolean on) {
        NammTheme t = NammTheme.get();
        g.fill(x, y, x + 3, y + h, on ? t.toggleOn() : t.toggleOff());
    }

    /** Checkbox (outline + optional fill). */
    public static void drawCheckbox(GuiGraphics g, int x, int y, int size, boolean checked) {
        NammTheme t = NammTheme.get();
        g.renderOutline(x, y, size, size, checked ? t.toggleOn() : t.toggleOff());
        if (checked) {
            g.fill(x + 2, y + 2, x + size - 2, y + size - 2, t.toggleOn());
        }
    }

    /** Bottom-only rounded panel (for window body below header). */
    public static void drawPanelBottom(GuiGraphics g, int x, int y, int w, int h) {
        NammTheme t = NammTheme.get();
        int bg = t.panelBg();
        g.fill(x, y, x + w, y + h - CORNER, bg);
        g.fill(x + CORNER, y + h - CORNER, x + w - CORNER, y + h, bg);
        g.fill(x + 1, y + h - CORNER, x + CORNER, y + h - 1, bg);
        g.fill(x + w - CORNER, y + h - CORNER, x + w - 1, y + h - 1, bg);
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/namm/ui/NammRenderer.java
git commit -m "feat: add NammRenderer with themed drawing primitives"
```

---

## Task 5: WindowContent Interface and NammWindow Container

**Files:**
- Create: `src/main/java/com/namm/ui/WindowContent.java`
- Create: `src/main/java/com/namm/ui/NammWindow.java`

- [ ] **Step 1: Create WindowContent.java**

```java
package com.namm.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Interface for window content renderers. NammWindow handles the container
 * (drag, collapse, scroll, themed panel); implementations handle content.
 */
public interface WindowContent {
    int getContentHeight();
    // Note: delta parameter added beyond spec's interface for EditBox cursor blink support
    void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float delta);
    boolean mouseClicked(int x, int y, int button);
    boolean mouseReleased(int x, int y, int button);
    boolean mouseScrolled(int x, int y, double amount);
    boolean mouseDragged(int x, int y, int button, double deltaX, double deltaY);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
    boolean charTyped(char chr, int modifiers);

    /**
     * Called when collapse state changes. Content renderers that own EditBox widgets
     * must add/remove them from the parent Screen here.
     */
    void onCollapseChanged(Screen parentScreen, boolean collapsed);
}
```

- [ ] **Step 2: Create NammWindow.java**

```java
package com.namm.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Reusable draggable, collapsible, scrollable window container.
 * Handles themed panel rendering, drag/collapse/scroll mechanics.
 * Content rendering is delegated to a WindowContent implementation.
 *
 * COORDINATE CONVENTION:
 * - render() passes SCREEN-ABSOLUTE coordinates to content.render() — the y parameter
 *   is (contentTop - scrollOffset), so content renders at screen positions directly.
 * - mouseClicked/mouseReleased/mouseDragged pass SCREEN-ABSOLUTE coordinates too —
 *   the raw mouseX/mouseY are forwarded as-is. Content renderers should compare
 *   against the same absolute positions they used in render().
 */
public class NammWindow {
    public static final int HEADER_HEIGHT = 18;
    public static final int ROW_HEIGHT = 16;
    public static final int MAX_HEIGHT = 300;

    private final String title;
    private final int width;
    private final Screen parentScreen;
    private WindowContent content;

    private int x, y;
    private boolean collapsed = false;
    private double scrollOffset = 0;

    // Drag state
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;
    private boolean didDrag = false;

    public NammWindow(String title, int width, int x, int y, Screen parentScreen) {
        this.title = title;
        this.width = width;
        this.x = x;
        this.y = y;
        this.parentScreen = parentScreen;
    }

    public void setContent(WindowContent content) {
        this.content = content;
    }

    public int getHeight() {
        if (collapsed || content == null) return HEADER_HEIGHT;
        int contentHeight = content.getContentHeight();
        return Math.min(MAX_HEIGHT, HEADER_HEIGHT + contentHeight + 4);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int h = getHeight();

        // Header
        NammRenderer.drawHeader(g, x, y, width, HEADER_HEIGHT, title, collapsed);

        if (collapsed || content == null) return;

        // Body
        NammRenderer.drawPanelBottom(g, x, y + HEADER_HEIGHT, width, h - HEADER_HEIGHT);

        // Scissor clip content area
        int contentTop = y + HEADER_HEIGHT;
        int contentBottom = y + h;
        g.enableScissor(x, contentTop, x + width, contentBottom);

        content.render(g, x, contentTop - (int) scrollOffset, width, mouseX, mouseY, delta);

        g.disableScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int h = getHeight();

        // Header click
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT) {
            // Collapse dots area (left 18px)
            if (mouseX < x + 18 && button == 0) {
                collapsed = !collapsed;
                scrollOffset = 0;
                if (content != null) {
                    content.onCollapseChanged(parentScreen, collapsed);
                }
                return true;
            }
            // Start drag
            if (button == 0) {
                dragging = true;
                didDrag = false;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
                return true;
            }
        }

        // Content click (if not collapsed) — pass screen-absolute coordinates
        // Content renderers compare against the same absolute positions used in render()
        if (!collapsed && content != null && isInContentArea(mouseX, mouseY)) {
            return content.mouseClicked((int) mouseX, (int) mouseY, button);
        }

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        if (!collapsed && content != null) {
            return content.mouseReleased((int) mouseX, (int) mouseY, button);
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            x = (int) (mouseX - dragOffsetX);
            y = (int) (mouseY - dragOffsetY);
            didDrag = true;
            return true;
        }
        if (!collapsed && content != null) {
            return content.mouseDragged((int) mouseX, (int) mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!collapsed && isMouseOver(mouseX, mouseY) && content != null) {
            double maxScroll = Math.max(0, content.getContentHeight() + 4 - (MAX_HEIGHT - HEADER_HEIGHT));
            scrollOffset = Math.max(0, Math.min(scrollOffset - amount * ROW_HEIGHT, maxScroll));
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!collapsed && content != null) {
            return content.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!collapsed && content != null) {
            return content.charTyped(chr, modifiers);
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        int h = getHeight();
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + h;
    }

    private boolean isInContentArea(double mouseX, double mouseY) {
        int h = getHeight();
        int contentTop = y + HEADER_HEIGHT;
        return mouseX >= x && mouseX < x + width && mouseY >= contentTop && mouseY < y + h;
    }

    public void clampToScreen(int screenW, int screenH) {
        x = Math.max(0, Math.min(x, screenW - width));
        y = Math.max(0, Math.min(y, screenH - HEADER_HEIGHT));
    }

    // Getters/setters
    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getWidth() { return width; }
    public boolean isCollapsed() { return collapsed; }
    public boolean isDragging() { return dragging; }
    public boolean didDrag() { return didDrag; }
    public void resetDrag() { didDrag = false; }
    public String getTitle() { return title; }
    public double getScrollOffset() { return scrollOffset; }
}
```

- [ ] **Step 3: Verify build compiles**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/namm/ui/WindowContent.java src/main/java/com/namm/ui/NammWindow.java
git commit -m "feat: add WindowContent interface and NammWindow container"
```

---

## Task 6: Toast Manager

**Files:**
- Create: `src/main/java/com/namm/ui/ToastManager.java`

- [ ] **Step 1: Create ToastManager.java**

```java
package com.namm.ui;

import com.namm.config.NammConfig;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages toast notifications. Thread-safe: events can be posted from any thread,
 * rendering happens on the render thread only.
 */
public class ToastManager {
    private static final ToastManager INSTANCE = new ToastManager();
    public static ToastManager get() { return INSTANCE; }

    private static final int MAX_VISIBLE = 4;
    private static final long FADE_IN_MS = 200;
    private static final long HOLD_MS = 3000;
    private static final long FADE_OUT_MS = 300;
    private static final int TOAST_WIDTH = 180;
    private static final int TOAST_HEIGHT = 24;
    private static final int TOAST_PADDING = 4;

    public enum Category {
        MACRO_TOGGLED,
        CHAT_COMMAND,
        PROFILE_SWITCHED,
        IMPORT_EXPORT,
        ERROR
    }

    public enum ToastType {
        SUCCESS, ERROR, INFO
    }

    private record ToastEvent(String message, ToastType type, Category category) {}

    private static class ActiveToast {
        final String message;
        final ToastType type;
        final long createdAt;

        ActiveToast(String message, ToastType type) {
            this.message = message;
            this.type = type;
            this.createdAt = System.currentTimeMillis();
        }

        float getAlpha() {
            long age = System.currentTimeMillis() - createdAt;
            long totalDuration = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

            if (age < FADE_IN_MS) {
                return (float) age / FADE_IN_MS;
            } else if (age < FADE_IN_MS + HOLD_MS) {
                return 1.0f;
            } else if (age < totalDuration) {
                return 1.0f - (float)(age - FADE_IN_MS - HOLD_MS) / FADE_OUT_MS;
            }
            return 0.0f;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > FADE_IN_MS + HOLD_MS + FADE_OUT_MS;
        }
    }

    private final ConcurrentLinkedQueue<ToastEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final List<ActiveToast> activeToasts = new ArrayList<>();

    /** Post a toast from any thread. */
    public void post(String message, ToastType type, Category category) {
        pendingEvents.add(new ToastEvent(message, type, category));
    }

    /** Render toasts. Call from HudRenderCallback on render thread. */
    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        NammConfig cfg = NammConfig.getInstance();

        // Drain pending events
        ToastEvent event;
        while ((event = pendingEvents.poll()) != null) {
            if (cfg.isNotificationsMuted()) continue;
            if (!isCategoryEnabled(cfg, event.category)) continue;

            // Evict oldest if at max
            if (activeToasts.size() >= MAX_VISIBLE) {
                activeToasts.remove(0);
            }
            activeToasts.add(new ActiveToast(event.message, event.type));
        }

        // Remove expired
        activeToasts.removeIf(ActiveToast::isExpired);

        // Render bottom-right, stacking upward
        int x = screenWidth - TOAST_WIDTH - 8;
        int baseY = screenHeight - 8;

        for (int i = activeToasts.size() - 1; i >= 0; i--) {
            ActiveToast toast = activeToasts.get(i);
            float alpha = toast.getAlpha();
            if (alpha <= 0) continue;

            int index = activeToasts.size() - 1 - i;
            int ty = baseY - (index + 1) * (TOAST_HEIGHT + TOAST_PADDING);

            int alphaInt = (int)(alpha * 255) << 24;

            // Background panel
            NammTheme t = NammTheme.get();
            int bg = (t.panelBg() & 0x00FFFFFF) | alphaInt;
            g.fill(x, ty, x + TOAST_WIDTH, ty + TOAST_HEIGHT, bg);

            // Status dot
            int dotColor = switch (toast.type) {
                case SUCCESS -> t.toastSuccess();
                case ERROR -> t.toastError();
                case INFO -> t.toastInfo();
            };
            dotColor = (dotColor & 0x00FFFFFF) | alphaInt;
            int dotY = ty + (TOAST_HEIGHT - 6) / 2;
            g.fill(x + 8, dotY, x + 14, dotY + 6, dotColor);

            // Message text
            int textColor = (t.textPrimary() & 0x00FFFFFF) | alphaInt;
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, toast.message, x + 20, ty + 8, textColor, false);
        }
    }

    private boolean isCategoryEnabled(NammConfig cfg, Category category) {
        return switch (category) {
            case MACRO_TOGGLED -> cfg.isNotifMacroToggled();
            case CHAT_COMMAND -> cfg.isNotifChatCommand();
            case PROFILE_SWITCHED -> cfg.isNotifProfileSwitched();
            case IMPORT_EXPORT -> cfg.isNotifImportExport();
            case ERROR -> cfg.isNotifErrors();
        };
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew build
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/namm/ui/ToastManager.java
git commit -m "feat: add ToastManager with thread-safe queue and fade animation"
```

---

## Task 7: Info Bar

**Files:**
- Create: `src/main/java/com/namm/ui/InfoBar.java`

- [ ] **Step 1: Create InfoBar.java**

```java
package com.namm.ui;

import com.namm.config.NammConfig;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Top info bar HUD. Renders NAMM label, active profile, macro count,
 * ping, clock, theme toggle, and notification bell.
 */
public class InfoBar {
    private static final InfoBar INSTANCE = new InfoBar();
    public static InfoBar get() { return INSTANCE; }

    private static final int BAR_HEIGHT = 20;
    private static final int PADDING = 6;
    private static final int ITEM_GAP = 12;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Ping caching (refresh every 2 seconds using wall clock, not frame count)
    private int cachedPing = 0;
    private long lastPingUpdateMs = 0;

    public boolean isAlwaysVisible() {
        return "always".equals(NammConfig.getInstance().getInfoBarVisibility());
    }

    /** Render the info bar. Call from HudRenderCallback or NammGuiScreen. */
    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        NammConfig cfg = NammConfig.getInstance();
        NammTheme t = NammTheme.get();
        Minecraft mc = Minecraft.getInstance();

        // Update ping cache (every 2 seconds, wall clock)
        long now = System.currentTimeMillis();
        if (now - lastPingUpdateMs > 2000) {
            lastPingUpdateMs = now;
            cachedPing = getPing(mc);
        }

        // Left bar
        int leftX = 4;
        int barY = 4;

        // Calculate left bar width
        String profileName = cfg.getActiveProfileName() != null ? cfg.getActiveProfileName() : "None";
        long activeMacroCount = cfg.getMacros().stream().filter(Macro::isEnabled).count();
        String macroCountStr = activeMacroCount + " active";
        String pingStr = cachedPing > 0 ? cachedPing + "ms" : "N/A";
        String timeStr = LocalTime.now().format(TIME_FMT);

        int leftBarWidth = PADDING
                + mc.font.width("NAMM") + ITEM_GAP
                + mc.font.width(profileName) + ITEM_GAP
                + mc.font.width(macroCountStr) + ITEM_GAP
                + mc.font.width(pingStr) + ITEM_GAP
                + mc.font.width(timeStr)
                + PADDING;

        // Draw left bar background
        NammRenderer.drawPanel(g, leftX, barY, leftBarWidth, BAR_HEIGHT);

        // Draw items
        int cx = leftX + PADDING;
        int textY = barY + (BAR_HEIGHT - 8) / 2;

        // NAMM label (accent color)
        NammRenderer.drawTextAccent(g, cx, textY, "NAMM");
        cx += mc.font.width("NAMM") + ITEM_GAP;

        // Active profile
        NammRenderer.drawText(g, cx, textY, profileName, true);
        cx += mc.font.width(profileName) + ITEM_GAP;

        // Active macros
        NammRenderer.drawText(g, cx, textY, macroCountStr, false);
        cx += mc.font.width(macroCountStr) + ITEM_GAP;

        // Ping
        NammRenderer.drawText(g, cx, textY, pingStr, false);
        cx += mc.font.width(pingStr) + ITEM_GAP;

        // Clock
        NammRenderer.drawText(g, cx, textY, timeStr, false);

        // Right bar (theme toggle + bell)
        int rightBarWidth = PADDING + IconType.SIZE + ITEM_GAP + IconType.SIZE + PADDING;
        int rightX = screenWidth - rightBarWidth - 4;

        NammRenderer.drawPanel(g, rightX, barY, rightBarWidth, BAR_HEIGHT);

        int iconY = barY + (BAR_HEIGHT - IconType.SIZE) / 2;

        // Sun/Moon icon
        int sunMoonX = rightX + PADDING;
        NammRenderer.drawIcon(g, sunMoonX, iconY, t.isDark() ? IconType.MOON : IconType.SUN);

        // Bell icon
        int bellX = sunMoonX + IconType.SIZE + ITEM_GAP;
        NammRenderer.drawIcon(g, bellX, iconY, cfg.isNotificationsMuted() ? IconType.BELL_MUTED : IconType.BELL);
    }

    /** Handle clicks on the info bar. Returns true if consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth) {
        NammConfig cfg = NammConfig.getInstance();
        int rightBarWidth = PADDING + IconType.SIZE + ITEM_GAP + IconType.SIZE + PADDING;
        int rightX = screenWidth - rightBarWidth - 4;
        int barY = 4;
        int iconY = barY + (BAR_HEIGHT - IconType.SIZE) / 2;

        // Sun/Moon click
        int sunMoonX = rightX + PADDING;
        if (mouseX >= sunMoonX && mouseX < sunMoonX + IconType.SIZE
                && mouseY >= iconY && mouseY < iconY + IconType.SIZE) {
            if (button == 0) {
                NammTheme.get().toggle();
                return true;
            }
        }

        // Bell click
        int bellX = sunMoonX + IconType.SIZE + ITEM_GAP;
        if (mouseX >= bellX && mouseX < bellX + IconType.SIZE
                && mouseY >= iconY && mouseY < iconY + IconType.SIZE) {
            if (button == 0) {
                // Toggle mute
                cfg.setNotificationsMuted(!cfg.isNotificationsMuted());
                cfg.save();
                return true;
            }
            if (button == 1) {
                // Right-click: open notification settings
                return true; // NammGuiScreen handles opening the overlay
            }
        }

        return false;
    }

    /** Returns true if right-click was on bell (caller should open notification settings). */
    public boolean wasRightClickOnBell(double mouseX, double mouseY, int screenWidth) {
        int rightBarWidth = PADDING + IconType.SIZE + ITEM_GAP + IconType.SIZE + PADDING;
        int rightX = screenWidth - rightBarWidth - 4;
        int barY = 4;
        int iconY = barY + (BAR_HEIGHT - IconType.SIZE) / 2;
        int bellX = rightX + PADDING + IconType.SIZE + ITEM_GAP;
        return mouseX >= bellX && mouseX < bellX + IconType.SIZE
                && mouseY >= iconY && mouseY < iconY + IconType.SIZE;
    }

    private int getPing(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) return 0;
        UUID uuid = mc.player.getUUID();
        PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
        return info != null ? info.getLatency() : 0;
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew build
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/namm/ui/InfoBar.java
git commit -m "feat: add InfoBar HUD with status, theme toggle, and bell"
```

---

## Task 8: Notification Settings Screen

**Files:**
- Create: `src/main/java/com/namm/ui/NotificationSettingsScreen.java`

- [ ] **Step 1: Create NotificationSettingsScreen.java**

A modal overlay rendered within `NammGuiScreen`, not a standalone `Screen`. Renders a themed panel with toggle rows. Input is intercepted — click outside closes it.

```java
package com.namm.ui;

import com.namm.config.NammConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Modal overlay for notification settings.
 * Rendered on top of NammGuiScreen when showingNotificationSettings is true.
 */
public class NotificationSettingsScreen {
    private static final int PANEL_WIDTH = 200;
    private static final int ROW_HEIGHT = 18;
    private static final int PADDING = 6;

    private int panelX, panelY, panelHeight;

    // Toggle labels and their config mappings
    private static final String[] LABELS = {
        "Info Bar Visibility",
        "Macro Toggled",
        "Chat Command Executed",
        "Profile Switched",
        "Import/Export",
        "Errors"
    };

    public void render(GuiGraphics g, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        NammTheme t = NammTheme.get();
        NammConfig cfg = NammConfig.getInstance();

        // Scrim
        g.fill(0, 0, screenWidth, screenHeight, t.scrim());

        // Center the panel
        panelHeight = NammWindow.HEADER_HEIGHT + (LABELS.length * ROW_HEIGHT) + PADDING * 2;
        panelX = (screenWidth - PANEL_WIDTH) / 2;
        panelY = (screenHeight - panelHeight) / 2;

        // Panel
        NammRenderer.drawPanel(g, panelX, panelY, PANEL_WIDTH, panelHeight);
        NammRenderer.drawHeader(g, panelX, panelY, PANEL_WIDTH, NammWindow.HEADER_HEIGHT, "Notification Settings", false);

        int cy = panelY + NammWindow.HEADER_HEIGHT + PADDING;

        for (int i = 0; i < LABELS.length; i++) {
            int rowY = cy + i * ROW_HEIGHT;
            boolean hovered = mouseX >= panelX + PADDING && mouseX < panelX + PANEL_WIDTH - PADDING
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            NammRenderer.drawRow(g, panelX, rowY, PANEL_WIDTH, ROW_HEIGHT, hovered);
            NammRenderer.drawText(g, panelX + PADDING, rowY + 5, LABELS[i], true);

            // Toggle state
            boolean on = getToggleState(cfg, i);
            String stateStr = i == 0 ? (on ? "Always" : "Menu") : (on ? "ON" : "OFF");
            int stateColor = on ? t.toggleOn() : t.toggleOff();
            int sw = Minecraft.getInstance().font.width(stateStr);
            NammRenderer.drawTextColored(g, panelX + PANEL_WIDTH - PADDING - sw, rowY + 5, stateStr, stateColor);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Click outside panel closes it
        if (mouseX < panelX || mouseX >= panelX + PANEL_WIDTH
                || mouseY < panelY || mouseY >= panelY + panelHeight) {
            return false; // Signal to close
        }

        if (button != 0) return true;

        NammConfig cfg = NammConfig.getInstance();
        int cy = panelY + NammWindow.HEADER_HEIGHT + PADDING;

        for (int i = 0; i < LABELS.length; i++) {
            int rowY = cy + i * ROW_HEIGHT;
            if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                toggleState(cfg, i);
                cfg.save();
                return true;
            }
        }

        return true;
    }

    private boolean getToggleState(NammConfig cfg, int index) {
        return switch (index) {
            case 0 -> "always".equals(cfg.getInfoBarVisibility());
            case 1 -> cfg.isNotifMacroToggled();
            case 2 -> cfg.isNotifChatCommand();
            case 3 -> cfg.isNotifProfileSwitched();
            case 4 -> cfg.isNotifImportExport();
            case 5 -> cfg.isNotifErrors();
            default -> false;
        };
    }

    private void toggleState(NammConfig cfg, int index) {
        switch (index) {
            case 0 -> cfg.setInfoBarVisibility(
                "always".equals(cfg.getInfoBarVisibility()) ? "menu_only" : "always");
            case 1 -> cfg.setNotifMacroToggled(!cfg.isNotifMacroToggled());
            case 2 -> cfg.setNotifChatCommand(!cfg.isNotifChatCommand());
            case 3 -> cfg.setNotifProfileSwitched(!cfg.isNotifProfileSwitched());
            case 4 -> cfg.setNotifImportExport(!cfg.isNotifImportExport());
            case 5 -> cfg.setNotifErrors(!cfg.isNotifErrors());
        }
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew build
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/namm/ui/NotificationSettingsScreen.java
git commit -m "feat: add NotificationSettingsScreen modal overlay"
```

---

## Task 9: Window Content Renderers

**Files:**
- Create: `src/main/java/com/namm/ui/windows/MacroWindowRenderer.java`
- Create: `src/main/java/com/namm/ui/windows/ProfileWindowRenderer.java`
- Create: `src/main/java/com/namm/ui/windows/EditorWindowRenderer.java`
- Create: `src/main/java/com/namm/ui/windows/ChatCommandWindowRenderer.java`

This is the largest task — extracting the 4 window content renderers from `NammGuiScreen.java`. Each renderer takes the rendering and click handling logic for its window from the current monolith.

**Important context for the implementer:** The current `NammGuiScreen.java` contains all rendering and interaction logic inline. Read the entire file (`src/main/java/com/namm/config/NammGuiScreen.java`, 2029 lines) before starting this task. The key methods to extract per window are:

- **MacroWindowRenderer:** `renderMacroWindow()` (lines 246-324), macro click handling from `mouseClicked()`, right-click context menu logic for macros
- **ProfileWindowRenderer:** `renderProfileWindow()` (lines 348-472), profile click handling, expand/collapse profiles, profile creation flow
- **EditorWindowRenderer:** `renderEditorWindow()` (lines 487-600+), all editor state (editorNameBox, editorDelayBox, editorRecordingIndex, editorAddPopup), step drag-to-reorder, key capture forwarding
- **ChatCommandWindowRenderer:** `renderChatWindow()` method, chat command editor state (chatNameBox, chatMessageBox), right-click context menu for chat commands

Each renderer implements `WindowContent`. Replace all hardcoded colors (e.g., `WINDOW_BG`, `HEADER_BG`, `TEXT_PRIMARY`) with `NammRenderer` and `NammTheme` calls. Replace `this.font` with `Minecraft.getInstance().font`. Replace `drawRoundedRect`/`drawRoundedRectTop`/`drawRoundedRectBottom` with `NammRenderer.drawPanel`/`drawPanelBottom`.

- [ ] **Step 0: Create callback interface for window-to-screen communication**

Content renderers need to communicate events back to `NammGuiScreen` (e.g., "open editor for this macro", "show context menu at x,y"). Create a callback interface that `NammGuiScreen` implements:

```java
package com.namm.ui.windows;

import com.namm.model.ChatCommand;
import com.namm.model.Macro;

/**
 * Callback interface for window content renderers to communicate with NammGuiScreen.
 * NammGuiScreen implements this and passes itself to each renderer's constructor.
 */
public interface WindowCallback {
    void editMacro(Macro macro);
    void closeMacroEditor();
    void showContextMenu(int index, int x, int y, ContextMenuType type);
    void addNewMacro();
    void addNewProfile();
    void editChatCommand(ChatCommand cmd);
    void profileSwitched(String profileName);

    enum ContextMenuType { MACRO, PROFILE, CHAT_COMMAND }
}
```

Context menu rendering and state (`contextMenuIndex`, `contextMenuX/Y`, type flags) remain in `NammGuiScreen`. When a renderer calls `showContextMenu(...)`, `NammGuiScreen` stores the state and renders the menu on top of everything. The context menu click handling also stays in `NammGuiScreen` since it may trigger cross-window actions (e.g., deleting a macro closes the editor).

- [ ] **Step 1: Create MacroWindowRenderer.java**

Extract macro list rendering logic from `NammGuiScreen.renderMacroWindow()` (lines 246-324) and macro click handling from `mouseClicked()`. The renderer needs access to `NammConfig` for the macro list and active profile, and uses `WindowCallback` for cross-window actions.

```java
package com.namm.ui.windows;

import com.namm.config.NammConfig;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import com.namm.ui.*;
import com.namm.util.KeyNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class MacroWindowRenderer implements WindowContent {
    private final WindowCallback callback;

    public MacroWindowRenderer(WindowCallback callback) {
        this.callback = callback;
    }

    @Override
    public int getContentHeight() {
        List<Macro> macros = NammConfig.getInstance().getMacros();
        return (macros.size() + 1) * NammWindow.ROW_HEIGHT + 4; // +1 for "+ New Macro"
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int mouseX, int mouseY, float delta) {
        // Extract from NammGuiScreen.renderMacroWindow() lines 269-323
        // Key changes:
        // - Replace all color constants (TEXT_PRIMARY, TOGGLE_ON, etc.) with NammRenderer/NammTheme calls
        // - Replace this.font with Minecraft.getInstance().font
        // - Replace g.fill hover/toggle/separator with NammRenderer.drawRow/drawToggleIndicator/drawSeparator
        // - Replace g.drawString with NammRenderer.drawText/drawTextRight
        // - y parameter is already scroll-adjusted (NammWindow handles scroll offset)

        List<Macro> macros = NammConfig.getInstance().getMacros();
        MacroProfile activeProfile = NammConfig.getInstance().getActiveProfile();

        for (int i = 0; i < macros.size(); i++) {
            Macro macro = macros.get(i);
            int rowY = y + (i * NammWindow.ROW_HEIGHT);
            boolean isOn = activeProfile != null ? activeProfile.isMacroActive(macro.getName()) : macro.isEnabled();

            // Hover
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + NammWindow.ROW_HEIGHT;
            NammRenderer.drawRow(g, x, rowY, width, NammWindow.ROW_HEIGHT, hovered);

            // Toggle indicator
            NammRenderer.drawToggleIndicator(g, x + 3, rowY + 3, NammWindow.ROW_HEIGHT - 6, isOn);

            // Macro name (truncated if too long)
            String name = truncate(macro.getName(), width - 48);
            NammRenderer.drawText(g, x + 10, rowY + 4, name, isOn);

            // Trigger key (right-aligned)
            String triggerName = macro.getTriggerKeyCode() == -1 ? ""
                    : KeyNames.getKeyName(macro.getTriggerKeyCode(), macro.isTriggerMouse());
            if (!triggerName.isEmpty()) {
                NammRenderer.drawTextRight(g, x + width - 5, rowY + 4, triggerName, false);
            }

            // Separator
            if (i < macros.size() - 1) {
                NammRenderer.drawSeparator(g, x + 8, rowY + NammWindow.ROW_HEIGHT - 1, width - 16);
            }
        }

        // "+ New Macro" row
        int newY = y + (macros.size() * NammWindow.ROW_HEIGHT);
        boolean hoverNew = mouseX >= x && mouseX < x + width && mouseY >= newY && mouseY < newY + NammWindow.ROW_HEIGHT;
        NammRenderer.drawRow(g, x, newY, width, NammWindow.ROW_HEIGHT, hoverNew);
        NammRenderer.drawTextCentered(g, x + width / 2, newY + 4, "+ New Macro", false);
    }

    @Override
    public boolean mouseClicked(int x, int y, int button) {
        // Left-click on a macro row: toggle enabled (or open editor if already selected)
        // Right-click: show context menu via callback.showContextMenu(...)
        // Click on "+ New Macro": callback.addNewMacro()
        // Port logic from NammGuiScreen's mouseClicked macro handling section
        return false;
    }

    // ... remaining WindowContent methods (no-op for mouseReleased, mouseDragged, etc.)

    @Override
    public void onCollapseChanged(Screen parentScreen, boolean collapsed) {
        // No EditBoxes to manage
    }

    private String truncate(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        while (font.width(text + "..") > maxWidth && text.length() > 1)
            text = text.substring(0, text.length() - 1);
        return text + "..";
    }
}
```

The implementer must read the full `NammGuiScreen.java` to extract the click handling correctly. The macro window click logic is scattered in `mouseClicked()` — search for `macroWinX` references.

- [ ] **Step 2: Create ProfileWindowRenderer.java**

Extract profile list rendering. This is the most complex window — it has profile expand/collapse, macro checkboxes within expanded profiles, profile creation with an EditBox, and context menus.

The renderer owns the `expandedProfiles` set, the `creatingProfile` flag, and the `profileNameBox` EditBox. It implements `onCollapseChanged` to manage the EditBox lifecycle.

- [ ] **Step 3: Create EditorWindowRenderer.java**

Extract the step editor. This renderer owns:
- `editingMacro` reference
- `editorNameBox`, `editorDelayBox` EditBoxes
- `editorRecordingIndex`, `editorDelayIndex` state
- `editorAddPopup` flag
- Step drag-to-reorder logic

It must implement `charTyped` and `keyPressed` to forward to EditBoxes and handle key capture for step recording. Implement `onCollapseChanged` to add/remove EditBoxes.

- [ ] **Step 4: Create ChatCommandWindowRenderer.java**

Extract chat command list rendering. This renderer owns:
- `editingChatCommand` reference
- `chatNameBox`, `chatMessageBox` EditBoxes
- Chat command context menu logic

Implement `onCollapseChanged` for EditBox lifecycle.

- [ ] **Step 5: Verify build compiles**

```bash
./gradlew build
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/namm/ui/windows/
git commit -m "feat: extract 4 window content renderers from NammGuiScreen"
```

---

## Task 10: Rewrite NammGuiScreen as Orchestrator

**Files:**
- Modify: `src/main/java/com/namm/config/NammGuiScreen.java`

Gut the current 2029-line file and rewrite it as a slim orchestrator (~400-500 lines) that:

1. Creates 4 `NammWindow` instances with their content renderers
2. Creates and renders the `InfoBar`
3. Manages the `NotificationSettingsScreen` overlay
4. Dispatches all input events to the correct window
5. Handles context menus (rendered on top of everything)
6. Handles import/export buttons
7. Saves window positions on close

- [ ] **Step 1: Rewrite NammGuiScreen.java**

The new structure:

```java
public class NammGuiScreen extends Screen {
    private final Screen parent;

    private NammWindow macroWindow, profileWindow, editorWindow, chatWindow;
    private NammWindow[] windows; // for iteration

    private MacroWindowRenderer macroRenderer;
    private ProfileWindowRenderer profileRenderer;
    private EditorWindowRenderer editorRenderer;
    private ChatCommandWindowRenderer chatRenderer;

    private boolean showingNotificationSettings = false;
    private NotificationSettingsScreen notificationSettings;

    // Context menu state (shared across windows)
    private int contextMenuIndex = -1;
    private int contextMenuX, contextMenuY;
    // ... context menu type flags

    @Override
    protected void init() {
        // Create windows with persisted positions
        // Create content renderers and assign to windows
        // Create notification settings overlay
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // 1. Screen overlay
        g.fill(0, 0, width, height, NammTheme.get().screenOverlay());

        // 2. Render all windows
        for (NammWindow w : windows) w.render(g, mouseX, mouseY, delta);

        // 3. Info bar
        InfoBar.get().render(g, width, height);

        // 4. Import/export buttons
        renderImportExport(g, mouseX, mouseY);

        // 5. Context menu (on top)
        if (contextMenuIndex >= 0) renderContextMenu(g, mouseX, mouseY);

        // 6. Notification settings overlay (on top of everything)
        if (showingNotificationSettings) {
            notificationSettings.render(g, width, height, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. Notification settings intercept
        if (showingNotificationSettings) {
            if (!notificationSettings.mouseClicked(mouseX, mouseY, button)) {
                showingNotificationSettings = false;
            }
            return true;
        }

        // 2. Info bar
        if (InfoBar.get().mouseClicked(mouseX, mouseY, button, width)) {
            if (button == 1 && InfoBar.get().wasRightClickOnBell(mouseX, mouseY, width)) {
                showingNotificationSettings = true;
            }
            return true;
        }

        // 3. Context menu
        // 4. Windows (iterate, dispatch to first that accepts)
        for (NammWindow w : windows) {
            if (w.isMouseOver(mouseX, mouseY)) {
                return w.mouseClicked(mouseX, mouseY, button);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // Similar dispatch for mouseReleased, mouseDragged, mouseScrolled, keyPressed, charTyped

    @Override
    public void onClose() {
        // Save window positions
        NammConfig cfg = NammConfig.getInstance();
        cfg.setMacroWinPos(macroWindow.getX(), macroWindow.getY());
        // ... other windows
        cfg.save();
        minecraft.setScreen(parent);
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew build
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/namm/config/NammGuiScreen.java
git commit -m "refactor: rewrite NammGuiScreen as slim orchestrator using NammWindow"
```

---

## Task 11: Register Keybind, HUD Callback, and Toast Events

**Files:**
- Modify: `src/main/java/com/namm/NammMod.java`
- Modify: `src/main/java/com/namm/input/TriggerKeyHandler.java`
- Modify: `src/main/java/com/namm/executor/MacroPlaybackState.java`
- Modify: `src/main/resources/assets/namm/lang/en_us.json`

- [ ] **Step 1: Update NammMod.java**

Replace raw GLFW keybind with registered `KeyMapping`. Register `HudRenderCallback`. Initialize singletons.

```java
// Replace the GLFW polling block with:
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;

// In onInitializeClient():
KeyMapping openMenu = KeyBindingHelper.registerKeyBinding(
    new KeyMapping("key.namm.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.namm")
);

// Replace the tick handler:
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (client.screen == null && client.player != null) {
        while (openMenu.consumeClick()) {
            client.setScreen(new NammGuiScreen(null));
        }
    }
});

// Register HUD callback:
// Note: In Fabric API for MC 1.21.x, HudRenderCallback signature is (GuiGraphics, DeltaTracker)
HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
    Minecraft mc = Minecraft.getInstance();
    int w = mc.getWindow().getGuiScaledWidth();
    int h = mc.getWindow().getGuiScaledHeight();

    // Always render toasts
    ToastManager.get().render(graphics, w, h);

    // Render info bar if always-visible and not in NAMM screen
    if (InfoBar.get().isAlwaysVisible() && !(mc.screen instanceof NammGuiScreen)) {
        InfoBar.get().render(graphics, w, h);
    }
});
```

Remove the `rightShiftWasPressed` field — no longer needed.

- [ ] **Step 2: Add toast events to TriggerKeyHandler**

After successful chat command execution in `TriggerKeyHandler.onClientTick()`, post toast:

```java
// After successful chat command execution (inside the if block that sends the message):
ToastManager.get().post("Sent " + message, ToastManager.ToastType.SUCCESS, ToastManager.Category.CHAT_COMMAND);
```

- [ ] **Step 3: Add toast events to MacroPlaybackState**

In `startMacro()` and `stopMacro()`, post toast:

```java
ToastManager.get().post(macro.getName() + " enabled", ToastManager.ToastType.SUCCESS, ToastManager.Category.MACRO_TOGGLED);
ToastManager.get().post(name + " disabled", ToastManager.ToastType.INFO, ToastManager.Category.MACRO_TOGGLED);
```

- [ ] **Step 4: Add toast events for profile switching and import/export**

Profile switching happens in `ProfileWindowRenderer` (or the `WindowCallback.profileSwitched()` callback). In `NammGuiScreen` when the active profile changes:

```java
ToastManager.get().post("Switched to " + profileName, ToastManager.ToastType.INFO, ToastManager.Category.PROFILE_SWITCHED);
```

Import/export happens in `NammGuiScreen`'s import/export handling. After successful import:

```java
ToastManager.get().post("Imported " + count + " macros", ToastManager.ToastType.SUCCESS, ToastManager.Category.IMPORT_EXPORT);
```

After successful export:

```java
ToastManager.get().post("Exported " + count + " macros", ToastManager.ToastType.SUCCESS, ToastManager.Category.IMPORT_EXPORT);
```

On error:

```java
ToastManager.get().post("Import failed", ToastManager.ToastType.ERROR, ToastManager.Category.ERROR);
```

- [ ] **Step 5: Update lang file**

Add to `en_us.json`:

```json
"key.categories.namm": "NAMM",
"key.namm.open_menu": "Open NAMM Menu",
"namm.infobar.no_profile": "None",
"namm.infobar.active_macros": "%d active",
"namm.infobar.ping_na": "N/A",
"namm.notifications.title": "Notification Settings",
"namm.notifications.infobar_visibility": "Info Bar Visibility",
"namm.notifications.menu_only": "Menu Only",
"namm.notifications.always_visible": "Always Visible",
"namm.notifications.category.macro_toggled": "Macro Toggled",
"namm.notifications.category.chat_command": "Chat Command Executed",
"namm.notifications.category.profile_switched": "Profile Switched",
"namm.notifications.category.import_export": "Import/Export",
"namm.notifications.category.errors": "Errors"
```

- [ ] **Step 6: Verify build compiles**

```bash
./gradlew build
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/namm/NammMod.java src/main/java/com/namm/input/TriggerKeyHandler.java src/main/java/com/namm/executor/MacroPlaybackState.java src/main/resources/assets/namm/lang/en_us.json
git commit -m "feat: register keybind, HUD callback, and toast events"
```

---

## Task 12: Post-Implementation Updates

**Files:**
- Modify: `CLAUDE.md`
- Modify: `gradle.properties`

- [ ] **Step 1: Update CLAUDE.md with final architecture**

Update the Project Structure and Key Design Decisions sections to reflect the completed refactoring. Include the `ui/` and `ui/windows/` packages with their actual contents.

- [ ] **Step 2: Bump version**

In `gradle.properties`, change `mod_version=1.5.0` to `mod_version=2.0.0`.

- [ ] **Step 3: Final build verification**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL, output JAR at `build/libs/namm-2.0.0.jar`.

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md gradle.properties
git commit -m "chore: bump version to 2.0.0, update CLAUDE.md with final architecture"
```

---

## Task 13: Manual In-Game Testing

**No files modified — verification only.**

Launch the game and test all features:

- [ ] **Step 1: Launch client**

```bash
./gradlew runClient
```

- [ ] **Step 2: Test checklist**

1. Press Right Shift — NAMM menu opens with themed panels
2. Verify dark theme colors match jbdsgn night screenshot
3. Click sun/moon icon — theme toggles to light mode
4. Verify light theme colors match jbdsgn day screenshot
5. Drag each window — positions persist when closing/reopening menu
6. Collapse/expand each window — content hides/shows correctly
7. Create a macro — editor window works, steps can be added
8. Toggle macro enabled/disabled — toast notification appears
9. Create a profile — profile window works
10. Switch profiles — toast notification appears
11. Create a chat command — chat command window works
12. Left-click bell — notifications mute/unmute
13. Right-click bell — notification settings overlay opens
14. Toggle info bar to "Always visible" — bar shows when menu is closed
15. Close menu, verify info bar shows profile, macro count, ping, clock
16. Go to Options → Controls → find "NAMM" category → rebind key → verify it works
17. Import/export macros — toast notification appears
18. Close game, reopen — verify config persists (theme, notifications, positions)
