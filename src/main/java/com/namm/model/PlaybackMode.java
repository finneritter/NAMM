package com.namm.model;

import net.minecraft.network.chat.Component;

public enum PlaybackMode {
	PLAY_ONCE("Play Once"),
	TOGGLE_LOOP("Toggle Loop"),
	HOLD_TO_PLAY("Hold to Play");

	private final String displayName;

	PlaybackMode(String displayName) {
		this.displayName = displayName;
	}

	public Component getDisplayName() {
		return Component.literal(displayName);
	}
}
