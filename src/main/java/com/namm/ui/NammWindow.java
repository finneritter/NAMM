package com.namm.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class NammWindow {
    public static final int HEADER_HEIGHT = 18;
    public static final int ROW_HEIGHT = 16;
    public static final int MAX_HEIGHT = 300;

    private final String title;
    private final int width;
    private final Screen parentScreen;
    private WindowContent content;

    private int x, y;
    private boolean collapsed = false;
    private double scrollOffset = 0;
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;
    private boolean didDrag = false;

    public NammWindow(String title, int width, int x, int y, Screen parentScreen) {
        this.title = title; this.width = width; this.x = x; this.y = y; this.parentScreen = parentScreen;
    }

    public void setContent(WindowContent content) { this.content = content; }

    public int getHeight() {
        if (collapsed || content == null) return HEADER_HEIGHT;
        return Math.min(MAX_HEIGHT, HEADER_HEIGHT + content.getContentHeight() + 4);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int h = getHeight();
        NammRenderer.drawHeader(g, x, y, width, HEADER_HEIGHT, title, collapsed);
        if (collapsed || content == null) return;
        NammRenderer.drawPanelBottom(g, x, y + HEADER_HEIGHT, width, h - HEADER_HEIGHT);
        int contentTop = y + HEADER_HEIGHT;
        int contentBottom = y + h;
        g.enableScissor(x, contentTop, x + width, contentBottom);
        content.render(g, x, contentTop - (int) scrollOffset, width, mouseX, mouseY, delta);
        g.disableScissor();
        // Render overflow elements (popups, dropdowns) outside scissor
        content.renderOverflow(g, x, y, width, h, mouseX, mouseY, delta, scrollOffset);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int h = getHeight();
        // Header
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT) {
            if (mouseX < x + 18 && button == 0) {
                collapsed = !collapsed; scrollOffset = 0;
                if (content != null) content.onCollapseChanged(parentScreen, collapsed);
                return true;
            }
            if (button == 0) {
                dragging = true; didDrag = false;
                dragOffsetX = mouseX - x; dragOffsetY = mouseY - y;
                return true;
            }
        }
        // Content (screen-absolute coordinates)
        if (!collapsed && content != null && isInContentArea(mouseX, mouseY)) {
            return content.mouseClicked((int) mouseX, (int) mouseY, button);
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) { dragging = false; return true; }
        if (!collapsed && content != null) {
            return content.mouseReleased((int) mouseX, (int) mouseY, button);
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            x = (int)(mouseX - dragOffsetX); y = (int)(mouseY - dragOffsetY);
            didDrag = true; return true;
        }
        if (!collapsed && content != null) {
            return content.mouseDragged((int) mouseX, (int) mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!collapsed && isMouseOver(mouseX, mouseY) && content != null) {
            double maxScroll = Math.max(0, content.getContentHeight() + 4 - (MAX_HEIGHT - HEADER_HEIGHT));
            scrollOffset = Math.max(0, Math.min(scrollOffset - amount * ROW_HEIGHT, maxScroll));
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!collapsed && content != null) return content.keyPressed(keyCode, scanCode, modifiers);
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!collapsed && content != null) return content.charTyped(chr, modifiers);
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        int h = getHeight();
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + h;
    }

    private boolean isInContentArea(double mouseX, double mouseY) {
        int h = getHeight();
        return mouseX >= x && mouseX < x + width && mouseY >= y + HEADER_HEIGHT && mouseY < y + h;
    }

    public void clampToScreen(int screenW, int screenH) {
        x = Math.max(0, Math.min(x, screenW - width));
        y = Math.max(0, Math.min(y, screenH - HEADER_HEIGHT));
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getWidth() { return width; }
    public boolean isCollapsed() { return collapsed; }
    public boolean isDragging() { return dragging; }
    public boolean didDrag() { return didDrag; }
    public void resetDrag() { didDrag = false; }
    public String getTitle() { return title; }
    public double getScrollOffset() { return scrollOffset; }
}
