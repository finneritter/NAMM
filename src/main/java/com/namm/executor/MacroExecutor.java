package com.namm.executor;

import com.namm.NammMod;
import com.namm.input.InputSimulator;
import com.namm.model.ActionType;
import com.namm.model.Macro;
import com.namm.model.MacroStep;
import net.minecraft.client.Minecraft;

public class MacroExecutor implements Runnable {
	private final Macro macro;
	private final boolean loop;

	public MacroExecutor(Macro macro, boolean loop) {
		this.macro = macro;
		this.loop = loop;
	}

	@Override
	public void run() {
		try {
			do {
				for (MacroStep step : macro.getSteps()) {
					if (Thread.interrupted()) return;

					if (step.getActionType() == ActionType.DELAY) {
						Thread.sleep(Math.max(20, step.getDelayMs()));
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
}
