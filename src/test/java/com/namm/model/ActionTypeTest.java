package com.namm.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionTypeTest {

	@Test
	void allValuesExist() {
		ActionType[] values = ActionType.values();
		assertEquals(5, values.length);
	}

	@Test
	void valueOfRoundTrips() {
		for (ActionType type : ActionType.values()) {
			assertEquals(type, ActionType.valueOf(type.name()));
		}
	}

	@Test
	void expectedConstants() {
		assertNotNull(ActionType.KEY_PRESS);
		assertNotNull(ActionType.KEY_RELEASE);
		assertNotNull(ActionType.MOUSE_CLICK);
		assertNotNull(ActionType.MOUSE_RELEASE);
		assertNotNull(ActionType.DELAY);
	}

	@Test
	void valueOfInvalidThrows() {
		assertThrows(IllegalArgumentException.class, () -> ActionType.valueOf("INVALID"));
	}
}
