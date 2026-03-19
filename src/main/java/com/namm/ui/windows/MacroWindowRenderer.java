package com.namm.ui.windows;

import com.namm.config.NammConfig;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import com.namm.ui.NammRenderer;
import com.namm.ui.NammWindow;
import com.namm.ui.WindowContent;
import com.namm.util.KeyNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * Content renderer for the macro list window.
 * Shows all macros with toggle indicators and trigger key labels.
 */
public class MacroWindowRenderer implements WindowContent {
    private static final int ROW_HEIGHT = NammWindow.ROW_HEIGHT;

    private final WindowCallback callback;

    // Stored from last render() call for hit testing
    private int renderX, renderY, renderWidth;

    public MacroWindowRenderer(WindowCallback callback) {
        this.callback = callback;
    }

    @Override
    public int getContentHeight() {
        List<Macro> macros = NammConfig.getInstance().getMacros();
        return (macros.size() + 1) * ROW_HEIGHT + 4;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int mouseX, int mouseY, float delta) {
        this.renderX = x;
        this.renderY = y;
        this.renderWidth = width;

        List<Macro> macros = NammConfig.getInstance().getMacros();
        MacroProfile activeProfile = NammConfig.getInstance().getActiveProfile();
        Minecraft mc = Minecraft.getInstance();

        for (int i = 0; i < macros.size(); i++) {
            Macro macro = macros.get(i);
            int rowY = y + (i * ROW_HEIGHT);

            boolean isOn = activeProfile != null
                    ? activeProfile.isMacroActive(macro.getName())
                    : macro.isEnabled();

            boolean hovered = mouseX >= x && mouseX < x + width
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            NammRenderer.drawRow(g, x, rowY, width, ROW_HEIGHT, hovered);

            NammRenderer.drawToggleIndicator(g, x + 3, rowY + 3, ROW_HEIGHT - 6, isOn);

            String name = truncate(mc, macro.getName(), width - 48);
            NammRenderer.drawText(g, x + 10, rowY + 4, name, isOn);

            String triggerName = macro.getTriggerKeyCode() == -1 ? ""
                    : KeyNames.getKeyName(macro.getTriggerKeyCode(), macro.isTriggerMouse());
            if (!triggerName.isEmpty()) {
                NammRenderer.drawTextRight(g, x + width - 5, rowY + 4, triggerName, false);
            }

            if (i < macros.size() - 1) {
                NammRenderer.drawSeparator(g, x + 8, rowY + ROW_HEIGHT - 1, width - 16);
            }
        }

        int newY = y + (macros.size() * ROW_HEIGHT);
        boolean hoverNew = mouseX >= x && mouseX < x + width
                && mouseY >= newY && mouseY < newY + ROW_HEIGHT;
        NammRenderer.drawRow(g, x, newY, width, ROW_HEIGHT, hoverNew);
        NammRenderer.drawTextCentered(g, x + width / 2, newY + 4, "+ New Macro", false);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        NammConfig cfg = NammConfig.getInstance();
        List<Macro> macros = cfg.getMacros();
        MacroProfile activeProfile = cfg.getActiveProfile();

        for (int i = 0; i < macros.size(); i++) {
            int rowY = renderY + (i * ROW_HEIGHT);
            if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                if (button == 0) {
                    Macro macro = macros.get(i);
                    if (activeProfile != null) {
                        activeProfile.setMacroActive(macro.getName(), !activeProfile.isMacroActive(macro.getName()));
                    } else {
                        macro.setEnabled(!macro.isEnabled());
                    }
                    cfg.save();
                    return true;
                } else if (button == 1) {
                    callback.showContextMenu(i, mouseX, mouseY, WindowCallback.ContextMenuType.MACRO);
                    return true;
                }
            }
        }

        // "+ New Macro"
        int newY = renderY + (macros.size() * ROW_HEIGHT);
        if (button == 0 && mouseY >= newY && mouseY < newY + ROW_HEIGHT) {
            callback.addNewMacro();
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    @Override
    public boolean charTyped(char chr, int modifiers) { return false; }

    @Override
    public void onCollapseChanged(Screen parentScreen, boolean collapsed) { }

    private static String truncate(Minecraft mc, String text, int maxW) {
        if (mc.font.width(text) <= maxW) return text;
        while (mc.font.width(text + "..") > maxW && text.length() > 1)
            text = text.substring(0, text.length() - 1);
        return text + "..";
    }
}
