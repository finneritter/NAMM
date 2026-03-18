package com.namm.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.namm.model.ActionType;
import com.namm.model.MacroStep;
import net.minecraft.client.KeyMapping;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InputSimulator {
	private static volatile boolean simulating = false;

	// Track all keys currently held down by macro simulation
	private static final Set<InputConstants.Key> pressedKeys = ConcurrentHashMap.newKeySet();

	public static boolean isSimulating() {
		return simulating;
	}

	public static void simulate(MacroStep step) {
		if (step.getActionType() == ActionType.DELAY) return;
		simulating = true;
		try {
			InputConstants.Key key;
			if (step.isMouse()) {
				key = InputConstants.Type.MOUSE.getOrCreate(step.getKeyCode());
			} else {
				key = InputConstants.Type.KEYSYM.getOrCreate(step.getKeyCode());
			}

			boolean press = step.getActionType() == ActionType.KEY_PRESS
					|| step.getActionType() == ActionType.MOUSE_CLICK;

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
