package com.namm.ui;

import com.namm.config.NammConfig;
import com.namm.model.Macro;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class InfoBar {
    private static final InfoBar INSTANCE = new InfoBar();
    public static InfoBar get() { return INSTANCE; }

    private static final int BAR_HEIGHT = 20;
    private static final int PADDING = 6;
    private static final int ITEM_GAP = 12;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private int cachedPing = 0;
    private long lastPingUpdateMs = 0;

    public boolean isAlwaysVisible() {
        return "always".equals(NammConfig.getInstance().getInfoBarVisibility());
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        NammConfig cfg = NammConfig.getInstance();
        NammTheme t = NammTheme.get();
        Minecraft mc = Minecraft.getInstance();

        // Update ping cache (every 2s wall clock)
        long now = System.currentTimeMillis();
        if (now - lastPingUpdateMs > 2000) {
            lastPingUpdateMs = now;
            cachedPing = getPing(mc);
        }

        // Prepare strings
        String profileName = cfg.getActiveProfileName() != null ? cfg.getActiveProfileName() : "None";
        long activeMacroCount = cfg.getMacros().stream().filter(Macro::isEnabled).count();
        String macroCountStr = activeMacroCount + " active";
        String pingStr = cachedPing > 0 ? cachedPing + "ms" : "N/A";
        String timeStr = LocalTime.now().format(TIME_FMT);

        // Left bar
        int leftX = 4, barY = 4;
        int leftBarWidth = PADDING + mc.font.width("NAMM") + ITEM_GAP
                + mc.font.width(profileName) + ITEM_GAP
                + mc.font.width(macroCountStr) + ITEM_GAP
                + mc.font.width(pingStr) + ITEM_GAP
                + mc.font.width(timeStr) + PADDING;

        NammRenderer.drawPanel(g, leftX, barY, leftBarWidth, BAR_HEIGHT);

        int cx = leftX + PADDING;
        int textY = barY + (BAR_HEIGHT - 8) / 2;
        NammRenderer.drawTextAccent(g, cx, textY, "NAMM");
        cx += mc.font.width("NAMM") + ITEM_GAP;
        NammRenderer.drawText(g, cx, textY, profileName, true);
        cx += mc.font.width(profileName) + ITEM_GAP;
        NammRenderer.drawText(g, cx, textY, macroCountStr, false);
        cx += mc.font.width(macroCountStr) + ITEM_GAP;
        NammRenderer.drawText(g, cx, textY, pingStr, false);
        cx += mc.font.width(pingStr) + ITEM_GAP;
        NammRenderer.drawText(g, cx, textY, timeStr, false);

        // Right bar
        int rightBarWidth = PADDING + IconType.SIZE + ITEM_GAP + IconType.SIZE + PADDING;
        int rightX = screenWidth - rightBarWidth - 4;
        NammRenderer.drawPanel(g, rightX, barY, rightBarWidth, BAR_HEIGHT);
        int iconY = barY + (BAR_HEIGHT - IconType.SIZE) / 2;
        int sunMoonX = rightX + PADDING;
        NammRenderer.drawIcon(g, sunMoonX, iconY, t.isDark() ? IconType.MOON : IconType.SUN);
        int bellX = sunMoonX + IconType.SIZE + ITEM_GAP;
        NammRenderer.drawIcon(g, bellX, iconY, cfg.isNotificationsMuted() ? IconType.BELL_MUTED : IconType.BELL);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth) {
        int rightBarWidth = PADDING + IconType.SIZE + ITEM_GAP + IconType.SIZE + PADDING;
        int rightX = screenWidth - rightBarWidth - 4;
        int barY = 4;
        int iconY = barY + (BAR_HEIGHT - IconType.SIZE) / 2;

        int sunMoonX = rightX + PADDING;
        if (mouseX >= sunMoonX && mouseX < sunMoonX + IconType.SIZE
                && mouseY >= iconY && mouseY < iconY + IconType.SIZE && button == 0) {
            NammTheme.get().toggle();
            return true;
        }

        int bellX = sunMoonX + IconType.SIZE + ITEM_GAP;
        if (mouseX >= bellX && mouseX < bellX + IconType.SIZE
                && mouseY >= iconY && mouseY < iconY + IconType.SIZE) {
            if (button == 0) {
                NammConfig cfg = NammConfig.getInstance();
                cfg.setNotificationsMuted(!cfg.isNotificationsMuted());
                cfg.save();
                return true;
            }
            if (button == 1) return true; // right-click handled by caller
        }
        return false;
    }

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
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info != null ? info.getLatency() : 0;
    }
}
