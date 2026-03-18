package com.namm.config;

import com.namm.NammMod;
import com.namm.model.ChatCommand;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NammConfig {
	private static NammConfig INSTANCE;

	private final Path configPath;
	private List<Macro> macros;
	private List<MacroProfile> profiles;
	private String activeProfileName;
	private List<ChatCommand> chatCommands;

	// Window positions (-1 = auto)
	private int macroWinX = -1, macroWinY = -1;
	private int profileWinX = -1, profileWinY = -1;
	private int editorWinX = -1, editorWinY = -1;
	private int chatWinX = -1, chatWinY = -1;

	private NammConfig() {
		this.configPath = FabricLoader.getInstance().getConfigDir().resolve("namm.json");
		this.macros = new ArrayList<>();
		this.profiles = new ArrayList<>();
		this.activeProfileName = null;
		this.chatCommands = new ArrayList<>();
	}

	public static NammConfig getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new NammConfig();
		}
		return INSTANCE;
	}

	public void load() {
		MacroSerializer.ConfigWrapper wrapper = MacroSerializer.load(configPath);
		macros = wrapper.macros;
		profiles = wrapper.profiles;
		activeProfileName = wrapper.activeProfileName;
		chatCommands = wrapper.chatCommands;
		macroWinX = wrapper.macroWinX;
		macroWinY = wrapper.macroWinY;
		profileWinX = wrapper.profileWinX;
		profileWinY = wrapper.profileWinY;
		editorWinX = wrapper.editorWinX;
		editorWinY = wrapper.editorWinY;
		chatWinX = wrapper.chatWinX;
		chatWinY = wrapper.chatWinY;
		NammMod.LOGGER.info("Loaded {} macros, {} profiles, {} chat commands", macros.size(), profiles.size(), chatCommands.size());
	}

	public void save() {
		MacroSerializer.ConfigWrapper wrapper = new MacroSerializer.ConfigWrapper();
		wrapper.macros = macros;
		wrapper.profiles = profiles;
		wrapper.activeProfileName = activeProfileName;
		wrapper.chatCommands = chatCommands;
		wrapper.macroWinX = macroWinX;
		wrapper.macroWinY = macroWinY;
		wrapper.profileWinX = profileWinX;
		wrapper.profileWinY = profileWinY;
		wrapper.editorWinX = editorWinX;
		wrapper.editorWinY = editorWinY;
		wrapper.chatWinX = chatWinX;
		wrapper.chatWinY = chatWinY;
		MacroSerializer.saveWrapper(wrapper, configPath);
	}

	public List<Macro> getMacros() { return macros; }
	public List<MacroProfile> getProfiles() { return profiles; }
	public String getActiveProfileName() { return activeProfileName; }
	public void setActiveProfileName(String name) { this.activeProfileName = name; }
	public List<ChatCommand> getChatCommands() { return chatCommands; }

	public MacroProfile getActiveProfile() {
		if (activeProfileName == null) return null;
		for (MacroProfile p : profiles) {
			if (p.getName().equals(activeProfileName)) return p;
		}
		return null;
	}

	public int getMacroWinX() { return macroWinX; }
	public int getMacroWinY() { return macroWinY; }
	public void setMacroWinPos(int x, int y) { macroWinX = x; macroWinY = y; }
	public int getProfileWinX() { return profileWinX; }
	public int getProfileWinY() { return profileWinY; }
	public void setProfileWinPos(int x, int y) { profileWinX = x; profileWinY = y; }
	public int getEditorWinX() { return editorWinX; }
	public int getEditorWinY() { return editorWinY; }
	public void setEditorWinPos(int x, int y) { editorWinX = x; editorWinY = y; }
	public int getChatWinX() { return chatWinX; }
	public int getChatWinY() { return chatWinY; }
	public void setChatWinPos(int x, int y) { chatWinX = x; chatWinY = y; }
}
