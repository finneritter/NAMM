package com.namm.config;

import com.namm.model.ChatCommand;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import com.namm.ui.InfoBar;
import com.namm.ui.NammRenderer;
import com.namm.ui.NammTheme;
import com.namm.ui.NammWindow;
import com.namm.ui.NotificationSettingsScreen;
import com.namm.ui.ToastManager;
import com.namm.ui.windows.ChatCommandWindowRenderer;
import com.namm.ui.windows.EditorWindowRenderer;
import com.namm.ui.windows.MacroWindowRenderer;
import com.namm.ui.windows.ProfileWindowRenderer;
import com.namm.ui.windows.SettingsWindowRenderer;
import com.namm.ui.windows.WindowCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main NAMM GUI screen -- slim orchestrator that delegates to window renderers.
 * Owns window layout, context menu, import/export, and notification settings overlay.
 */
public class NammGuiScreen extends Screen {
	private final Screen parent;

	// Layout constants
	private static final int MACRO_WIN_WIDTH = 160;
	private static final int PROFILE_WIN_WIDTH = 160;
	private static final int EDITOR_WIN_WIDTH = 220;
	private static final int CHAT_WIN_WIDTH = 180;
	private static final int SETTINGS_WIN_WIDTH = 180;

	// Windows
	private NammWindow macroWindow;
	private NammWindow profileWindow;
	private NammWindow editorWindow;
	private NammWindow chatWindow;
	private NammWindow settingsWindow;

	// Content renderers
	private MacroWindowRenderer macroRenderer;
	private ProfileWindowRenderer profileRenderer;
	private EditorWindowRenderer editorRenderer;
	private ChatCommandWindowRenderer chatRenderer;
	private SettingsWindowRenderer settingsRenderer;

	// Settings window state
	private boolean settingsWindowOpen = false;

	// Context menu state (cross-window, stays here)
	private int contextMenuIndex = -1;
	private int contextMenuX, contextMenuY;
	private WindowCallback.ContextMenuType contextMenuType = null;

	// Notification settings overlay
	private boolean notificationSettingsOpen = false;
	private NotificationSettingsScreen notificationSettings;

	public NammGuiScreen(Screen parent) {
		super(Component.literal("NAMM"));
		this.parent = parent;
	}

	// --- Initialization ---

	@Override
	protected void init() {
		super.init();

		NammConfig cfg = NammConfig.getInstance();

		// Create callback
		WindowCallback cb = createCallback();

		// Create renderers
		macroRenderer = new MacroWindowRenderer(cb);
		profileRenderer = new ProfileWindowRenderer(cb);
		editorRenderer = new EditorWindowRenderer(cb);
		chatRenderer = new ChatCommandWindowRenderer(cb);
		settingsRenderer = new SettingsWindowRenderer();

		// Load persisted positions, auto-position if -1
		int macroX = cfg.getMacroWinX();
		int macroY = cfg.getMacroWinY();
		if (macroX < 0 || macroY < 0) {
			macroX = this.width / 4 - MACRO_WIN_WIDTH / 2;
			macroY = 30;
		}

		int profileX = cfg.getProfileWinX();
		int profileY = cfg.getProfileWinY();
		if (profileX < 0 || profileY < 0) {
			profileX = 3 * this.width / 4 - PROFILE_WIN_WIDTH / 2;
			profileY = 30;
		}

		int editorX = cfg.getEditorWinX();
		int editorY = cfg.getEditorWinY();
		if (editorX < 0 || editorY < 0) {
			editorX = (this.width - EDITOR_WIN_WIDTH) / 2;
			editorY = 30;
		}

		int chatX = cfg.getChatWinX();
		int chatY = cfg.getChatWinY();
		if (chatX < 0 || chatY < 0) {
			chatX = 3 * this.width / 4 - CHAT_WIN_WIDTH / 2 + PROFILE_WIN_WIDTH + 10;
			chatY = 30;
		}

		// Create windows
		macroWindow = new NammWindow("Macros", MACRO_WIN_WIDTH, macroX, macroY, this);
		macroWindow.setContent(macroRenderer);

		profileWindow = new NammWindow("Profiles", PROFILE_WIN_WIDTH, profileX, profileY, this);
		profileWindow.setContent(profileRenderer);

		editorWindow = new NammWindow("Editor", EDITOR_WIN_WIDTH, editorX, editorY, this);
		editorWindow.setContent(editorRenderer);

		chatWindow = new NammWindow("Chat Commands", CHAT_WIN_WIDTH, chatX, chatY, this);
		chatWindow.setContent(chatRenderer);

		int settingsX = cfg.getSettingsWinX();
		int settingsY = cfg.getSettingsWinY();
		if (settingsX < 0 || settingsY < 0) {
			settingsX = this.width / 2 - SETTINGS_WIN_WIDTH / 2;
			settingsY = 30;
		}
		settingsWindow = new NammWindow("Settings", SETTINGS_WIN_WIDTH, settingsX, settingsY, this);
		settingsWindow.setContent(settingsRenderer);

		// Clamp to screen
		macroWindow.clampToScreen(this.width, this.height);
		profileWindow.clampToScreen(this.width, this.height);
		editorWindow.clampToScreen(this.width, this.height);
		chatWindow.clampToScreen(this.width, this.height);
		settingsWindow.clampToScreen(this.width, this.height);

		// Reset state
		contextMenuIndex = -1;
		settingsWindowOpen = false;
		notificationSettingsOpen = false;
		notificationSettings = new NotificationSettingsScreen();
	}

