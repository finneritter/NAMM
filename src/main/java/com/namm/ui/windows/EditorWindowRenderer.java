package com.namm.ui.windows;

import com.namm.config.NammConfig;
import com.namm.model.ActionType;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import com.namm.model.MacroStep;
import com.namm.model.PlaybackMode;
import com.namm.ui.NammRenderer;
import com.namm.ui.NammTheme;
import com.namm.ui.NammWindow;
import com.namm.ui.WindowContent;
import com.namm.util.KeyNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;

/**
 * Content renderer for the macro step editor window.
 * Contains name EditBox, playback mode, enabled toggle, trigger button, and step list.
 */
public class EditorWindowRenderer implements WindowContent {
    private static final int ROW_HEIGHT = NammWindow.ROW_HEIGHT;

    private final WindowCallback callback;

    // The macro being edited (null when no editor is open)
    private Macro editingMacro = null;

    // Editor sub-state
    private int recordingIndex = -1;
    private int delayEditIndex = -1;
    private EditBox delayBox = null;
    private EditBox nameBox = null;
    private boolean addPopup = false;

    // Stored from last render() for hit testing
    private int renderX, renderY, renderWidth;
    // Step area top (after fixed controls)
    private int stepAreaTop;

    public EditorWindowRenderer(WindowCallback callback) {
        this.callback = callback;
    }

    public Macro getEditingMacro() { return editingMacro; }
    public boolean isRecording() { return recordingIndex >= 0; }
    public boolean isEditingDelay() { return delayEditIndex >= 0; }
    public boolean isAddPopupOpen() { return addPopup; }
    public EditBox getNameBox() { return nameBox; }
    public EditBox getDelayBox() { return delayBox; }

    // --- Lifecycle ---

    public void openEditor(int winX, int winY, int winWidth, Macro macro) {
        closeEditor();
        editingMacro = macro;
        recordingIndex = -1;
        delayEditIndex = -1;
        addPopup = false;

        Minecraft mc = Minecraft.getInstance();
        int cx = winX + 4;
        int cy = winY + NammWindow.HEADER_HEIGHT + 4;
        int controlW = winWidth - 8;
        nameBox = new EditBox(mc.font, cx, cy, controlW, 16, Component.literal("Name"));
        nameBox.setValue(macro.getName());
        nameBox.setResponder(val -> {
            macro.setName(val);
            NammConfig.getInstance().save();
        });
        callback.addWidget(nameBox);
    }

    public void closeEditor() {
        editingMacro = null;
        recordingIndex = -1;
        addPopup = false;
        if (delayBox != null) {
            callback.removeWidget(delayBox);
            delayBox = null;
        }
        delayEditIndex = -1;
        if (nameBox != null) {
            callback.removeWidget(nameBox);
            nameBox = null;
        }
    }

    /** Called from the header close button. */
    public void requestClose() {
        callback.closeMacroEditor();
    }

