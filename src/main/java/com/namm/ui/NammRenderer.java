package com.namm.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Static themed drawing primitives with anti-aliased edges.
 * Uses scanline rendering with sub-pixel alpha blending for smooth corners.
 * All colors come from NammTheme. Reference: jbdsgn "Your Client" screenshots.
 */
public class NammRenderer {
    private static final Identifier ICONS = Identifier.fromNamespaceAndPath("namm", "textures/gui/icons.png");
    private static final Identifier NAMM_FONT_ID = Identifier.fromNamespaceAndPath("namm", "hud");
    private static final Style NAMM_FONT_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(NAMM_FONT_ID));
    static final int RADIUS = 3;

    // --- Anti-aliased rounded rectangle primitives ---

    /** Compute exact inset (as a float) for a given row in the corner arc. */
    private static float exactInset(int r, int dy) {
        return r - (float) Math.sqrt((double) r * r - (double) (r - dy) * (r - dy));
    }

    /** Apply alpha to an ARGB color. */
    private static int withAlpha(int argb, float alpha) {
        int a = (int)(((argb >>> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Draw a single scanline row of a rounded corner with anti-aliased edge pixels.
     * The fractional part of the inset determines the edge pixel's alpha.
     */
    private static void drawAARow(GuiGraphics g, int x, int y, int w, float inset, int color) {
        int intInset = (int) inset;
        float frac = inset - intInset;

        // Anti-aliased edge pixels (left and right)
        if (frac > 0.01f) {
            int edgeColor = withAlpha(color, 1.0f - frac);
            g.fill(x + intInset, y, x + intInset + 1, y + 1, edgeColor);
            g.fill(x + w - intInset - 1, y, x + w - intInset, y + 1, edgeColor);
            // Full-opacity interior
            if (w - (intInset + 1) * 2 > 0) {
                g.fill(x + intInset + 1, y, x + w - intInset - 1, y + 1, color);
            }
        } else {
            // No fractional part, just fill the row
            if (w - intInset * 2 > 0) {
                g.fill(x + intInset, y, x + w - intInset, y + 1, color);
            }
        }
    }

    /** Filled rounded rectangle with anti-aliased edges. */
    public static void drawRoundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        // Top arc rows with AA
        for (int dy = 0; dy < r; dy++) {
            float inset = exactInset(r, dy);
            drawAARow(g, x, y + dy, w, inset, color);
        }
        // Middle band
        if (h > r * 2) {
            g.fill(x, y + r, x + w, y + h - r, color);
        }
        // Bottom arc rows with AA
        for (int dy = 0; dy < r; dy++) {
            float inset = exactInset(r, dy);
            drawAARow(g, x, y + h - 1 - dy, w, inset, color);
        }
    }

    /** Filled rounded rectangle with only top corners rounded + AA. */
    public static void drawRoundedRectTop(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h));
        for (int dy = 0; dy < r; dy++) {
            float inset = exactInset(r, dy);
            drawAARow(g, x, y + dy, w, inset, color);
        }
        if (h > r) {
            g.fill(x, y + r, x + w, y + h, color);
        }
    }

    /** Filled rounded rectangle with only bottom corners rounded + AA. */
    public static void drawRoundedRectBottom(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h));
        if (h > r) {
            g.fill(x, y, x + w, y + h - r, color);
        }
        for (int dy = 0; dy < r; dy++) {
            float inset = exactInset(r, dy);
            drawAARow(g, x, y + h - 1 - dy, w, inset, color);
        }
    }

    /** 1px anti-aliased rounded outline. */
    public static void drawRoundedOutline(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        // Top and bottom arc edge pixels with AA
        for (int dy = 0; dy < r; dy++) {
            float inset = exactInset(r, dy);
            int intInset = (int) inset;
            float frac = inset - intInset;
            // Outer edge pixel (AA)
            if (frac > 0.01f) {
                int edgeColor = withAlpha(color, 1.0f - frac);
                // Top corners
                g.fill(x + intInset, y + dy, x + intInset + 1, y + dy + 1, edgeColor);
                g.fill(x + w - intInset - 1, y + dy, x + w - intInset, y + dy + 1, edgeColor);
                // Bottom corners
                g.fill(x + intInset, y + h - 1 - dy, x + intInset + 1, y + h - dy, edgeColor);
                g.fill(x + w - intInset - 1, y + h - 1 - dy, x + w - intInset, y + h - dy, edgeColor);
                // Inner edge pixel (full alpha)
                g.fill(x + intInset + 1, y + dy, x + intInset + 2, y + dy + 1, color);
                g.fill(x + w - intInset - 2, y + dy, x + w - intInset - 1, y + dy + 1, color);
                g.fill(x + intInset + 1, y + h - 1 - dy, x + intInset + 2, y + h - dy, color);
                g.fill(x + w - intInset - 2, y + h - 1 - dy, x + w - intInset - 1, y + h - dy, color);
            } else {
                // Top corners
                g.fill(x + intInset, y + dy, x + intInset + 1, y + dy + 1, color);
                g.fill(x + w - intInset - 1, y + dy, x + w - intInset, y + dy + 1, color);
                // Bottom corners
                g.fill(x + intInset, y + h - 1 - dy, x + intInset + 1, y + h - dy, color);
                g.fill(x + w - intInset - 1, y + h - 1 - dy, x + w - intInset, y + h - dy, color);
            }
        }
        // Top and bottom straight edges
        if (w > r * 2) {
            g.fill(x + r, y, x + w - r, y + 1, color);
            g.fill(x + r, y + h - 1, x + w - r, y + h, color);
        }
        // Left and right straight edges
        if (h > r * 2) {
            g.fill(x, y + r, x + 1, y + h - r, color);
            g.fill(x + w - 1, y + r, x + w, y + h - r, color);
        }
    }

    // --- Themed panel drawing ---

    /** Semi-transparent panel with AA rounded corners and border. Flat, no shadow. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        NammTheme t = NammTheme.get();
        drawRoundedRect(g, x, y, w, h, RADIUS, t.panelBg());
        drawRoundedOutline(g, x, y, w, h, RADIUS, t.border());
    }

    /** Header bar with collapse dots and centered title. AA top corners. */
    public static void drawHeader(GuiGraphics g, int x, int y, int w, int h, String title, boolean collapsed) {
        NammTheme t = NammTheme.get();
        int bg = t.headerBg();

        if (collapsed) {
            drawRoundedRect(g, x, y, w, h, RADIUS, t.panelBg());
            drawRoundedOutline(g, x, y, w, h, RADIUS, t.border());
            drawRoundedRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(RADIUS - 1, 0), bg);
        } else {
            drawRoundedRectTop(g, x, y, w, h, RADIUS, bg);
        }

        drawIcon(g, x + 2, y + (h - IconType.SIZE) / 2, IconType.COLLAPSE_DOTS);
        drawTextCentered(g, x + w / 2, y + (h - 8) / 2, title, true);
        // Subtle separator line between header and content
        if (!collapsed) {
            g.fill(x + 1, y + h - 1, x + w - 1, y + h, t.separator());
        }
    }

    /** Bottom-only rounded panel (for window body below header). */
    public static void drawPanelBottom(GuiGraphics g, int x, int y, int w, int h) {
        NammTheme t = NammTheme.get();
        drawRoundedRectBottom(g, x, y, w, h, RADIUS, t.panelBg());
        // Side and bottom borders
        if (h > RADIUS) {
            g.fill(x, y, x + 1, y + h - RADIUS, t.border());
            g.fill(x + w - 1, y, x + w, y + h - RADIUS, t.border());
        }
        for (int dy = 0; dy < RADIUS; dy++) {
            float inset = exactInset(RADIUS, dy);
            int intInset = (int) inset;
            float frac = inset - intInset;
            if (frac > 0.01f) {
                int edgeColor = withAlpha(t.border(), 1.0f - frac);
                g.fill(x + intInset, y + h - 1 - dy, x + intInset + 1, y + h - dy, edgeColor);
                g.fill(x + w - intInset - 1, y + h - 1 - dy, x + w - intInset, y + h - dy, edgeColor);
            } else {
                g.fill(x + intInset, y + h - 1 - dy, x + intInset + 1, y + h - dy, t.border());
                g.fill(x + w - intInset - 1, y + h - 1 - dy, x + w - intInset, y + h - dy, t.border());
            }
        }
        if (w > RADIUS * 2) {
            g.fill(x + RADIUS, y + h - 1, x + w - RADIUS, y + h, t.border());
        }
    }

    // --- Simple drawing helpers ---

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

    /** Draw a filled circle approximated with AA scanlines. */
    public static void drawCircle(GuiGraphics g, int cx, int cy, int radius, int color) {
        drawRoundedRect(g, cx, cy, radius * 2, radius * 2, radius, color);
    }

    /** Draw a small dot separator "·" for info bar items. */
    public static void drawDotSeparator(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 2, y + 2, NammTheme.get().textSecondary());
    }

    // --- Text rendering (uses NAMM custom font, no shadow) ---

    /** Styled component with NAMM font applied. */
    private static Component styled(String text) {
        return Component.literal(text).withStyle(NAMM_FONT_STYLE);
    }

    /** Get text width using the NAMM font. */
    public static int fontWidth(String text) {
        return Minecraft.getInstance().font.width(styled(text));
    }

    /** Draw text in theme primary or secondary color. No shadow. */
    public static void drawText(GuiGraphics g, int x, int y, String text, boolean primary) {
        NammTheme t = NammTheme.get();
        g.drawString(Minecraft.getInstance().font, styled(text), x, y, primary ? t.textPrimary() : t.textSecondary(), false);
    }

    /** Draw right-aligned text. */
    public static void drawTextRight(GuiGraphics g, int x, int y, String text, boolean primary) {
        int w = fontWidth(text);
        drawText(g, x - w, y, text, primary);
    }

    /** Draw centered text. No shadow. */
    public static void drawTextCentered(GuiGraphics g, int centerX, int y, String text, boolean primary) {
        NammTheme t = NammTheme.get();
        Component styled = styled(text);
        int w = Minecraft.getInstance().font.width(styled);
        g.drawString(Minecraft.getInstance().font, styled, centerX - w / 2, y, primary ? t.textPrimary() : t.textSecondary(), false);
    }

    /** Draw text in accent color. No shadow. */
    public static void drawTextAccent(GuiGraphics g, int x, int y, String text) {
        g.drawString(Minecraft.getInstance().font, styled(text), x, y, NammTheme.get().accent(), false);
    }

    /** Draw text in a specific color. No shadow. */
    public static void drawTextColored(GuiGraphics g, int x, int y, String text, int color) {
        g.drawString(Minecraft.getInstance().font, styled(text), x, y, color, false);
    }

    // --- Widgets ---

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
}
