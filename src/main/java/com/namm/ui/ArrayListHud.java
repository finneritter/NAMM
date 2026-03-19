package com.namm.ui;

import com.namm.config.NammConfig;
import com.namm.model.ChatCommand;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Hacked-client-style array list HUD showing enabled macros/commands.
 * Renders floating text sorted by width (longest first) in the accent color.
 */
public class ArrayListHud {
	private static final ArrayListHud INSTANCE = new ArrayListHud();
	public static ArrayListHud get() { return INSTANCE; }

	private static final int ROW_HEIGHT = 10;
	private static final int MARGIN = 4;

	public void render(GuiGraphics g, int screenWidth, int screenHeight) {
		NammConfig cfg = NammConfig.getInstance();
		if (!cfg.isArrayListEnabled()) return;

		List<String> entries = collectEntries(cfg);
		if (entries.isEmpty()) return;

		// Sort by text width descending (longest first)
		entries.sort((a, b) -> Integer.compare(NammRenderer.fontWidth(b), NammRenderer.fontWidth(a)));

		int accent = NammTheme.get().accent();
		String pos = cfg.getArrayListPosition();
		boolean right = pos.contains("right");
		boolean bottom = pos.contains("bottom");

		for (int i = 0; i < entries.size(); i++) {
			String name = entries.get(i);
			int textW = NammRenderer.fontWidth(name);

			int x;
			if (right) {
				x = screenWidth - textW - MARGIN;
			} else {
				x = MARGIN;
			}

			int y;
			if (bottom) {
				y = screenHeight - MARGIN - (entries.size() - i) * ROW_HEIGHT;
			} else {
				y = MARGIN + i * ROW_HEIGHT;
			}

			NammRenderer.drawTextColored(g, x, y, name, accent);
		}
	}

	private List<String> collectEntries(NammConfig cfg) {
		List<String> entries = new ArrayList<>();
		MacroProfile activeProfile = cfg.getActiveProfile();

		if (cfg.isArrayListShowMacros()) {
			for (Macro macro : cfg.getMacros()) {
				boolean isOn = activeProfile != null
						? activeProfile.isMacroActive(macro.getName())
						: macro.isEnabled();
				if (isOn) {
					entries.add(macro.getName());
				}
			}
		}

		if (cfg.isArrayListShowChatCommands()) {
			for (ChatCommand cmd : cfg.getChatCommands()) {
				if (cmd.isEnabled()) {
					entries.add(cmd.getName());
				}
			}
		}

		return entries;
	}
}
