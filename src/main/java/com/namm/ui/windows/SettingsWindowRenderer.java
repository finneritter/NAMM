package com.namm.ui.windows;

import com.namm.config.NammConfig;
import com.namm.ui.NammRenderer;
import com.namm.ui.NammTheme;
import com.namm.ui.NammWindow;
import com.namm.ui.WindowContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Content renderer for the settings window.
 * Provides accent color, array list, and info bar configuration.
 */
public class SettingsWindowRenderer implements WindowContent {
	private static final int ROW_HEIGHT = NammWindow.ROW_HEIGHT;
	private static final int ROW_COUNT = 8;

	private static final String[] ACCENT_COLORS = {"purple", "blue", "green", "red", "orange", "white"};
	private static final String[] POSITIONS = {"top_right", "top_left", "bottom_right", "bottom_left"};

	private int renderX, renderY, renderWidth;
	private boolean notificationSettingsRequested = false;

	@Override
	public int getContentHeight() {
		return ROW_COUNT * ROW_HEIGHT + 4;
	}

	@Override
	public void render(GuiGraphics g, int x, int y, int width, int mouseX, int mouseY, float delta) {
		this.renderX = x;
		this.renderY = y;
		this.renderWidth = width;

		NammConfig cfg = NammConfig.getInstance();
		NammTheme t = NammTheme.get();

		// Row 0: Accent Color
		drawSettingsRow(g, x, y, width, 0, mouseX, mouseY, "Accent Color", capitalize(cfg.getAccentColor()));

		// Row 1: Array List toggle
		drawSettingsRow(g, x, y, width, 1, mouseX, mouseY, "Array List", cfg.isArrayListEnabled() ? "ON" : "OFF");

		// Row 2: Position
		drawSettingsRow(g, x, y, width, 2, mouseX, mouseY, "Position", formatPosition(cfg.getArrayListPosition()));

		// Row 3: Show Macros
		drawSettingsRow(g, x, y, width, 3, mouseX, mouseY, "Show Macros", cfg.isArrayListShowMacros() ? "ON" : "OFF");

		// Row 4: Show Commands
		drawSettingsRow(g, x, y, width, 4, mouseX, mouseY, "Show Commands", cfg.isArrayListShowChatCommands() ? "ON" : "OFF");

		// Row 5: Info Bar
		drawSettingsRow(g, x, y, width, 5, mouseX, mouseY, "Info Bar", "always".equals(cfg.getInfoBarVisibility()) ? "Always" : "Menu");

		// Row 6: Target HUD toggle
		drawSettingsRow(g, x, y, width, 6, mouseX, mouseY, "Target HUD", cfg.isTargetHudEnabled() ? "ON" : "OFF");

		// Row 7: Notifications...
		int rowY = y + 7 * ROW_HEIGHT;
		boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
		NammRenderer.drawRow(g, x, rowY, width, ROW_HEIGHT, hovered);
		NammRenderer.drawText(g, x + 4, rowY + 4, "Notifications...", true);
	}

	private void drawSettingsRow(GuiGraphics g, int x, int y, int width, int row, int mouseX, int mouseY,
								  String label, String value) {
		int rowY = y + row * ROW_HEIGHT;
		boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
		NammRenderer.drawRow(g, x, rowY, width, ROW_HEIGHT, hovered);
		NammRenderer.drawText(g, x + 4, rowY + 4, label, true);
		NammRenderer.drawTextRight(g, x + width - 4, rowY + 4, value, false);
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0) return true;

		NammConfig cfg = NammConfig.getInstance();
		int row = (mouseY - renderY) / ROW_HEIGHT;

		switch (row) {
			case 0 -> { // Accent Color - cycle
				String current = cfg.getAccentColor();
				int idx = 0;
				for (int i = 0; i < ACCENT_COLORS.length; i++) {
					if (ACCENT_COLORS[i].equals(current)) { idx = i; break; }
				}
				cfg.setAccentColor(ACCENT_COLORS[(idx + 1) % ACCENT_COLORS.length]);
				cfg.save();
			}
			case 1 -> { // Array List toggle
				cfg.setArrayListEnabled(!cfg.isArrayListEnabled());
				cfg.save();
			}
			case 2 -> { // Position - cycle
				String current = cfg.getArrayListPosition();
				int idx = 0;
				for (int i = 0; i < POSITIONS.length; i++) {
					if (POSITIONS[i].equals(current)) { idx = i; break; }
				}
				cfg.setArrayListPosition(POSITIONS[(idx + 1) % POSITIONS.length]);
				cfg.save();
			}
			case 3 -> { // Show Macros toggle
				cfg.setArrayListShowMacros(!cfg.isArrayListShowMacros());
				cfg.save();
			}
			case 4 -> { // Show Commands toggle
				cfg.setArrayListShowChatCommands(!cfg.isArrayListShowChatCommands());
				cfg.save();
			}
			case 5 -> { // Info Bar visibility toggle
				String v = cfg.getInfoBarVisibility();
				cfg.setInfoBarVisibility("always".equals(v) ? "menu_only" : "always");
				cfg.save();
			}
			case 6 -> { // Target HUD toggle
				cfg.setTargetHudEnabled(!cfg.isTargetHudEnabled());
				cfg.save();
			}
			case 7 -> { // Notifications...
				notificationSettingsRequested = true;
			}
		}
		return true;
	}

	public boolean isNotificationSettingsRequested() {
		if (notificationSettingsRequested) {
			notificationSettingsRequested = false;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(int x, int y, int button) { return false; }

	@Override
	public boolean mouseScrolled(int x, int y, double amount) { return false; }

	@Override
	public boolean mouseDragged(int x, int y, int button, double deltaX, double deltaY) { return false; }

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

	@Override
	public boolean charTyped(char chr, int modifiers) { return false; }

	@Override
	public void onCollapseChanged(Screen parentScreen, boolean collapsed) { }

	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private static String formatPosition(String pos) {
		return switch (pos) {
			case "top_right" -> "Top Right";
			case "top_left" -> "Top Left";
			case "bottom_right" -> "Bottom Right";
			case "bottom_left" -> "Bottom Left";
			default -> "Top Right";
		};
	}
}
