package com.namm.executor;

import com.namm.NammMod;
import com.namm.input.InputSimulator;
import com.namm.model.Macro;
import com.namm.ui.ToastManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MacroPlaybackState {
	private static final MacroPlaybackState INSTANCE = new MacroPlaybackState();

	private final ConcurrentHashMap<String, Future<?>> activeMacros = new ConcurrentHashMap<>();
	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
	private final ExecutorService executor = new ThreadPoolExecutor(
		2, 8,
		30L, TimeUnit.SECONDS,
		new SynchronousQueue<>(),
		r -> {
			Thread t = new Thread(r, "NAMM-Macro-" + THREAD_COUNTER.getAndIncrement());
			t.setDaemon(true);
			t.setPriority(Thread.MAX_PRIORITY);
			return t;
		}
	);

	public static MacroPlaybackState getInstance() {
		return INSTANCE;
	}

	public void startMacro(Macro macro, boolean loop) {
		if (macro.getSteps().isEmpty()) {
			return;
		}

		String name = macro.getName();

		// Stop any existing execution of this macro silently to avoid duplicate toasts
		stopInternal(name, true);

		MacroExecutor task = new MacroExecutor(macro, loop);
		Future<?> future = executor.submit(task);
		activeMacros.put(name, future);
		NammMod.LOGGER.debug("Started macro: {} (loop={})", name, loop);
		ToastManager.get().post(macro.getName() + " enabled", ToastManager.ToastType.SUCCESS, ToastManager.Category.MACRO_TOGGLED);
	}

	public void stopMacro(String name) {
		stopInternal(name, false);
	}

	private void stopInternal(String name, boolean silent) {
		Future<?> future = activeMacros.remove(name);
		if (future != null) {
			future.cancel(true);
			NammMod.LOGGER.debug("Stopped macro: {}", name);
			if (!silent) {
				ToastManager.get().post(name + " disabled", ToastManager.ToastType.INFO, ToastManager.Category.MACRO_TOGGLED);
			}
		}
	}

	public boolean isRunning(String name) {
		Future<?> future = activeMacros.get(name);
		return future != null && !future.isDone();
	}

	public void stopAll() {
		for (String name : new ArrayList<>(activeMacros.keySet())) {
			stopMacro(name);
		}
		// Ensure all keys are released after stopping all macros
		try {
			Minecraft.getInstance().execute(InputSimulator::releaseAll);
		} catch (Exception ignored) {}
		NammMod.LOGGER.debug("Stopped all macros");
	}
}
