package com.namm.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.namm.NammMod;
import com.namm.model.ChatCommand;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import com.namm.model.MacroStep;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MacroSerializer {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type WRAPPER_TYPE = new TypeToken<ConfigWrapper>() {}.getType();

	public static void save(List<Macro> macros, List<MacroProfile> profiles, String activeProfileName, Path file) {
		ConfigWrapper wrapper = new ConfigWrapper();
		wrapper.macros = macros;
		wrapper.profiles = profiles;
		wrapper.activeProfileName = activeProfileName;
		saveWrapper(wrapper, file);
	}

	public static void saveWrapper(ConfigWrapper wrapper, Path file) {
		try {
			Files.writeString(file, GSON.toJson(wrapper));
		} catch (IOException e) {
			NammMod.LOGGER.error("Failed to save config to {}", file, e);
		}
	}

	public static ConfigWrapper load(Path file) {
		if (!Files.exists(file)) {
			return new ConfigWrapper();
		}
		try {
			String json = Files.readString(file);
			ConfigWrapper wrapper = GSON.fromJson(json, WRAPPER_TYPE);
			if (wrapper == null) {
				return new ConfigWrapper();
			}
			if (wrapper.macros == null) wrapper.macros = new ArrayList<>();
			if (wrapper.profiles == null) wrapper.profiles = new ArrayList<>();
			if (wrapper.chatCommands == null) wrapper.chatCommands = new ArrayList<>();
			if (wrapper.theme == null) wrapper.theme = "dark";
			if (wrapper.infoBarVisibility == null) wrapper.infoBarVisibility = "menu_only";
			if (wrapper.arrayListPosition == null) wrapper.arrayListPosition = "top_right";
			if (wrapper.accentColor == null) wrapper.accentColor = "purple";
			migrateSteps(wrapper.macros);
			return wrapper;
		} catch (JsonSyntaxException e) {
			NammMod.LOGGER.error("Failed to parse config JSON", e);
			return new ConfigWrapper();
		} catch (IOException e) {
			NammMod.LOGGER.error("Failed to load config from {}", file, e);
			return new ConfigWrapper();
		}
	}

	public static String exportToJson(List<Macro> macros) {
		ConfigWrapper wrapper = new ConfigWrapper();
		wrapper.macros = macros;
		return GSON.toJson(wrapper);
	}

	public static List<Macro> importFromJson(String json) {
		try {
			ConfigWrapper wrapper = GSON.fromJson(json, WRAPPER_TYPE);
			if (wrapper == null || wrapper.macros == null) {
				return new ArrayList<>();
			}
			List<Macro> macros = new ArrayList<>(wrapper.macros);
			migrateSteps(macros);
			return macros;
		} catch (JsonSyntaxException e) {
			NammMod.LOGGER.error("Failed to parse macro JSON", e);
			return new ArrayList<>();
		}
	}

	private static void migrateSteps(List<Macro> macros) {
		for (Macro macro : macros) {
			List<MacroStep> oldSteps = macro.getSteps();
			List<MacroStep> newSteps = new ArrayList<>();
			boolean migrated = false;

			for (MacroStep step : oldSteps) {
				if (step.getDelayBeforeMs() > 0) {
					newSteps.add(MacroStep.delay(step.getDelayBeforeMs()));
					step.setDelayBeforeMs(0);
					migrated = true;
				}
				newSteps.add(step);
				if (step.getDelayAfterMs() > 0) {
					newSteps.add(MacroStep.delay(step.getDelayAfterMs()));
					step.setDelayAfterMs(0);
					migrated = true;
				}
			}

			if (migrated) {
				macro.setSteps(newSteps);
				NammMod.LOGGER.info("Migrated macro '{}' to new delay format ({} -> {} steps)",
						macro.getName(), oldSteps.size(), newSteps.size());
			}
		}
	}

	public static class ConfigWrapper {
		public List<Macro> macros = new ArrayList<>();
		public List<MacroProfile> profiles = new ArrayList<>();
		public String activeProfileName = null;
		public List<ChatCommand> chatCommands = new ArrayList<>();
		public int macroWinX = -1;
		public int macroWinY = -1;
		public int profileWinX = -1;
		public int profileWinY = -1;
		public int editorWinX = -1;
		public int editorWinY = -1;
		public int chatWinX = -1;
		public int chatWinY = -1;
		public int settingsWinX = -1;
		public int settingsWinY = -1;
		public boolean arrayListEnabled = true;
		public String arrayListPosition = "top_right";
		public boolean arrayListShowMacros = true;
		public boolean arrayListShowChatCommands = true;
		public String accentColor = "purple";
		public String theme = "dark";
		public String infoBarVisibility = "menu_only";
		public boolean notificationsMuted = false;
		public boolean notifMacroToggled = true;
		public boolean notifChatCommand = true;
		public boolean notifProfileSwitched = true;
		public boolean notifImportExport = true;
		public boolean notifErrors = true;
	}
}
