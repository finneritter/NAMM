package com.namm.model;

public class ChatCommand {
	private String name;
	private String message;
	private int triggerKeyCode;
	private boolean triggerMouse;
	private boolean enabled;

	public ChatCommand() {
		this.name = "New Command";
		this.message = "";
		this.triggerKeyCode = -1;
		this.triggerMouse = false;
		this.enabled = true;
	}

	public ChatCommand(String name, String message, int triggerKeyCode, boolean triggerMouse, boolean enabled) {
		this.name = name;
		this.message = message;
		this.triggerKeyCode = triggerKeyCode;
		this.triggerMouse = triggerMouse;
		this.enabled = enabled;
	}

	public ChatCommand copy() {
		return new ChatCommand(name, message, triggerKeyCode, triggerMouse, enabled);
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }

	public int getTriggerKeyCode() { return triggerKeyCode; }
	public void setTriggerKeyCode(int triggerKeyCode) { this.triggerKeyCode = triggerKeyCode; }

	public boolean isTriggerMouse() { return triggerMouse; }
	public void setTriggerMouse(boolean triggerMouse) { this.triggerMouse = triggerMouse; }

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
