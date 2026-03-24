package com.namm.config;

import com.namm.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NammConfig behavior through ConfigWrapper and MacroSerializer,
 * since NammConfig's constructor depends on FabricLoader and cannot be
 * instantiated directly in unit tests.
 *
 * Tests cover: wrapper-to-config field mapping correctness, profile lookup
 * logic, and state management patterns.
 */
class NammConfigTest {

	@TempDir
	Path tempDir;

	// --- ConfigWrapper field mapping ---

	@Test
	void configWrapper_allFieldsMapped_onSaveAndLoad() {
		Path file = tempDir.resolve("config.json");

		MacroSerializer.ConfigWrapper wrapper = new MacroSerializer.ConfigWrapper();
		wrapper.macros = List.of(new Macro("M1", List.of(MacroStep.delay(50)), PlaybackMode.PLAY_ONCE, 65, false, true));
		wrapper.profiles = List.of(new MacroProfile("P1", Set.of("M1")));
		wrapper.activeProfileName = "P1";
		wrapper.chatCommands = List.of(new ChatCommand("cmd", "/hello", 72, false, true));
		wrapper.theme = "light";
		wrapper.infoBarVisibility = "always";
		wrapper.notificationsMuted = true;
		wrapper.notifMacroToggled = false;
		wrapper.notifChatCommand = false;
		wrapper.notifProfileSwitched = false;
		wrapper.notifImportExport = false;
		wrapper.notifErrors = false;
		wrapper.arrayListEnabled = false;
		wrapper.arrayListPosition = "bottom_left";
		wrapper.arrayListShowMacros = false;
		wrapper.arrayListShowChatCommands = false;
		wrapper.accentColor = "red";
		wrapper.macroWinX = 10;
		wrapper.macroWinY = 20;
		wrapper.profileWinX = 30;
		wrapper.profileWinY = 40;
		wrapper.editorWinX = 50;
		wrapper.editorWinY = 60;
		wrapper.chatWinX = 70;
		wrapper.chatWinY = 80;
		wrapper.settingsWinX = 90;
		wrapper.settingsWinY = 100;

		MacroSerializer.saveWrapper(wrapper, file);
		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(file);

		// Verify every field survived the round-trip
		assertEquals(1, loaded.macros.size());
		assertEquals("M1", loaded.macros.get(0).getName());
		assertEquals(1, loaded.profiles.size());
		assertEquals("P1", loaded.profiles.get(0).getName());
		assertTrue(loaded.profiles.get(0).isMacroActive("M1"));
		assertEquals("P1", loaded.activeProfileName);
		assertEquals(1, loaded.chatCommands.size());
		assertEquals("cmd", loaded.chatCommands.get(0).getName());
		assertEquals("/hello", loaded.chatCommands.get(0).getMessage());
		assertEquals("light", loaded.theme);
		assertEquals("always", loaded.infoBarVisibility);
		assertTrue(loaded.notificationsMuted);
		assertFalse(loaded.notifMacroToggled);
		assertFalse(loaded.notifChatCommand);
		assertFalse(loaded.notifProfileSwitched);
		assertFalse(loaded.notifImportExport);
		assertFalse(loaded.notifErrors);
		assertFalse(loaded.arrayListEnabled);
		assertEquals("bottom_left", loaded.arrayListPosition);
		assertFalse(loaded.arrayListShowMacros);
		assertFalse(loaded.arrayListShowChatCommands);
		assertEquals("red", loaded.accentColor);
		assertEquals(10, loaded.macroWinX);
		assertEquals(20, loaded.macroWinY);
		assertEquals(30, loaded.profileWinX);
		assertEquals(40, loaded.profileWinY);
		assertEquals(50, loaded.editorWinX);
		assertEquals(60, loaded.editorWinY);
		assertEquals(70, loaded.chatWinX);
		assertEquals(80, loaded.chatWinY);
		assertEquals(90, loaded.settingsWinX);
		assertEquals(100, loaded.settingsWinY);
	}

	// --- Profile lookup behavior (simulating getActiveProfile logic) ---

	@Test
	void getActiveProfile_withNullName_returnsNull() {
		// Simulating the NammConfig.getActiveProfile() logic
		String activeProfileName = null;
		List<MacroProfile> profiles = List.of(new MacroProfile("P1", Set.of()));

		MacroProfile result = findActiveProfile(activeProfileName, profiles);

		assertNull(result);
	}

	@Test
	void getActiveProfile_withMatchingName_returnsProfile() {
		String activeProfileName = "Combat";
		MacroProfile combat = new MacroProfile("Combat", Set.of("Attack"));
		MacroProfile mining = new MacroProfile("Mining", Set.of("Mine"));

		MacroProfile result = findActiveProfile(activeProfileName, List.of(combat, mining));

		assertNotNull(result);
		assertEquals("Combat", result.getName());
		assertTrue(result.isMacroActive("Attack"));
	}

	@Test
	void getActiveProfile_withNonMatchingName_returnsNull() {
		String activeProfileName = "Nonexistent";
		List<MacroProfile> profiles = List.of(new MacroProfile("Combat", Set.of()));

		MacroProfile result = findActiveProfile(activeProfileName, profiles);

		assertNull(result);
	}

