package com.namm.model;

import net.minecraft.network.chat.Component;

public enum ActionType {
	KEY_PRESS("Key Press"),
	KEY_RELEASE("Key Release"),
	MOUSE_CLICK("Mouse Click"),
	MOUSE_RELEASE("Mouse Release"),
	DELAY("Delay");

	private final String displayName;

	ActionType(String displayName) {
		this.displayName = displayName;
	}

	public Component getDisplayName() {
		return Component.literal(displayName);
	}
}
