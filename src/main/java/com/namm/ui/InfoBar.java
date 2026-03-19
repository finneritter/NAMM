package com.namm.ui;

import com.namm.config.NammConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Top info bar matching jbdsgn "Your Client" screenshots.
 * Single pill: NAMM · [head] playerName · profile · HH:mm
 */
public class InfoBar {
    private static final InfoBar INSTANCE = new InfoBar();
    public static InfoBar get() { return INSTANCE; }

    private static final int BAR_HEIGHT = 16;
    private static final int PADDING = 4;
    private static final int ITEM_GAP = 6;
    private static final int HEAD_SIZE = 8;
    private static final int DOT_GAP = 6;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public boolean isAlwaysVisible() {
        return "always".equals(NammConfig.getInstance().getInfoBarVisibility());
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        NammConfig cfg = NammConfig.getInstance();
        NammTheme t = NammTheme.get();
        Minecraft mc = Minecraft.getInstance();

        String playerName = mc.player != null ? mc.player.getName().getString() : "Player";
        String profileName = cfg.getActiveProfileName() != null ? cfg.getActiveProfileName() : "None";
        String timeStr = LocalTime.now().format(TIME_FMT);

        // Calculate left pill width: NAMM · [head] name · profile · time
        int nammW = NammRenderer.fontWidth("NAMM");
        int nameW = HEAD_SIZE + 3 + NammRenderer.fontWidth(playerName);
        int profileW = NammRenderer.fontWidth(profileName);
        int timeW = NammRenderer.fontWidth(timeStr);
        int dotW = 2; // dot separator width
        int leftBarWidth = PADDING + nammW + DOT_GAP + dotW + DOT_GAP
                + nameW + DOT_GAP + dotW + DOT_GAP
                + profileW + DOT_GAP + dotW + DOT_GAP
                + timeW + PADDING;

        int leftX = 4, barY = 4;
        NammRenderer.drawPanel(g, leftX, barY, leftBarWidth, BAR_HEIGHT);

        int cx = leftX + PADDING;
        int textY = barY + (BAR_HEIGHT - 8) / 2;
        int dotY = barY + (BAR_HEIGHT - 2) / 2;

        // NAMM label
        NammRenderer.drawTextAccent(g, cx, textY, "NAMM");
        cx += nammW + DOT_GAP;

        // · separator
        NammRenderer.drawDotSeparator(g, cx, dotY);
        cx += dotW + DOT_GAP;

        // Player head + name
        if (mc.player != null) {
            renderPlayerHead(g, mc, cx, barY + (BAR_HEIGHT - HEAD_SIZE) / 2, HEAD_SIZE);
        }
        cx += HEAD_SIZE + 3;
        NammRenderer.drawText(g, cx, textY, playerName, false);
        cx += NammRenderer.fontWidth(playerName) + DOT_GAP;

        // · separator
        NammRenderer.drawDotSeparator(g, cx, dotY);
        cx += dotW + DOT_GAP;

        // Profile
        NammRenderer.drawText(g, cx, textY, profileName, false);
        cx += profileW + DOT_GAP;

        // · separator
        NammRenderer.drawDotSeparator(g, cx, dotY);
        cx += dotW + DOT_GAP;

        // Time
        NammRenderer.drawText(g, cx, textY, timeStr, false);
    }

    private void renderPlayerHead(GuiGraphics g, Minecraft mc, int x, int y, int size) {
        if (mc.player == null) return;
        Identifier skinTexture = mc.player.getSkin().body().texturePath();
        g.blit(RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 8.0f, 8.0f, size, size, 8, 8, 64, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 40.0f, 8.0f, size, size, 8, 8, 64, 64);
    }
}