    @Override
    public int getContentHeight() {
        if (editingMacro == null) return 0;
        int fixedHeight = 4 + 22 + 18 + 20;
        int stepRows = editingMacro.getSteps().size();
        int stepAreaHeight = (stepRows + 1) * ROW_HEIGHT + 4;
        return fixedHeight + stepAreaHeight;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int mouseX, int mouseY, float delta) {
        if (editingMacro == null) return;

        this.renderX = x;
        this.renderY = y;
        this.renderWidth = width;

        Minecraft mc = Minecraft.getInstance();
        NammTheme t = NammTheme.get();

        int cy = y + 4;
        int cx = x + 4;
        int controlW = width - 8;

        // Row 1: Name EditBox
        if (nameBox != null) {
            nameBox.setX(cx);
            nameBox.setY(cy);
            nameBox.setWidth(controlW);
            nameBox.render(g, mouseX, mouseY, delta);
        }
        cy += 22;

        // Row 2: Playback mode (left) + Enabled toggle (right)
        String modeStr = editingMacro.getPlaybackMode().getDisplayName().getString();
        boolean hoverMode = mouseX >= cx && mouseX < cx + controlW / 2 - 2 && mouseY >= cy && mouseY < cy + 16;
        NammRenderer.drawRow(g, cx, cy, controlW / 2 - 2, 16, hoverMode);
        NammRenderer.drawText(g, cx + 2, cy + 4, modeStr, true);

        int toggleX = cx + controlW / 2 + 2;
        int toggleW = controlW / 2 - 2;
        boolean isEnabled = editingMacro.isEnabled();
        MacroProfile activeProfile = NammConfig.getInstance().getActiveProfile();
        if (activeProfile != null) {
            isEnabled = activeProfile.isMacroActive(editingMacro.getName());
        }
        boolean hoverToggle = mouseX >= toggleX && mouseX < toggleX + toggleW && mouseY >= cy && mouseY < cy + 16;
        NammRenderer.drawRow(g, toggleX, cy, toggleW, 16, hoverToggle);
        NammRenderer.drawToggleIndicator(g, toggleX, cy + 2, 12, isEnabled);
        NammRenderer.drawText(g, toggleX + 6, cy + 4, isEnabled ? "Enabled" : "Disabled", isEnabled);
        cy += 18;

        // Row 3: Trigger Key
        String triggerLabel = editingMacro.getTriggerKeyCode() == -1 ? "Trigger: None"
                : "Trigger: " + KeyNames.getKeyName(editingMacro.getTriggerKeyCode(), editingMacro.isTriggerMouse());
        boolean hoverTrigger = mouseX >= cx && mouseX < cx + controlW && mouseY >= cy && mouseY < cy + 16;
        NammRenderer.drawRow(g, cx, cy, controlW, 16, hoverTrigger);
        NammRenderer.drawText(g, cx + 2, cy + 4, triggerLabel, true);
        cy += 20;

        // Step list
        this.stepAreaTop = cy;
        List<MacroStep> steps = editingMacro.getSteps();

        for (int i = 0; i < steps.size(); i++) {
            int rowY = cy + (i * ROW_HEIGHT);
            MacroStep step = steps.get(i);

            if (i == recordingIndex) {
                g.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, NammTheme.get().hover());
            }
            if (i == delayEditIndex) {
                g.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, NammTheme.get().hover());
            }

            NammRenderer.drawText(g, x + 6, rowY + 4, (i + 1) + ".", false);

            if (i == recordingIndex) {
                NammRenderer.drawTextAccent(g, x + 22, rowY + 4, "Press a key...");
            } else if (i == delayEditIndex && delayBox != null) {
                // EditBox renders itself via widget system
            } else {
                String desc = truncate(step.getDisplaySummary(), width - 70);
                NammRenderer.drawText(g, x + 22, rowY + 4, desc, true);
            }

            int btnRight = x + width - 5;
            boolean hoverX = mouseX >= btnRight - 10 && mouseX < btnRight && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            NammRenderer.drawTextColored(g, btnRight - 8, rowY + 4, "X",
                    hoverX ? t.destructive() : t.textSecondary());

