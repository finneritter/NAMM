package com.namm.input;

import com.namm.config.NammConfig;
import com.namm.executor.MacroPlaybackState;
import com.namm.model.ChatCommand;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import com.namm.model.PlaybackMode;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class TriggerKeyHandler {
	private final Map<String, Boolean> previousKeyState = new HashMap<>();
	private final Map<String, Boolean> previousChatKeyState = new HashMap<>();

	public void onClientTick(Minecraft client) {
		// In-game guard: only process when player is in-game with no screen open
		if (client.screen != null || client.player == null) {
			return;
		}

		// Don't trigger macros from simulated input
		if (InputSimulator.isSimulating()) {
			return;
		}

		long window = client.getWindow().handle();
		MacroPlaybackState playbackState = MacroPlaybackState.getInstance();
		NammConfig config = NammConfig.getInstance();
		MacroProfile activeProfile = config.getActiveProfile();

		for (Macro macro : config.getMacros()) {
			if (!macro.isEnabled() || macro.getTriggerKeyCode() == -1) {
				continue;
			}

			// Profile filter: if a profile is active, only fire macros in that profile
			if (activeProfile != null && !activeProfile.isMacroActive(macro.getName())) {
				continue;
			}

			boolean currentlyPressed = isKeyPressed(window, macro.getTriggerKeyCode(), macro.isTriggerMouse());
			boolean wasPressed = previousKeyState.getOrDefault(macro.getName(), false);
			previousKeyState.put(macro.getName(), currentlyPressed);

			boolean risingEdge = currentlyPressed && !wasPressed;
			boolean fallingEdge = !currentlyPressed && wasPressed;

			switch (macro.getPlaybackMode()) {
				case PLAY_ONCE:
					if (risingEdge && !playbackState.isRunning(macro.getName())) {
						playbackState.startMacro(macro, false);
					}
					break;

				case TOGGLE_LOOP:
					if (risingEdge) {
						playbackState.startMacro(macro, true);
					} else if (fallingEdge) {
						playbackState.stopMacro(macro.getName());
					}
					break;

				case HOLD_TO_PLAY:
					if (risingEdge) {
						playbackState.startMacro(macro, true);
					} else if (fallingEdge) {
						playbackState.stopMacro(macro.getName());
					}
					break;
			}
		}

		// Chat command triggers
		for (ChatCommand cmd : config.getChatCommands()) {
			if (!cmd.isEnabled() || cmd.getTriggerKeyCode() == -1) {
				continue;
			}

			String key = "cmd:" + cmd.getName();
			boolean currentlyPressed = isKeyPressed(window, cmd.getTriggerKeyCode(), cmd.isTriggerMouse());
			boolean wasPressed = previousChatKeyState.getOrDefault(key, false);
			previousChatKeyState.put(key, currentlyPressed);

			if (currentlyPressed && !wasPressed) {
				String message = cmd.getMessage();
				if (message != null && !message.isEmpty()) {
					if (message.startsWith("/")) {
						client.player.connection.sendCommand(message.substring(1));
					} else {
						client.player.connection.sendChat(message);
					}
				}
			}
		}
	}

	private boolean isKeyPressed(long window, int keyCode, boolean isMouse) {
		if (isMouse) {
			return GLFW.glfwGetMouseButton(window, keyCode) == GLFW.GLFW_PRESS;
		} else {
			return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
		}
	}
}
