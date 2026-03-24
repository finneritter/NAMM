package com.namm.executor;

import com.namm.NammMod;
import com.namm.config.NammConfig;
import com.namm.input.InputSimulator;
import com.namm.model.ActionType;
import com.namm.model.Macro;
import com.namm.model.MacroStep;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ThreadLocalRandom;

public class MacroExecutor implements Runnable {
	private static final long SLEEP_THRESHOLD_NS = 2_000_000L; // 2ms in nanos
	private static final long MS_TO_NS = 1_000_000L;
	// Max variance per level (ms): 0=0, 1=3, 2=8, 3=15, 4=25, 5=40
	private static final int[] VARIANCE_CAPS = {0, 3, 8, 15, 25, 40};

	private final Macro macro;
	private final boolean loop;

	public MacroExecutor(Macro macro, boolean loop) {
		this.macro = macro;
		this.loop = loop;
	}

	@Override
	public void run() {
		MacroStep[] steps = macro.getSteps().toArray(new MacroStep[0]);
		try {
			do {
				for (MacroStep step : steps) {
					if (Thread.interrupted()) return;

					if (step.getActionType() == ActionType.DELAY) {
						long base = Math.max(20, step.getDelayMs());
						int level = NammConfig.getInstance().getDelayVariability();
						if (level > 0 && level < VARIANCE_CAPS.length) {
							base += ThreadLocalRandom.current().nextInt(VARIANCE_CAPS[level] + 1);
						}
						preciseDelay(base);
						continue;
					}

					if (Thread.interrupted()) return;

					// Execute input on render thread, blocking until complete
					Minecraft.getInstance().executeBlocking(() -> InputSimulator.simulate(step));

					if (Thread.interrupted()) return;
				}
			} while (loop && !Thread.interrupted());
		} catch (InterruptedException e) {
			// Normal cancellation
		} catch (Exception e) {
			NammMod.LOGGER.error("Error executing macro: {}", macro.getName(), e);
		} finally {
			// CRITICAL: Release all keys that were pressed during this macro
			// to prevent stuck keys when the macro is interrupted mid-execution
			try {
				Minecraft.getInstance().execute(InputSimulator::releaseAll);
			} catch (Exception ignored) {
				// Game might be shutting down
			}
		}
	}

	private static void preciseDelay(long delayMs) throws InterruptedException {
		long deadlineNs = System.nanoTime() + delayMs * MS_TO_NS;
		long remainingNs = deadlineNs - System.nanoTime();

		// Coarse sleep phase: sleep until we're within 2ms of the deadline
		if (remainingNs > SLEEP_THRESHOLD_NS) {
			Thread.sleep((remainingNs - SLEEP_THRESHOLD_NS) / MS_TO_NS);
		}

		// Busy-wait phase: spin for the final stretch with nanoTime precision
		while ((remainingNs = deadlineNs - System.nanoTime()) > 0) {
			if (Thread.interrupted()) throw new InterruptedException();
			Thread.onSpinWait();
		}
	}
}