	@Test
	void getActiveProfile_withEmptyProfiles_returnsNull() {
		String activeProfileName = "Combat";
		List<MacroProfile> profiles = new ArrayList<>();

		MacroProfile result = findActiveProfile(activeProfileName, profiles);

		assertNull(result);
	}

	@Test
	void getActiveProfile_multipleProfilesWithSameName_returnsFirst() {
		String activeProfileName = "Combat";
		MacroProfile first = new MacroProfile("Combat", Set.of("Attack"));
		MacroProfile second = new MacroProfile("Combat", Set.of("Heal"));

		MacroProfile result = findActiveProfile(activeProfileName, List.of(first, second));

		assertNotNull(result);
		assertTrue(result.isMacroActive("Attack"));
		assertFalse(result.isMacroActive("Heal"));
	}

	// --- State defaults ---

	@Test
	void configWrapper_loadedFromEmpty_hasCorrectDefaults() throws IOException {
		Path file = tempDir.resolve("empty.json");
		Files.writeString(file, "{}");

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(file);

		// Verify all defaults match what NammConfig's fields would be
		assertEquals("dark", loaded.theme);
		assertEquals("menu_only", loaded.infoBarVisibility);
		assertFalse(loaded.notificationsMuted);
		assertTrue(loaded.notifMacroToggled);
		assertTrue(loaded.notifChatCommand);
		assertTrue(loaded.notifProfileSwitched);
		assertTrue(loaded.notifImportExport);
		assertTrue(loaded.notifErrors);
		assertTrue(loaded.arrayListEnabled);
		assertEquals("top_right", loaded.arrayListPosition);
		assertTrue(loaded.arrayListShowMacros);
		assertTrue(loaded.arrayListShowChatCommands);
		assertEquals("purple", loaded.accentColor);
		assertEquals(-1, loaded.macroWinX);
		assertEquals(-1, loaded.macroWinY);
	}

	// --- Profile state management ---

	@Test
	void macroProfile_isMacroActive_correctlyTracksState() {
		MacroProfile profile = new MacroProfile("Test", Set.of("Macro1", "Macro2"));

		assertTrue(profile.isMacroActive("Macro1"));
		assertTrue(profile.isMacroActive("Macro2"));
		assertFalse(profile.isMacroActive("Macro3"));

		profile.setMacroActive("Macro3", true);
		assertTrue(profile.isMacroActive("Macro3"));

		profile.setMacroActive("Macro1", false);
		assertFalse(profile.isMacroActive("Macro1"));
	}

	@Test
	void macroProfile_setMacroActive_idempotent() {
		MacroProfile profile = new MacroProfile("Test", Set.of("Macro1"));

		// Adding again should not throw or duplicate
		profile.setMacroActive("Macro1", true);
		assertTrue(profile.isMacroActive("Macro1"));

		// Removing non-existent should not throw
		profile.setMacroActive("NonExistent", false);
		assertFalse(profile.isMacroActive("NonExistent"));
	}

	@Test
	void macroProfile_roundTrips_throughSerialization() {
		Path file = tempDir.resolve("profiles.json");
		MacroProfile profile = new MacroProfile("PvP", Set.of("AutoAttack", "Shield", "Heal"));

		MacroSerializer.save(new ArrayList<>(), List.of(profile), "PvP", file);
		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(file);

		MacroProfile loadedProfile = loaded.profiles.get(0);
		assertEquals("PvP", loadedProfile.getName());
		assertTrue(loadedProfile.isMacroActive("AutoAttack"));
		assertTrue(loadedProfile.isMacroActive("Shield"));
		assertTrue(loadedProfile.isMacroActive("Heal"));
		assertFalse(loadedProfile.isMacroActive("Other"));
	}

	// --- ChatCommand state ---

	@Test
	void chatCommand_roundTrips_allFields() {
		Path file = tempDir.resolve("chat.json");
		ChatCommand cmd = new ChatCommand("tp", "/tp @s ~ ~10 ~", 84, false, true);
		ChatCommand cmd2 = new ChatCommand("gm", "/gamemode creative", 0, true, false);

		MacroSerializer.ConfigWrapper wrapper = new MacroSerializer.ConfigWrapper();
		wrapper.chatCommands = List.of(cmd, cmd2);
		MacroSerializer.saveWrapper(wrapper, file);

		MacroSerializer.ConfigWrapper loaded = MacroSerializer.load(file);

		assertEquals(2, loaded.chatCommands.size());
		ChatCommand l1 = loaded.chatCommands.get(0);
		assertEquals("tp", l1.getName());
		assertEquals("/tp @s ~ ~10 ~", l1.getMessage());
		assertEquals(84, l1.getTriggerKeyCode());
		assertFalse(l1.isTriggerMouse());
		assertTrue(l1.isEnabled());

		ChatCommand l2 = loaded.chatCommands.get(1);
		assertEquals("gm", l2.getName());
		assertTrue(l2.isTriggerMouse());
		assertFalse(l2.isEnabled());
	}

	/**
	 * Mirrors NammConfig.getActiveProfile() logic for testability.
	 */
	private MacroProfile findActiveProfile(String activeProfileName, List<MacroProfile> profiles) {
		if (activeProfileName == null) return null;
		for (MacroProfile p : profiles) {
			if (p.getName().equals(activeProfileName)) return p;
		}
		return null;
	}
}
