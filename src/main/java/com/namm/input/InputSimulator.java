package com.namm.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.namm.model.ActionType;
import com.namm.model.MacroStep;
import net.minecraft.client.KeyMapping;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InputSimulator {
	private static volatile boolean simulating = false;

	// Track all keys currently held down by macro simulation
	private static final Set<InputConstants.Key> pressedKeys = ConcurrentHashMap.newKeySet();

	// Cache key lookups to avoid repeated map lookups in tight macro loops
	private static final Map<Integer, InputConstants.Key> keyCache = new ConcurrentHashMap<>();
	private static final Map<Integer, InputConstants.Key> mouseCache = new ConcurrentHashMap<>();

	// Pre-cache ordinals for hot-path enum comparison
	private static final int ORD_KEY_PRESS = ActionType.KEY_PRESS.ordinal();
	private static final int ORD_MOUSE_CLICK = ActionType.MOUSE_CLICK.ordinal();

	public static boolean isSimulating() {
		return simulating;
	}

	public static void simulate(MacroStep step) {
		simulating = true;
		try {
			int keyCode = step.getKeyCode();
			InputConstants.Key key = step.isMouse()
					? mouseCache.computeIfAbsent(keyCode, InputConstants.Type.MOUSE::getOrCreate)
					: keyCache.computeIfAbsent(keyCode, InputConstants.Type.KEYSYM::getOrCreate);

			int ordinal = step.getActionType().ordinal();
			boolean press = ordinal == ORD_KEY_PRESS || ordinal == ORD_MOUSE_CLICK;

			KeyMapping.set(key, press);

			if (press) {
				KeyMapping.click(key);
				pressedKeys.add(key);
			} else {
				pressedKeys.remove(key);
			}
		} finally {
			simulating = false;
		}
	}

	/**
	 * Release all keys that were pressed by macro simulation.
	 * Called when a macro is cancelled/stopped to prevent stuck keys.
	 */
	public static void releaseAll() {
		simulating = true;
		try {
			for (InputConstants.Key key : pressedKeys) {
				KeyMapping.set(key, false);
			}
			pressedKeys.clear();
		} finally {
			simulating = false;
		}
	}
}
