package com.namm.model;

import java.util.HashSet;
import java.util.Set;

public class MacroProfile {
	private String name;
	private Set<String> activeMacroNames;

	public MacroProfile() {
		this.name = "New Profile";
		this.activeMacroNames = new HashSet<>();
	}

	public MacroProfile(String name, Set<String> activeMacroNames) {
		this.name = name;
		this.activeMacroNames = new HashSet<>(activeMacroNames);
	}

	public boolean isMacroActive(String macroName) {
		return activeMacroNames.contains(macroName);
	}

	public void setMacroActive(String macroName, boolean active) {
		if (active) {
			activeMacroNames.add(macroName);
		} else {
			activeMacroNames.remove(macroName);
		}
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public Set<String> getActiveMacroNames() { return activeMacroNames; }
	public void setActiveMacroNames(Set<String> activeMacroNames) { this.activeMacroNames = activeMacroNames; }
}
