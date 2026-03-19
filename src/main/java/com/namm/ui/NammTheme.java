package com.namm.ui;

import com.namm.config.NammConfig;

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

    public int panelBg() { return isDark() ? 0xD9181820 : 0xD9FFFFFF; }
    public int headerBg() { return isDark() ? 0xF21E1E26 : 0xF2F5F5F8; }
    public int textPrimary() { return isDark() ? 0xFFE0E0E8 : 0xFF1A1A2E; }
    public int textSecondary() { return isDark() ? 0xFF6E6E78 : 0xFF8888A0; }
    public int accent() { return 0xFF7C6FE0; }
    public int hover() { return isDark() ? 0x1FFFFFFF : 0x0F000000; }
    public int border() { return isDark() ? 0x0FFFFFFF : 0x14000000; }
    public int toggleOn() { return 0xFF7C6FE0; }
    public int toggleOff() { return isDark() ? 0xFF3A3A42 : 0xFFC8C8D0; }
    public int separator() { return isDark() ? 0x12FFFFFF : 0x0F000000; }
    public int destructive() { return 0xFFD45555; }
    public int toastSuccess() { return 0xFF55B87A; }
    public int toastError() { return 0xFFD45555; }
    public int toastInfo() { return 0xFF5588D4; }
    public int screenOverlay() { return isDark() ? 0x90000000 : 0x60000000; }
    public int scrim() { return 0x80000000; }
}
