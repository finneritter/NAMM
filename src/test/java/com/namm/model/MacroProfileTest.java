package com.namm.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MacroProfileTest {

	@Test
	void defaultConstructor() {
		MacroProfile profile = new MacroProfile();
		assertEquals("New Profile", profile.getName());
		assertTrue(profile.getActiveMacroNames().isEmpty());
	}

	@Test
	void parameterizedConstructorDefensivelyCopies() {
		Set<String> names = new HashSet<>();
		names.add("macro1");
		names.add("macro2");

		MacroProfile profile = new MacroProfile("test", names);
		assertEquals("test", profile.getName());
		assertEquals(2, profile.getActiveMacroNames().size());

		// Verify defensive copy
		names.add("macro3");
		assertEquals(2, profile.getActiveMacroNames().size());
	}

	@Test
	void isMacroActive() {
		Set<String> names = new HashSet<>();
		names.add("active-macro");

		MacroProfile profile = new MacroProfile("p", names);
		assertTrue(profile.isMacroActive("active-macro"));
		assertFalse(profile.isMacroActive("inactive-macro"));
	}

	@Test
	void setMacroActiveAddsAndRemoves() {
		MacroProfile profile = new MacroProfile();

		profile.setMacroActive("macro1", true);
		assertTrue(profile.isMacroActive("macro1"));

		profile.setMacroActive("macro1", false);
		assertFalse(profile.isMacroActive("macro1"));
	}

	@Test
	void setMacroActiveIdempotent() {
		MacroProfile profile = new MacroProfile();
		profile.setMacroActive("m", true);
		profile.setMacroActive("m", true);
		assertEquals(1, profile.getActiveMacroNames().size());

		profile.setMacroActive("m", false);
		profile.setMacroActive("m", false);
		assertEquals(0, profile.getActiveMacroNames().size());
	}

	@Test
	void settersWork() {
		MacroProfile profile = new MacroProfile();
		profile.setName("renamed");
		assertEquals("renamed", profile.getName());

		Set<String> newSet = new HashSet<>();
		newSet.add("x");
		profile.setActiveMacroNames(newSet);
		assertTrue(profile.isMacroActive("x"));
	}
}