	private WindowCallback createCallback() {
		return new WindowCallback() {
			@Override
			public void editMacro(Macro macro) {
				openEditor(macro);
			}

			@Override
			public void closeMacroEditor() {
				closeEditor();
			}

			@Override
			public void showContextMenu(int index, int x, int y, ContextMenuType type) {
				contextMenuIndex = index;
				contextMenuX = x;
				contextMenuY = y;
				contextMenuType = type;
			}

			@Override
			public void addNewMacro() {
				createNewMacro();
			}

			@Override
			public void addNewProfile() {
				startProfileCreation();
			}

			@Override
			public void editChatCommand(ChatCommand cmd) {
				openChatEditor(cmd);
			}

			@Override
			public void profileSwitched(String profileName) {
				ToastManager.get().post("Switched to " + profileName, ToastManager.ToastType.INFO, ToastManager.Category.PROFILE_SWITCHED);
			}

			@Override
			public void openKeyCaptureForMacro(Macro macro) {
				NammGuiScreen.this.minecraft.setScreen(new KeyCaptureScreen(NammGuiScreen.this, (keyCode, isMouse) -> {
					macro.setTriggerKeyCode(keyCode);
					macro.setTriggerMouse(isMouse);
					NammConfig.getInstance().save();
					NammGuiScreen.this.minecraft.setScreen(NammGuiScreen.this);
				}));
			}

			@Override
			public void openKeyCaptureForChatCommand(ChatCommand cmd) {
				NammGuiScreen.this.minecraft.setScreen(new KeyCaptureScreen(NammGuiScreen.this, (keyCode, isMouse) -> {
					cmd.setTriggerKeyCode(keyCode);
					cmd.setTriggerMouse(isMouse);
					NammConfig.getInstance().save();
					NammGuiScreen.this.minecraft.setScreen(NammGuiScreen.this);
				}));
			}

			@Override
			public void addWidget(EditBox widget) {
				NammGuiScreen.this.addRenderableWidget(widget);
			}

			@Override
			public void removeWidget(EditBox widget) {
				NammGuiScreen.this.removeWidget(widget);
			}
		};
	}

	// --- Rendering ---

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		NammTheme t = NammTheme.get();

		// Screen overlay
		g.fill(0, 0, this.width, this.height, t.screenOverlay());

		// Render windows
		macroWindow.render(g, mouseX, mouseY, delta);
		profileWindow.render(g, mouseX, mouseY, delta);

		if (editorRenderer.getEditingMacro() != null) {
			editorWindow.render(g, mouseX, mouseY, delta);
		}

		chatWindow.render(g, mouseX, mouseY, delta);

		if (settingsWindowOpen) {
			settingsWindow.render(g, mouseX, mouseY, delta);
		}

		// Info bar
		InfoBar.get().render(g, this.width, this.height);

		// Bottom bar buttons
		renderBottomBar(g, mouseX, mouseY);

		// Context menu on top
		if (contextMenuIndex >= 0) {
			renderContextMenu(g, mouseX, mouseY);
		}

		// Toast notifications
		ToastManager.get().render(g, this.width, this.height);

