package com.namm.executor;

import com.namm.NammMod;
import com.namm.input.InputSimulator;
import com.namm.model.Macro;
import com.namm.ui.ToastManager;
import net.minecraft.client.Minecraft;

import java.util.concurrent.*;

public class MacroPlaybackState {
	private static final MacroPlaybackState INSTANCE = new MacroPlaybackState();

	private final ConcurrentHashMap<String, Future<?>> activeMacros = new ConcurrentHashMap<>();
	private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "NAMM-MacroExecutor");
		t.setDaemon(true);
		return t;
	});

	public static MacroPlaybackState getInstance() {
		return INSTANCE;
	}

	public void startMacro(Macro macro, boolean loop) {
		if (macro.getSteps().isEmpty()) {
			return;
		}

		String name = macro.getName();

		// Stop any existing execution of this macro
		stopMacro(name);

		MacroExecutor task = new MacroExecutor(macro, loop);
		Future<?> future = executor.submit(task);
		activeMacros.put(name, future);
		NammMod.LOGGER.debug("Started macro: {} (loop={})", name, loop);
		ToastManager.get().post(macro.getName() + " enabled", ToastManager.ToastType.SUCCESS, ToastManager.Category.MACRO_TOGGLED);
	}

	public void stopMacro(String name) {
		Future<?> future = activeMacros.remove(name);
		if (future != null) {
			future.cancel(true);
			NammMod.LOGGER.debug("Stopped macro: {}", name);
			ToastManager.get().post(name + " disabled", ToastManager.ToastType.INFO, ToastManager.Category.MACRO_TOGGLED);
		}
	}

	public boolean isRunning(String name) {
		Future<?> future = activeMacros.get(name);
		return future != null && !future.isDone();
	}

	public void stopAll() {
		for (String name : activeMacros.keySet()) {
			stopMacro(name);
		}
		// Ensure all keys are released after stopping all macros
		try {
			Minecraft.getInstance().execute(InputSimulator::releaseAll);
		} catch (Exception ignored) {}
		NammMod.LOGGER.debug("Stopped all macros");
	}
}
