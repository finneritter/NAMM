package com.namm.ui.windows;

import com.namm.config.NammConfig;
import com.namm.model.ChatCommand;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Content renderer for the chat command list window.
 * Shows commands with inline editing (name, message, trigger, enabled, done).
 */
public class ChatCommandWindowRenderer implements WindowContent {
    private static final int ROW_HEIGHT = NammWindow.ROW_HEIGHT;

    private final WindowCallback callback;

    // Inline editor state
    private ChatCommand editingCommand = null;
    private EditBox nameBox = null;
    private EditBox messageBox = null;

    // Stored from last render() for hit testing
    private int renderX, renderY, renderWidth;

    public ChatCommandWindowRenderer(WindowCallback callback) {
        this.callback = callback;
    }

    public ChatCommand getEditingCommand() { return editingCommand; }
    public EditBox getNameBox() { return nameBox; }
    public EditBox getMessageBox() { return messageBox; }

    @Override
    public int getContentHeight() {
        List<ChatCommand> commands = NammConfig.getInstance().getChatCommands();
        int rows = commands.size() + 1; // +1 for "+ New Command"
        if (editingCommand != null) {
            rows += 4; // name, message, trigger+enabled, done button
        }
        return rows * ROW_HEIGHT + 4;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int mouseX, int mouseY, float delta) {
        this.renderX = x;
        this.renderY = y;
        this.renderWidth = width;

        Minecraft mc = Minecraft.getInstance();
        List<ChatCommand> commands = NammConfig.getInstance().getChatCommands();
        int rowIndex = 0;

        for (int i = 0; i < commands.size(); i++) {
            ChatCommand cmd = commands.get(i);
            int rowY = y + (rowIndex * ROW_HEIGHT);
            rowIndex++;

            boolean isOn = cmd.isEnabled();

            boolean hovered = mouseX >= x && mouseX < x + width
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            NammRenderer.drawRow(g, x, rowY, width, ROW_HEIGHT, hovered);

            NammRenderer.drawToggleIndicator(g, x + 3, rowY + 3, ROW_HEIGHT - 6, isOn);

            String name = truncate(cmd.getName(), width - 48);
            NammRenderer.drawText(g, x + 10, rowY + 4, name, isOn);

            String triggerName = cmd.getTriggerKeyCode() == -1 ? ""
                    : KeyNames.getKeyName(cmd.getTriggerKeyCode(), cmd.isTriggerMouse());
            if (!triggerName.isEmpty()) {
                NammRenderer.drawTextRight(g, x + width - 5, rowY + 4, triggerName, false);
            }

            // Inline editor
            if (editingCommand == cmd) {
                int editX = x + 4;
                int editW = width - 8;

                // Row 1: Name EditBox
                int nameRowY = y + (rowIndex * ROW_HEIGHT);
                rowIndex++;
                if (nameBox != null) {
                    nameBox.setX(editX);
                    nameBox.setY(nameRowY + 1);
                    nameBox.setWidth(editW);
                    nameBox.render(g, mouseX, mouseY, delta);
                }

                // Row 2: Message EditBox
                int msgRowY = y + (rowIndex * ROW_HEIGHT);
                rowIndex++;
                if (messageBox != null) {
                    messageBox.setX(editX);
                    messageBox.setY(msgRowY + 1);
                    messageBox.setWidth(editW);
                    messageBox.render(g, mouseX, mouseY, delta);
                }

                // Row 3: Trigger key button + Enabled toggle
                int trigRowY = y + (rowIndex * ROW_HEIGHT);
                rowIndex++;
                String trigLabel = cmd.getTriggerKeyCode() == -1 ? "Trigger: None"
                        : "Trigger: " + KeyNames.getKeyName(cmd.getTriggerKeyCode(), cmd.isTriggerMouse());
                boolean hoverTrig = mouseX >= editX && mouseX < editX + editW / 2 - 2
                        && mouseY >= trigRowY && mouseY < trigRowY + ROW_HEIGHT;
                NammRenderer.drawRow(g, editX, trigRowY, editW / 2 - 2, ROW_HEIGHT, hoverTrig);
                NammRenderer.drawText(g, editX + 2, trigRowY + 4, trigLabel, true);

                int tX = editX + editW / 2 + 2;
                int tW = editW / 2 - 2;
                boolean hoverEn = mouseX >= tX && mouseX < tX + tW && mouseY >= trigRowY && mouseY < trigRowY + ROW_HEIGHT;
                NammRenderer.drawRow(g, tX, trigRowY, tW, ROW_HEIGHT, hoverEn);
                NammRenderer.drawToggleIndicator(g, tX, trigRowY + 2, ROW_HEIGHT - 4, cmd.isEnabled());
                NammRenderer.drawText(g, tX + 6, trigRowY + 4, cmd.isEnabled() ? "Enabled" : "Disabled", cmd.isEnabled());

                // Row 4: Done button
                int doneRowY = y + (rowIndex * ROW_HEIGHT);
                rowIndex++;
                boolean hoverDone = mouseX >= x && mouseX < x + width
                        && mouseY >= doneRowY && mouseY < doneRowY + ROW_HEIGHT;
                NammRenderer.drawRow(g, x, doneRowY, width, ROW_HEIGHT, hoverDone);
                NammRenderer.drawTextAccent(g, x + width / 2 - NammRenderer.fontWidth("Done") / 2, doneRowY + 4, "Done");
            }
        }

        // "+ New Command"
        int newY = y + (rowIndex * ROW_HEIGHT);
        boolean hoverNew = mouseX >= x && mouseX < x + width
                && mouseY >= newY && mouseY < newY + ROW_HEIGHT;
        NammRenderer.drawRow(g, x, newY, width, ROW_HEIGHT, hoverNew);
        NammRenderer.drawTextCentered(g, x + width / 2, newY + 4, "+ New Command", false);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        NammConfig cfg = NammConfig.getInstance();
        List<ChatCommand> commands = cfg.getChatCommands();
        int rowIndex = 0;

        // Handle EditBox focus
        if (nameBox != null && editingCommand != null) {
            if (mouseX >= nameBox.getX() && mouseX <= nameBox.getX() + nameBox.getWidth()
                    && mouseY >= nameBox.getY() && mouseY <= nameBox.getY() + nameBox.getHeight()) {
                nameBox.setFocused(true);
                if (messageBox != null) messageBox.setFocused(false);
                return true;
            } else {
                nameBox.setFocused(false);
            }
        }
        if (messageBox != null && editingCommand != null) {
            if (mouseX >= messageBox.getX() && mouseX <= messageBox.getX() + messageBox.getWidth()
                    && mouseY >= messageBox.getY() && mouseY <= messageBox.getY() + messageBox.getHeight()) {
                messageBox.setFocused(true);
                if (nameBox != null) nameBox.setFocused(false);
                return true;
            } else {
                messageBox.setFocused(false);
            }
        }

        for (int i = 0; i < commands.size(); i++) {
            ChatCommand cmd = commands.get(i);
            int rowY = renderY + (rowIndex * ROW_HEIGHT);
            rowIndex++;

            if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                if (button == 0) {
                    cmd.setEnabled(!cmd.isEnabled());
                    cfg.save();
                    return true;
                } else if (button == 1) {
                    callback.showContextMenu(i, mouseX, mouseY, WindowCallback.ContextMenuType.CHAT_COMMAND);
                    return true;
                }
            }

            // Inline editor rows
            if (editingCommand == cmd) {
                rowIndex++; // name box
                rowIndex++; // message box

                // Row 3: Trigger key + Enabled toggle
                int trigRowY = renderY + (rowIndex * ROW_HEIGHT);
                rowIndex++;
                if (button == 0 && mouseY >= trigRowY && mouseY < trigRowY + ROW_HEIGHT) {
                    int editX = renderX + 4;
                    int editW = renderWidth - 8;
                    if (mouseX >= editX && mouseX < editX + editW / 2 - 2) {
                        callback.openKeyCaptureForChatCommand(editingCommand);
                        return true;
                    }
                    int tX = editX + editW / 2 + 2;
                    int tW = editW / 2 - 2;
                    if (mouseX >= tX && mouseX < tX + tW) {
                        cmd.setEnabled(!cmd.isEnabled());
                        cfg.save();
                        return true;
                    }
                }

                // Row 4: Done button
                int doneRowY = renderY + (rowIndex * ROW_HEIGHT);
                rowIndex++;
                if (button == 0 && mouseY >= doneRowY && mouseY < doneRowY + ROW_HEIGHT) {
                    closeEditor();
                    return true;
                }
            }
        }

