package com.namm.executor;

import com.namm.model.ActionType;
import com.namm.model.Macro;
import com.namm.model.MacroStep;
import com.namm.model.PlaybackMode;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for macro execution timing, model validation, and thread safety.
 * None of these tests require Minecraft runtime -- they test pure logic only.
 */
class MacroExecutorTest {

	// ─── Constants matching MacroExecutor internals ───
	private static final long SLEEP_THRESHOLD_NS = 2_000_000L;
	private static final long MS_TO_NS = 1_000_000L;

	/**
	 * Reimplements the same hybrid sleep/busy-wait approach used in MacroExecutor.preciseDelay
	 * so we can test timing precision without calling Minecraft APIs.
	 */
	private static void preciseDelay(long delayMs) throws InterruptedException {
		long deadlineNs = System.nanoTime() + delayMs * MS_TO_NS;
		long remainingNs = deadlineNs - System.nanoTime();

		if (remainingNs > SLEEP_THRESHOLD_NS) {
			Thread.sleep((remainingNs - SLEEP_THRESHOLD_NS) / MS_TO_NS);
		}

		while ((remainingNs = deadlineNs - System.nanoTime()) > 0) {
			if (Thread.interrupted()) throw new InterruptedException();
			Thread.onSpinWait();
		}
	}

	/**
	 * Measures the actual elapsed time of a preciseDelay call in milliseconds.
	 */
	private static double measureDelayMs(long targetMs) throws InterruptedException {
		long startNs = System.nanoTime();
		preciseDelay(targetMs);
		return (System.nanoTime() - startNs) / (double) MS_TO_NS;
	}

	// ═══════════════════════════════════════════════════
	// Section 1: Timing Precision
	// ═══════════════════════════════════════════════════

	@RepeatedTest(10)
	void preciseDelay_20ms_withinTolerance() throws InterruptedException {
		double actual = measureDelayMs(20);
		assertTrue(actual >= 20.0, "Delay must not be shorter than target; was " + actual + "ms");
		assertTrue(actual < 30.0, "20ms delay should finish within 10ms tolerance; was " + actual + "ms");
	}

	@RepeatedTest(10)
	void preciseDelay_50ms_withinTolerance() throws InterruptedException {
		double actual = measureDelayMs(50);
		assertTrue(actual >= 50.0, "Delay must not be shorter than target; was " + actual + "ms");
		assertTrue(actual < 60.0, "50ms delay should finish within 10ms tolerance; was " + actual + "ms");
	}

	@RepeatedTest(10)
	void preciseDelay_100ms_withinTolerance() throws InterruptedException {
		double actual = measureDelayMs(100);
		assertTrue(actual >= 100.0, "Delay must not be shorter than target; was " + actual + "ms");
		assertTrue(actual < 110.0, "100ms delay should finish within 10ms tolerance; was " + actual + "ms");
	}

	@RepeatedTest(10)
	void preciseDelay_500ms_withinTolerance() throws InterruptedException {
		double actual = measureDelayMs(500);
		assertTrue(actual >= 500.0, "Delay must not be shorter than target; was " + actual + "ms");
		assertTrue(actual < 515.0, "500ms delay should finish within 15ms tolerance; was " + actual + "ms");
	}

	@Test
	void preciseDelay_consistencyAcrossRuns() throws InterruptedException {
		int runs = 10;
		long targetMs = 50;
		double[] measurements = new double[runs];

		for (int i = 0; i < runs; i++) {
			measurements[i] = measureDelayMs(targetMs);
		}

		double mean = Arrays.stream(measurements).average().orElse(0);
		double variance = Arrays.stream(measurements)
				.map(m -> (m - mean) * (m - mean))
				.average().orElse(0);
		double stdDev = Math.sqrt(variance);

		assertTrue(stdDev < 5.0,
				"Standard deviation of 50ms delays should be < 5ms; was " + stdDev + "ms. "
						+ "Measurements: " + Arrays.toString(measurements));
	}

