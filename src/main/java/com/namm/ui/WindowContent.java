package com.namm.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public interface WindowContent {
    int getContentHeight();
    // Note: delta parameter added beyond original spec for EditBox cursor blink support
    void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, float delta);
    boolean mouseClicked(int x, int y, int button);
    boolean mouseReleased(int x, int y, int button);
    boolean mouseScrolled(int x, int y, double amount);
    boolean mouseDragged(int x, int y, int button, double deltaX, double deltaY);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
    boolean charTyped(char chr, int modifiers);
    void onCollapseChanged(Screen parentScreen, boolean collapsed);

    /** Render floating elements (popups, dropdowns) outside the window's scissor region. */
    default void renderOverflow(GuiGraphics g, int winX, int winY, int winW, int winH,
                                int mouseX, int mouseY, float delta, double scrollOffset) {}
}