        // "+ New Command"
        int newY = renderY + (rowIndex * ROW_HEIGHT);
        if (button == 0 && mouseY >= newY && mouseY < newY + ROW_HEIGHT) {
            createNewCommand();
            return true;
        }
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
        // EditBox keyboard is handled by NammGuiScreen forwarding KeyEvent/CharacterEvent
        if (nameBox != null && nameBox.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                nameBox.setFocused(false);
                return true;
            }
            return true; // consume
        }
        if (messageBox != null && messageBox.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                messageBox.setFocused(false);
                return true;
            }
            return true; // consume
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // charTyped for EditBoxes is handled by NammGuiScreen forwarding CharacterEvent
        if (nameBox != null && nameBox.isFocused()) return true;
        if (messageBox != null && messageBox.isFocused()) return true;
        return false;
    }

    @Override
    public void onCollapseChanged(Screen parentScreen, boolean collapsed) {
        if (collapsed) {
            if (nameBox != null) callback.removeWidget(nameBox);
            if (messageBox != null) callback.removeWidget(messageBox);
        } else {
            if (nameBox != null) callback.addWidget(nameBox);
            if (messageBox != null) callback.addWidget(messageBox);
        }
    }

    // --- Editor management ---

    public void openEditor(ChatCommand cmd, int winX, int winWidth) {
        closeEditor();
        editingCommand = cmd;
        Minecraft mc = Minecraft.getInstance();

        nameBox = new EditBox(mc.font, winX + 4, 0, winWidth - 8, 14, Component.literal("Name"));
        nameBox.setValue(cmd.getName());
        nameBox.setResponder(val -> {
            cmd.setName(val);
            NammConfig.getInstance().save();
        });
        callback.addWidget(nameBox);

        messageBox = new EditBox(mc.font, winX + 4, 0, winWidth - 8, 14, Component.literal("Message"));
        messageBox.setValue(cmd.getMessage());
        messageBox.setResponder(val -> {
            cmd.setMessage(val);
            NammConfig.getInstance().save();
        });
        callback.addWidget(messageBox);
    }

    public void closeEditor() {
        editingCommand = null;
        if (nameBox != null) {
            callback.removeWidget(nameBox);
            nameBox = null;
        }
        if (messageBox != null) {
            callback.removeWidget(messageBox);
            messageBox = null;
        }
    }

    private void createNewCommand() {
        ChatCommand cmd = new ChatCommand();
        Set<String> names = new HashSet<>();
        for (ChatCommand c : NammConfig.getInstance().getChatCommands()) names.add(c.getName());
        String base = cmd.getName();
        int s = 1;
        while (names.contains(cmd.getName())) cmd.setName(base + " " + s++);
        NammConfig.getInstance().getChatCommands().add(cmd);
        NammConfig.getInstance().save();
        callback.editChatCommand(cmd);
    }

    private static String truncate(String text, int maxW) {
        if (NammRenderer.fontWidth(text) <= maxW) return text;
        while (NammRenderer.fontWidth(text + "..") > maxW && text.length() > 1)
            text = text.substring(0, text.length() - 1);
        return text + "..";
    }
}