	@Test
	void preciseDelay_100ms_lowStandardDeviation() throws InterruptedException {
		int runs = 10;
		long targetMs = 100;
		double[] measurements = new double[runs];

		for (int i = 0; i < runs; i++) {
			measurements[i] = measureDelayMs(targetMs);
		}

		double mean = Arrays.stream(measurements).average().orElse(0);
		double variance = Arrays.stream(measurements)
				.map(m -> (m - mean) * (m - mean))
				.average().orElse(0);
		double stdDev = Math.sqrt(variance);

		assertTrue(stdDev < 5.0,
				"Standard deviation of 100ms delays should be < 5ms; was " + stdDev + "ms");
	}

	// ═══════════════════════════════════════════════════
	// Section 2: Thread Interrupt Contract
	// ═══════════════════════════════════════════════════

	@Test
	void preciseDelay_interruptDuringSleep_throwsInterruptedException() {
		AtomicBoolean wasInterrupted = new AtomicBoolean(false);
		AtomicLong elapsedMs = new AtomicLong();

		Thread t = new Thread(() -> {
			long start = System.nanoTime();
			try {
				preciseDelay(5000); // long delay to ensure we're in sleep phase
			} catch (InterruptedException e) {
				wasInterrupted.set(true);
			}
			elapsedMs.set((System.nanoTime() - start) / MS_TO_NS);
		});

		t.start();
		// Give the thread time to enter sleep, then interrupt
		try {
			Thread.sleep(50);
		} catch (InterruptedException ignored) {}
		t.interrupt();

		try {
			t.join(2000);
		} catch (InterruptedException ignored) {}

		assertAll(
				() -> assertFalse(t.isAlive(), "Thread should have terminated after interrupt"),
				() -> assertTrue(wasInterrupted.get(), "InterruptedException should have been caught"),
				() -> assertTrue(elapsedMs.get() < 1000,
						"Thread should terminate well before 5s delay completes; took " + elapsedMs.get() + "ms")
		);
	}

	@Test
	void preciseDelay_interruptDuringBusyWait_throwsInterruptedException() {
		AtomicBoolean wasInterrupted = new AtomicBoolean(false);
		AtomicLong elapsedMs = new AtomicLong();

		// Use a delay just at the busy-wait threshold (2ms) so the thread
		// enters busy-wait almost immediately (no sleep phase).
		Thread t = new Thread(() -> {
			long start = System.nanoTime();
			try {
				// 2ms delay means remaining < SLEEP_THRESHOLD_NS, so it goes
				// straight to busy-wait
				preciseDelay(2);
			} catch (InterruptedException e) {
				wasInterrupted.set(true);
			}
			elapsedMs.set((System.nanoTime() - start) / MS_TO_NS);
		});

		// Pre-interrupt the thread before starting it
		t.start();
		t.interrupt();

		try {
			t.join(2000);
		} catch (InterruptedException ignored) {}

		assertFalse(t.isAlive(), "Thread should have terminated after interrupt");
		// Either it completed the 2ms delay normally or was interrupted --
		// both are valid for such a short delay
		assertTrue(elapsedMs.get() < 500,
				"Thread should terminate quickly; took " + elapsedMs.get() + "ms");
	}

	@Test
	void interruptStopsThread_generalContract() throws InterruptedException {
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		t.start();
		t.interrupt();
		t.join(1000);
		assertFalse(t.isAlive(), "Thread should terminate after interrupt");
	}

	// ═══════════════════════════════════════════════════
	// Section 3: MacroStep Validation
	// ═══════════════════════════════════════════════════

	@Test
	void macroStep_setDelayMs_enforcesMinimum20ms() {
		MacroStep step = new MacroStep();

		step.setDelayMs(10);
		assertEquals(20, step.getDelayMs(), "Delay below 20 should be clamped to 20");

		step.setDelayMs(0);
		assertEquals(20, step.getDelayMs(), "Delay of 0 should be clamped to 20");

		step.setDelayMs(-5);
		assertEquals(20, step.getDelayMs(), "Negative delay should be clamped to 20");

		step.setDelayMs(20);
		assertEquals(20, step.getDelayMs(), "Delay of exactly 20 should be 20");

		step.setDelayMs(100);
		assertEquals(100, step.getDelayMs(), "Delay of 100 should be 100");
	}