		// Notification settings overlay (on top of everything)
		if (notificationSettingsOpen) {
			notificationSettings.render(g, this.width, this.height, mouseX, mouseY);
		}

	}

	// --- Import/Export rendering ---

	private void renderBottomBar(GuiGraphics g, int mouseX, int mouseY) {
		NammTheme t = NammTheme.get();
		int y = this.height - 20;
		int centerX = this.width / 2;
		String[] labels = {"Settings", "Notifications", "Export", "Import NAMM", "Import Razer"};
		int totalW = 0;
		int gap = 8;
		int[] widths = new int[labels.length];
		for (int i = 0; i < labels.length; i++) {
			widths[i] = NammRenderer.fontWidth(labels[i]) + 12;
			totalW += widths[i];
		}
		totalW += gap * (labels.length - 1);

		int bx = centerX - totalW / 2;
		for (int i = 0; i < labels.length; i++) {
			int bw = widths[i];
			boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= y && mouseY < y + 16;
			NammRenderer.drawPanel(g, bx, y, bw, 16);
			if (hover) {
				g.fill(bx + 1, y + 1, bx + bw - 1, y + 15, t.hover());
			}
			NammRenderer.drawTextCentered(g, bx + bw / 2, y + 4, labels[i], hover);
			bx += bw + gap;
		}
	}

	// --- Context Menu ---

	private void renderContextMenu(GuiGraphics g, int mouseX, int mouseY) {
		NammTheme t = NammTheme.get();
		int menuW = 70;
		int optionH = 16;
		boolean isProfileMenu = contextMenuType == WindowCallback.ContextMenuType.PROFILE;
		int optionCount = isProfileMenu ? 1 : 2;
		int menuH = optionCount * optionH + 4;

		int menuX = Math.max(2, Math.min(contextMenuX, this.width - menuW - 2));
		int menuY = Math.max(2, Math.min(contextMenuY, this.height - menuH - 2));

		NammRenderer.drawPanel(g, menuX, menuY, menuW, menuH);

		if (!isProfileMenu) {
			boolean hEdit = mouseX >= menuX && mouseX < menuX + menuW && mouseY >= menuY + 2 && mouseY < menuY + 2 + optionH;
			NammRenderer.drawRow(g, menuX + 2, menuY + 2, menuW - 4, optionH, hEdit);
			NammRenderer.drawText(g, menuX + 8, menuY + 6, "Edit", true);

			boolean hDel = mouseX >= menuX && mouseX < menuX + menuW && mouseY >= menuY + 2 + optionH && mouseY < menuY + menuH - 2;
			NammRenderer.drawRow(g, menuX + 2, menuY + 2 + optionH, menuW - 4, optionH, hDel);
			NammRenderer.drawTextColored(g, menuX + 8, menuY + 6 + optionH, "Delete", t.destructive());
		} else {
			boolean hDel = mouseX >= menuX && mouseX < menuX + menuW && mouseY >= menuY + 2 && mouseY < menuY + menuH - 2;
			NammRenderer.drawRow(g, menuX + 2, menuY + 2, menuW - 4, menuH - 4, hDel);
			NammRenderer.drawTextColored(g, menuX + 8, menuY + 6, "Delete", t.destructive());
		}
	}

	// --- Mouse Input ---

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		int button = event.button();
		double mouseX = event.x();
		double mouseY = event.y();

		// Notification settings overlay consumes all input when open
		if (notificationSettingsOpen) {
			boolean consumed = notificationSettings.mouseClicked(mouseX, mouseY, button);
			if (!consumed) {
				notificationSettingsOpen = false;
			}
			return true;
		}

		// Check if settings renderer requested notification settings
		if (settingsRenderer.isNotificationSettingsRequested()) {
			notificationSettingsOpen = true;
			return true;
		}

		// Recording mode in editor: capture mouse
		if (editorRenderer.isRecording()) {
			return editorRenderer.handleRecordingMouseClick(button);
		}

		// Context menu
		if (contextMenuIndex >= 0) {
			if (handleContextMenuClick(button, mouseX, mouseY)) return true;
			contextMenuIndex = -1;
			return true;
		}

		// Profile name creation
		if (profileRenderer.isCreatingProfile()) {
			EditBox box = profileRenderer.getProfileNameBox();
			if (box != null && mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
					&& mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight()) {
				box.setFocused(true);
				box.mouseClicked(event, bl);
				return true;
			}
			profileRenderer.cancelProfileCreation();
		}

		// Settings window
		if (settingsWindowOpen && settingsWindow.mouseClicked(mouseX, mouseY, button)) {
			// Check again after click processing
			if (settingsRenderer.isNotificationSettingsRequested()) {
				notificationSettingsOpen = true;
			}
			return true;
		}

		// Editor window (check first since it may overlap)
		if (editorRenderer.getEditingMacro() != null && editorWindow.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		// Macro window
		if (macroWindow.mouseClicked(mouseX, mouseY, button)) return true;

		// Profile window
		if (profileWindow.mouseClicked(mouseX, mouseY, button)) return true;

		// Chat window
		if (chatWindow.mouseClicked(mouseX, mouseY, button)) return true;

		// Bottom bar buttons
		if (handleBottomBarClick(mouseX, mouseY)) return true;

		return super.mouseClicked(event, bl);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();

		NammWindow dragged = null;
		if (settingsWindowOpen && settingsWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) dragged = settingsWindow;
		if (dragged == null && editorRenderer.getEditingMacro() != null && editorWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) dragged = editorWindow;
		if (dragged == null && macroWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) dragged = macroWindow;
		if (dragged == null && profileWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) dragged = profileWindow;
		if (dragged == null && chatWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) dragged = chatWindow;

		if (dragged != null && dragged.isDragging()) {
			snapToNeighbors(dragged);
			return true;
		}
		if (dragged != null) return true;

		return super.mouseDragged(event, deltaX, deltaY);
	}

	private static final int SNAP_DISTANCE = 8;

	private void snapToNeighbors(NammWindow target) {
		NammWindow[] others = getAllWindowsExcept(target);
		int tx = target.getX(), ty = target.getY();
		int tw = target.getWidth(), th = target.getHeight();

		for (NammWindow other : others) {
			int ox = other.getX(), oy = other.getY();
			int ow = other.getWidth(), oh = other.getHeight();

			// Snap right edge of target to left edge of other
			if (Math.abs((tx + tw) - ox) < SNAP_DISTANCE) tx = ox - tw - 1;
			// Snap left edge of target to right edge of other
			if (Math.abs(tx - (ox + ow)) < SNAP_DISTANCE) tx = ox + ow + 1;
			// Snap left edges aligned
			if (Math.abs(tx - ox) < SNAP_DISTANCE) tx = ox;
			// Snap top edges aligned
			if (Math.abs(ty - oy) < SNAP_DISTANCE) ty = oy;
			// Snap bottom of target to top of other
			if (Math.abs((ty + th) - oy) < SNAP_DISTANCE) ty = oy - th - 1;
			// Snap top of target to bottom of other
			if (Math.abs(ty - (oy + oh)) < SNAP_DISTANCE) ty = oy + oh + 1;
		}
		target.setPosition(tx, ty);
	}

	private NammWindow[] getAllWindowsExcept(NammWindow target) {
		NammWindow[] all = { macroWindow, profileWindow, editorWindow, chatWindow, settingsWindow };
		return java.util.Arrays.stream(all).filter(w -> w != target).toArray(NammWindow[]::new);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();

		boolean anyReleased = false;

		if (settingsWindowOpen && settingsWindow.mouseReleased(mouseX, mouseY, button)) anyReleased = true;
		if (!anyReleased && editorRenderer.getEditingMacro() != null && editorWindow.mouseReleased(mouseX, mouseY, button)) anyReleased = true;
		if (!anyReleased && macroWindow.mouseReleased(mouseX, mouseY, button)) anyReleased = true;
		if (!anyReleased && profileWindow.mouseReleased(mouseX, mouseY, button)) anyReleased = true;
		if (!anyReleased && chatWindow.mouseReleased(mouseX, mouseY, button)) anyReleased = true;

		if (anyReleased) {
			saveWindowPositions();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (settingsWindowOpen && settingsWindow.mouseScrolled(mx, my, sy)) return true;
		if (editorRenderer.getEditingMacro() != null && editorWindow.mouseScrolled(mx, my, sy)) return true;
		if (macroWindow.mouseScrolled(mx, my, sy)) return true;
		if (profileWindow.mouseScrolled(mx, my, sy)) return true;
		if (chatWindow.mouseScrolled(mx, my, sy)) return true;
		return true;
	}

	// --- Keyboard ---

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		int key = keyEvent.key();

		// Notification settings: Escape closes it
		if (notificationSettingsOpen && key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			notificationSettingsOpen = false;
			return true;
		}

		// Recording mode in editor: capture key
		if (editorRenderer.isRecording()) {
			return editorRenderer.handleRecordingKeyPress(key);
		}

		// Context menu escape
		if (contextMenuIndex >= 0 && key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			contextMenuIndex = -1;
			return true;
		}

		// Editor delay box: forward KeyEvent directly to EditBox
		if (editorRenderer.isEditingDelay()) {
			EditBox delayBox = editorRenderer.getDelayBox();
			if (delayBox != null) {
				if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER
						|| key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
					editorRenderer.commitDelayEdit();
					return true;
				}
				return delayBox.keyPressed(keyEvent);
			}
		}

		// Editor name box: forward KeyEvent directly to EditBox
		EditBox editorNameBox = editorRenderer.getNameBox();
		if (editorNameBox != null && editorNameBox.isFocused()) {
			if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				editorNameBox.setFocused(false);
				return true;
			}
			return editorNameBox.keyPressed(keyEvent);
		}

		// Chat command edit boxes: forward KeyEvent directly
		EditBox chatNameBox = chatRenderer.getNameBox();
		if (chatNameBox != null && chatNameBox.isFocused()) {
			if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				chatNameBox.setFocused(false);
				return true;
			}
			return chatNameBox.keyPressed(keyEvent);
		}
		EditBox chatMsgBox = chatRenderer.getMessageBox();
		if (chatMsgBox != null && chatMsgBox.isFocused()) {
			if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				chatMsgBox.setFocused(false);
				return true;
			}
			return chatMsgBox.keyPressed(keyEvent);
		}

		// Profile creation: forward KeyEvent to EditBox
		if (profileRenderer.isCreatingProfile()) {
			EditBox profileBox = profileRenderer.getProfileNameBox();
			if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				profileRenderer.commitProfileCreation();
				return true;
			}
			if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
				profileRenderer.cancelProfileCreation();
				return true;
			}
			if (profileBox != null) return profileBox.keyPressed(keyEvent);
		}

		// Delegate to windows for non-EditBox key handling (editor first)
		if (editorRenderer.getEditingMacro() != null && editorWindow.keyPressed(key, keyEvent.scancode(), keyEvent.modifiers())) return true;
		if (chatWindow.keyPressed(key, keyEvent.scancode(), keyEvent.modifiers())) return true;

		return super.keyPressed(keyEvent);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		// Editor name box
		EditBox editorNameBox = editorRenderer.getNameBox();
		if (editorNameBox != null && editorNameBox.isFocused()) {
			return editorNameBox.charTyped(event);
		}

		// Editor delay box
		EditBox delayBox = editorRenderer.getDelayBox();
		if (delayBox != null && delayBox.isFocused()) {
			return delayBox.charTyped(event);
		}

		// Profile creation box
		if (profileRenderer.isCreatingProfile()) {
			EditBox profileBox = profileRenderer.getProfileNameBox();
			if (profileBox != null && profileBox.isFocused()) {
				return profileBox.charTyped(event);
			}
		}

		// Chat command boxes
		EditBox chatNameBox = chatRenderer.getNameBox();
		if (chatNameBox != null && chatNameBox.isFocused()) {
			return chatNameBox.charTyped(event);
		}
		EditBox chatMsgBox = chatRenderer.getMessageBox();
		if (chatMsgBox != null && chatMsgBox.isFocused()) {
			return chatMsgBox.charTyped(event);
		}

		return super.charTyped(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		if (editorRenderer.shouldBlockEsc()) return false;
		if (profileRenderer.isCreatingProfile()) return false;
		if (contextMenuIndex >= 0) return false;
		if (notificationSettingsOpen) return false;
		return true;
	}

	@Override
	public void onClose() {
		saveWindowPositions();
		chatRenderer.closeEditor();
		editorRenderer.closeEditor();
		this.minecraft.setScreen(parent);
	}

	// --- Context menu click handling ---

	private boolean handleContextMenuClick(int button, double mx, double my) {
		if (button != 0) return false;

		int menuW = 70;
		int optionH = 16;
		boolean isProfileMenu = contextMenuType == WindowCallback.ContextMenuType.PROFILE;
		int optionCount = isProfileMenu ? 1 : 2;
		int menuH = optionCount * optionH + 4;

		int menuX = Math.max(2, Math.min(contextMenuX, this.width - menuW - 2));
		int menuY = Math.max(2, Math.min(contextMenuY, this.height - menuH - 2));

		if (mx < menuX || mx >= menuX + menuW || my < menuY || my >= menuY + menuH) return false;

		if (contextMenuType == WindowCallback.ContextMenuType.CHAT_COMMAND) {
			int opt = (int) ((my - menuY - 2) / optionH);
			if (opt == 0) {
				ChatCommand cmd = NammConfig.getInstance().getChatCommands().get(contextMenuIndex);
				contextMenuIndex = -1;
				openChatEditor(cmd);
				return true;
			} else if (opt == 1) {
				final int idx = contextMenuIndex;
				contextMenuIndex = -1;
				this.minecraft.setScreen(new ConfirmScreen(
						confirmed -> {
							if (confirmed) {
								NammConfig.getInstance().getChatCommands().remove(idx);
								NammConfig.getInstance().save();
								chatRenderer.closeEditor();
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
		} else if (contextMenuType == WindowCallback.ContextMenuType.MACRO) {
			int opt = (int) ((my - menuY - 2) / optionH);
			if (opt == 0) {
				Macro macro = NammConfig.getInstance().getMacros().get(contextMenuIndex);
				contextMenuIndex = -1;
				openEditor(macro);
				return true;
			} else if (opt == 1) {
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
		} else if (contextMenuType == WindowCallback.ContextMenuType.PROFILE) {
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

	// --- Import/Export ---

	private boolean handleBottomBarClick(double mx, double my) {
		int y = this.height - 20;
		int centerX = this.width / 2;
		String[] labels = {"Settings", "Notifications", "Export", "Import NAMM", "Import Razer"};
		int totalW = 0;
		int gap = 8;
		int[] widths = new int[labels.length];
		for (int i = 0; i < labels.length; i++) {
			widths[i] = NammRenderer.fontWidth(labels[i]) + 12;
			totalW += widths[i];
		}
		totalW += gap * (labels.length - 1);

		int bx = centerX - totalW / 2;
		for (int i = 0; i < labels.length; i++) {
			int bw = widths[i];
			if (mx >= bx && mx < bx + bw && my >= y && my < y + 16) {
				switch (i) {
					case 0 -> settingsWindowOpen = !settingsWindowOpen;
					case 1 -> notificationSettingsOpen = true;
					case 2 -> doExport();
					case 3 -> doImport();
					case 4 -> doImportRazer();
				}
				return true;
			}
			bx += bw + gap;
		}
		return false;
	}

	// --- Editor management ---

	private void openEditor(Macro macro) {
		editorRenderer.openEditor(editorWindow.getX(), editorWindow.getY(), editorWindow.getWidth(), macro);
	}

	private void closeEditor() {
		editorRenderer.closeEditor();
	}

	private void openChatEditor(ChatCommand cmd) {
		chatRenderer.openEditor(cmd, chatWindow.getX(), chatWindow.getWidth());
	}

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
		profileRenderer.startProfileCreation(profileWindow.getX(),
				profileWindow.getY() + NammWindow.HEADER_HEIGHT, profileWindow.getWidth());
	}

	// --- Persistence ---

	private void saveWindowPositions() {
		NammConfig cfg = NammConfig.getInstance();
		cfg.setMacroWinPos(macroWindow.getX(), macroWindow.getY());
		cfg.setProfileWinPos(profileWindow.getX(), profileWindow.getY());
		cfg.setEditorWinPos(editorWindow.getX(), editorWindow.getY());
		cfg.setChatWinPos(chatWindow.getX(), chatWindow.getY());
		cfg.setSettingsWinPos(settingsWindow.getX(), settingsWindow.getY());
		cfg.save();
	}

	// --- Import/Export actions ---

	private void doExport() {
		new Thread(() -> {
			String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog("Export Macros", "namm-macros.json", null, "JSON files");
			if (result != null) {
				List<Macro> macros = NammConfig.getInstance().getMacros();
				String json = MacroSerializer.exportToJson(macros);
				try {
					java.nio.file.Files.writeString(Path.of(result), json);
					int count = macros.size();
					ToastManager.get().post("Exported " + count + " macros", ToastManager.ToastType.SUCCESS, ToastManager.Category.IMPORT_EXPORT);
				} catch (Exception ignored) {}
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
					int count = imported.size();
					addImportedMacros(imported);
					ToastManager.get().post("Imported " + count + " macros", ToastManager.ToastType.SUCCESS, ToastManager.Category.IMPORT_EXPORT);
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
