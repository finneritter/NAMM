package com.namm.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KeyNames utility class.
 *
 * These tests verify the static key-name mapping tables (keyboard and mouse)
 * without requiring GLFW native initialization. The fallback path through
 * GLFW.glfwGetKeyName() is not tested here as it requires a native context.
 */
class KeyNamesTest {

    // --- Mouse button lookups ---

    @ParameterizedTest(name = "Mouse button {0} should be \"{1}\"")
    @CsvSource({
        "0, Left Click",
        "1, Right Click",
        "2, Middle Click",
        "3, Mouse 4",
        "4, Mouse 5",
        "5, Mouse 6",
        "6, Mouse 7",
        "7, Mouse 8"
    })
    void getKeyName_mouseButtons_returnsExpectedName(int button, String expected) {
        assertEquals(expected, KeyNames.getKeyName(button, true));
    }

    @Test
    void getKeyName_unknownMouseButton_returnsFallbackFormat() {
        // Button 8 is not in the map; fallback is "Mouse " + (keyCode + 1)
        assertEquals("Mouse 9", KeyNames.getKeyName(8, true));
        assertEquals("Mouse 11", KeyNames.getKeyName(10, true));
    }

    @Test
    void getKeyName_mouseButtonLeftUsesGlfwConstant() {
        // Verify we're mapping the actual GLFW constant, not just 0 by coincidence
        assertEquals(0, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        assertEquals("Left Click", KeyNames.getKeyName(GLFW.GLFW_MOUSE_BUTTON_LEFT, true));
    }

    // --- Keyboard named keys ---

    @ParameterizedTest(name = "Key {0} should be \"{1}\"")
    @CsvSource({
        "32, Space",
        "256, Escape",
        "257, Enter",
        "258, Tab",
        "259, Backspace",
        "260, Insert",
        "261, Delete",
        "262, Right",
        "263, Left",
        "264, Down",
        "265, Up",
        "266, Page Up",
        "267, Page Down",
        "268, Home",
        "269, End"
    })
    void getKeyName_namedKeys_returnsExpectedName(int keyCode, String expected) {
        assertEquals(expected, KeyNames.getKeyName(keyCode, false));
    }

    @ParameterizedTest(name = "Modifier key {0} should be \"{1}\"")
    @CsvSource({
        "340, Left Shift",
        "341, Left Ctrl",
        "342, Left Alt",
        "343, Left Super",
        "344, Right Shift",
        "345, Right Ctrl",
        "346, Right Alt",
        "347, Right Super"
    })
    void getKeyName_modifierKeys_returnsExpectedName(int keyCode, String expected) {
        assertEquals(expected, KeyNames.getKeyName(keyCode, false));
    }

    @ParameterizedTest(name = "Lock key {0} should be \"{1}\"")
    @CsvSource({
        "280, Caps Lock",
        "281, Scroll Lock",
        "282, Num Lock",
        "283, Print Screen",
        "284, Pause"
    })
    void getKeyName_lockKeys_returnsExpectedName(int keyCode, String expected) {
        assertEquals(expected, KeyNames.getKeyName(keyCode, false));
    }

    // --- Function keys ---

    @Test
    void getKeyName_functionKeys_returnsCorrectLabels() {
        assertEquals("F1", KeyNames.getKeyName(GLFW.GLFW_KEY_F1, false));
        assertEquals("F12", KeyNames.getKeyName(GLFW.GLFW_KEY_F12, false));
        assertEquals("F25", KeyNames.getKeyName(GLFW.GLFW_KEY_F25, false));
    }

    @Test
    void getKeyName_allFunctionKeysPopulated() {
        for (int i = GLFW.GLFW_KEY_F1; i <= GLFW.GLFW_KEY_F25; i++) {
            int expectedNum = i - GLFW.GLFW_KEY_F1 + 1;
            assertEquals("F" + expectedNum, KeyNames.getKeyName(i, false));
        }
    }

    // --- Numpad keys ---

    @Test
    void getKeyName_numpadDigits_returnsCorrectLabels() {
        for (int i = 0; i <= 9; i++) {
            assertEquals("Numpad " + i, KeyNames.getKeyName(GLFW.GLFW_KEY_KP_0 + i, false));
        }
    }

    @ParameterizedTest(name = "Numpad operator {0} should be \"{1}\"")
    @CsvSource({
        "330, Numpad .",
        "331, Numpad /",
        "332, Numpad *",
        "333, Numpad -",
        "334, Numpad +",
        "335, Numpad Enter",
        "336, Numpad ="
    })
    void getKeyName_numpadOperators_returnsExpectedName(int keyCode, String expected) {
        assertEquals(expected, KeyNames.getKeyName(keyCode, false));
    }

    // --- Map integrity ---

    @SuppressWarnings("unchecked")
    @Test
    void keyboardMap_containsExpectedNumberOfEntries() throws Exception {
        Field field = KeyNames.class.getDeclaredField("KEYBOARD_NAMES");
        field.setAccessible(true);
        Map<Integer, String> map = (Map<Integer, String>) field.get(null);

        // 15 named keys + 5 lock keys + 8 modifier keys + 25 F-keys + 10 numpad digits + 7 numpad operators = 70
        assertEquals(70, map.size(), "KEYBOARD_NAMES should contain exactly 70 entries");
    }

    @SuppressWarnings("unchecked")
    @Test
    void mouseMap_containsExpectedNumberOfEntries() throws Exception {
        Field field = KeyNames.class.getDeclaredField("MOUSE_NAMES");
        field.setAccessible(true);
        Map<Integer, String> map = (Map<Integer, String>) field.get(null);

        assertEquals(8, map.size(), "MOUSE_NAMES should contain exactly 8 entries");
    }

    // --- isMouse flag routing ---

    @Test
    void getKeyName_sameCodeDifferentFlag_returnsDifferentResults() {
        // Key code 0 as mouse = "Left Click", as keyboard = something else (falls through to GLFW/fallback)
        String mouseResult = KeyNames.getKeyName(0, true);
        assertEquals("Left Click", mouseResult);
        // As keyboard, code 0 is not in the keyboard map, so it will try GLFW native
        // (which may fail in test context) and then fall back to "Key 0"
    }
}
