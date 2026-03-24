package com.namm.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackModeTest {

	@Test
	void allValuesExist() {
		assertEquals(3, PlaybackMode.values().length);
	}

	@Test
	void valueOfRoundTrips() {
		for (PlaybackMode mode : PlaybackMode.values()) {
			assertEquals(mode, PlaybackMode.valueOf(mode.name()));
		}
	}

	@Test
	void expectedConstants() {
		assertNotNull(PlaybackMode.PLAY_ONCE);
		assertNotNull(PlaybackMode.TOGGLE_LOOP);
		assertNotNull(PlaybackMode.HOLD_TO_PLAY);
	}
}