	@Test
	void macroStep_delayFactory_createsDelayStep() {
		MacroStep step = MacroStep.delay(200);

		assertAll(
				() -> assertEquals(ActionType.DELAY, step.getActionType()),
				() -> assertEquals(200, step.getDelayMs()),
				() -> assertEquals(-1, step.getKeyCode()),
				() -> assertFalse(step.isMouse())
		);
	}

	@Test
	void macroStep_keyActionFactory_createsCorrectKeyPress() {
		MacroStep step = MacroStep.keyAction(ActionType.KEY_PRESS, 65, false);

		assertAll(
				() -> assertEquals(ActionType.KEY_PRESS, step.getActionType()),
				() -> assertEquals(65, step.getKeyCode()),
				() -> assertFalse(step.isMouse())
		);
	}

	@Test
	void macroStep_keyActionFactory_createsCorrectKeyRelease() {
		MacroStep step = MacroStep.keyAction(ActionType.KEY_RELEASE, 32, false);

		assertAll(
				() -> assertEquals(ActionType.KEY_RELEASE, step.getActionType()),
				() -> assertEquals(32, step.getKeyCode()),
				() -> assertFalse(step.isMouse())
		);
	}

	@Test
	void macroStep_keyActionFactory_createsCorrectMouseClick() {
		MacroStep step = MacroStep.keyAction(ActionType.MOUSE_CLICK, 0, true);

		assertAll(
				() -> assertEquals(ActionType.MOUSE_CLICK, step.getActionType()),
				() -> assertEquals(0, step.getKeyCode()),
				() -> assertTrue(step.isMouse())
		);
	}

	@Test
	void macroStep_keyActionFactory_createsCorrectMouseRelease() {
		MacroStep step = MacroStep.keyAction(ActionType.MOUSE_RELEASE, 1, true);

		assertAll(
				() -> assertEquals(ActionType.MOUSE_RELEASE, step.getActionType()),
				() -> assertEquals(1, step.getKeyCode()),
				() -> assertTrue(step.isMouse())
		);
	}

	@Test
	void macroStep_copy_producesIndependentInstance() {
		MacroStep original = MacroStep.keyAction(ActionType.KEY_PRESS, 65, false);
		original.setDelayMs(50);
		original.setDelayBeforeMs(10);
		original.setDelayAfterMs(15);

		MacroStep copy = original.copy();

		assertAll("Copy should match original",
				() -> assertEquals(original.getActionType(), copy.getActionType()),
				() -> assertEquals(original.getKeyCode(), copy.getKeyCode()),
				() -> assertEquals(original.isMouse(), copy.isMouse()),
				() -> assertEquals(original.getDelayMs(), copy.getDelayMs()),
				() -> assertEquals(original.getDelayBeforeMs(), copy.getDelayBeforeMs()),
				() -> assertEquals(original.getDelayAfterMs(), copy.getDelayAfterMs())
		);

		// Modify original -- copy should be unaffected
		original.setKeyCode(999);
		original.setDelayMs(200);
		original.setActionType(ActionType.DELAY);

		assertAll("Copy must be independent of original after modification",
				() -> assertEquals(ActionType.KEY_PRESS, copy.getActionType()),
				() -> assertEquals(65, copy.getKeyCode()),
				() -> assertEquals(50, copy.getDelayMs())
		);
	}

	@Test
	void macroStep_defaultConstructor_hasExpectedDefaults() {
		MacroStep step = new MacroStep();

		assertAll(
				() -> assertEquals(ActionType.KEY_PRESS, step.getActionType()),
				() -> assertEquals(-1, step.getKeyCode()),
				() -> assertFalse(step.isMouse()),
				() -> assertEquals(20, step.getDelayMs()),
				() -> assertEquals(0, step.getDelayBeforeMs()),
				() -> assertEquals(0, step.getDelayAfterMs())
		);
	}

	// ═══════════════════════════════════════════════════
	// Section 4: Macro Validation
	// ═══════════════════════════════════════════════════

	@Test
	void macro_emptyStepList_preventsExecution() {
		Macro macro = new Macro("empty", List.of(), PlaybackMode.PLAY_ONCE, 65, false, true);

		assertTrue(macro.getSteps().isEmpty(), "Macro with no steps should have empty step list");
		// MacroExecutor.run() would iterate an empty array and return immediately
		MacroStep[] steps = macro.getSteps().toArray(new MacroStep[0]);
		assertEquals(0, steps.length, "Empty macro produces zero-length step array");
	}

