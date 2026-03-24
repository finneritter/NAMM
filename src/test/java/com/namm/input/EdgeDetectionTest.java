package com.namm.input;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the rising/falling edge detection logic used by TriggerKeyHandler.
 * Extracted from the handler since the actual handler requires Minecraft/GLFW.
 */
class EdgeDetectionTest {

	private final Map<String, Boolean> previousState = new HashMap<>();

	private boolean risingEdge(String key, boolean currentlyPressed) {
		boolean wasPressed = previousState.getOrDefault(key, false);
		previousState.put(key, currentlyPressed);
		return currentlyPressed && !wasPressed;
	}

	private boolean fallingEdge(String key, boolean currentlyPressed) {
		boolean wasPressed = previousState.getOrDefault(key, false);
		previousState.put(key, currentlyPressed);
		return !currentlyPressed && wasPressed;
	}

	@Test
	void risingEdgeOnFirstPress() {
		assertTrue(risingEdge("key1", true));
	}

	@Test
	void noRisingEdgeOnHold() {
		risingEdge("key1", true);
		assertFalse(risingEdge("key1", true), "Holding should not trigger again");
	}

	@Test
	void noRisingEdgeOnRelease() {
		assertFalse(risingEdge("key1", false));
	}

	@Test
	void fallingEdgeOnRelease() {
		// First press
		previousState.put("key1", true);
		assertTrue(fallingEdge("key1", false));
	}

	@Test
	void noFallingEdgeWithoutPriorPress() {
		assertFalse(fallingEdge("key1", false));
	}

	@Test
	void risingEdgeAfterRelease() {
		// Press -> release -> press should fire rising edge again
		risingEdge("key1", true);
		previousState.put("key1", false);
		assertTrue(risingEdge("key1", true));
	}

	@Test
	void independentKeys() {
		assertTrue(risingEdge("key1", true));
		assertTrue(risingEdge("key2", true));
		assertFalse(risingEdge("key1", true), "key1 still held");
	}
}
