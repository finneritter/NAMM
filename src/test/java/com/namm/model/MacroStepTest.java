package com.namm.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MacroStepTest {

	@Test
	void defaultConstructor() {
		MacroStep step = new MacroStep();
		assertEquals(ActionType.KEY_PRESS, step.getActionType());
		assertEquals(-1, step.getKeyCode());
		assertFalse(step.isMouse());
		assertEquals(20, step.getDelayMs());
		assertEquals(0, step.getDelayBeforeMs());
		assertEquals(0, step.getDelayAfterMs());
	}

	@Test
	void delayFactory() {
		MacroStep step = MacroStep.delay(500);
		assertEquals(ActionType.DELAY, step.getActionType());
		assertEquals(500, step.getDelayMs());
	}

	@Test
	void keyActionFactory() {
		MacroStep step = MacroStep.keyAction(ActionType.MOUSE_CLICK, 0, true);
		assertEquals(ActionType.MOUSE_CLICK, step.getActionType());
		assertEquals(0, step.getKeyCode());
		assertTrue(step.isMouse());
	}

	@Test
	void setDelayMsClampsMinimum() {
		MacroStep step = new MacroStep();
		step.setDelayMs(5);
		assertEquals(20, step.getDelayMs(), "Should clamp to minimum 20ms");

		step.setDelayMs(0);
		assertEquals(20, step.getDelayMs());

		step.setDelayMs(-100);
		assertEquals(20, step.getDelayMs());

		step.setDelayMs(100);
		assertEquals(100, step.getDelayMs(), "Values >= 20 should pass through");
	}

	@Test
	void settersWork() {
		MacroStep step = new MacroStep();
		step.setActionType(ActionType.KEY_RELEASE);
		step.setKeyCode(65);
		step.setMouse(true);
		step.setDelayBeforeMs(50);
		step.setDelayAfterMs(100);

		assertEquals(ActionType.KEY_RELEASE, step.getActionType());
		assertEquals(65, step.getKeyCode());
		assertTrue(step.isMouse());
		assertEquals(50, step.getDelayBeforeMs());
		assertEquals(100, step.getDelayAfterMs());
	}

	@Test
	void copyIsIndependent() {
		MacroStep original = MacroStep.keyAction(ActionType.KEY_PRESS, 42, false);
		original.setDelayBeforeMs(10);
		original.setDelayAfterMs(20);

		MacroStep copy = original.copy();
		assertEquals(original.getActionType(), copy.getActionType());
		assertEquals(original.getKeyCode(), copy.getKeyCode());
		assertEquals(original.getDelayBeforeMs(), copy.getDelayBeforeMs());
		assertEquals(original.getDelayAfterMs(), copy.getDelayAfterMs());

		copy.setKeyCode(99);
		assertNotEquals(original.getKeyCode(), copy.getKeyCode());
	}
}
