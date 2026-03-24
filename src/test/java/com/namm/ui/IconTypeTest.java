package com.namm.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IconType enum.
 *
 * Verifies atlas UV coordinates, constants, and enum completeness.
 */
class IconTypeTest {

    // --- Constants ---

    @Test
    void size_is16() {
        assertEquals(16, IconType.SIZE);
    }

    @Test
    void atlasSize_is128() {
        assertEquals(128, IconType.ATLAS_SIZE);
    }

    // --- Enum values ---

    @Test
    void hasExactly11Values() {
        assertEquals(11, IconType.values().length);
    }

    @Test
    void containsAllExpectedValues() {
        assertAll(
            () -> assertNotNull(IconType.valueOf("BELL")),
            () -> assertNotNull(IconType.valueOf("BELL_MUTED")),
            () -> assertNotNull(IconType.valueOf("SUN")),
            () -> assertNotNull(IconType.valueOf("MOON")),
            () -> assertNotNull(IconType.valueOf("DOT_GREEN")),
            () -> assertNotNull(IconType.valueOf("DOT_RED")),
            () -> assertNotNull(IconType.valueOf("DOT_BLUE")),
            () -> assertNotNull(IconType.valueOf("TOGGLE_ON")),
            () -> assertNotNull(IconType.valueOf("TOGGLE_OFF")),
            () -> assertNotNull(IconType.valueOf("COLLAPSE_DOTS")),
            () -> assertNotNull(IconType.valueOf("GEAR"))
        );
    }

    // --- UV coordinate calculation ---

    @Test
    void bell_isAtOrigin() {
        assertEquals(0, IconType.BELL.u);
        assertEquals(0, IconType.BELL.v);
    }

    @Test
    void bellMuted_isSecondColumn() {
        assertEquals(16, IconType.BELL_MUTED.u);
        assertEquals(0, IconType.BELL_MUTED.v);
    }

    @Test
    void sun_isThirdColumn() {
        assertEquals(32, IconType.SUN.u);
        assertEquals(0, IconType.SUN.v);
    }

    @Test
    void moon_isFourthColumn() {
        assertEquals(48, IconType.MOON.u);
        assertEquals(0, IconType.MOON.v);
    }

    @Test
    void dotGreen_isRow1Col0() {
        assertEquals(0, IconType.DOT_GREEN.u);
        assertEquals(16, IconType.DOT_GREEN.v);
    }

    @Test
    void dotRed_isRow1Col1() {
        assertEquals(16, IconType.DOT_RED.u);
        assertEquals(16, IconType.DOT_RED.v);
    }

    @Test
    void dotBlue_isRow1Col2() {
        assertEquals(32, IconType.DOT_BLUE.u);
        assertEquals(16, IconType.DOT_BLUE.v);
    }

    @Test
    void toggleOn_isRow2Col0() {
        assertEquals(0, IconType.TOGGLE_ON.u);
        assertEquals(32, IconType.TOGGLE_ON.v);
    }

    @Test
    void toggleOff_isRow2Col1() {
        assertEquals(16, IconType.TOGGLE_OFF.u);
        assertEquals(32, IconType.TOGGLE_OFF.v);
    }

    @Test
    void collapseDots_isRow3Col0() {
        assertEquals(0, IconType.COLLAPSE_DOTS.u);
        assertEquals(48, IconType.COLLAPSE_DOTS.v);
    }

    @Test
    void gear_isRow3Col1() {
        assertEquals(16, IconType.GEAR.u);
        assertEquals(48, IconType.GEAR.v);
    }

    // --- UV formula: u = col * SIZE, v = row * SIZE ---

    @ParameterizedTest
    @EnumSource(IconType.class)
    void uCoordinate_isMultipleOfSize(IconType icon) {
        assertEquals(0, icon.u % IconType.SIZE,
            icon.name() + " u-coordinate should be a multiple of SIZE");
    }

    @ParameterizedTest
    @EnumSource(IconType.class)
    void vCoordinate_isMultipleOfSize(IconType icon) {
        assertEquals(0, icon.v % IconType.SIZE,
            icon.name() + " v-coordinate should be a multiple of SIZE");
    }

    @ParameterizedTest
    @EnumSource(IconType.class)
    void uvCoordinates_fitWithinAtlas(IconType icon) {
        assertTrue(icon.u >= 0 && icon.u + IconType.SIZE <= IconType.ATLAS_SIZE,
            icon.name() + " u-coordinate out of atlas bounds");
        assertTrue(icon.v >= 0 && icon.v + IconType.SIZE <= IconType.ATLAS_SIZE,
            icon.name() + " v-coordinate out of atlas bounds");
    }

    // --- No duplicate positions ---

    @Test
    void allIcons_haveUniqueUvPositions() {
        Set<String> positions = new HashSet<>();
        for (IconType icon : IconType.values()) {
            String pos = icon.u + "," + icon.v;
            assertTrue(positions.add(pos),
                "Duplicate UV position found for " + icon.name() + " at (" + icon.u + ", " + icon.v + ")");
        }
    }

    // --- Row grouping (semantic) ---

    @Test
    void row0_containsBellAndThemeIcons() {
        assertEquals(0, IconType.BELL.v);
        assertEquals(0, IconType.BELL_MUTED.v);
        assertEquals(0, IconType.SUN.v);
        assertEquals(0, IconType.MOON.v);
    }

    @Test
    void row1_containsStatusDots() {
        assertEquals(16, IconType.DOT_GREEN.v);
        assertEquals(16, IconType.DOT_RED.v);
        assertEquals(16, IconType.DOT_BLUE.v);
    }

    @Test
    void row2_containsToggleIcons() {
        assertEquals(32, IconType.TOGGLE_ON.v);
        assertEquals(32, IconType.TOGGLE_OFF.v);
    }

    @Test
    void row3_containsUiControls() {
        assertEquals(48, IconType.COLLAPSE_DOTS.v);
        assertEquals(48, IconType.GEAR.v);
    }
}
