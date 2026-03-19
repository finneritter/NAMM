package com.namm;

import com.namm.config.NammConfig;
import com.namm.config.NammGuiScreen;
import com.namm.executor.MacroPlaybackState;
import com.namm.input.TriggerKeyHandler;
import com.namm.model.MacroProfile;
import com.namm.ui.ArrayListHud;
import com.namm.ui.InfoBar;
import com.namm.ui.ToastManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NammMod implements ClientModInitializer {
	public static final String MOD_ID = "namm";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("NAMM (Not Another Macro Mod) initializing...");

		// Load config
		NammConfig.getInstance().load();

		// Register trigger key handler
		TriggerKeyHandler triggerHandler = new TriggerKeyHandler();
		ClientTickEvents.END_CLIENT_TICK.register(triggerHandler::onClientTick);

		// Register configurable keybinding for opening NAMM menu
		KeyMapping.Category nammCategory = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("namm", "namm")
		);
		KeyMapping openMenu = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.namm.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT, nammCategory)
		);
		KeyMapping cycleNextProfile = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.namm.cycle_next_profile", GLFW.GLFW_KEY_UNKNOWN, nammCategory)
		);
		KeyMapping cyclePrevProfile = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.namm.cycle_prev_profile", GLFW.GLFW_KEY_UNKNOWN, nammCategory)
		);

		// Open NAMM GUI when keybind is pressed; handle profile cycling
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.screen == null && client.player != null) {
				while (openMenu.consumeClick()) {
					client.setScreen(new NammGuiScreen(null));
				}
				while (cycleNextProfile.consumeClick()) {
					cycleProfile(1);
				}
				while (cyclePrevProfile.consumeClick()) {
					cycleProfile(-1);
				}
			}
		});

		// Register HUD callback to render toasts and info bar outside the menu
		HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
			Minecraft mc = Minecraft.getInstance();
			int w = mc.getWindow().getGuiScaledWidth();
			int h = mc.getWindow().getGuiScaledHeight();

			// Always render array list HUD
			ArrayListHud.get().render(graphics, w, h);

			// Always render toasts
			ToastManager.get().render(graphics, w, h);

			// Render info bar if always-visible and not in NAMM screen
			if (InfoBar.get().isAlwaysVisible() && !(mc.screen instanceof NammGuiScreen)) {
				InfoBar.get().render(graphics, w, h);
			}
		});

		// Stop all macros on disconnect
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			MacroPlaybackState.getInstance().stopAll();
		});

		LOGGER.info("NAMM initialized successfully");
	}

	private void cycleProfile(int direction) {
		NammConfig cfg = NammConfig.getInstance();
		java.util.List<MacroProfile> profiles = cfg.getProfiles();
		if (profiles.isEmpty()) return;

		String current = cfg.getActiveProfileName();
		// Build list: null (no profile), then each profile name
		int currentIdx = -1; // -1 means "no profile" slot at index 0
		for (int i = 0; i < profiles.size(); i++) {
			if (profiles.get(i).getName().equals(current)) {
				currentIdx = i;
				break;
			}
		}

		// Total slots = profiles.size() + 1 (the +1 is for "no profile")
		int totalSlots = profiles.size() + 1;
		// currentIdx in slot space: no profile = 0, first profile = 1, etc.
		int slotIdx = (current == null) ? 0 : currentIdx + 1;
		slotIdx = (slotIdx + direction + totalSlots) % totalSlots;

		if (slotIdx == 0) {
			cfg.setActiveProfileName(null);
			cfg.save();
			ToastManager.get().post("Profile: None", ToastManager.ToastType.INFO, ToastManager.Category.PROFILE_SWITCHED);
		} else {
			String name = profiles.get(slotIdx - 1).getName();
			cfg.setActiveProfileName(name);
			cfg.save();
			ToastManager.get().post("Switched to " + name, ToastManager.ToastType.INFO, ToastManager.Category.PROFILE_SWITCHED);
		}
	}
}
