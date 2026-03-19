package com.namm.ui;

import com.namm.config.NammConfig;

/**
 * Color palette matched to jbdsgn "Your Client" screenshots.
 * Flat, minimal, neutral grays with very subtle borders.
 * Always dark mode.
 */
public class NammTheme {
    private static final NammTheme INSTANCE = new NammTheme();
    public static NammTheme get() { return INSTANCE; }

    // Panel backgrounds — semi-transparent, flat
    public int panelBg() { return 0xD91C1C1E; }
    public int headerBg() { return 0xEB202022; }

    // Text — neutral grays, no accent tints
    public int textPrimary() { return 0xFFD2D2D7; }
    public int textSecondary() { return 0xFF78787D; }

    // Accent — configurable color
    public int accent() {
        return switch (NammConfig.getInstance().getAccentColor()) {
            case "blue" -> 0xFF5588D4;
            case "green" -> 0xFF55B87A;
            case "red" -> 0xFFD45555;
            case "orange" -> 0xFFD4A055;
            case "white" -> 0xFFD2D2D7;
            default -> 0xFF7C6FE0; // purple
        };
    }

    // Interaction
    public int hover() { return 0x14FFFFFF; }
    public int border() { return 0x0FFFFFFF; }

    // Toggles
    public int toggleOn() { return 0xFFD2D2D7; }
    public int toggleOff() { return 0xFF3A3A3E; }

    // Separators (header/content divider)
    public int separator() { return 0x0CFFFFFF; }

    // Status colors
    public int destructive() { return 0xFFD45555; }
    public int toastSuccess() { return 0xFF55B87A; }
    public int toastError() { return 0xFFD45555; }
    public int toastInfo() { return 0xFF5588D4; }

    // Overlays — very subtle, game world should show through
    public int screenOverlay() { return 0x40000000; }
    public int scrim() { return 0x60000000; }
}
