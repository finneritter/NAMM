package com.namm.config;

import com.namm.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MacroSerializerTest {

	@TempDir
	Path tempDir;

	private Path configFile;

	@BeforeEach
	void setUp() {
		configFile = tempDir.resolve("test-config.json");
	}

	// --- Round-trip serialization ---

	@Test
	void saveAndLoad_emptyConfig_roundTrips() {
		MacroSerializer.save(new ArrayList<>(), new ArrayList<>(), null, configFile);

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertNotNull(loaded);
		assertTrue(loaded.macros.isEmpty());
		assertTrue(loaded.profiles.isEmpty());
		assertNull(loaded.activeProfileName);
	}

	@Test
	void saveAndLoad_withMacros_roundTrips() {
		Macro macro = new Macro();
		macro.setName("TestMacro");
		macro.setTriggerKeyCode(65); // A key
		macro.setTriggerMouse(false);
		macro.setEnabled(true);
		macro.setPlaybackMode(PlaybackMode.TOGGLE_LOOP);
		macro.setSteps(List.of(
				MacroStep.keyAction(ActionType.KEY_PRESS, 65, false),
				MacroStep.delay(100),
				MacroStep.keyAction(ActionType.KEY_RELEASE, 65, false)
		));

		MacroSerializer.save(List.of(macro), new ArrayList<>(), null, configFile);
		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertEquals(1, loaded.macros.size());
		Macro loadedMacro = loaded.macros.get(0);
		assertEquals("TestMacro", loadedMacro.getName());
		assertEquals(65, loadedMacro.getTriggerKeyCode());
		assertFalse(loadedMacro.isTriggerMouse());
		assertTrue(loadedMacro.isEnabled());
		assertEquals(PlaybackMode.TOGGLE_LOOP, loadedMacro.getPlaybackMode());
		assertEquals(3, loadedMacro.getSteps().size());
		assertEquals(ActionType.KEY_PRESS, loadedMacro.getSteps().get(0).getActionType());
		assertEquals(ActionType.DELAY, loadedMacro.getSteps().get(1).getActionType());
		assertEquals(100, loadedMacro.getSteps().get(1).getDelayMs());
		assertEquals(ActionType.KEY_RELEASE, loadedMacro.getSteps().get(2).getActionType());
	}

	@Test
	void saveAndLoad_withProfiles_roundTrips() {
		MacroProfile profile = new MacroProfile("Combat", Set.of("Attack", "Heal"));
		MacroSerializer.save(new ArrayList<>(), List.of(profile), "Combat", configFile);

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertEquals(1, loaded.profiles.size());
		assertEquals("Combat", loaded.profiles.get(0).getName());
		assertTrue(loaded.profiles.get(0).getActiveMacroNames().contains("Attack"));
		assertTrue(loaded.profiles.get(0).getActiveMacroNames().contains("Heal"));
		assertEquals("Combat", loaded.activeProfileName);
	}

	@Test
	void saveAndLoad_withChatCommands_roundTrips() {
		MacroSerializer.ConfigWrapper wrapper = new MacroSerializer.ConfigWrapper();
		wrapper.chatCommands = List.of(
				new ChatCommand("greet", "/say hello", 71, false, true)
		);
		MacroSerializer.saveWrapper(wrapper, configFile);

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertEquals(1, loaded.chatCommands.size());
		assertEquals("greet", loaded.chatCommands.get(0).getName());
		assertEquals("/say hello", loaded.chatCommands.get(0).getMessage());
		assertEquals(71, loaded.chatCommands.get(0).getTriggerKeyCode());
		assertTrue(loaded.chatCommands.get(0).isEnabled());
	}

	@Test
	void saveAndLoad_uiSettings_roundTrips() {
		MacroSerializer.ConfigWrapper wrapper = new MacroSerializer.ConfigWrapper();
		wrapper.theme = "light";
		wrapper.accentColor = "blue";
		wrapper.infoBarVisibility = "always";
		wrapper.arrayListEnabled = false;
		wrapper.arrayListPosition = "bottom_left";
		wrapper.arrayListShowMacros = false;
		wrapper.arrayListShowChatCommands = false;
		wrapper.notificationsMuted = true;
		wrapper.notifMacroToggled = false;
		wrapper.notifChatCommand = false;
		wrapper.notifProfileSwitched = false;
		wrapper.notifImportExport = false;
		wrapper.notifErrors = false;
		wrapper.macroWinX = 100;
		wrapper.macroWinY = 200;
		wrapper.profileWinX = 300;
		wrapper.profileWinY = 400;
		wrapper.editorWinX = 500;
		wrapper.editorWinY = 600;
		wrapper.chatWinX = 700;
		wrapper.chatWinY = 800;
		wrapper.settingsWinX = 900;
		wrapper.settingsWinY = 1000;

		MacroSerializer.saveWrapper(wrapper, configFile);
		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertEquals("light", loaded.theme);
		assertEquals("blue", loaded.accentColor);
		assertEquals("always", loaded.infoBarVisibility);
		assertFalse(loaded.arrayListEnabled);
		assertEquals("bottom_left", loaded.arrayListPosition);
		assertFalse(loaded.arrayListShowMacros);
		assertFalse(loaded.arrayListShowChatCommands);
		assertTrue(loaded.notificationsMuted);
		assertFalse(loaded.notifMacroToggled);
		assertFalse(loaded.notifChatCommand);
		assertFalse(loaded.notifProfileSwitched);
		assertFalse(loaded.notifImportExport);
		assertFalse(loaded.notifErrors);
		assertEquals(100, loaded.macroWinX);
		assertEquals(200, loaded.macroWinY);
		assertEquals(300, loaded.profileWinX);
		assertEquals(400, loaded.profileWinY);
		assertEquals(500, loaded.editorWinX);
		assertEquals(600, loaded.editorWinY);
		assertEquals(700, loaded.chatWinX);
		assertEquals(800, loaded.chatWinY);
		assertEquals(900, loaded.settingsWinX);
		assertEquals(1000, loaded.settingsWinY);
	}

	@Test
	void saveAndLoad_multipleMacrosAndProfiles_roundTrips() {
		Macro m1 = new Macro("First", List.of(MacroStep.delay(50)), PlaybackMode.PLAY_ONCE, 65, false, true);
		Macro m2 = new Macro("Second", List.of(MacroStep.delay(100)), PlaybackMode.HOLD_TO_PLAY, 0, true, false);
		MacroProfile p1 = new MacroProfile("Profile1", Set.of("First"));
		MacroProfile p2 = new MacroProfile("Profile2", Set.of("Second"));

		MacroSerializer.save(List.of(m1, m2), List.of(p1, p2), "Profile1", configFile);
		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertEquals(2, loaded.macros.size());
		assertEquals(2, loaded.profiles.size());
		assertEquals("Profile1", loaded.activeProfileName);
		assertEquals("First", loaded.macros.get(0).getName());
		assertEquals("Second", loaded.macros.get(1).getName());
		assertEquals(PlaybackMode.HOLD_TO_PLAY, loaded.macros.get(1).getPlaybackMode());
		assertTrue(loaded.macros.get(1).isTriggerMouse());
		assertFalse(loaded.macros.get(1).isEnabled());
	}

	// --- Load edge cases ---

	@Test
	void load_nonExistentFile_returnsEmptyWrapper() {
		Path nonExistent = tempDir.resolve("does-not-exist.json");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(nonExistent);

		assertNotNull(loaded);
		assertTrue(loaded.macros.isEmpty());
		assertTrue(loaded.profiles.isEmpty());
	}

	@Test
	void load_emptyFile_returnsEmptyWrapper() throws IOException {
		Files.writeString(configFile, "");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertNotNull(loaded);
		assertTrue(loaded.macros.isEmpty());
		assertTrue(loaded.profiles.isEmpty());
	}

	@Test
	void load_nullJsonLiteral_returnsEmptyWrapper() throws IOException {
		Files.writeString(configFile, "null");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertNotNull(loaded);
		assertTrue(loaded.macros.isEmpty());
	}

	@Test
	void load_malformedJson_returnsEmptyWrapper() throws IOException {
		Files.writeString(configFile, "{{{not valid json!!!");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertNotNull(loaded);
		assertTrue(loaded.macros.isEmpty());
		assertTrue(loaded.profiles.isEmpty());
	}

	@Test
	void load_jsonWithExtraFields_ignoresUnknownFields() throws IOException {
		Files.writeString(configFile, """
				{
				  "macros": [],
				  "profiles": [],
				  "unknownField": "some value",
				  "anotherUnknown": 42
				}
				""");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertNotNull(loaded);
		assertTrue(loaded.macros.isEmpty());
	}

	@Test
	void load_jsonWithMissingFields_fillsDefaults() throws IOException {
		Files.writeString(configFile, """
				{
				  "macros": []
				}
				""");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertNotNull(loaded);
		assertTrue(loaded.profiles.isEmpty());
		assertTrue(loaded.chatCommands.isEmpty());
		assertEquals("dark", loaded.theme);
		assertEquals("menu_only", loaded.infoBarVisibility);
		assertEquals("top_right", loaded.arrayListPosition);
		assertEquals("purple", loaded.accentColor);
	}

	@Test
	void load_jsonWithNullFields_fillsDefaults() throws IOException {
		Files.writeString(configFile, """
				{
				  "macros": null,
				  "profiles": null,
				  "chatCommands": null,
				  "theme": null,
				  "infoBarVisibility": null,
				  "arrayListPosition": null,
				  "accentColor": null
				}
				""");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertNotNull(loaded.macros);
		assertNotNull(loaded.profiles);
		assertNotNull(loaded.chatCommands);
		assertEquals("dark", loaded.theme);
		assertEquals("menu_only", loaded.infoBarVisibility);
		assertEquals("top_right", loaded.arrayListPosition);
		assertEquals("purple", loaded.accentColor);
	}

	@Test
	void load_emptyJsonObject_fillsAllDefaults() throws IOException {
		Files.writeString(configFile, "{}");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertNotNull(loaded);
		assertNotNull(loaded.macros);
		assertNotNull(loaded.profiles);
		assertNotNull(loaded.chatCommands);
		assertEquals("dark", loaded.theme);
		assertEquals("purple", loaded.accentColor);
	}

	// --- Export/Import ---

	@Test
	void exportAndImport_roundTrips() {
		Macro macro = new Macro("Exported", List.of(
				MacroStep.keyAction(ActionType.KEY_PRESS, 87, false),
				MacroStep.delay(50),
				MacroStep.keyAction(ActionType.KEY_RELEASE, 87, false)
		), PlaybackMode.PLAY_ONCE, 290, false, true);

		String json = MacroSerializer.exportToJson(List.of(macro));
		List<Macro> imported = MacroSerializer.importFromJson(json);

		assertEquals(1, imported.size());
		assertEquals("Exported", imported.get(0).getName());
		assertEquals(3, imported.get(0).getSteps().size());
		assertEquals(87, imported.get(0).getSteps().get(0).getKeyCode());
	}

	@Test
	void importFromJson_nullJson_returnsEmptyList() {
		List<Macro> result = MacroSerializer.importFromJson("null");

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void importFromJson_malformedJson_returnsEmptyList() {
		List<Macro> result = MacroSerializer.importFromJson("not json at all");

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void importFromJson_emptyMacros_returnsEmptyList() {
		List<Macro> result = MacroSerializer.importFromJson("{\"macros\": []}");

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void importFromJson_wrapperWithNullMacros_returnsEmptyList() {
		List<Macro> result = MacroSerializer.importFromJson("{\"macros\": null}");

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void exportToJson_emptyList_producesValidJson() {
		String json = MacroSerializer.exportToJson(new ArrayList<>());

		assertNotNull(json);
		assertFalse(json.isEmpty());
		// Should be parseable back
		List<Macro> imported = MacroSerializer.importFromJson(json);
		assertTrue(imported.isEmpty());
	}

	@Test
	void exportToJson_multipleMacros_preservesOrder() {
		List<Macro> macros = List.of(
				new Macro("Alpha", List.of(MacroStep.delay(10)), PlaybackMode.PLAY_ONCE, -1, false, true),
				new Macro("Beta", List.of(MacroStep.delay(20)), PlaybackMode.PLAY_ONCE, -1, false, true),
				new Macro("Gamma", List.of(MacroStep.delay(30)), PlaybackMode.PLAY_ONCE, -1, false, true)
		);

		String json = MacroSerializer.exportToJson(macros);
		List<Macro> imported = MacroSerializer.importFromJson(json);

		assertEquals(3, imported.size());
		assertEquals("Alpha", imported.get(0).getName());
		assertEquals("Beta", imported.get(1).getName());
		assertEquals("Gamma", imported.get(2).getName());
	}

	// --- Migration logic ---

	@Test
	void load_migratesDelayBefore_toSeparateDelayStep() throws IOException {
		// Simulate legacy format: a step with delayBeforeMs set
		Files.writeString(configFile, """
				{
				  "macros": [
				    {
				      "name": "Legacy",
				      "steps": [
				        {
				          "actionType": "KEY_PRESS",
				          "keyCode": 65,
				          "mouse": false,
				          "delayMs": 20,
				          "delayBeforeMs": 200,
				          "delayAfterMs": 0
				        }
				      ],
				      "playbackMode": "PLAY_ONCE",
				      "triggerKeyCode": -1,
				      "triggerMouse": false,
				      "enabled": true
				    }
				  ]
				}
				""");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		assertEquals(1, loaded.macros.size());
		List<MacroStep> steps = loaded.macros.get(0).getSteps();
		// Should have been migrated: delay(200) + KEY_PRESS
		assertEquals(2, steps.size());
		assertEquals(ActionType.DELAY, steps.get(0).getActionType());
		assertEquals(200, steps.get(0).getDelayMs());
		assertEquals(ActionType.KEY_PRESS, steps.get(1).getActionType());
		assertEquals(0, steps.get(1).getDelayBeforeMs());
	}

	@Test
	void load_migratesDelayAfter_toSeparateDelayStep() throws IOException {
		Files.writeString(configFile, """
				{
				  "macros": [
				    {
				      "name": "Legacy",
				      "steps": [
				        {
				          "actionType": "KEY_PRESS",
				          "keyCode": 65,
				          "mouse": false,
				          "delayMs": 20,
				          "delayBeforeMs": 0,
				          "delayAfterMs": 150
				        }
				      ],
				      "playbackMode": "PLAY_ONCE",
				      "triggerKeyCode": -1,
				      "triggerMouse": false,
				      "enabled": true
				    }
				  ]
				}
				""");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		List<MacroStep> steps = loaded.macros.get(0).getSteps();
		// KEY_PRESS + delay(150)
		assertEquals(2, steps.size());
		assertEquals(ActionType.KEY_PRESS, steps.get(0).getActionType());
		assertEquals(0, steps.get(0).getDelayAfterMs());
		assertEquals(ActionType.DELAY, steps.get(1).getActionType());
		assertEquals(150, steps.get(1).getDelayMs());
	}

	@Test
	void load_migratesBothDelays_toSeparateDelaySteps() throws IOException {
		Files.writeString(configFile, """
				{
				  "macros": [
				    {
				      "name": "Legacy",
				      "steps": [
				        {
				          "actionType": "KEY_PRESS",
				          "keyCode": 65,
				          "mouse": false,
				          "delayMs": 20,
				          "delayBeforeMs": 100,
				          "delayAfterMs": 200
				        }
				      ],
				      "playbackMode": "PLAY_ONCE",
				      "triggerKeyCode": -1,
				      "triggerMouse": false,
				      "enabled": true
				    }
				  ]
				}
				""");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		List<MacroStep> steps = loaded.macros.get(0).getSteps();
		// delay(100) + KEY_PRESS + delay(200)
		assertEquals(3, steps.size());
		assertEquals(ActionType.DELAY, steps.get(0).getActionType());
		assertEquals(100, steps.get(0).getDelayMs());
		assertEquals(ActionType.KEY_PRESS, steps.get(1).getActionType());
		assertEquals(ActionType.DELAY, steps.get(2).getActionType());
		assertEquals(200, steps.get(2).getDelayMs());
	}

	@Test
	void load_noLegacyDelays_doesNotModifySteps() throws IOException {
		Files.writeString(configFile, """
				{
				  "macros": [
				    {
				      "name": "Modern",
				      "steps": [
				        {
				          "actionType": "KEY_PRESS",
				          "keyCode": 65,
				          "mouse": false,
				          "delayMs": 20,
				          "delayBeforeMs": 0,
				          "delayAfterMs": 0
				        },
				        {
				          "actionType": "DELAY",
				          "keyCode": -1,
				          "mouse": false,
				          "delayMs": 100,
				          "delayBeforeMs": 0,
				          "delayAfterMs": 0
				        }
				      ],
				      "playbackMode": "PLAY_ONCE",
				      "triggerKeyCode": -1,
				      "triggerMouse": false,
				      "enabled": true
				    }
				  ]
				}
				""");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		List<MacroStep> steps = loaded.macros.get(0).getSteps();
		assertEquals(2, steps.size());
		assertEquals(ActionType.KEY_PRESS, steps.get(0).getActionType());
		assertEquals(ActionType.DELAY, steps.get(1).getActionType());
	}

	// --- ConfigWrapper defaults ---

	@Test
	void configWrapper_defaultValues_areCorrect() {
		MacroSerializer.ConfigWrapper wrapper = new MacroSerializer.ConfigWrapper();

		assertNotNull(wrapper.macros);
		assertTrue(wrapper.macros.isEmpty());
		assertNotNull(wrapper.profiles);
		assertTrue(wrapper.profiles.isEmpty());
		assertNull(wrapper.activeProfileName);
		assertNotNull(wrapper.chatCommands);
		assertTrue(wrapper.chatCommands.isEmpty());
		assertEquals(-1, wrapper.macroWinX);
		assertEquals(-1, wrapper.macroWinY);
		assertEquals(-1, wrapper.profileWinX);
		assertEquals(-1, wrapper.profileWinY);
		assertEquals(-1, wrapper.editorWinX);
		assertEquals(-1, wrapper.editorWinY);
		assertEquals(-1, wrapper.chatWinX);
		assertEquals(-1, wrapper.chatWinY);
		assertEquals(-1, wrapper.settingsWinX);
		assertEquals(-1, wrapper.settingsWinY);
		assertTrue(wrapper.arrayListEnabled);
		assertEquals("top_right", wrapper.arrayListPosition);
		assertTrue(wrapper.arrayListShowMacros);
		assertTrue(wrapper.arrayListShowChatCommands);
		assertEquals("purple", wrapper.accentColor);
		assertEquals("dark", wrapper.theme);
		assertEquals("menu_only", wrapper.infoBarVisibility);
		assertFalse(wrapper.notificationsMuted);
		assertTrue(wrapper.notifMacroToggled);
		assertTrue(wrapper.notifChatCommand);
		assertTrue(wrapper.notifProfileSwitched);
		assertTrue(wrapper.notifImportExport);
		assertTrue(wrapper.notifErrors);
	}

	// --- Macro step types roundtrip ---

	@Test
	void saveAndLoad_allStepTypes_roundTrip() {
		Macro macro = new Macro();
		macro.setName("AllTypes");
		macro.setSteps(List.of(
				MacroStep.keyAction(ActionType.KEY_PRESS, 65, false),
				MacroStep.keyAction(ActionType.KEY_RELEASE, 65, false),
				MacroStep.keyAction(ActionType.MOUSE_CLICK, 0, true),
				MacroStep.keyAction(ActionType.MOUSE_RELEASE, 0, true),
				MacroStep.delay(500)
		));

		MacroSerializer.save(List.of(macro), new ArrayList<>(), null, configFile);
		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);

		List<MacroStep> steps = loaded.macros.get(0).getSteps();
		assertEquals(5, steps.size());
		assertEquals(ActionType.KEY_PRESS, steps.get(0).getActionType());
		assertFalse(steps.get(0).isMouse());
		assertEquals(ActionType.KEY_RELEASE, steps.get(1).getActionType());
		assertEquals(ActionType.MOUSE_CLICK, steps.get(2).getActionType());
		assertTrue(steps.get(2).isMouse());
		assertEquals(ActionType.MOUSE_RELEASE, steps.get(3).getActionType());
		assertTrue(steps.get(3).isMouse());
		assertEquals(ActionType.DELAY, steps.get(4).getActionType());
		assertEquals(500, steps.get(4).getDelayMs());
	}

	// --- File write edge cases ---

	@Test
	void save_overwritesExistingFile() throws IOException {
		// Save first version
		MacroSerializer.save(
				List.of(new Macro("First", List.of(), PlaybackMode.PLAY_ONCE, -1, false, true)),
				new ArrayList<>(), null, configFile);

		// Overwrite with second version
		MacroSerializer.save(
				List.of(new Macro("Second", List.of(), PlaybackMode.PLAY_ONCE, -1, false, true)),
				new ArrayList<>(), null, configFile);

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(configFile);
		assertEquals(1, loaded.macros.size());
		assertEquals("Second", loaded.macros.get(0).getName());
	}

	@Test
	void importFromJson_importedListIsMutable() {
		String json = MacroSerializer.exportToJson(
				List.of(new Macro("Test", List.of(MacroStep.delay(50)), PlaybackMode.PLAY_ONCE, -1, false, true))
		);

		List<Macro> imported = MacroSerializer.importFromJson(json);

		// Should be able to add/remove from returned list
		assertDoesNotThrow(() -> imported.add(new Macro()));
		assertEquals(2, imported.size());
	}
}