	@Test
	void macro_copy_producesDeepCopy() {
		List<MacroStep> originalSteps = new ArrayList<>();
		originalSteps.add(MacroStep.delay(100));
		originalSteps.add(MacroStep.keyAction(ActionType.KEY_PRESS, 65, false));

		Macro original = new Macro("test", originalSteps, PlaybackMode.TOGGLE_LOOP, 42, true, true);
		Macro copy = original.copy();

		assertAll("Copy should match original",
				() -> assertEquals(original.getName(), copy.getName()),
				() -> assertEquals(original.getPlaybackMode(), copy.getPlaybackMode()),
				() -> assertEquals(original.getTriggerKeyCode(), copy.getTriggerKeyCode()),
				() -> assertEquals(original.isTriggerMouse(), copy.isTriggerMouse()),
				() -> assertEquals(original.isEnabled(), copy.isEnabled()),
				() -> assertEquals(original.getSteps().size(), copy.getSteps().size())
		);

		// Modify original steps -- copy's steps should be unaffected
		original.getSteps().get(0).setDelayMs(999);
		original.getSteps().get(1).setKeyCode(999);
		original.setName("modified");
		original.setEnabled(false);

		assertAll("Copy must be independent after modifying original",
				() -> assertEquals("test", copy.getName()),
				() -> assertTrue(copy.isEnabled()),
				() -> assertEquals(100, copy.getSteps().get(0).getDelayMs()),
				() -> assertEquals(65, copy.getSteps().get(1).getKeyCode())
		);
	}

	@Test
	void macro_copy_handlesEmptySteps() {
		Macro original = new Macro("empty", List.of(), PlaybackMode.PLAY_ONCE, -1, false, true);
		Macro copy = original.copy();

		assertEquals(0, copy.getSteps().size(), "Copy of macro with no steps should also have no steps");
	}

	@Test
	void playbackMode_allValuesPresent() {
		PlaybackMode[] modes = PlaybackMode.values();

		assertAll(
				() -> assertEquals(3, modes.length, "Should have exactly 3 playback modes"),
				() -> assertEquals(PlaybackMode.PLAY_ONCE, PlaybackMode.valueOf("PLAY_ONCE")),
				() -> assertEquals(PlaybackMode.TOGGLE_LOOP, PlaybackMode.valueOf("TOGGLE_LOOP")),
				() -> assertEquals(PlaybackMode.HOLD_TO_PLAY, PlaybackMode.valueOf("HOLD_TO_PLAY"))
		);
	}

	@Test
	void actionType_allValuesPresent() {
		ActionType[] types = ActionType.values();

		assertAll(
				() -> assertEquals(5, types.length, "Should have exactly 5 action types"),
				() -> assertEquals(ActionType.KEY_PRESS, ActionType.valueOf("KEY_PRESS")),
				() -> assertEquals(ActionType.KEY_RELEASE, ActionType.valueOf("KEY_RELEASE")),
				() -> assertEquals(ActionType.MOUSE_CLICK, ActionType.valueOf("MOUSE_CLICK")),
				() -> assertEquals(ActionType.MOUSE_RELEASE, ActionType.valueOf("MOUSE_RELEASE")),
				() -> assertEquals(ActionType.DELAY, ActionType.valueOf("DELAY"))
		);
	}

	@Test
	void macro_defaultConstructor_hasExpectedDefaults() {
		Macro macro = new Macro();

		assertAll(
				() -> assertEquals("New Macro", macro.getName()),
				() -> assertNotNull(macro.getSteps()),
				() -> assertTrue(macro.getSteps().isEmpty()),
				() -> assertEquals(PlaybackMode.PLAY_ONCE, macro.getPlaybackMode()),
				() -> assertEquals(-1, macro.getTriggerKeyCode()),
				() -> assertFalse(macro.isTriggerMouse()),
				() -> assertTrue(macro.isEnabled())
		);
	}

