package com.namm.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MacroTest {

	@Test
	void defaultConstructor() {
		Macro macro = new Macro();
		assertEquals("New Macro", macro.getName());
		assertTrue(macro.getSteps().isEmpty());
		assertEquals(PlaybackMode.PLAY_ONCE, macro.getPlaybackMode());
		assertEquals(-1, macro.getTriggerKeyCode());
		assertFalse(macro.isTriggerMouse());
		assertTrue(macro.isEnabled());
	}

	@Test
	void parameterizedConstructorDefensivelyCopiesSteps() {
		List<MacroStep> steps = new ArrayList<>();
		steps.add(MacroStep.delay(100));

		Macro macro = new Macro("test", steps, PlaybackMode.TOGGLE_LOOP, 65, true, false);
		assertEquals("test", macro.getName());
		assertEquals(1, macro.getSteps().size());
		assertEquals(PlaybackMode.TOGGLE_LOOP, macro.getPlaybackMode());
		assertTrue(macro.isTriggerMouse());
		assertFalse(macro.isEnabled());

		// Verify defensive copy — modifying original list doesn't affect macro
		steps.add(MacroStep.delay(200));
		assertEquals(1, macro.getSteps().size());
	}

	@Test
	void settersWork() {
		Macro macro = new Macro();
		macro.setName("updated");
		macro.setPlaybackMode(PlaybackMode.HOLD_TO_PLAY);
		macro.setTriggerKeyCode(32);
		macro.setTriggerMouse(true);
		macro.setEnabled(false);

		assertEquals("updated", macro.getName());
		assertEquals(PlaybackMode.HOLD_TO_PLAY, macro.getPlaybackMode());
		assertEquals(32, macro.getTriggerKeyCode());
		assertTrue(macro.isTriggerMouse());
		assertFalse(macro.isEnabled());
	}

	@Test
	void copyIsDeepCopy() {
		List<MacroStep> steps = new ArrayList<>();
		steps.add(MacroStep.keyAction(ActionType.KEY_PRESS, 65, false));
		Macro original = new Macro("orig", steps, PlaybackMode.PLAY_ONCE, 10, false, true);

		Macro copy = original.copy();
		assertEquals(original.getName(), copy.getName());
		assertEquals(original.getSteps().size(), copy.getSteps().size());

		// Modifying copy's step doesn't affect original
		copy.getSteps().get(0).setKeyCode(99);
		assertEquals(65, original.getSteps().get(0).getKeyCode());

		// Modifying copy's name doesn't affect original
		copy.setName("changed");
		assertEquals("orig", original.getName());
	}

	@Test
	void emptyStepsList() {
		Macro macro = new Macro("empty", new ArrayList<>(), PlaybackMode.PLAY_ONCE, -1, false, true);
		assertTrue(macro.getSteps().isEmpty());
		assertNotNull(macro.copy().getSteps());
	}
}
