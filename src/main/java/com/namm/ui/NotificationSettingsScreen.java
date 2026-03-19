package com.namm.ui;

import com.namm.config.NammConfig;
import net.minecraft.client.gui.GuiGraphics;

public class NotificationSettingsScreen {
    private static final int PANEL_WIDTH = 200;
    private static final int ROW_HEIGHT = 18;
    private static final int PADDING = 6;

    private int panelX, panelY, panelHeight;

    // "All" toggle at index 0, then individual toggles
    private static final String[] LABELS = {
        "All",
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

        NammRenderer.drawPanel(g, panelX, panelY, PANEL_WIDTH, panelHeight);
        NammRenderer.drawHeader(g, panelX, panelY, PANEL_WIDTH, NammWindow.HEADER_HEIGHT, "Notification Settings", false);

        int cy = panelY + NammWindow.HEADER_HEIGHT + PADDING;

        for (int i = 0; i < LABELS.length; i++) {
            int rowY = cy + i * ROW_HEIGHT;
            boolean hovered = mouseX >= panelX + PADDING && mouseX < panelX + PANEL_WIDTH - PADDING
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            NammRenderer.drawRow(g, panelX, rowY, PANEL_WIDTH, ROW_HEIGHT, hovered);
            NammRenderer.drawText(g, panelX + PADDING, rowY + 5, LABELS[i], true);

            boolean on = getToggleState(cfg, i);
            String stateStr = on ? "ON" : "OFF";
            int stateColor = on ? t.toggleOn() : t.toggleOff();
            int sw = NammRenderer.fontWidth(stateStr);
            NammRenderer.drawTextColored(g, panelX + PANEL_WIDTH - PADDING - sw, rowY + 5, stateStr, stateColor);
        }
    }

    /** Returns false if click was outside panel (signal to close). Returns true if consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < panelX || mouseX >= panelX + PANEL_WIDTH
                || mouseY < panelY || mouseY >= panelY + panelHeight) {
            return false; // Outside - close signal
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
            case 0 -> allOn(cfg); // "All"
            case 1 -> cfg.isNotifMacroToggled();
            case 2 -> cfg.isNotifChatCommand();
            case 3 -> cfg.isNotifProfileSwitched();
            case 4 -> cfg.isNotifImportExport();
            case 5 -> cfg.isNotifErrors();
            default -> false;
        };
    }

    private boolean allOn(NammConfig cfg) {
        return cfg.isNotifMacroToggled()
                && cfg.isNotifChatCommand()
                && cfg.isNotifProfileSwitched()
                && cfg.isNotifImportExport()
                && cfg.isNotifErrors();
    }

    private void toggleState(NammConfig cfg, int index) {
        switch (index) {
            case 0 -> { // "All" toggle
                boolean newState = !allOn(cfg);
                cfg.setNotifMacroToggled(newState);
                cfg.setNotifChatCommand(newState);
                cfg.setNotifProfileSwitched(newState);
                cfg.setNotifImportExport(newState);
                cfg.setNotifErrors(newState);
            }
            case 1 -> cfg.setNotifMacroToggled(!cfg.isNotifMacroToggled());
            case 2 -> cfg.setNotifChatCommand(!cfg.isNotifChatCommand());
            case 3 -> cfg.setNotifProfileSwitched(!cfg.isNotifProfileSwitched());
            case 4 -> cfg.setNotifImportExport(!cfg.isNotifImportExport());
            case 5 -> cfg.setNotifErrors(!cfg.isNotifErrors());
        }
    }
}
