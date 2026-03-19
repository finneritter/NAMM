package com.namm.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Static themed drawing primitives. All colors come from NammTheme.
 * Reference: jbdsgn "Your Client" screenshots.
 */
public class NammRenderer {
    private static final Identifier ICONS = Identifier.fromNamespaceAndPath("namm", "textures/gui/icons.png");
    private static final int CORNER = 3;

    /** Semi-transparent panel with 1px border and chamfered corners. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        NammTheme t = NammTheme.get();
        int bg = t.panelBg();
        int border = t.border();

        // Fill with chamfered corners (inset by 1px at each corner)
        g.fill(x + CORNER, y, x + w - CORNER, y + CORNER, bg);      // top edge
        g.fill(x, y + CORNER, x + w, y + h - CORNER, bg);            // middle
        g.fill(x + CORNER, y + h - CORNER, x + w - CORNER, y + h, bg); // bottom edge
        g.fill(x + 1, y + 1, x + CORNER, y + CORNER, bg);
        g.fill(x + w - CORNER, y + 1, x + w - 1, y + CORNER, bg);
        g.fill(x + 1, y + h - CORNER, x + CORNER, y + h - 1, bg);
        g.fill(x + w - CORNER, y + h - CORNER, x + w - 1, y + h - 1, bg);

        // Border lines
        g.fill(x + CORNER, y, x + w - CORNER, y + 1, border);
        g.fill(x + CORNER, y + h - 1, x + w - CORNER, y + h, border);
        g.fill(x, y + CORNER, x + 1, y + h - CORNER, border);
        g.fill(x + w - 1, y + CORNER, x + w, y + h - CORNER, border);
    }

    /** Header bar with collapse dots and centered title. */
    public static void drawHeader(GuiGraphics g, int x, int y, int w, int h, String title, boolean collapsed) {
        NammTheme t = NammTheme.get();
        int bg = t.headerBg();

        if (collapsed) {
            drawPanel(g, x, y, w, h);
            g.fill(x + CORNER, y + 1, x + w - CORNER, y + h - 1, bg);
            g.fill(x + 1, y + CORNER, x + w - 1, y + h - CORNER, bg);
        } else {
            g.fill(x + CORNER, y, x + w - CORNER, y + CORNER, bg);
            g.fill(x, y + CORNER, x + w, y + h, bg);
            g.fill(x + 1, y + 1, x + CORNER, y + CORNER, bg);
            g.fill(x + w - CORNER, y + 1, x + w - 1, y + CORNER, bg);
        }

        drawIcon(g, x + 2, y + (h - IconType.SIZE) / 2, IconType.COLLAPSE_DOTS);
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
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, (float) icon.u, (float) icon.v, IconType.SIZE, IconType.SIZE, IconType.ATLAS_SIZE, IconType.ATLAS_SIZE);
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

    /** Draw text in a specific color (for non-themed elements). */
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
