package com.namm.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatCommandTest {

	@Test
	void defaultConstructorSetsDefaults() {
		ChatCommand cmd = new ChatCommand();
		assertEquals("New Command", cmd.getName());
		assertEquals("", cmd.getMessage());
		assertEquals(-1, cmd.getTriggerKeyCode());
		assertFalse(cmd.isTriggerMouse());
		assertTrue(cmd.isEnabled());
	}

	@Test
	void parameterizedConstructor() {
		ChatCommand cmd = new ChatCommand("hello", "/say hi", 42, true, false);
		assertEquals("hello", cmd.getName());
		assertEquals("/say hi", cmd.getMessage());
		assertEquals(42, cmd.getTriggerKeyCode());
		assertTrue(cmd.isTriggerMouse());
		assertFalse(cmd.isEnabled());
	}

	@Test
	void settersUpdateFields() {
		ChatCommand cmd = new ChatCommand();
		cmd.setName("test");
		cmd.setMessage("/tp 0 0 0");
		cmd.setTriggerKeyCode(65);
		cmd.setTriggerMouse(true);
		cmd.setEnabled(false);

		assertEquals("test", cmd.getName());
		assertEquals("/tp 0 0 0", cmd.getMessage());
		assertEquals(65, cmd.getTriggerKeyCode());
		assertTrue(cmd.isTriggerMouse());
		assertFalse(cmd.isEnabled());
	}

	@Test
	void copyIsIndependent() {
		ChatCommand original = new ChatCommand("orig", "/msg", 10, false, true);
		ChatCommand copy = original.copy();

		assertEquals(original.getName(), copy.getName());
		assertEquals(original.getMessage(), copy.getMessage());
		assertEquals(original.getTriggerKeyCode(), copy.getTriggerKeyCode());

		copy.setName("modified");
		copy.setMessage("/different");
		assertNotEquals(original.getName(), copy.getName());
		assertNotEquals(original.getMessage(), copy.getMessage());
	}

	@Test
	void edgeCases() {
		ChatCommand cmd = new ChatCommand("", "", -999, false, false);
		assertEquals("", cmd.getName());
		assertEquals("", cmd.getMessage());
		assertEquals(-999, cmd.getTriggerKeyCode());
	}
}
