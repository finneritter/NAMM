package com.namm.util;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeyNames {
	private static final Map<Integer, String> KEYBOARD_NAMES = new HashMap<>();
	private static final Map<Integer, String> MOUSE_NAMES = new HashMap<>();

	static {
		MOUSE_NAMES.put(GLFW.GLFW_MOUSE_BUTTON_LEFT, "Left Click");
		MOUSE_NAMES.put(GLFW.GLFW_MOUSE_BUTTON_RIGHT, "Right Click");
		MOUSE_NAMES.put(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, "Middle Click");
		MOUSE_NAMES.put(3, "Mouse 4");
		MOUSE_NAMES.put(4, "Mouse 5");
		MOUSE_NAMES.put(5, "Mouse 6");
		MOUSE_NAMES.put(6, "Mouse 7");
		MOUSE_NAMES.put(7, "Mouse 8");

		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_SPACE, "Space");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_ESCAPE, "Escape");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_ENTER, "Enter");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_TAB, "Tab");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_BACKSPACE, "Backspace");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_INSERT, "Insert");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_DELETE, "Delete");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_RIGHT, "Right");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_LEFT, "Left");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_DOWN, "Down");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_UP, "Up");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_PAGE_UP, "Page Up");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_PAGE_DOWN, "Page Down");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_HOME, "Home");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_END, "End");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "Caps Lock");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_SCROLL_LOCK, "Scroll Lock");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_NUM_LOCK, "Num Lock");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_PRINT_SCREEN, "Print Screen");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_PAUSE, "Pause");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "Left Shift");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "Left Ctrl");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "Left Alt");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_LEFT_SUPER, "Left Super");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "Right Shift");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "Right Ctrl");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "Right Alt");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_RIGHT_SUPER, "Right Super");

		for (int i = GLFW.GLFW_KEY_F1; i <= GLFW.GLFW_KEY_F25; i++) {
			KEYBOARD_NAMES.put(i, "F" + (i - GLFW.GLFW_KEY_F1 + 1));
		}

		for (int i = GLFW.GLFW_KEY_KP_0; i <= GLFW.GLFW_KEY_KP_9; i++) {
			KEYBOARD_NAMES.put(i, "Numpad " + (i - GLFW.GLFW_KEY_KP_0));
		}
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_KP_DECIMAL, "Numpad .");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_KP_DIVIDE, "Numpad /");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_KP_MULTIPLY, "Numpad *");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_KP_SUBTRACT, "Numpad -");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_KP_ADD, "Numpad +");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_KP_ENTER, "Numpad Enter");
		KEYBOARD_NAMES.put(GLFW.GLFW_KEY_KP_EQUAL, "Numpad =");
	}

	public static String getKeyName(int keyCode, boolean isMouse) {
		if (isMouse) {
			return MOUSE_NAMES.getOrDefault(keyCode, "Mouse " + (keyCode + 1));
		}

		// Check our map first
		String name = KEYBOARD_NAMES.get(keyCode);
		if (name != null) {
			return name;
		}

		// Try GLFW for printable keys
		String glfwName = GLFW.glfwGetKeyName(keyCode, 0);
		if (glfwName != null) {
			return glfwName.toUpperCase();
		}

		return "Key " + keyCode;
	}
}
