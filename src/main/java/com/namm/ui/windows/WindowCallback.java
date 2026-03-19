package com.namm.ui.windows;

import com.namm.model.ChatCommand;
import com.namm.model.Macro;
import net.minecraft.client.gui.components.EditBox;

/**
 * Callback interface for renderer-to-screen communication.
 * Content renderers use this to request actions that affect the parent screen.
 */
public interface WindowCallback {
    void editMacro(Macro macro);
    void closeMacroEditor();
    void showContextMenu(int index, int x, int y, ContextMenuType type);
    void addNewMacro();
    void addNewProfile();
    void editChatCommand(ChatCommand cmd);
    void profileSwitched(String profileName);
    void openKeyCaptureForMacro(Macro macro);
    void openKeyCaptureForChatCommand(ChatCommand cmd);

    /** Add an EditBox widget to the parent screen's renderable list. */
    void addWidget(EditBox widget);

    /** Remove an EditBox widget from the parent screen. */
    void removeWidget(EditBox widget);

    enum ContextMenuType { MACRO, PROFILE, CHAT_COMMAND }
}
