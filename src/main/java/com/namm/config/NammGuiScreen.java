package com.namm.config;

import com.namm.model.ActionType;
import com.namm.model.ChatCommand;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import com.namm.model.MacroStep;
import com.namm.model.PlaybackMode;
import com.namm.util.KeyNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NammGuiScreen extends Screen {
	private final Screen parent;

	// Premium desaturated color scheme
	private static final int BG_OVERLAY = 0x90000000;
	private static final int WINDOW_BG = 0xF0181820;
	private static final int HEADER_BG = 0xFF3D3658;
	private static final int HEADER_TEXT = 0xFFD0CDE0;
	private static final int HOVER_BG = 0x30FFFFFF;
	private static final int TOGGLE_ON = 0xFF7C6FE0;
	private static final int TOGGLE_OFF = 0xFF3A3A42;
	private static final int TEXT_PRIMARY = 0xFFCCCCD0;
	private static final int TEXT_SECONDARY = 0xFF6E6E78;
	private static final int ACCENT = 0xFF7C6FE0;
	private static final int DESTRUCTIVE = 0xFFD45555;
	private static final int BORDER = 0xFF2C2C38;
	private static final int SEPARATOR = 0x18FFFFFF;

	// Layout
	private static final int MACRO_WIN_WIDTH = 160;
	private static final int PROFILE_WIN_WIDTH = 160;
	private static final int EDITOR_WIN_WIDTH = 220;
	private static final int CHAT_WIN_WIDTH = 180;
	private static final int HEADER_HEIGHT = 18;
	private static final int ROW_HEIGHT = 16;
	private static final int CORNER_RADIUS = 3;
	private static final int WINDOW_MAX_HEIGHT = 300;

	// Window positions
	private int macroWinX, macroWinY;
	private int profileWinX, profileWinY;
	private int editorWinX, editorWinY;
	private int chatWinX, chatWinY;

	// Collapsed state
	private boolean macroCollapsed = false;
	private boolean profileCollapsed = false;
	private boolean editorCollapsed = false;
	private boolean chatCollapsed = false;

	// Scrolling
	private double macroScroll = 0;
	private double profileScroll = 0;
	private double editorScroll = 0;
	private double chatScroll = 0;

	// Dragging
	private int draggingWindow = -1; // -1=none, 0=macros, 1=profiles, 2=editor, 3=chat
	private double dragOffsetX, dragOffsetY;
	private boolean didDrag = false;

	// Context menu
	private int contextMenuIndex = -1;
	private int contextMenuX, contextMenuY;
	private boolean contextMenuIsProfile = false;

	// Profile creation
	private boolean creatingProfile = false;
	private EditBox profileNameBox;

	// Profile dropdowns
	private final Set<String> expandedProfiles = new HashSet<>();

	// Editor state
	private Macro editingMacro = null;
	private int editorRecordingIndex = -1;
	private int editorDelayIndex = -1;
	private EditBox editorDelayBox = null;
	private EditBox editorNameBox = null;
	private boolean editorAddPopup = false;

	// Chat command editor state
	private ChatCommand editingChatCommand = null;
	private EditBox chatNameBox = null;
	private EditBox chatMessageBox = null;
	private boolean contextMenuIsChatCommand = false;

	public NammGuiScreen(Screen parent) {
		super(Component.literal("NAMM"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		// Load persisted positions
		NammConfig cfg = NammConfig.getInstance();
		macroWinX = cfg.getMacroWinX();
		macroWinY = cfg.getMacroWinY();
		profileWinX = cfg.getProfileWinX();
		profileWinY = cfg.getProfileWinY();
		editorWinX = cfg.getEditorWinX();
		editorWinY = cfg.getEditorWinY();
		chatWinX = cfg.getChatWinX();
		chatWinY = cfg.getChatWinY();

		// Auto-position if -1
		if (macroWinX < 0 || macroWinY < 0) {
			macroWinX = this.width / 4 - MACRO_WIN_WIDTH / 2;
			macroWinY = 30;
		}
		if (profileWinX < 0 || profileWinY < 0) {
			profileWinX = 3 * this.width / 4 - PROFILE_WIN_WIDTH / 2;
			profileWinY = 30;
		}
		if (editorWinX < 0 || editorWinY < 0) {
			editorWinX = (this.width - EDITOR_WIN_WIDTH) / 2;
			editorWinY = 30;
		}
		if (chatWinX < 0 || chatWinY < 0) {
			chatWinX = 3 * this.width / 4 - CHAT_WIN_WIDTH / 2 + PROFILE_WIN_WIDTH + 10;
			chatWinY = 30;
		}

		// Clamp to screen
		clampAllWindows();

		// Reset editing state
		macroScroll = 0;
		profileScroll = 0;
		editorScroll = 0;
		contextMenuIndex = -1;
		creatingProfile = false;
		profileNameBox = null;
		editingMacro = null;
		editorRecordingIndex = -1;
		editorDelayIndex = -1;
		editorDelayBox = null;
		editorNameBox = null;
		editorAddPopup = false;
		chatScroll = 0;
		editingChatCommand = null;
		chatNameBox = null;
		chatMessageBox = null;
		didDrag = false;
	}

	private void clampAllWindows() {
		macroWinX = clamp(macroWinX, 0, this.width - MACRO_WIN_WIDTH);
		macroWinY = clamp(macroWinY, 0, this.height - HEADER_HEIGHT);
		profileWinX = clamp(profileWinX, 0, this.width - PROFILE_WIN_WIDTH);
		profileWinY = clamp(profileWinY, 0, this.height - HEADER_HEIGHT);
		editorWinX = clamp(editorWinX, 0, this.width - EDITOR_WIN_WIDTH);
		editorWinY = clamp(editorWinY, 0, this.height - HEADER_HEIGHT);
		chatWinX = clamp(chatWinX, 0, this.width - CHAT_WIN_WIDTH);
		chatWinY = clamp(chatWinY, 0, this.height - HEADER_HEIGHT);
	}

	private int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(val, max));
	}

	// --- Rendering ---

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// Semi-transparent overlay (blur removed — conflicts with Minecraft's frame blur)
		g.fill(0, 0, this.width, this.height, BG_OVERLAY);

		// Render windows
		renderMacroWindow(g, mouseX, mouseY);
		renderProfileWindow(g, mouseX, mouseY);

		if (editingMacro != null) {
			renderEditorWindow(g, mouseX, mouseY, delta);
		}

		renderChatWindow(g, mouseX, mouseY, delta);

		renderImportExport(g, mouseX, mouseY);

		// Profile name creation box
		if (creatingProfile && profileNameBox != null) {
			profileNameBox.render(g, mouseX, mouseY, delta);
		}

		// Context menu on top
		if (contextMenuIndex >= 0) {
			renderContextMenu(g, mouseX, mouseY);
		}
	}

	// --- Rounded rectangle helpers ---

	private void drawRoundedRect(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x + CORNER_RADIUS, y, x + w - CORNER_RADIUS, y + CORNER_RADIUS, color);
		g.fill(x, y + CORNER_RADIUS, x + w, y + h - CORNER_RADIUS, color);
		g.fill(x + CORNER_RADIUS, y + h - CORNER_RADIUS, x + w - CORNER_RADIUS, y + h, color);
		g.fill(x + 1, y + 1, x + CORNER_RADIUS, y + CORNER_RADIUS, color);
		g.fill(x + w - CORNER_RADIUS, y + 1, x + w - 1, y + CORNER_RADIUS, color);
		g.fill(x + 1, y + h - CORNER_RADIUS, x + CORNER_RADIUS, y + h - 1, color);
		g.fill(x + w - CORNER_RADIUS, y + h - CORNER_RADIUS, x + w - 1, y + h - 1, color);
	}

	private void drawRoundedRectTop(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x + CORNER_RADIUS, y, x + w - CORNER_RADIUS, y + CORNER_RADIUS, color);
		g.fill(x, y + CORNER_RADIUS, x + w, y + h, color);
		g.fill(x + 1, y + 1, x + CORNER_RADIUS, y + CORNER_RADIUS, color);
		g.fill(x + w - CORNER_RADIUS, y + 1, x + w - 1, y + CORNER_RADIUS, color);
	}

	private void drawRoundedRectBottom(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + h - CORNER_RADIUS, color);
		g.fill(x + CORNER_RADIUS, y + h - CORNER_RADIUS, x + w - CORNER_RADIUS, y + h, color);
		g.fill(x + 1, y + h - CORNER_RADIUS, x + CORNER_RADIUS, y + h - 1, color);
		g.fill(x + w - CORNER_RADIUS, y + h - CORNER_RADIUS, x + w - 1, y + h - 1, color);
	}

	// --- Macro Window ---

	private int getMacroWindowHeight() {
		if (macroCollapsed) return HEADER_HEIGHT;
		List<Macro> macros = NammConfig.getInstance().getMacros();
		int contentRows = macros.size() + 1; // +1 for "+ New Macro"
		int contentHeight = contentRows * ROW_HEIGHT + 4;
		return Math.min(WINDOW_MAX_HEIGHT, HEADER_HEIGHT + contentHeight);
	}

	private void renderMacroWindow(GuiGraphics g, int mouseX, int mouseY) {
		int winW = MACRO_WIN_WIDTH;
		int winH = getMacroWindowHeight();
		String arrow = macroCollapsed ? "\u25B6" : "\u25BC";

		// Header
		if (macroCollapsed) {
			drawRoundedRect(g, macroWinX, macroWinY, winW, HEADER_HEIGHT, HEADER_BG);
		} else {
			drawRoundedRectTop(g, macroWinX, macroWinY, winW, HEADER_HEIGHT, HEADER_BG);
		}
		g.drawString(this.font, arrow, macroWinX + 4, macroWinY + 5, HEADER_TEXT);
		g.drawCenteredString(this.font, "Macros", macroWinX + winW / 2, macroWinY + 5, HEADER_TEXT);

		if (macroCollapsed) return;

		// Body
		drawRoundedRectBottom(g, macroWinX, macroWinY + HEADER_HEIGHT, winW, winH - HEADER_HEIGHT, WINDOW_BG);

		int contentTop = macroWinY + HEADER_HEIGHT;
		int contentBottom = macroWinY + winH;
		g.enableScissor(macroWinX, contentTop, macroWinX + winW, contentBottom);

		List<Macro> macros = NammConfig.getInstance().getMacros();
		MacroProfile activeProfile = NammConfig.getInstance().getActiveProfile();

		for (int i = 0; i < macros.size(); i++) {
			Macro macro = macros.get(i);
			int rowY = contentTop + (i * ROW_HEIGHT) - (int) macroScroll;
			if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) continue;

			boolean isOn = activeProfile != null ? activeProfile.isMacroActive(macro.getName()) : macro.isEnabled();

			// Hover
			if (mouseX >= macroWinX && mouseX < macroWinX + winW && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
					&& mouseY >= contentTop && mouseY < contentBottom) {
				g.fill(macroWinX + 1, rowY, macroWinX + winW - 1, rowY + ROW_HEIGHT, HOVER_BG);
			}

			// Toggle indicator
			int toggleColor = isOn ? TOGGLE_ON : TOGGLE_OFF;
			g.fill(macroWinX + 3, rowY + 3, macroWinX + 6, rowY + ROW_HEIGHT - 3, toggleColor);

			// Macro name
			String name = macro.getName();
			int maxNameW = winW - 48;
			if (this.font.width(name) > maxNameW) {
				while (this.font.width(name + "..") > maxNameW && name.length() > 1)
					name = name.substring(0, name.length() - 1);
				name += "..";
			}
			g.drawString(this.font, name, macroWinX + 10, rowY + 4, isOn ? TEXT_PRIMARY : TEXT_SECONDARY);

			// Trigger key
			String triggerName = macro.getTriggerKeyCode() == -1 ? ""
					: KeyNames.getKeyName(macro.getTriggerKeyCode(), macro.isTriggerMouse());
			if (!triggerName.isEmpty()) {
				int tw = this.font.width(triggerName);
				g.drawString(this.font, triggerName, macroWinX + winW - tw - 5, rowY + 4, TEXT_SECONDARY);
			}

			// Separator
			if (i < macros.size() - 1) {
				g.fill(macroWinX + 8, rowY + ROW_HEIGHT - 1, macroWinX + winW - 8, rowY + ROW_HEIGHT, SEPARATOR);
			}
		}

		// "+ New Macro"
		int newY = contentTop + (macros.size() * ROW_HEIGHT) - (int) macroScroll;
		if (newY + ROW_HEIGHT >= contentTop && newY < contentBottom) {
			if (mouseX >= macroWinX && mouseX < macroWinX + winW && mouseY >= newY && mouseY < newY + ROW_HEIGHT
					&& mouseY >= contentTop && mouseY < contentBottom) {
				g.fill(macroWinX + 1, newY, macroWinX + winW - 1, newY + ROW_HEIGHT, HOVER_BG);
			}
			g.drawCenteredString(this.font, "+ New Macro", macroWinX + winW / 2, newY + 4, TEXT_SECONDARY);
		}

		g.disableScissor();
	}

	// --- Profile Window ---

	private int getProfileContentRows() {
		List<MacroProfile> profiles = NammConfig.getInstance().getProfiles();
		int rows = profiles.size() + 1; // +1 for "+ New Profile"
		if (creatingProfile) rows++;
		// Expanded profiles add macro rows
		List<Macro> allMacros = NammConfig.getInstance().getMacros();
		for (MacroProfile profile : profiles) {
			if (expandedProfiles.contains(profile.getName())) {
				rows += allMacros.size();
			}
		}
		return rows;
	}

	private int getProfileWindowHeight() {
		if (profileCollapsed) return HEADER_HEIGHT;
		int contentHeight = getProfileContentRows() * ROW_HEIGHT + 4;
		return Math.min(WINDOW_MAX_HEIGHT, HEADER_HEIGHT + contentHeight);
	}

	private void renderProfileWindow(GuiGraphics g, int mouseX, int mouseY) {
		int winW = PROFILE_WIN_WIDTH;
		int winH = getProfileWindowHeight();
		String arrow = profileCollapsed ? "\u25B6" : "\u25BC";

		if (profileCollapsed) {
			drawRoundedRect(g, profileWinX, profileWinY, winW, HEADER_HEIGHT, HEADER_BG);
		} else {
			drawRoundedRectTop(g, profileWinX, profileWinY, winW, HEADER_HEIGHT, HEADER_BG);
		}
		g.drawString(this.font, arrow, profileWinX + 4, profileWinY + 5, HEADER_TEXT);
		g.drawCenteredString(this.font, "Profiles", profileWinX + winW / 2, profileWinY + 5, HEADER_TEXT);

		if (profileCollapsed) return;

		drawRoundedRectBottom(g, profileWinX, profileWinY + HEADER_HEIGHT, winW, winH - HEADER_HEIGHT, WINDOW_BG);

		int contentTop = profileWinY + HEADER_HEIGHT;
		int contentBottom = profileWinY + winH;
		g.enableScissor(profileWinX, contentTop, profileWinX + winW, contentBottom);

		List<MacroProfile> profiles = NammConfig.getInstance().getProfiles();
		List<Macro> allMacros = NammConfig.getInstance().getMacros();
		String activeName = NammConfig.getInstance().getActiveProfileName();

		int rowIndex = 0;

		// Profile creation row
		if (creatingProfile) {
			int boxY = contentTop + (rowIndex * ROW_HEIGHT) - (int) profileScroll;
			if (boxY + ROW_HEIGHT >= contentTop && boxY < contentBottom) {
				g.fill(profileWinX + 1, boxY, profileWinX + winW - 1, boxY + ROW_HEIGHT, HOVER_BG);
			}
			rowIndex++;
		}

		for (int i = 0; i < profiles.size(); i++) {
			MacroProfile profile = profiles.get(i);
			boolean isActive = profile.getName().equals(activeName);
			boolean isExpanded = expandedProfiles.contains(profile.getName());
			int rowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) profileScroll;
			rowIndex++;

			if (rowY + ROW_HEIGHT >= contentTop && rowY <= contentBottom) {
				// Hover
				if (mouseX >= profileWinX && mouseX < profileWinX + winW && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
						&& mouseY >= contentTop && mouseY < contentBottom) {
					g.fill(profileWinX + 1, rowY, profileWinX + winW - 1, rowY + ROW_HEIGHT, HOVER_BG);
				}

				// Active indicator
				if (isActive) {
					g.fill(profileWinX + 3, rowY + 3, profileWinX + 6, rowY + ROW_HEIGHT - 3, ACCENT);
				}

				// Expand arrow
				String pArrow = isExpanded ? "\u25BC" : "\u25B6";
				g.drawString(this.font, pArrow, profileWinX + 10, rowY + 4, TEXT_SECONDARY);

				// Profile name
				g.drawString(this.font, profile.getName(), profileWinX + 20, rowY + 4, isActive ? TEXT_PRIMARY : TEXT_SECONDARY);

				if (isActive) {
					String badge = "ACTIVE";
					int bw = this.font.width(badge);
					g.drawString(this.font, badge, profileWinX + winW - bw - 5, rowY + 4, ACCENT);
				}

				// Separator
				if (i < profiles.size() - 1 || !isExpanded) {
					g.fill(profileWinX + 8, rowY + ROW_HEIGHT - 1, profileWinX + winW - 8, rowY + ROW_HEIGHT, SEPARATOR);
				}
			}

			// Expanded macro list
			if (isExpanded) {
				for (int m = 0; m < allMacros.size(); m++) {
					Macro macro = allMacros.get(m);
					int mRowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) profileScroll;
					rowIndex++;

					if (mRowY + ROW_HEIGHT < contentTop || mRowY > contentBottom) continue;

					boolean macroActive = profile.isMacroActive(macro.getName());

					// Hover
					if (mouseX >= profileWinX && mouseX < profileWinX + winW && mouseY >= mRowY && mouseY < mRowY + ROW_HEIGHT
							&& mouseY >= contentTop && mouseY < contentBottom) {
						g.fill(profileWinX + 1, mRowY, profileWinX + winW - 1, mRowY + ROW_HEIGHT, HOVER_BG);
					}

					// Checkbox
					int cbX = profileWinX + 18;
					int cbY = mRowY + 3;
					int cbSize = 10;
					g.renderOutline(cbX, cbY, cbSize, cbSize, macroActive ? TOGGLE_ON : TOGGLE_OFF);
					if (macroActive) {
						g.fill(cbX + 2, cbY + 2, cbX + cbSize - 2, cbY + cbSize - 2, TOGGLE_ON);
					}

					// Macro name (indented)
					String mName = macro.getName();
					int maxMNameW = winW - 40;
					if (this.font.width(mName) > maxMNameW) {
						while (this.font.width(mName + "..") > maxMNameW && mName.length() > 1)
							mName = mName.substring(0, mName.length() - 1);
						mName += "..";
					}
					g.drawString(this.font, mName, profileWinX + 32, mRowY + 4, macroActive ? TEXT_PRIMARY : TEXT_SECONDARY);
				}
			}
		}

		// "+ New Profile"
		int newY = contentTop + (rowIndex * ROW_HEIGHT) - (int) profileScroll;
		if (newY + ROW_HEIGHT >= contentTop && newY < contentBottom) {
			if (mouseX >= profileWinX && mouseX < profileWinX + winW && mouseY >= newY && mouseY < newY + ROW_HEIGHT
					&& mouseY >= contentTop && mouseY < contentBottom) {
				g.fill(profileWinX + 1, newY, profileWinX + winW - 1, newY + ROW_HEIGHT, HOVER_BG);
			}
			g.drawCenteredString(this.font, "+ New Profile", profileWinX + winW / 2, newY + 4, TEXT_SECONDARY);
		}

		g.disableScissor();
	}

	// --- Editor Window ---

	private int getEditorWindowHeight() {
		if (editorCollapsed || editingMacro == null) return HEADER_HEIGHT;
		// Row 1: Name box (20), Row 2: Mode + Enabled (20), Row 3: Trigger (20)
		// Gap, then step list + add step button
		int fixedHeight = HEADER_HEIGHT + 4 + 20 + 2 + 20 + 2 + 20 + 4;
		int stepRows = editingMacro.getSteps().size();
		int stepAreaHeight = (stepRows + 1) * ROW_HEIGHT + 4; // +1 for "Add Step"
		int totalContent = fixedHeight + stepAreaHeight;
		return Math.min(WINDOW_MAX_HEIGHT, totalContent);
	}

	private void renderEditorWindow(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (editingMacro == null) return;

		int winW = EDITOR_WIN_WIDTH;
		int winH = getEditorWindowHeight();
		String arrow = editorCollapsed ? "\u25B6" : "\u25BC";

		// Header
		if (editorCollapsed) {
			drawRoundedRect(g, editorWinX, editorWinY, winW, HEADER_HEIGHT, HEADER_BG);
		} else {
			drawRoundedRectTop(g, editorWinX, editorWinY, winW, HEADER_HEIGHT, HEADER_BG);
		}
		g.drawString(this.font, arrow, editorWinX + 4, editorWinY + 5, HEADER_TEXT);

		// Title: "Edit: MacroName"
		String editTitle = "Edit: " + editingMacro.getName();
		int maxTitleW = winW - 30;
		if (this.font.width(editTitle) > maxTitleW) {
			while (this.font.width(editTitle + "..") > maxTitleW && editTitle.length() > 1)
				editTitle = editTitle.substring(0, editTitle.length() - 1);
			editTitle += "..";
		}
		g.drawCenteredString(this.font, editTitle, editorWinX + winW / 2, editorWinY + 5, HEADER_TEXT);

		// Close button (X) on right side of header
		String closeX = "X";
		int closeXW = this.font.width(closeX);
		int closeXPos = editorWinX + winW - closeXW - 5;
		boolean hoverClose = mouseX >= closeXPos - 2 && mouseX < closeXPos + closeXW + 2
				&& mouseY >= editorWinY + 2 && mouseY < editorWinY + HEADER_HEIGHT - 2;
		g.drawString(this.font, closeX, closeXPos, editorWinY + 5, hoverClose ? DESTRUCTIVE : HEADER_TEXT);

		if (editorCollapsed) return;

		// Body
		drawRoundedRectBottom(g, editorWinX, editorWinY + HEADER_HEIGHT, winW, winH - HEADER_HEIGHT, WINDOW_BG);

		int contentTop = editorWinY + HEADER_HEIGHT;
		int contentBottom = editorWinY + winH;

		// Fixed controls area (not scrolled)
		int cy = contentTop + 4;
		int cx = editorWinX + 4;
		int controlW = winW - 8;

		// Row 1: Name EditBox
		if (editorNameBox != null) {
			editorNameBox.setX(cx);
			editorNameBox.setY(cy);
			editorNameBox.setWidth(controlW);
			editorNameBox.render(g, mouseX, mouseY, delta);
		}
		cy += 22;

		// Row 2: Playback mode label + Enabled toggle
		// Playback mode (left side)
		String modeStr = editingMacro.getPlaybackMode().getDisplayName().getString();
		boolean hoverMode = mouseX >= cx && mouseX < cx + controlW / 2 - 2 && mouseY >= cy && mouseY < cy + 16;
		if (hoverMode) {
			g.fill(cx, cy, cx + controlW / 2 - 2, cy + 16, HOVER_BG);
		}
		g.drawString(this.font, modeStr, cx + 2, cy + 4, TEXT_PRIMARY);

		// Enabled toggle (right side)
		int toggleX = cx + controlW / 2 + 2;
		int toggleW = controlW / 2 - 2;
		boolean isEnabled = editingMacro.isEnabled();
		boolean hoverToggle = mouseX >= toggleX && mouseX < toggleX + toggleW && mouseY >= cy && mouseY < cy + 16;
		int toggleBg = isEnabled ? TOGGLE_ON : TOGGLE_OFF;
		if (hoverToggle) {
			g.fill(toggleX, cy, toggleX + toggleW, cy + 16, HOVER_BG);
		}
		g.fill(toggleX, cy + 2, toggleX + 4, cy + 14, toggleBg);
		g.drawString(this.font, isEnabled ? "Enabled" : "Disabled", toggleX + 6, cy + 4, isEnabled ? TEXT_PRIMARY : TEXT_SECONDARY);
		cy += 18;

		// Row 3: Trigger Key button
		String triggerLabel = editingMacro.getTriggerKeyCode() == -1 ? "Trigger: None"
				: "Trigger: " + KeyNames.getKeyName(editingMacro.getTriggerKeyCode(), editingMacro.isTriggerMouse());
		boolean hoverTrigger = mouseX >= cx && mouseX < cx + controlW && mouseY >= cy && mouseY < cy + 16;
		if (hoverTrigger) {
			g.fill(cx, cy, cx + controlW, cy + 16, HOVER_BG);
		}
		g.drawString(this.font, triggerLabel, cx + 2, cy + 4, TEXT_PRIMARY);
		cy += 20;

		// Step list (scrollable)
		int stepAreaTop = cy;
		int stepAreaBottom = contentBottom;
		g.enableScissor(editorWinX, stepAreaTop, editorWinX + winW, stepAreaBottom);

		List<MacroStep> steps = editingMacro.getSteps();
		for (int i = 0; i < steps.size(); i++) {
			int rowY = stepAreaTop + (i * ROW_HEIGHT) - (int) editorScroll;
			if (rowY + ROW_HEIGHT < stepAreaTop || rowY > stepAreaBottom) continue;

			MacroStep step = steps.get(i);

			// Recording highlight
			if (i == editorRecordingIndex) {
				g.fill(editorWinX + 1, rowY, editorWinX + winW - 1, rowY + ROW_HEIGHT, 0x303D3658);
			}

			// Delay editing highlight
			if (i == editorDelayIndex) {
				g.fill(editorWinX + 1, rowY, editorWinX + winW - 1, rowY + ROW_HEIGHT, 0x30584D3D);
			}

			// Step number
			g.drawString(this.font, (i + 1) + ".", editorWinX + 6, rowY + 4, TEXT_SECONDARY);

			// Description
			if (i == editorRecordingIndex) {
				g.drawString(this.font, "Press a key...", editorWinX + 22, rowY + 4, ACCENT);
			} else if (i == editorDelayIndex && editorDelayBox != null) {
				// EditBox renders itself
			} else {
				String desc = step.getDisplaySummary();
				int maxDescW = winW - 70;
				if (this.font.width(desc) > maxDescW) {
					while (this.font.width(desc + "..") > maxDescW && desc.length() > 1)
						desc = desc.substring(0, desc.length() - 1);
					desc += "..";
				}
				g.drawString(this.font, desc, editorWinX + 22, rowY + 4, TEXT_PRIMARY);
			}

			// Row buttons: X, down, up
			int btnRight = editorWinX + winW - 5;
			// X button
			boolean hoverX = mouseX >= btnRight - 10 && mouseX < btnRight && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
			g.drawString(this.font, "X", btnRight - 8, rowY + 4, hoverX ? DESTRUCTIVE : TEXT_SECONDARY);

			// Down arrow
			if (i < steps.size() - 1) {
				boolean hoverDown = mouseX >= btnRight - 24 && mouseX < btnRight - 14 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
				g.drawString(this.font, "\u2193", btnRight - 22, rowY + 4, hoverDown ? TEXT_PRIMARY : TEXT_SECONDARY);
			}

			// Up arrow
			if (i > 0) {
				boolean hoverUp = mouseX >= btnRight - 38 && mouseX < btnRight - 28 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
				g.drawString(this.font, "\u2191", btnRight - 36, rowY + 4, hoverUp ? TEXT_PRIMARY : TEXT_SECONDARY);
			}

			// Separator
			if (i < steps.size() - 1) {
				g.fill(editorWinX + 6, rowY + ROW_HEIGHT - 1, editorWinX + winW - 6, rowY + ROW_HEIGHT, SEPARATOR);
			}
		}

		// "+ Add Step"
		int addStepY = stepAreaTop + (steps.size() * ROW_HEIGHT) - (int) editorScroll;
		if (addStepY + ROW_HEIGHT >= stepAreaTop && addStepY < stepAreaBottom) {
			boolean hoverAdd = mouseX >= editorWinX && mouseX < editorWinX + winW
					&& mouseY >= addStepY && mouseY < addStepY + ROW_HEIGHT
					&& mouseY >= stepAreaTop && mouseY < stepAreaBottom;
			if (hoverAdd) {
				g.fill(editorWinX + 1, addStepY, editorWinX + winW - 1, addStepY + ROW_HEIGHT, HOVER_BG);
			}
			g.drawCenteredString(this.font, "+ Add Step", editorWinX + winW / 2, addStepY + 4, TEXT_SECONDARY);
		}

		g.disableScissor();

		// Render delay edit box on top
		if (editorDelayIndex >= 0 && editorDelayBox != null) {
			editorDelayBox.render(g, mouseX, mouseY, delta);
		}

		// Add step popup
		if (editorAddPopup) {
			int popupW = 80;
			int popupH = 36;
			int popupX = editorWinX + (winW - popupW) / 2;
			int popupY = addStepY - popupH - 2;
			if (popupY < stepAreaTop) {
				popupY = addStepY + ROW_HEIGHT + 2;
			}
			drawRoundedRect(g, popupX, popupY, popupW, popupH, WINDOW_BG);
			g.renderOutline(popupX, popupY, popupW, popupH, BORDER);

			boolean hoverInput = mouseX >= popupX && mouseX < popupX + popupW
					&& mouseY >= popupY && mouseY < popupY + popupH / 2;
			boolean hoverDelay = mouseX >= popupX && mouseX < popupX + popupW
					&& mouseY >= popupY + popupH / 2 && mouseY < popupY + popupH;
			if (hoverInput) g.fill(popupX + 1, popupY + 1, popupX + popupW - 1, popupY + popupH / 2, HOVER_BG);
			if (hoverDelay) g.fill(popupX + 1, popupY + popupH / 2, popupX + popupW - 1, popupY + popupH - 1, HOVER_BG);

			g.drawCenteredString(this.font, "Input", popupX + popupW / 2, popupY + 5, TEXT_PRIMARY);
			g.fill(popupX + 4, popupY + popupH / 2 - 1, popupX + popupW - 4, popupY + popupH / 2, SEPARATOR);
			g.drawCenteredString(this.font, "Delay", popupX + popupW / 2, popupY + popupH / 2 + 4, TEXT_PRIMARY);
		}
	}

	// --- Chat Commands Window ---

	private int getChatWindowHeight() {
		if (chatCollapsed) return HEADER_HEIGHT;
		List<ChatCommand> commands = NammConfig.getInstance().getChatCommands();
		int rows = commands.size() + 1; // +1 for "+ New Command"
		// If editing inline, add extra rows for the edit form
		if (editingChatCommand != null) {
			rows += 4; // name, message, trigger+enabled, done button
		}
		int contentHeight = rows * ROW_HEIGHT + 4;
		return Math.min(WINDOW_MAX_HEIGHT, HEADER_HEIGHT + contentHeight);
	}

	private void renderChatWindow(GuiGraphics g, int mouseX, int mouseY, float delta) {
		int winW = CHAT_WIN_WIDTH;
		int winH = getChatWindowHeight();
		String arrow = chatCollapsed ? "\u25B6" : "\u25BC";

		if (chatCollapsed) {
			drawRoundedRect(g, chatWinX, chatWinY, winW, HEADER_HEIGHT, HEADER_BG);
		} else {
			drawRoundedRectTop(g, chatWinX, chatWinY, winW, HEADER_HEIGHT, HEADER_BG);
		}
		g.drawString(this.font, arrow, chatWinX + 4, chatWinY + 5, HEADER_TEXT);
		g.drawCenteredString(this.font, "Chat Commands", chatWinX + winW / 2, chatWinY + 5, HEADER_TEXT);

		if (chatCollapsed) return;

		drawRoundedRectBottom(g, chatWinX, chatWinY + HEADER_HEIGHT, winW, winH - HEADER_HEIGHT, WINDOW_BG);

		int contentTop = chatWinY + HEADER_HEIGHT;
		int contentBottom = chatWinY + winH;
		g.enableScissor(chatWinX, contentTop, chatWinX + winW, contentBottom);

		List<ChatCommand> commands = NammConfig.getInstance().getChatCommands();
		int rowIndex = 0;

		for (int i = 0; i < commands.size(); i++) {
			ChatCommand cmd = commands.get(i);
			int rowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) chatScroll;
			rowIndex++;

			if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) {
				// Skip rendering but still count inline editor rows
				if (editingChatCommand == cmd) rowIndex += 4;
				continue;
			}

			boolean isOn = cmd.isEnabled();

			// Hover
			if (mouseX >= chatWinX && mouseX < chatWinX + winW && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT
					&& mouseY >= contentTop && mouseY < contentBottom) {
				g.fill(chatWinX + 1, rowY, chatWinX + winW - 1, rowY + ROW_HEIGHT, HOVER_BG);
			}

			// Toggle indicator
			int toggleColor = isOn ? TOGGLE_ON : TOGGLE_OFF;
			g.fill(chatWinX + 3, rowY + 3, chatWinX + 6, rowY + ROW_HEIGHT - 3, toggleColor);

			// Command name
			String name = cmd.getName();
			int maxNameW = winW - 48;
			if (this.font.width(name) > maxNameW) {
				while (this.font.width(name + "..") > maxNameW && name.length() > 1)
					name = name.substring(0, name.length() - 1);
				name += "..";
			}
			g.drawString(this.font, name, chatWinX + 10, rowY + 4, isOn ? TEXT_PRIMARY : TEXT_SECONDARY);

			// Trigger key
			String triggerName = cmd.getTriggerKeyCode() == -1 ? ""
					: KeyNames.getKeyName(cmd.getTriggerKeyCode(), cmd.isTriggerMouse());
			if (!triggerName.isEmpty()) {
				int tw = this.font.width(triggerName);
				g.drawString(this.font, triggerName, chatWinX + winW - tw - 5, rowY + 4, TEXT_SECONDARY);
			}

			// Separator
			if (i < commands.size() - 1 && editingChatCommand != cmd) {
				g.fill(chatWinX + 8, rowY + ROW_HEIGHT - 1, chatWinX + winW - 8, rowY + ROW_HEIGHT, SEPARATOR);
			}

			// Inline editor for this command
			if (editingChatCommand == cmd) {
				int editX = chatWinX + 4;
				int editW = winW - 8;

				// Row 1: Name EditBox
				int nameRowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) chatScroll;
				rowIndex++;
				if (chatNameBox != null) {
					chatNameBox.setX(editX);
					chatNameBox.setY(nameRowY + 1);
					chatNameBox.setWidth(editW);
					chatNameBox.render(g, mouseX, mouseY, delta);
				}

				// Row 2: Message EditBox
				int msgRowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) chatScroll;
				rowIndex++;
				if (chatMessageBox != null) {
					chatMessageBox.setX(editX);
					chatMessageBox.setY(msgRowY + 1);
					chatMessageBox.setWidth(editW);
					chatMessageBox.render(g, mouseX, mouseY, delta);
				}

				// Row 3: Trigger key button + Enabled toggle
				int trigRowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) chatScroll;
				rowIndex++;
				// Trigger button (left side)
				String trigLabel = cmd.getTriggerKeyCode() == -1 ? "Trigger: None"
						: "Trigger: " + KeyNames.getKeyName(cmd.getTriggerKeyCode(), cmd.isTriggerMouse());
				boolean hoverTrig = mouseX >= editX && mouseX < editX + editW / 2 - 2
						&& mouseY >= trigRowY && mouseY < trigRowY + ROW_HEIGHT;
				if (hoverTrig) {
					g.fill(editX, trigRowY, editX + editW / 2 - 2, trigRowY + ROW_HEIGHT, HOVER_BG);
				}
				g.drawString(this.font, trigLabel, editX + 2, trigRowY + 4, TEXT_PRIMARY);

				// Enabled toggle (right side)
				int tX = editX + editW / 2 + 2;
				int tW = editW / 2 - 2;
				boolean hoverEn = mouseX >= tX && mouseX < tX + tW && mouseY >= trigRowY && mouseY < trigRowY + ROW_HEIGHT;
				int tBg = cmd.isEnabled() ? TOGGLE_ON : TOGGLE_OFF;
				if (hoverEn) {
					g.fill(tX, trigRowY, tX + tW, trigRowY + ROW_HEIGHT, HOVER_BG);
				}
				g.fill(tX, trigRowY + 2, tX + 4, trigRowY + ROW_HEIGHT - 2, tBg);
				g.drawString(this.font, cmd.isEnabled() ? "Enabled" : "Disabled", tX + 6, trigRowY + 4,
						cmd.isEnabled() ? TEXT_PRIMARY : TEXT_SECONDARY);

				// Row 4: Done button
				int doneRowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) chatScroll;
				rowIndex++;
				boolean hoverDone = mouseX >= chatWinX && mouseX < chatWinX + winW
						&& mouseY >= doneRowY && mouseY < doneRowY + ROW_HEIGHT
						&& mouseY >= contentTop && mouseY < contentBottom;
				if (hoverDone) {
					g.fill(chatWinX + 1, doneRowY, chatWinX + winW - 1, doneRowY + ROW_HEIGHT, HOVER_BG);
				}
				g.drawCenteredString(this.font, "Done", chatWinX + winW / 2, doneRowY + 4, ACCENT);
			}
		}

		// "+ New Command"
		int newY = contentTop + (rowIndex * ROW_HEIGHT) - (int) chatScroll;
		if (newY + ROW_HEIGHT >= contentTop && newY < contentBottom) {
			if (mouseX >= chatWinX && mouseX < chatWinX + winW && mouseY >= newY && mouseY < newY + ROW_HEIGHT
					&& mouseY >= contentTop && mouseY < contentBottom) {
				g.fill(chatWinX + 1, newY, chatWinX + winW - 1, newY + ROW_HEIGHT, HOVER_BG);
			}
			g.drawCenteredString(this.font, "+ New Command", chatWinX + winW / 2, newY + 4, TEXT_SECONDARY);
		}

		g.disableScissor();
	}

	// --- Import/Export ---

	private void renderImportExport(GuiGraphics g, int mouseX, int mouseY) {
		// Render as a row of buttons at the bottom of the screen
		int y = this.height - 20;
		int centerX = this.width / 2;
		String[] labels = {"Export", "Import NAMM", "Import Razer"};
		int totalW = 0;
		int gap = 8;
		int[] widths = new int[labels.length];
		for (int i = 0; i < labels.length; i++) {
			widths[i] = this.font.width(labels[i]) + 12;
			totalW += widths[i];
		}
		totalW += gap * (labels.length - 1);

		int bx = centerX - totalW / 2;
		for (int i = 0; i < labels.length; i++) {
			int bw = widths[i];
			boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= y && mouseY < y + 16;
			drawRoundedRect(g, bx, y, bw, 16, hover ? HOVER_BG : BORDER);
			g.drawCenteredString(this.font, labels[i], bx + bw / 2, y + 4, hover ? TEXT_PRIMARY : TEXT_SECONDARY);
			bx += bw + gap;
		}
	}

	// --- Context Menu ---

	private void renderContextMenu(GuiGraphics g, int mouseX, int mouseY) {
		int menuW = 70;
		int optionH = 16;
		int optionCount = contextMenuIsProfile ? 1 : 2;
		int menuH = optionCount * optionH + 4;

		int menuX = Math.max(2, Math.min(contextMenuX, this.width - menuW - 2));
		int menuY = Math.max(2, Math.min(contextMenuY, this.height - menuH - 2));

		drawRoundedRect(g, menuX, menuY, menuW, menuH, WINDOW_BG);

		if (!contextMenuIsProfile) {
			boolean hEdit = mouseX >= menuX && mouseX < menuX + menuW && mouseY >= menuY + 2 && mouseY < menuY + 2 + optionH;
			if (hEdit) g.fill(menuX + 2, menuY + 2, menuX + menuW - 2, menuY + 2 + optionH, HOVER_BG);
			g.drawString(this.font, "Edit", menuX + 8, menuY + 6, TEXT_PRIMARY);

			boolean hDel = mouseX >= menuX && mouseX < menuX + menuW && mouseY >= menuY + 2 + optionH && mouseY < menuY + menuH - 2;
			if (hDel) g.fill(menuX + 2, menuY + 2 + optionH, menuX + menuW - 2, menuY + menuH - 2, HOVER_BG);
			g.drawString(this.font, "Delete", menuX + 8, menuY + 6 + optionH, DESTRUCTIVE);
		} else {
			boolean hDel = mouseX >= menuX && mouseX < menuX + menuW && mouseY >= menuY + 2 && mouseY < menuY + menuH - 2;
			if (hDel) g.fill(menuX + 2, menuY + 2, menuX + menuW - 2, menuY + menuH - 2, HOVER_BG);
			g.drawString(this.font, "Delete", menuX + 8, menuY + 6, DESTRUCTIVE);
		}
	}

	// --- Mouse Input ---

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		int button = event.button();
		double mouseX = event.x();
		double mouseY = event.y();

		// Recording mode in editor: capture mouse
		if (editorRecordingIndex >= 0 && editingMacro != null) {
			MacroStep step = editingMacro.getSteps().get(editorRecordingIndex);
			step.setKeyCode(button);
			if (!step.isMouse()) {
				step.setMouse(true);
				if (step.getActionType() == ActionType.KEY_PRESS) {
					step.setActionType(ActionType.MOUSE_CLICK);
				} else if (step.getActionType() == ActionType.KEY_RELEASE) {
					step.setActionType(ActionType.MOUSE_RELEASE);
				}
			}
			NammConfig.getInstance().save();
			editorRecordingIndex = -1;
			return true;
		}

		// Context menu
		if (contextMenuIndex >= 0) {
			if (handleContextMenuClick(button, mouseX, mouseY)) return true;
			contextMenuIndex = -1;
			return true;
		}

		// Profile name creation
		if (creatingProfile && profileNameBox != null) {
			if (mouseX >= profileNameBox.getX() && mouseX <= profileNameBox.getX() + profileNameBox.getWidth()
					&& mouseY >= profileNameBox.getY() && mouseY <= profileNameBox.getY() + profileNameBox.getHeight()) {
				profileNameBox.setFocused(true);
				profileNameBox.mouseClicked(event, bl);
				return true;
			}
			cancelProfileCreation();
		}

		// Editor add popup
		if (editorAddPopup && editingMacro != null) {
			if (handleEditorAddPopupClick(mouseX, mouseY)) return true;
			editorAddPopup = false;
			return true;
		}

		// Editor delay editing: click away commits
		if (editorDelayIndex >= 0 && editorDelayBox != null) {
			if (!isMouseOverDelayBox(mouseX, mouseY)) {
				commitEditorDelayEdit();
			} else {
				editorDelayBox.setFocused(true);
				editorDelayBox.mouseClicked(event, bl);
				return true;
			}
		}

		// Editor name box
		if (editorNameBox != null && editingMacro != null) {
			if (mouseX >= editorNameBox.getX() && mouseX <= editorNameBox.getX() + editorNameBox.getWidth()
					&& mouseY >= editorNameBox.getY() && mouseY <= editorNameBox.getY() + editorNameBox.getHeight()) {
				editorNameBox.setFocused(true);
				editorNameBox.mouseClicked(event, bl);
				return true;
			} else {
				editorNameBox.setFocused(false);
			}
		}

		// Chat command edit boxes
		if (chatNameBox != null && editingChatCommand != null) {
			if (mouseX >= chatNameBox.getX() && mouseX <= chatNameBox.getX() + chatNameBox.getWidth()
					&& mouseY >= chatNameBox.getY() && mouseY <= chatNameBox.getY() + chatNameBox.getHeight()) {
				chatNameBox.setFocused(true);
				if (chatMessageBox != null) chatMessageBox.setFocused(false);
				chatNameBox.mouseClicked(event, bl);
				return true;
			} else {
				chatNameBox.setFocused(false);
			}
		}
		if (chatMessageBox != null && editingChatCommand != null) {
			if (mouseX >= chatMessageBox.getX() && mouseX <= chatMessageBox.getX() + chatMessageBox.getWidth()
					&& mouseY >= chatMessageBox.getY() && mouseY <= chatMessageBox.getY() + chatMessageBox.getHeight()) {
				chatMessageBox.setFocused(true);
				if (chatNameBox != null) chatNameBox.setFocused(false);
				chatMessageBox.mouseClicked(event, bl);
				return true;
			} else {
				chatMessageBox.setFocused(false);
			}
		}

		// Check window header clicks (for dragging and collapsing)
		// Editor window (check first since it may overlap)
		if (editingMacro != null && isInHeader(mouseX, mouseY, editorWinX, editorWinY, EDITOR_WIN_WIDTH)) {
			// Check close button
			String closeXStr = "X";
			int closeXW = this.font.width(closeXStr);
			int closeXPos = editorWinX + EDITOR_WIN_WIDTH - closeXW - 5;
			if (mouseX >= closeXPos - 2 && mouseX < closeXPos + closeXW + 2
					&& mouseY >= editorWinY + 2 && mouseY < editorWinY + HEADER_HEIGHT - 2) {
				closeEditor();
				return true;
			}
			// If collapsed, expand on click without starting drag
			if (editorCollapsed && button == 0) {
				editorCollapsed = false;
				if (editorNameBox != null) {
					addRenderableWidget(editorNameBox);
				}
				return true;
			}
			if (button == 0) {
				draggingWindow = 2;
				dragOffsetX = mouseX - editorWinX;
				dragOffsetY = mouseY - editorWinY;
				didDrag = false;
				return true;
			}
		}

		// Editor content clicks
		if (editingMacro != null && !editorCollapsed && button == 0) {
			if (handleEditorContentClick(mouseX, mouseY)) return true;
		}

		// Macro window header
		if (isInHeader(mouseX, mouseY, macroWinX, macroWinY, MACRO_WIN_WIDTH)) {
			if (button == 0) {
				draggingWindow = 0;
				dragOffsetX = mouseX - macroWinX;
				dragOffsetY = mouseY - macroWinY;
				didDrag = false;
				return true;
			}
		}

		// Macro window content
		if (!macroCollapsed && isInWindowContent(mouseX, mouseY, macroWinX, macroWinY, MACRO_WIN_WIDTH, getMacroWindowHeight())) {
			return handleMacroClick(button, mouseX, mouseY);
		}

		// Profile window header
		if (isInHeader(mouseX, mouseY, profileWinX, profileWinY, PROFILE_WIN_WIDTH)) {
			if (button == 0) {
				draggingWindow = 1;
				dragOffsetX = mouseX - profileWinX;
				dragOffsetY = mouseY - profileWinY;
				didDrag = false;
				return true;
			}
		}

		// Profile window content
		if (!profileCollapsed && isInWindowContent(mouseX, mouseY, profileWinX, profileWinY, PROFILE_WIN_WIDTH, getProfileWindowHeight())) {
			return handleProfileClick(button, mouseX, mouseY);
		}

		// Chat window header
		if (isInHeader(mouseX, mouseY, chatWinX, chatWinY, CHAT_WIN_WIDTH)) {
			if (button == 0) {
				draggingWindow = 3;
				dragOffsetX = mouseX - chatWinX;
				dragOffsetY = mouseY - chatWinY;
				didDrag = false;
				return true;
			}
		}

		// Chat window content
		if (!chatCollapsed && isInWindowContent(mouseX, mouseY, chatWinX, chatWinY, CHAT_WIN_WIDTH, getChatWindowHeight())) {
			return handleChatClick(button, mouseX, mouseY);
		}

		if (handleImportExportClick(mouseX, mouseY)) return true;

		return super.mouseClicked(event, bl);
	}

	private boolean isInHeader(double mx, double my, int winX, int winY, int winW) {
		return mx >= winX && mx < winX + winW && my >= winY && my < winY + HEADER_HEIGHT;
	}

	private boolean isInWindowContent(double mx, double my, int winX, int winY, int winW, int winH) {
		return mx >= winX && mx < winX + winW && my >= winY + HEADER_HEIGHT && my < winY + winH;
	}

	// --- Dragging ---

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (draggingWindow >= 0 && button == 0) {
			didDrag = true;
			int newX = (int) (mouseX - dragOffsetX);
			int newY = (int) (mouseY - dragOffsetY);

			switch (draggingWindow) {
				case 0:
					macroWinX = clamp(newX, 0, this.width - MACRO_WIN_WIDTH);
					macroWinY = clamp(newY, 0, this.height - HEADER_HEIGHT);
					break;
				case 1:
					profileWinX = clamp(newX, 0, this.width - PROFILE_WIN_WIDTH);
					profileWinY = clamp(newY, 0, this.height - HEADER_HEIGHT);
					break;
				case 2:
					editorWinX = clamp(newX, 0, this.width - EDITOR_WIN_WIDTH);
					editorWinY = clamp(newY, 0, this.height - HEADER_HEIGHT);
					break;
				case 3:
					chatWinX = clamp(newX, 0, this.width - CHAT_WIN_WIDTH);
					chatWinY = clamp(newY, 0, this.height - HEADER_HEIGHT);
					break;
			}
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (draggingWindow >= 0 && button == 0) {
			// Save positions
			NammConfig cfg = NammConfig.getInstance();
			cfg.setMacroWinPos(macroWinX, macroWinY);
			cfg.setProfileWinPos(profileWinX, profileWinY);
			cfg.setEditorWinPos(editorWinX, editorWinY);
			cfg.setChatWinPos(chatWinX, chatWinY);
			cfg.save();

			// If no drag occurred, toggle collapsed
			if (!didDrag) {
				switch (draggingWindow) {
					case 0: macroCollapsed = !macroCollapsed; break;
					case 1: profileCollapsed = !profileCollapsed; break;
					case 2:
					editorCollapsed = !editorCollapsed;
					if (editorCollapsed && editorNameBox != null) {
						removeWidget(editorNameBox);
					} else if (!editorCollapsed && editorNameBox != null) {
						addRenderableWidget(editorNameBox);
					}
					break;
				case 3: chatCollapsed = !chatCollapsed; break;
				}
			}

			draggingWindow = -1;
			return true;
		}
		return super.mouseReleased(event);
	}

	// --- Macro window click handling ---

	private boolean handleMacroClick(int button, double mx, double my) {
		int contentTop = macroWinY + HEADER_HEIGHT;
		List<Macro> macros = NammConfig.getInstance().getMacros();
		MacroProfile activeProfile = NammConfig.getInstance().getActiveProfile();

		for (int i = 0; i < macros.size(); i++) {
			int rowY = contentTop + (i * ROW_HEIGHT) - (int) macroScroll;
			if (my >= rowY && my < rowY + ROW_HEIGHT && my >= contentTop) {
				if (button == 0) {
					Macro macro = macros.get(i);
					if (activeProfile != null) {
						activeProfile.setMacroActive(macro.getName(), !activeProfile.isMacroActive(macro.getName()));
					} else {
						macro.setEnabled(!macro.isEnabled());
					}
					NammConfig.getInstance().save();
					return true;
				} else if (button == 1) {
					contextMenuIndex = i;
					contextMenuX = (int) mx;
					contextMenuY = (int) my;
					contextMenuIsProfile = false;
					contextMenuIsChatCommand = false;
					return true;
				}
			}
		}

		// "+ New Macro"
		int newY = contentTop + (macros.size() * ROW_HEIGHT) - (int) macroScroll;
		if (button == 0 && my >= newY && my < newY + ROW_HEIGHT) {
			createNewMacro();
			return true;
		}
		return true;
	}

	// --- Profile window click handling ---

	private boolean handleProfileClick(int button, double mx, double my) {
		int contentTop = profileWinY + HEADER_HEIGHT;
		List<MacroProfile> profiles = NammConfig.getInstance().getProfiles();
		List<Macro> allMacros = NammConfig.getInstance().getMacros();

		int rowIndex = 0;

		if (creatingProfile) {
			rowIndex++;
		}

		for (int i = 0; i < profiles.size(); i++) {
			MacroProfile profile = profiles.get(i);
			boolean isExpanded = expandedProfiles.contains(profile.getName());
			int rowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) profileScroll;
			rowIndex++;

			if (my >= rowY && my < rowY + ROW_HEIGHT && my >= contentTop) {
				if (button == 0) {
					// Left-click: toggle expand or activate
					// Check if clicking on the expand arrow area (left side)
					if (mx < profileWinX + 20) {
						// Toggle expand
						if (isExpanded) {
							expandedProfiles.remove(profile.getName());
						} else {
							expandedProfiles.add(profile.getName());
						}
					} else {
						// Activate/deactivate profile
						String active = NammConfig.getInstance().getActiveProfileName();
						NammConfig.getInstance().setActiveProfileName(
								profile.getName().equals(active) ? null : profile.getName());
						NammConfig.getInstance().save();
					}
					return true;
				} else if (button == 1) {
					contextMenuIndex = i;
					contextMenuX = (int) mx;
					contextMenuY = (int) my;
					contextMenuIsProfile = true;
					contextMenuIsChatCommand = false;
					return true;
				}
			}

			// Expanded macro rows
			if (isExpanded) {
				for (int m = 0; m < allMacros.size(); m++) {
					Macro macro = allMacros.get(m);
					int mRowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) profileScroll;
					rowIndex++;

					if (my >= mRowY && my < mRowY + ROW_HEIGHT && my >= contentTop && button == 0) {
						// Toggle macro in profile
						profile.setMacroActive(macro.getName(), !profile.isMacroActive(macro.getName()));
						NammConfig.getInstance().save();
						return true;
					}
				}
			}
		}

		// "+ New Profile"
		int newY = contentTop + (rowIndex * ROW_HEIGHT) - (int) profileScroll;
		if (button == 0 && my >= newY && my < newY + ROW_HEIGHT) {
			startProfileCreation();
			return true;
		}
		return true;
	}

	// --- Editor content click handling ---

	private boolean handleEditorContentClick(double mx, double my) {
		if (editingMacro == null) return false;

		int winW = EDITOR_WIN_WIDTH;
		int contentTop = editorWinY + HEADER_HEIGHT;

		// Check if click is within editor bounds
		if (mx < editorWinX || mx >= editorWinX + winW || my < contentTop || my >= editorWinY + getEditorWindowHeight()) {
			return false;
		}

		int cx = editorWinX + 4;
		int controlW = winW - 8;
		int cy = contentTop + 4;

		// Row 1: Name EditBox (handled by widget system)
		cy += 22;

		// Row 2: Playback mode (left) + Enabled (right)
		if (my >= cy && my < cy + 16) {
			if (mx >= cx && mx < cx + controlW / 2 - 2) {
				// Cycle playback mode
				PlaybackMode[] modes = PlaybackMode.values();
				int currentIdx = 0;
				for (int i = 0; i < modes.length; i++) {
					if (modes[i] == editingMacro.getPlaybackMode()) { currentIdx = i; break; }
				}
				editingMacro.setPlaybackMode(modes[(currentIdx + 1) % modes.length]);
				NammConfig.getInstance().save();
				return true;
			}
			int toggleX = cx + controlW / 2 + 2;
			int toggleW = controlW / 2 - 2;
			if (mx >= toggleX && mx < toggleX + toggleW) {
				MacroProfile activeProfile = NammConfig.getInstance().getActiveProfile();
				if (activeProfile != null) {
					activeProfile.setMacroActive(editingMacro.getName(), !activeProfile.isMacroActive(editingMacro.getName()));
				} else {
					editingMacro.setEnabled(!editingMacro.isEnabled());
				}
				NammConfig.getInstance().save();
				return true;
			}
		}
		cy += 18;

		// Row 3: Trigger key
		if (my >= cy && my < cy + 16 && mx >= cx && mx < cx + controlW) {
			this.minecraft.setScreen(new KeyCaptureScreen(this, (keyCode, isMouse) -> {
				editingMacro.setTriggerKeyCode(keyCode);
				editingMacro.setTriggerMouse(isMouse);
				NammConfig.getInstance().save();
				this.minecraft.setScreen(this);
			}));
			return true;
		}
		cy += 20;

		// Step list area
		int stepAreaTop = cy;
		List<MacroStep> steps = editingMacro.getSteps();
		int btnRight = editorWinX + winW - 5;

		for (int i = 0; i < steps.size(); i++) {
			int rowY = stepAreaTop + (i * ROW_HEIGHT) - (int) editorScroll;
			if (rowY + ROW_HEIGHT < stepAreaTop) continue;

			if (my >= rowY && my < rowY + ROW_HEIGHT && my >= stepAreaTop) {
				// X button
				if (mx >= btnRight - 10 && mx < btnRight) {
					steps.remove(i);
					NammConfig.getInstance().save();
					return true;
				}
				// Down arrow
				if (mx >= btnRight - 24 && mx < btnRight - 14 && i < steps.size() - 1) {
					Collections.swap(steps, i, i + 1);
					NammConfig.getInstance().save();
					return true;
				}
				// Up arrow
				if (mx >= btnRight - 38 && mx < btnRight - 28 && i > 0) {
					Collections.swap(steps, i, i - 1);
					NammConfig.getInstance().save();
					return true;
				}
				// Click on description area
				if (mx >= editorWinX + 22 && mx < btnRight - 38) {
					MacroStep step = steps.get(i);
					if (step.getActionType() == ActionType.DELAY) {
						startEditorDelayEdit(i, rowY);
					} else {
						editorRecordingIndex = i;
					}
					return true;
				}
			}
		}

		// "+ Add Step"
		int addStepY = stepAreaTop + (steps.size() * ROW_HEIGHT) - (int) editorScroll;
		if (my >= addStepY && my < addStepY + ROW_HEIGHT && my >= stepAreaTop) {
			editorAddPopup = !editorAddPopup;
			return true;
		}

		return true;
	}

	private boolean handleEditorAddPopupClick(double mx, double my) {
		if (editingMacro == null) return false;

		List<MacroStep> steps = editingMacro.getSteps();
		int stepAreaTop = editorWinY + HEADER_HEIGHT + 4 + 22 + 18 + 20;
		int addStepY = stepAreaTop + (steps.size() * ROW_HEIGHT) - (int) editorScroll;

		int popupW = 80;
		int popupH = 36;
		int popupX = editorWinX + (EDITOR_WIN_WIDTH - popupW) / 2;
		int popupY = addStepY - popupH - 2;
		if (popupY < stepAreaTop) {
			popupY = addStepY + ROW_HEIGHT + 2;
		}

		if (mx >= popupX && mx < popupX + popupW && my >= popupY && my < popupY + popupH) {
			if (my < popupY + popupH / 2) {
				// Input: add KEY_PRESS + KEY_RELEASE pair
				steps.add(MacroStep.keyAction(ActionType.KEY_PRESS, -1, false));
				steps.add(MacroStep.keyAction(ActionType.KEY_RELEASE, -1, false));
				NammConfig.getInstance().save();
				editorRecordingIndex = steps.size() - 2;
				editorAddPopup = false;
				return true;
			} else {
				// Delay
				steps.add(MacroStep.delay(20));
				NammConfig.getInstance().save();
				editorAddPopup = false;
				return true;
			}
		}
		return false;
	}

	// --- Chat window click handling ---

	private boolean handleChatClick(int button, double mx, double my) {
		int contentTop = chatWinY + HEADER_HEIGHT;
		List<ChatCommand> commands = NammConfig.getInstance().getChatCommands();
		int rowIndex = 0;

		for (int i = 0; i < commands.size(); i++) {
			ChatCommand cmd = commands.get(i);
			int rowY = contentTop + (rowIndex * ROW_HEIGHT) - (int) chatScroll;
			rowIndex++;

			if (my >= rowY && my < rowY + ROW_HEIGHT && my >= contentTop) {
				if (button == 0) {
					// Left-click: toggle enabled
					cmd.setEnabled(!cmd.isEnabled());
					NammConfig.getInstance().save();
					return true;
				} else if (button == 1) {
					// Right-click: context menu
					contextMenuIndex = i;
					contextMenuX = (int) mx;
					contextMenuY = (int) my;
					contextMenuIsProfile = false;
					contextMenuIsChatCommand = true;
					return true;
				}
			}

			// Inline editor rows
			if (editingChatCommand == cmd) {
				// Row 1: Name box (handled by widget)
				rowIndex++;

				// Row 2: Message box (handled by widget)
				rowIndex++;

				// Row 3: Trigger key + Enabled toggle
				int trigRowY = contentTop + ((rowIndex) * ROW_HEIGHT) - (int) chatScroll;
				rowIndex++;
				if (button == 0 && my >= trigRowY && my < trigRowY + ROW_HEIGHT && my >= contentTop) {
					int editX = chatWinX + 4;
					int editW = CHAT_WIN_WIDTH - 8;
					if (mx >= editX && mx < editX + editW / 2 - 2) {
						// Trigger key button
						this.minecraft.setScreen(new KeyCaptureScreen(this, (keyCode, isMouse) -> {
							editingChatCommand.setTriggerKeyCode(keyCode);
							editingChatCommand.setTriggerMouse(isMouse);
							NammConfig.getInstance().save();
							this.minecraft.setScreen(this);
						}));
						return true;
					}
					int tX = editX + editW / 2 + 2;
					int tW = editW / 2 - 2;
					if (mx >= tX && mx < tX + tW) {
						// Enabled toggle
						cmd.setEnabled(!cmd.isEnabled());
						NammConfig.getInstance().save();
						return true;
					}
				}

				// Row 4: Done button
				int doneRowY = contentTop + ((rowIndex) * ROW_HEIGHT) - (int) chatScroll;
				rowIndex++;
				if (button == 0 && my >= doneRowY && my < doneRowY + ROW_HEIGHT && my >= contentTop) {
					closeChatEditor();
					return true;
				}
			}
		}

		// "+ New Command"
		int newY = contentTop + (rowIndex * ROW_HEIGHT) - (int) chatScroll;
		if (button == 0 && my >= newY && my < newY + ROW_HEIGHT) {
			createNewChatCommand();
			return true;
		}
		return true;
	}

	private void createNewChatCommand() {
		ChatCommand cmd = new ChatCommand();
		Set<String> names = new HashSet<>();
		for (ChatCommand c : NammConfig.getInstance().getChatCommands()) names.add(c.getName());
		String base = cmd.getName();
		int s = 1;
		while (names.contains(cmd.getName())) cmd.setName(base + " " + s++);
		NammConfig.getInstance().getChatCommands().add(cmd);
		NammConfig.getInstance().save();
		openChatEditor(cmd);
	}

	private void openChatEditor(ChatCommand cmd) {
		closeChatEditor();
		editingChatCommand = cmd;

		chatNameBox = new EditBox(this.font, chatWinX + 4, 0, CHAT_WIN_WIDTH - 8, 14, Component.literal("Name"));
		chatNameBox.setValue(cmd.getName());
		chatNameBox.setResponder(val -> {
			cmd.setName(val);
			NammConfig.getInstance().save();
		});
		addRenderableWidget(chatNameBox);

		chatMessageBox = new EditBox(this.font, chatWinX + 4, 0, CHAT_WIN_WIDTH - 8, 14, Component.literal("Message"));
		chatMessageBox.setValue(cmd.getMessage());
		chatMessageBox.setResponder(val -> {
			cmd.setMessage(val);
			NammConfig.getInstance().save();
		});
		addRenderableWidget(chatMessageBox);
	}

	private void closeChatEditor() {
		editingChatCommand = null;
		if (chatNameBox != null) {
			removeWidget(chatNameBox);
			chatNameBox = null;
		}
		if (chatMessageBox != null) {
			removeWidget(chatMessageBox);
			chatMessageBox = null;
		}
	}

	// --- Context menu click handling ---

	private boolean handleContextMenuClick(int button, double mx, double my) {
		if (button != 0) return false;

		int menuW = 70;
		int optionH = 16;
		int optionCount = contextMenuIsProfile ? 1 : 2;
		int menuH = optionCount * optionH + 4;

		int menuX = Math.max(2, Math.min(contextMenuX, this.width - menuW - 2));
		int menuY = Math.max(2, Math.min(contextMenuY, this.height - menuH - 2));

		if (mx < menuX || mx >= menuX + menuW || my < menuY || my >= menuY + menuH) return false;

		if (contextMenuIsChatCommand) {
			int opt = (int) ((my - menuY - 2) / optionH);
			if (opt == 0) {
				// Edit
				ChatCommand cmd = NammConfig.getInstance().getChatCommands().get(contextMenuIndex);
				contextMenuIndex = -1;
				contextMenuIsChatCommand = false;
				openChatEditor(cmd);
				return true;
			} else if (opt == 1) {
				// Delete
				final int idx = contextMenuIndex;
				contextMenuIndex = -1;
				contextMenuIsChatCommand = false;
				this.minecraft.setScreen(new ConfirmScreen(
						confirmed -> {
							if (confirmed) {
								NammConfig.getInstance().getChatCommands().remove(idx);
								NammConfig.getInstance().save();
								closeChatEditor();
							}
							this.minecraft.setScreen(this);
						},
						Component.literal("Delete Command"),
						Component.literal("Are you sure you want to delete this chat command?"),
						Component.literal("Delete").withStyle(ChatFormatting.RED),
						Component.literal("Go Back")
				));
				return true;
			}
		} else if (!contextMenuIsProfile) {
			int opt = (int) ((my - menuY - 2) / optionH);
			if (opt == 0) {
				// Edit
				Macro macro = NammConfig.getInstance().getMacros().get(contextMenuIndex);
				contextMenuIndex = -1;
				openEditor(macro);
				return true;
			} else if (opt == 1) {
				// Delete
				final int idx = contextMenuIndex;
				contextMenuIndex = -1;
				this.minecraft.setScreen(new ConfirmScreen(
						confirmed -> {
							if (confirmed) {
								NammConfig.getInstance().getMacros().remove(idx);
								NammConfig.getInstance().save();
							}
							this.minecraft.setScreen(this);
						},
						Component.literal("Delete Macro"),
						Component.literal("Are you sure you want to delete this macro?"),
						Component.literal("Delete").withStyle(ChatFormatting.RED),
						Component.literal("Go Back")
				));
				return true;
			}
		} else {
			final int idx = contextMenuIndex;
			contextMenuIndex = -1;
			MacroProfile profile = NammConfig.getInstance().getProfiles().get(idx);
			this.minecraft.setScreen(new ConfirmScreen(
					confirmed -> {
						if (confirmed) {
							String an = NammConfig.getInstance().getActiveProfileName();
							if (profile.getName().equals(an)) NammConfig.getInstance().setActiveProfileName(null);
							NammConfig.getInstance().getProfiles().remove(idx);
							NammConfig.getInstance().save();
						}
						this.minecraft.setScreen(this);
					},
					Component.literal("Delete Profile"),
					Component.literal("Are you sure you want to delete this profile?"),
					Component.literal("Delete").withStyle(ChatFormatting.RED),
					Component.literal("Go Back")
			));
			return true;
		}
		return false;
	}

	private boolean handleImportExportClick(double mx, double my) {
		int y = this.height - 20;
		int centerX = this.width / 2;
		String[] labels = {"Export", "Import NAMM", "Import Razer"};
		int totalW = 0;
		int gap = 8;
		int[] widths = new int[labels.length];
		for (int i = 0; i < labels.length; i++) {
			widths[i] = this.font.width(labels[i]) + 12;
			totalW += widths[i];
		}
		totalW += gap * (labels.length - 1);

		int bx = centerX - totalW / 2;
		for (int i = 0; i < labels.length; i++) {
			int bw = widths[i];
			if (mx >= bx && mx < bx + bw && my >= y && my < y + 16) {
				switch (i) {
					case 0 -> doExport();
					case 1 -> doImport();
					case 2 -> doImportRazer();
				}
				return true;
			}
			bx += bw + gap;
		}
		return false;
	}

	// --- Scrolling ---

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		// Editor window
		if (editingMacro != null && !editorCollapsed
				&& mx >= editorWinX && mx < editorWinX + EDITOR_WIN_WIDTH
				&& my >= editorWinY && my < editorWinY + getEditorWindowHeight()) {
			int stepAreaTop = editorWinY + HEADER_HEIGHT + 4 + 22 + 18 + 20;
			int stepAreaHeight = editorWinY + getEditorWindowHeight() - stepAreaTop;
			int contentH = (editingMacro.getSteps().size() + 1) * ROW_HEIGHT;
			if (contentH > stepAreaHeight) {
				editorScroll = Math.max(0, Math.min(editorScroll - sy * 8, contentH - stepAreaHeight));
			}
			return true;
		}

		// Macro window
		if (!macroCollapsed && mx >= macroWinX && mx < macroWinX + MACRO_WIN_WIDTH
				&& my >= macroWinY && my < macroWinY + getMacroWindowHeight()) {
			List<Macro> macros = NammConfig.getInstance().getMacros();
			int contentH = (macros.size() + 1) * ROW_HEIGHT;
			int visH = getMacroWindowHeight() - HEADER_HEIGHT;
			if (contentH > visH) {
				macroScroll = Math.max(0, Math.min(macroScroll - sy * 8, contentH - visH));
			}
			return true;
		}

		// Profile window
		if (!profileCollapsed && mx >= profileWinX && mx < profileWinX + PROFILE_WIN_WIDTH
				&& my >= profileWinY && my < profileWinY + getProfileWindowHeight()) {
			int contentH = getProfileContentRows() * ROW_HEIGHT;
			int visH = getProfileWindowHeight() - HEADER_HEIGHT;
			if (contentH > visH) {
				profileScroll = Math.max(0, Math.min(profileScroll - sy * 8, contentH - visH));
			}
			return true;
		}

		// Chat window
		if (!chatCollapsed && mx >= chatWinX && mx < chatWinX + CHAT_WIN_WIDTH
				&& my >= chatWinY && my < chatWinY + getChatWindowHeight()) {
			List<ChatCommand> commands = NammConfig.getInstance().getChatCommands();
			int rows = commands.size() + 1;
			if (editingChatCommand != null) rows += 4;
			int contentH = rows * ROW_HEIGHT;
			int visH = getChatWindowHeight() - HEADER_HEIGHT;
			if (contentH > visH) {
				chatScroll = Math.max(0, Math.min(chatScroll - sy * 8, contentH - visH));
			}
			return true;
		}

		return true;
	}

	// --- Keyboard ---

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		int key = keyEvent.key();

		// Recording mode in editor: capture key
		if (editorRecordingIndex >= 0 && editingMacro != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				editorRecordingIndex = -1;
				return true;
			}
			MacroStep step = editingMacro.getSteps().get(editorRecordingIndex);
			step.setKeyCode(key);
			if (step.isMouse()) {
				step.setMouse(false);
				if (step.getActionType() == ActionType.MOUSE_CLICK) {
					step.setActionType(ActionType.KEY_PRESS);
				} else if (step.getActionType() == ActionType.MOUSE_RELEASE) {
					step.setActionType(ActionType.KEY_RELEASE);
				}
			}
			NammConfig.getInstance().save();
			editorRecordingIndex = -1;
			return true;
		}

		// Editor delay editing
		if (editorDelayIndex >= 0 && editorDelayBox != null) {
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				commitEditorDelayEdit();
				return true;
			}
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				commitEditorDelayEdit();
				return true;
			}
			return editorDelayBox.keyPressed(keyEvent);
		}

		// Editor name box
		if (editorNameBox != null && editorNameBox.isFocused()) {
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				editorNameBox.setFocused(false);
				return true;
			}
			return editorNameBox.keyPressed(keyEvent);
		}

		// Chat command edit boxes
		if (chatNameBox != null && chatNameBox.isFocused()) {
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				chatNameBox.setFocused(false);
				return true;
			}
			return chatNameBox.keyPressed(keyEvent);
		}
		if (chatMessageBox != null && chatMessageBox.isFocused()) {
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				chatMessageBox.setFocused(false);
				return true;
			}
			return chatMessageBox.keyPressed(keyEvent);
		}

		// Profile creation
		if (creatingProfile && profileNameBox != null) {
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { commitProfileCreation(); return true; }
			if (key == GLFW.GLFW_KEY_ESCAPE) { cancelProfileCreation(); return true; }
			return profileNameBox.keyPressed(keyEvent);
		}

		// Context menu escape
		if (contextMenuIndex >= 0 && key == GLFW.GLFW_KEY_ESCAPE) { contextMenuIndex = -1; return true; }

		// Editor open + Escape: close editor
		if (editingMacro != null && key == GLFW.GLFW_KEY_ESCAPE) {
			closeEditor();
			return true;
		}

		// Add popup escape
		if (editorAddPopup && key == GLFW.GLFW_KEY_ESCAPE) {
			editorAddPopup = false;
			return true;
		}

		return super.keyPressed(keyEvent);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
		if (editorNameBox != null && editorNameBox.isFocused()) {
			return editorNameBox.charTyped(event);
		}
		if (editorDelayBox != null && editorDelayBox.isFocused()) {
			return editorDelayBox.charTyped(event);
		}
		if (profileNameBox != null && profileNameBox.isFocused()) {
			return profileNameBox.charTyped(event);
		}
		if (chatNameBox != null && chatNameBox.isFocused()) {
			return chatNameBox.charTyped(event);
		}
		if (chatMessageBox != null && chatMessageBox.isFocused()) {
			return chatMessageBox.charTyped(event);
		}
		return super.charTyped(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		if (editingMacro != null) return false;
		if (editorRecordingIndex >= 0) return false;
		if (editorDelayIndex >= 0) return false;
		if (creatingProfile) return false;
		if (contextMenuIndex >= 0) return false;
		if (editorAddPopup) return false;
		return true;
	}

	@Override
	public void onClose() {
		// Save window positions
		NammConfig cfg = NammConfig.getInstance();
		cfg.setMacroWinPos(macroWinX, macroWinY);
		cfg.setProfileWinPos(profileWinX, profileWinY);
		cfg.setEditorWinPos(editorWinX, editorWinY);
		cfg.setChatWinPos(chatWinX, chatWinY);
		cfg.save();
		closeChatEditor();
		this.minecraft.setScreen(parent);
	}

	// --- Editor management ---

	private void openEditor(Macro macro) {
		closeEditor(); // Clean up any previous editor state
		editingMacro = macro;
		editorScroll = 0;
		editorRecordingIndex = -1;
		editorDelayIndex = -1;
		editorAddPopup = false;
		editorCollapsed = false;

		// Create name EditBox
		int cx = editorWinX + 4;
		int cy = editorWinY + HEADER_HEIGHT + 4;
		int controlW = EDITOR_WIN_WIDTH - 8;
		editorNameBox = new EditBox(this.font, cx, cy, controlW, 16, Component.literal("Name"));
		editorNameBox.setValue(macro.getName());
		editorNameBox.setResponder(val -> {
			macro.setName(val);
			NammConfig.getInstance().save();
		});
		addRenderableWidget(editorNameBox);
	}

	private void closeEditor() {
		editingMacro = null;
		editorRecordingIndex = -1;
		editorAddPopup = false;
		if (editorDelayBox != null) {
			removeWidget(editorDelayBox);
			editorDelayBox = null;
		}
		editorDelayIndex = -1;
		if (editorNameBox != null) {
			removeWidget(editorNameBox);
			editorNameBox = null;
		}
	}

	private void startEditorDelayEdit(int index, int rowY) {
		editorDelayIndex = index;
		MacroStep step = editingMacro.getSteps().get(index);

		editorDelayBox = new EditBox(this.font, editorWinX + 22, rowY + 1, 80, 14, Component.literal("Delay"));
		editorDelayBox.setValue(String.valueOf(step.getDelayMs()));
		editorDelayBox.setFilter(s -> s.matches("\\d*"));
		editorDelayBox.setFocused(true);
		editorDelayBox.setResponder(val -> {});
		addRenderableWidget(editorDelayBox);
	}

	private void commitEditorDelayEdit() {
		if (editorDelayIndex < 0 || editorDelayBox == null || editingMacro == null) return;

		MacroStep step = editingMacro.getSteps().get(editorDelayIndex);
		String val = editorDelayBox.getValue();
		if (!val.isEmpty()) {
			try {
				step.setDelayMs(Integer.parseInt(val));
			} catch (NumberFormatException ignored) {}
		}
		NammConfig.getInstance().save();

		removeWidget(editorDelayBox);
		editorDelayBox = null;
		editorDelayIndex = -1;
	}

	private boolean isMouseOverDelayBox(double mouseX, double mouseY) {
		if (editorDelayBox == null) return false;
		return mouseX >= editorDelayBox.getX() && mouseX <= editorDelayBox.getX() + editorDelayBox.getWidth()
				&& mouseY >= editorDelayBox.getY() && mouseY <= editorDelayBox.getY() + editorDelayBox.getHeight();
	}

	// --- Actions ---

	private void createNewMacro() {
		Macro m = new Macro();
		Set<String> names = new HashSet<>();
		for (Macro x : NammConfig.getInstance().getMacros()) names.add(x.getName());
		String base = m.getName();
		int s = 1;
		while (names.contains(m.getName())) m.setName(base + " " + s++);
		NammConfig.getInstance().getMacros().add(m);
		NammConfig.getInstance().save();
		openEditor(m);
	}

	private void startProfileCreation() {
		creatingProfile = true;
		profileScroll = 0;
		int contentTop = profileWinY + HEADER_HEIGHT;
		profileNameBox = new EditBox(this.font, profileWinX + 4, contentTop + 1, PROFILE_WIN_WIDTH - 8, ROW_HEIGHT - 2, Component.literal("Name"));
		profileNameBox.setValue("");
		profileNameBox.setFocused(true);
		addRenderableWidget(profileNameBox);
	}

	private void commitProfileCreation() {
		if (profileNameBox == null) return;
		String name = profileNameBox.getValue().trim();
		if (!name.isEmpty()) {
			MacroProfile p = new MacroProfile();
			p.setName(name);
			Set<String> existing = new HashSet<>();
			for (MacroProfile x : NammConfig.getInstance().getProfiles()) existing.add(x.getName());
			while (existing.contains(p.getName())) p.setName(p.getName() + " (copy)");
			NammConfig.getInstance().getProfiles().add(p);
			NammConfig.getInstance().save();
		}
		cancelProfileCreation();
	}

	private void cancelProfileCreation() {
		creatingProfile = false;
		if (profileNameBox != null) { removeWidget(profileNameBox); profileNameBox = null; }
	}

	private void doExport() {
		new Thread(() -> {
			String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog("Export Macros", "namm-macros.json", null, "JSON files");
			if (result != null) {
				String json = MacroSerializer.exportToJson(NammConfig.getInstance().getMacros());
				try { java.nio.file.Files.writeString(Path.of(result), json); } catch (Exception ignored) {}
			}
		}, "NAMM-Export").start();
	}

	private void doImport() {
		new Thread(() -> {
			String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Import NAMM Macros", "", null, "JSON files", false);
			if (result != null) {
				try {
					String json = java.nio.file.Files.readString(Path.of(result));
					List<Macro> imported = MacroSerializer.importFromJson(json);
					addImportedMacros(imported);
				} catch (Exception ignored) {}
			}
		}, "NAMM-Import").start();
	}

	private void doImportRazer() {
		new Thread(() -> {
			String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Import Razer Synapse Macros", "", null, "XML files", false);
			if (result != null) {
				try {
					List<Macro> imported = RazerSynapseImporter.importFromFile(new java.io.File(result));
					addImportedMacros(imported);
				} catch (Exception ignored) {}
			}
		}, "NAMM-ImportRazer").start();
	}

	private void addImportedMacros(List<Macro> imported) {
		Minecraft.getInstance().execute(() -> {
			List<Macro> macros = NammConfig.getInstance().getMacros();
			Set<String> existing = new HashSet<>();
			for (Macro m : macros) existing.add(m.getName());
			for (Macro m : imported) {
				while (existing.contains(m.getName())) m.setName(m.getName() + " (imported)");
				existing.add(m.getName());
				macros.add(m);
			}
			NammConfig.getInstance().save();
		});
	}
}
