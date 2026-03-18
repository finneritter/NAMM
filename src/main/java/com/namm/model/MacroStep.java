package com.namm.model;

import com.namm.util.KeyNames;

public class MacroStep {
	private ActionType actionType;
	private int keyCode;
	private boolean mouse;
	private int delayMs;
	private int delayBeforeMs;
	private int delayAfterMs;

	public MacroStep() {
		this.actionType = ActionType.KEY_PRESS;
		this.keyCode = -1;
		this.mouse = false;
		this.delayMs = 20;
		this.delayBeforeMs = 0;
		this.delayAfterMs = 0;
	}

	public static MacroStep delay(int ms) {
		MacroStep step = new MacroStep();
		step.actionType = ActionType.DELAY;
		step.delayMs = ms;
		return step;
	}

	public static MacroStep keyAction(ActionType type, int keyCode, boolean isMouse) {
		MacroStep step = new MacroStep();
		step.actionType = type;
		step.keyCode = keyCode;
		step.mouse = isMouse;
		return step;
	}

	public String getDisplaySummary() {
		if (actionType == ActionType.DELAY) {
			return "Delay: " + delayMs + "ms";
		}
		String keyName = keyCode == -1 ? "None" : KeyNames.getKeyName(keyCode, mouse);
		return actionType.getDisplayName().getString() + ": " + keyName;
	}

	public MacroStep copy() {
		MacroStep c = new MacroStep();
		c.actionType = actionType;
		c.keyCode = keyCode;
		c.mouse = mouse;
		c.delayMs = delayMs;
		c.delayBeforeMs = delayBeforeMs;
		c.delayAfterMs = delayAfterMs;
		return c;
	}

	public ActionType getActionType() { return actionType; }
	public void setActionType(ActionType actionType) { this.actionType = actionType; }

	public int getKeyCode() { return keyCode; }
	public void setKeyCode(int keyCode) { this.keyCode = keyCode; }

	public boolean isMouse() { return mouse; }
	public void setMouse(boolean mouse) { this.mouse = mouse; }

	public int getDelayMs() { return delayMs; }
	public void setDelayMs(int delayMs) { this.delayMs = Math.max(20, delayMs); }

	public int getDelayBeforeMs() { return delayBeforeMs; }
	public void setDelayBeforeMs(int delayBeforeMs) { this.delayBeforeMs = delayBeforeMs; }

	public int getDelayAfterMs() { return delayAfterMs; }
	public void setDelayAfterMs(int delayAfterMs) { this.delayAfterMs = delayAfterMs; }
}
