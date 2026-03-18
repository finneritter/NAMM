package com.namm;

import com.namm.config.NammConfig;
import com.namm.config.NammGuiScreen;
import com.namm.executor.MacroPlaybackState;
import com.namm.input.TriggerKeyHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NammMod implements ClientModInitializer {
	public static final String MOD_ID = "namm";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static boolean rightShiftWasPressed = false;

	@Override
	public void onInitializeClient() {
		LOGGER.info("NAMM (Not Another Macro Mod) initializing...");

		// Load config
		NammConfig.getInstance().load();

		// Register trigger key handler
		TriggerKeyHandler triggerHandler = new TriggerKeyHandler();
		ClientTickEvents.END_CLIENT_TICK.register(triggerHandler::onClientTick);

		// Right Shift opens the NAMM GUI
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.screen == null && client.player != null) {
				long window = client.getWindow().handle();
				if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
					if (!rightShiftWasPressed) {
						client.setScreen(new NammGuiScreen(null));
					}
					rightShiftWasPressed = true;
				} else {
					rightShiftWasPressed = false;
				}
			} else {
				rightShiftWasPressed = false;
			}
		});

		// Stop all macros on disconnect
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			MacroPlaybackState.getInstance().stopAll();
		});

		LOGGER.info("NAMM initialized successfully");
	}
}