            if (i < steps.size() - 1) {
                boolean hoverDown = mouseX >= btnRight - 24 && mouseX < btnRight - 14 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                NammRenderer.drawText(g, btnRight - 22, rowY + 4, "\u2193", hoverDown);
            }
            if (i > 0) {
                boolean hoverUp = mouseX >= btnRight - 38 && mouseX < btnRight - 28 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                NammRenderer.drawText(g, btnRight - 36, rowY + 4, "\u2191", hoverUp);
            }
        }

        // "+ Add Step"
        int addStepY = cy + (steps.size() * ROW_HEIGHT);
        boolean hoverAdd = mouseX >= x && mouseX < x + width
                && mouseY >= addStepY && mouseY < addStepY + ROW_HEIGHT;
        NammRenderer.drawRow(g, x, addStepY, width, ROW_HEIGHT, hoverAdd);
        NammRenderer.drawTextCentered(g, x + width / 2, addStepY + 4, "+ Add Step", false);

        // Render delay edit box on top
        if (delayEditIndex >= 0 && delayBox != null) {
            delayBox.render(g, mouseX, mouseY, delta);
        }

        // Add step popup rendered in renderOverflow() to avoid scissor clipping
    }

    private void renderAddPopup(GuiGraphics g, int mouseX, int mouseY, int addStepY) {
        NammTheme t = NammTheme.get();
        int popupW = 80;
        int popupH = 36;
        int popupX = renderX + (renderWidth - popupW) / 2;
        int popupY = addStepY - popupH - 2;
        if (popupY < stepAreaTop) {
            popupY = addStepY + ROW_HEIGHT + 2;
        }
        NammRenderer.drawPanel(g, popupX, popupY, popupW, popupH);
        g.renderOutline(popupX, popupY, popupW, popupH, t.border());

        boolean hoverInput = mouseX >= popupX && mouseX < popupX + popupW
                && mouseY >= popupY && mouseY < popupY + popupH / 2;
        boolean hoverDelay = mouseX >= popupX && mouseX < popupX + popupW
                && mouseY >= popupY + popupH / 2 && mouseY < popupY + popupH;
        NammRenderer.drawRow(g, popupX + 1, popupY + 1, popupW - 2, popupH / 2 - 1, hoverInput);
        NammRenderer.drawRow(g, popupX + 1, popupY + popupH / 2, popupW - 2, popupH / 2 - 1, hoverDelay);

        NammRenderer.drawTextCentered(g, popupX + popupW / 2, popupY + 5, "Input", true);
        NammRenderer.drawSeparator(g, popupX + 4, popupY + popupH / 2 - 1, popupW - 8);
        NammRenderer.drawTextCentered(g, popupX + popupW / 2, popupY + popupH / 2 + 4, "Delay", true);
    }

    @Override
    public void renderOverflow(GuiGraphics g, int winX, int winY, int winW, int winH,
                               int mouseX, int mouseY, float delta, double scrollOffset) {
        if (!addPopup || editingMacro == null) return;
        List<MacroStep> steps = editingMacro.getSteps();
        int addStepY = stepAreaTop + (steps.size() * ROW_HEIGHT) - (int) scrollOffset;
        renderAddPopup(g, mouseX, mouseY, addStepY);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (editingMacro == null) return false;

        int cx = renderX + 4;
        int controlW = renderWidth - 8;
        int cy = renderY + 4;

        // Row 1: Name EditBox focus management
        if (nameBox != null) {
            if (mouseX >= nameBox.getX() && mouseX <= nameBox.getX() + nameBox.getWidth()
                    && mouseY >= nameBox.getY() && mouseY <= nameBox.getY() + nameBox.getHeight()) {
                nameBox.setFocused(true);
                return true;
            } else {
                nameBox.setFocused(false);
            }
        }
        cy += 22;

        // Delay editing: click away commits
        if (delayEditIndex >= 0 && delayBox != null) {
            if (!isMouseOverDelayBox(mouseX, mouseY)) {
                commitDelayEdit();
            } else {
                delayBox.setFocused(true);
                return true;
            }
        }

        // Add popup
        if (addPopup) {
            if (handleAddPopupClick(mouseX, mouseY)) return true;
            addPopup = false;
            return true;
        }

        // Row 2: Playback mode (left) + Enabled (right)
        if (mouseY >= cy && mouseY < cy + 16) {
            if (mouseX >= cx && mouseX < cx + controlW / 2 - 2) {
                PlaybackMode[] modes = PlaybackMode.values();
                int currentIdx = 0;
                for (int i = 0; i < modes.length; i++) {
                    if (modes[i] == editingMacro.getPlaybackMode()) { currentIdx = i; break; }
                }
                editingMacro.setPlaybackMode(modes[(currentIdx + 1) % modes.length]);
                NammConfig.getInstance().save();
                return true;
            }
            int toggleXPos = cx + controlW / 2 + 2;
            int toggleW = controlW / 2 - 2;
            if (mouseX >= toggleXPos && mouseX < toggleXPos + toggleW) {
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
        if (mouseY >= cy && mouseY < cy + 16 && mouseX >= cx && mouseX < cx + controlW) {
            callback.openKeyCaptureForMacro(editingMacro);
            return true;
        }
        cy += 20;

        // Step list
        List<MacroStep> steps = editingMacro.getSteps();
        int btnRight = renderX + renderWidth - 5;

        for (int i = 0; i < steps.size(); i++) {
            int rowY = cy + (i * ROW_HEIGHT);
            if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                if (mouseX >= btnRight - 10 && mouseX < btnRight) {
                    steps.remove(i);
                    NammConfig.getInstance().save();
                    return true;
                }
                if (mouseX >= btnRight - 24 && mouseX < btnRight - 14 && i < steps.size() - 1) {
                    Collections.swap(steps, i, i + 1);
                    NammConfig.getInstance().save();
                    return true;
                }
                if (mouseX >= btnRight - 38 && mouseX < btnRight - 28 && i > 0) {
                    Collections.swap(steps, i, i - 1);
                    NammConfig.getInstance().save();
                    return true;
                }
                if (mouseX >= renderX + 22 && mouseX < btnRight - 38) {
                    MacroStep step = steps.get(i);
                    if (step.getActionType() == ActionType.DELAY) {
                        startDelayEdit(i, rowY);
                    } else {
                        recordingIndex = i;
                    }
                    return true;
                }
            }
        }

        // "+ Add Step"
        int addStepY = cy + (steps.size() * ROW_HEIGHT);
        if (mouseY >= addStepY && mouseY < addStepY + ROW_HEIGHT) {
            addPopup = !addPopup;
            return true;
        }

        return true;
    }

    private boolean handleAddPopupClick(int mouseX, int mouseY) {
        if (editingMacro == null) return false;

        List<MacroStep> steps = editingMacro.getSteps();
        int addStepY = stepAreaTop + (steps.size() * ROW_HEIGHT);

        int popupW = 80;
        int popupH = 36;
        int popupX = renderX + (renderWidth - popupW) / 2;
        int popupY = addStepY - popupH - 2;
        if (popupY < stepAreaTop) {
            popupY = addStepY + ROW_HEIGHT + 2;
        }

        if (mouseX >= popupX && mouseX < popupX + popupW && mouseY >= popupY && mouseY < popupY + popupH) {
            if (mouseY < popupY + popupH / 2) {
                steps.add(MacroStep.keyAction(ActionType.KEY_PRESS, -1, false));
                steps.add(MacroStep.keyAction(ActionType.KEY_RELEASE, -1, false));
                NammConfig.getInstance().save();
                recordingIndex = steps.size() - 2;
                addPopup = false;
                return true;
            } else {
                steps.add(MacroStep.delay(20));
                NammConfig.getInstance().save();
                addPopup = false;
                return true;
            }
        }
        return false;
    }

    /** Handle mouse click captured during recording mode. Returns true if consumed. */
    public boolean handleRecordingMouseClick(int button) {
        if (recordingIndex < 0 || editingMacro == null) return false;
        MacroStep step = editingMacro.getSteps().get(recordingIndex);
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
        recordingIndex = -1;
        return true;
    }

    /** Handle key press captured during recording mode. Returns true if consumed. */
    public boolean handleRecordingKeyPress(int keyCode) {
        if (recordingIndex < 0 || editingMacro == null) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            recordingIndex = -1;
            return true;
        }
        MacroStep step = editingMacro.getSteps().get(recordingIndex);
        step.setKeyCode(keyCode);
        if (step.isMouse()) {
            step.setMouse(false);
            if (step.getActionType() == ActionType.MOUSE_CLICK) {
                step.setActionType(ActionType.KEY_PRESS);
            } else if (step.getActionType() == ActionType.MOUSE_RELEASE) {
                step.setActionType(ActionType.KEY_RELEASE);
            }
        }
        NammConfig.getInstance().save();
        recordingIndex = -1;
        return true;
    }

    @Override
    public boolean mouseReleased(int x, int y, int button) { return false; }

    @Override
    public boolean mouseScrolled(int x, int y, double amount) { return false; }

    @Override
    public boolean mouseDragged(int x, int y, int button, double deltaX, double deltaY) { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Delay editing: Enter/Escape commits
        if (delayEditIndex >= 0 && delayBox != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                    || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                commitDelayEdit();
                return true;
            }
            // Other keys for delay box are handled by NammGuiScreen forwarding KeyEvent
            return true; // consume to prevent further handling
        }

        // Name box focus: handled by NammGuiScreen forwarding KeyEvent
        if (nameBox != null && nameBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                nameBox.setFocused(false);
                return true;
            }
            return true; // consume
        }

        // Add popup escape
        if (addPopup && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            addPopup = false;
            return true;
        }

        // Editor escape = close editor
        if (editingMacro != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            requestClose();
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // charTyped for EditBoxes is handled by NammGuiScreen forwarding CharacterEvent
        if (nameBox != null && nameBox.isFocused()) return true;
        if (delayBox != null && delayBox.isFocused()) return true;
        return false;
    }

    @Override
    public void onCollapseChanged(Screen parentScreen, boolean collapsed) {
        if (nameBox != null) {
            if (collapsed) {
                callback.removeWidget(nameBox);
            } else {
                callback.addWidget(nameBox);
            }
        }
    }

    // --- Delay editing ---

    public void startDelayEdit(int index, int rowY) {
        delayEditIndex = index;
        MacroStep step = editingMacro.getSteps().get(index);
        Minecraft mc = Minecraft.getInstance();

        delayBox = new EditBox(mc.font, renderX + 22, rowY + 1, 80, 14, Component.literal("Delay"));
        delayBox.setValue(String.valueOf(step.getDelayMs()));
        delayBox.setFilter(s -> s.matches("\\d*"));
        delayBox.setFocused(true);
        delayBox.setResponder(val -> {});
        callback.addWidget(delayBox);
    }

    public void commitDelayEdit() {
        if (delayEditIndex < 0 || delayBox == null || editingMacro == null) return;

        MacroStep step = editingMacro.getSteps().get(delayEditIndex);
        String val = delayBox.getValue();
        if (!val.isEmpty()) {
            try {
                step.setDelayMs(Integer.parseInt(val));
            } catch (NumberFormatException ignored) {}
        }
        NammConfig.getInstance().save();

        callback.removeWidget(delayBox);
        delayBox = null;
        delayEditIndex = -1;
    }

    private boolean isMouseOverDelayBox(double mouseX, double mouseY) {
        if (delayBox == null) return false;
        return mouseX >= delayBox.getX() && mouseX <= delayBox.getX() + delayBox.getWidth()
                && mouseY >= delayBox.getY() && mouseY <= delayBox.getY() + delayBox.getHeight();
    }

    public boolean shouldBlockEsc() {
        return editingMacro != null || recordingIndex >= 0 || delayEditIndex >= 0 || addPopup;
    }

    private static String truncate(String text, int maxW) {
        if (NammRenderer.fontWidth(text) <= maxW) return text;
        while (NammRenderer.fontWidth(text + "..") > maxW && text.length() > 1)
            text = text.substring(0, text.length() - 1);
        return text + "..";
    }
}