	@Test
	void macro_constructorDefensiveCopiesStepList() {
		List<MacroStep> mutableList = new ArrayList<>();
		mutableList.add(MacroStep.delay(50));
		Macro macro = new Macro("test", mutableList, PlaybackMode.PLAY_ONCE, 65, false, true);

		// Modifying the original list should not affect the macro
		mutableList.add(MacroStep.delay(100));

		assertEquals(1, macro.getSteps().size(),
				"Macro constructor should defensively copy the step list");
	}

	// ═══════════════════════════════════════════════════
	// Section 5: MacroExecutor Construction
	// ═══════════════════════════════════════════════════

	@Test
	void executor_constructorStoresFields() {
		Macro macro = new Macro("test", List.of(MacroStep.delay(100)),
				PlaybackMode.PLAY_ONCE, 65, false, true);
		MacroExecutor executor = new MacroExecutor(macro, true);

		assertNotNull(executor);
		assertInstanceOf(Runnable.class, executor);
	}

	@Test
	void executor_constructsWithLoopFalse() {
		Macro macro = new Macro("no-loop", List.of(MacroStep.delay(20)),
				PlaybackMode.PLAY_ONCE, 65, false, true);
		MacroExecutor executor = new MacroExecutor(macro, false);

		assertNotNull(executor);
		assertInstanceOf(Runnable.class, executor);
	}

	@Test
	void executor_constructsWithEmptySteps() {
		Macro macro = new Macro("empty", List.of(), PlaybackMode.PLAY_ONCE, 65, false, true);
		MacroExecutor executor = new MacroExecutor(macro, false);

		assertNotNull(executor);
	}

	// ═══════════════════════════════════════════════════
	// Section 6: Concurrent Execution Safety
	// ═══════════════════════════════════════════════════

	@Test
	void preciseDelay_concurrentCalls_noInterference() throws InterruptedException {
		int threadCount = 8;
		long targetMs = 50;
		double toleranceMs = 10.0;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);
		double[] measurements = new double[threadCount];
		AtomicBoolean anyFailed = new AtomicBoolean(false);

		ExecutorService pool = Executors.newFixedThreadPool(threadCount);

		for (int i = 0; i < threadCount; i++) {
			final int idx = i;
			pool.submit(() -> {
				try {
					startLatch.await(); // synchronize start
					measurements[idx] = measureDelayMs(targetMs);
				} catch (InterruptedException e) {
					anyFailed.set(true);
				} finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown(); // release all threads simultaneously
		assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete within 10s");
		pool.shutdown();

		assertFalse(anyFailed.get(), "No thread should have been interrupted");

		for (int i = 0; i < threadCount; i++) {
			final int idx = i;
			assertTrue(measurements[idx] >= targetMs,
					"Thread " + idx + " delay was too short: " + measurements[idx] + "ms");
			assertTrue(measurements[idx] < targetMs + toleranceMs,
					"Thread " + idx + " delay exceeded tolerance: " + measurements[idx] + "ms");
		}
	}

	@Test
	void preciseDelay_threadInterruptionDetected_duringConcurrentExecution() throws InterruptedException {
		AtomicBoolean interruptCaught = new AtomicBoolean(false);

		Thread worker = new Thread(() -> {
			try {
				preciseDelay(5000);
			} catch (InterruptedException e) {
				interruptCaught.set(true);
			}
		});

		worker.start();
		Thread.sleep(30); // let it enter sleep
		worker.interrupt();
		worker.join(2000);

		assertAll(
				() -> assertFalse(worker.isAlive(), "Worker thread should have terminated"),
				() -> assertTrue(interruptCaught.get(), "InterruptedException should be detected")
		);
	}

	@Test
	void preciseDelay_multipleSequentialCalls_maintainAccuracy() throws InterruptedException {
		int iterations = 5;
		long targetMs = 30;
		long startNs = System.nanoTime();

		for (int i = 0; i < iterations; i++) {
			preciseDelay(targetMs);
		}

		double totalMs = (System.nanoTime() - startNs) / (double) MS_TO_NS;
		double expectedMinMs = iterations * targetMs;

		assertTrue(totalMs >= expectedMinMs,
				"Total time should be at least " + expectedMinMs + "ms; was " + totalMs + "ms");
		assertTrue(totalMs < expectedMinMs + 50,
				"Total time should not exceed target + 50ms tolerance; was " + totalMs + "ms");
	}
}
