package com.namm.model;

import java.util.ArrayList;
import java.util.List;

public class Macro {
	private String name;
	private List<MacroStep> steps;
	private PlaybackMode playbackMode;
	private int triggerKeyCode;
	private boolean triggerMouse;
	private boolean enabled;

	public Macro() {
		this.name = "New Macro";
		this.steps = new ArrayList<>();
		this.playbackMode = PlaybackMode.PLAY_ONCE;
		this.triggerKeyCode = -1;
		this.triggerMouse = false;
		this.enabled = true;
	}

	public Macro(String name, List<MacroStep> steps, PlaybackMode playbackMode,
				 int triggerKeyCode, boolean triggerMouse, boolean enabled) {
		this.name = name;
		this.steps = new ArrayList<>(steps);
		this.playbackMode = playbackMode;
		this.triggerKeyCode = triggerKeyCode;
		this.triggerMouse = triggerMouse;
		this.enabled = enabled;
	}

	public Macro copy() {
		List<MacroStep> copiedSteps = new ArrayList<>();
		for (MacroStep step : steps) {
			copiedSteps.add(step.copy());
		}
		return new Macro(name, copiedSteps, playbackMode, triggerKeyCode, triggerMouse, enabled);
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public List<MacroStep> getSteps() { return steps; }
	public void setSteps(List<MacroStep> steps) { this.steps = steps; }

	public PlaybackMode getPlaybackMode() { return playbackMode; }
	public void setPlaybackMode(PlaybackMode playbackMode) { this.playbackMode = playbackMode; }

	public int getTriggerKeyCode() { return triggerKeyCode; }
	public void setTriggerKeyCode(int triggerKeyCode) { this.triggerKeyCode = triggerKeyCode; }

	public boolean isTriggerMouse() { return triggerMouse; }
	public void setTriggerMouse(boolean triggerMouse) { this.triggerMouse = triggerMouse; }

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
