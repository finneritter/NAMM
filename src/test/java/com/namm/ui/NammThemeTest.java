package com.namm.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NammTheme color constants.
 *
 * The accent() method depends on NammConfig (which requires FabricLoader)
 * and cannot be tested in a unit test context. All other methods return
 * fixed color values and are fully testable.
 */
class NammThemeTest {

    private final NammTheme theme = NammTheme.get();

    // --- Singleton ---

    @Test
    void get_returnsSameInstance() {
        assertSame(NammTheme.get(), NammTheme.get());
    }

    // --- Panel backgrounds ---

    @Test
    void panelBg_returnsSemiTransparentDark() {
        int color = theme.panelBg();
        assertEquals(0xD91C1C1E, color);
        // Alpha should be 0xD9 (217/255 ~ 85% opaque)
        int alpha = (color >>> 24) & 0xFF;
        assertEquals(0xD9, alpha);
    }

    @Test
    void headerBg_returnsHigherOpacityThanPanel() {
        int headerAlpha = (theme.headerBg() >>> 24) & 0xFF;
        int panelAlpha = (theme.panelBg() >>> 24) & 0xFF;
        assertTrue(headerAlpha > panelAlpha,
            "Header should be more opaque than panel background");
    }

    @Test
    void headerBg_returnsExpectedValue() {
        assertEquals(0xEB202022, theme.headerBg());
    }

    // --- Text colors ---

    @Test
    void textPrimary_isFullyOpaque() {
        int alpha = (theme.textPrimary() >>> 24) & 0xFF;
        assertEquals(0xFF, alpha, "Primary text should be fully opaque");
    }

    @Test
    void textPrimary_returnsExpectedValue() {
        assertEquals(0xFFD2D2D7, theme.textPrimary());
    }

    @Test
    void textSecondary_isFullyOpaque() {
        int alpha = (theme.textSecondary() >>> 24) & 0xFF;
        assertEquals(0xFF, alpha, "Secondary text should be fully opaque");
    }

    @Test
    void textSecondary_returnsExpectedValue() {
        assertEquals(0xFF78787D, theme.textSecondary());
    }

    @Test
    void textSecondary_isDimmerThanPrimary() {
        // Secondary text should have lower RGB brightness
        int primaryR = (theme.textPrimary() >> 16) & 0xFF;
        int secondaryR = (theme.textSecondary() >> 16) & 0xFF;
        assertTrue(secondaryR < primaryR,
            "Secondary text should be dimmer than primary text");
    }

    // --- Interaction colors ---

    @Test
    void hover_isLowAlphaWhite() {
        int color = theme.hover();
        int alpha = (color >>> 24) & 0xFF;
        assertEquals(0x14, alpha, "Hover should be very subtle (alpha 0x14)");
        assertEquals(0x00FFFFFF, color & 0x00FFFFFF, "Hover RGB should be white");
    }

    @Test
    void border_isLowAlphaWhite() {
        int color = theme.border();
        int alpha = (color >>> 24) & 0xFF;
        assertEquals(0x0F, alpha, "Border should be very subtle (alpha 0x0F)");
        assertEquals(0x00FFFFFF, color & 0x00FFFFFF, "Border RGB should be white");
    }

    // --- Toggle colors ---

    @Test
    void toggleOn_matchesPrimaryText() {
        assertEquals(theme.textPrimary(), theme.toggleOn(),
            "Toggle ON should use primary text color for visibility");
    }

    @Test
    void toggleOff_returnsExpectedValue() {
        assertEquals(0xFF3A3A3E, theme.toggleOff());
    }

    @Test
    void toggleOff_isDarkerThanToggleOn() {
        int onBrightness = (theme.toggleOn() >> 16) & 0xFF;
        int offBrightness = (theme.toggleOff() >> 16) & 0xFF;
        assertTrue(offBrightness < onBrightness,
            "Toggle OFF should be darker than toggle ON");
    }

    // --- Separator ---

    @Test
    void separator_isVerySubtleWhite() {
        int color = theme.separator();
        int alpha = (color >>> 24) & 0xFF;
        assertEquals(0x0C, alpha, "Separator should be extremely subtle");
        assertEquals(0x00FFFFFF, color & 0x00FFFFFF);
    }

    // --- Status colors ---

    @Test
    void destructive_returnsRedTone() {
        assertEquals(0xFFD45555, theme.destructive());
    }

    @Test
    void toastSuccess_returnsGreenTone() {
        assertEquals(0xFF55B87A, theme.toastSuccess());
    }

    @Test
    void toastError_matchesDestructive() {
        assertEquals(theme.destructive(), theme.toastError(),
            "Toast error color should match destructive color for consistency");
    }

    @Test
    void toastInfo_returnsBlueTone() {
        assertEquals(0xFF5588D4, theme.toastInfo());
    }

    @Test
    void statusColors_areAllFullyOpaque() {
        assertAll(
            () -> assertEquals(0xFF, (theme.destructive() >>> 24) & 0xFF, "destructive"),
            () -> assertEquals(0xFF, (theme.toastSuccess() >>> 24) & 0xFF, "toastSuccess"),
            () -> assertEquals(0xFF, (theme.toastError() >>> 24) & 0xFF, "toastError"),
            () -> assertEquals(0xFF, (theme.toastInfo() >>> 24) & 0xFF, "toastInfo")
        );
    }

    // --- Overlay colors ---

    @Test
    void screenOverlay_isSemiTransparentBlack() {
        int color = theme.screenOverlay();
        assertEquals(0x40000000, color);
        int alpha = (color >>> 24) & 0xFF;
        assertEquals(0x40, alpha, "Screen overlay should be ~25% opaque");
        assertEquals(0x000000, color & 0x00FFFFFF, "Overlay RGB should be black");
    }

    @Test
    void scrim_isMoreOpaqueThanOverlay() {
        int scrimAlpha = (theme.scrim() >>> 24) & 0xFF;
        int overlayAlpha = (theme.screenOverlay() >>> 24) & 0xFF;
        assertTrue(scrimAlpha > overlayAlpha,
            "Scrim should be more opaque than screen overlay");
    }

    @Test
    void scrim_returnsExpectedValue() {
        assertEquals(0x60000000, theme.scrim());
    }

    // --- All colors are non-zero ---

    @Test
    void allColorMethods_returnNonZeroValues() {
        assertAll(
            () -> assertNotEquals(0, theme.panelBg()),
            () -> assertNotEquals(0, theme.headerBg()),
            () -> assertNotEquals(0, theme.textPrimary()),
            () -> assertNotEquals(0, theme.textSecondary()),
            () -> assertNotEquals(0, theme.hover()),
            () -> assertNotEquals(0, theme.border()),
            () -> assertNotEquals(0, theme.toggleOn()),
            () -> assertNotEquals(0, theme.toggleOff()),
            () -> assertNotEquals(0, theme.separator()),
            () -> assertNotEquals(0, theme.destructive()),
            () -> assertNotEquals(0, theme.toastSuccess()),
            () -> assertNotEquals(0, theme.toastError()),
            () -> assertNotEquals(0, theme.toastInfo()),
            () -> assertNotEquals(0, theme.screenOverlay()),
            () -> assertNotEquals(0, theme.scrim())
        );
    }
}
