package com.namm.ui.windows;

import com.namm.config.NammConfig;
import com.namm.model.Macro;
import com.namm.model.MacroProfile;
import com.namm.ui.NammRenderer;
import com.namm.ui.NammTheme;
import com.namm.ui.NammWindow;
import com.namm.ui.WindowContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Content renderer for the profile list window.
 * Shows profiles with expand/collapse for per-macro toggles,
 * an inline profile creation EditBox, and a "+ New Profile" row.
 */
public class ProfileWindowRenderer implements WindowContent {
    private static final int ROW_HEIGHT = NammWindow.ROW_HEIGHT;

    private final WindowCallback callback;

    // Profile creation state
    private boolean creatingProfile = false;
    private EditBox profileNameBox = null;

    // Expanded profile names
    private final Set<String> expandedProfiles = new HashSet<>();

    // Stored from last render() call for hit testing
    private int renderX, renderY, renderWidth;

    public ProfileWindowRenderer(WindowCallback callback) {
        this.callback = callback;
    }

    private int getContentRows() {
        List<MacroProfile> profiles = NammConfig.getInstance().getProfiles();
        int rows = profiles.size() + 1; // +1 for "+ New Profile"
        if (creatingProfile) rows++;
        List<Macro> allMacros = NammConfig.getInstance().getMacros();
        for (MacroProfile profile : profiles) {
            if (expandedProfiles.contains(profile.getName())) {
                rows += allMacros.size();
            }
        }
        return rows;
    }

    @Override
    public int getContentHeight() {
        return getContentRows() * ROW_HEIGHT + 4;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int width, int mouseX, int mouseY, float delta) {
        this.renderX = x;
        this.renderY = y;
        this.renderWidth = width;

        Minecraft mc = Minecraft.getInstance();
        List<MacroProfile> profiles = NammConfig.getInstance().getProfiles();
        List<Macro> allMacros = NammConfig.getInstance().getMacros();
        String activeName = NammConfig.getInstance().getActiveProfileName();

        int rowIndex = 0;

        // Profile creation row
        if (creatingProfile) {
            int boxY = y + (rowIndex * ROW_HEIGHT);
            NammRenderer.drawRow(g, x, boxY, width, ROW_HEIGHT, true);
            if (profileNameBox != null) {
                profileNameBox.setX(x + 4);
                profileNameBox.setY(boxY + 1);
                profileNameBox.setWidth(width - 8);
                profileNameBox.render(g, mouseX, mouseY, delta);
            }
            rowIndex++;
        }

        for (int i = 0; i < profiles.size(); i++) {
            MacroProfile profile = profiles.get(i);
            boolean isActive = profile.getName().equals(activeName);
            boolean isExpanded = expandedProfiles.contains(profile.getName());
            int rowY = y + (rowIndex * ROW_HEIGHT);
            rowIndex++;

            boolean hovered = mouseX >= x && mouseX < x + width
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            NammRenderer.drawRow(g, x, rowY, width, ROW_HEIGHT, hovered);

            if (isActive) {
                NammRenderer.drawToggleIndicator(g, x + 3, rowY + 3, ROW_HEIGHT - 6, true);
            }

            String pArrow = isExpanded ? "\u25BC" : "\u25B6";
            NammRenderer.drawText(g, x + 10, rowY + 4, pArrow, false);
            NammRenderer.drawText(g, x + 20, rowY + 4, profile.getName(), isActive);

            if (isActive) {
                NammRenderer.drawTextAccent(g, x + width - 5 - mc.font.width("ACTIVE"), rowY + 4, "ACTIVE");
            }

            if (i < profiles.size() - 1 || !isExpanded) {
                NammRenderer.drawSeparator(g, x + 8, rowY + ROW_HEIGHT - 1, width - 16);
            }

            if (isExpanded) {
                for (int m = 0; m < allMacros.size(); m++) {
                    Macro macro = allMacros.get(m);
                    int mRowY = y + (rowIndex * ROW_HEIGHT);
                    rowIndex++;

                    boolean macroActive = profile.isMacroActive(macro.getName());
                    boolean mHovered = mouseX >= x && mouseX < x + width
                            && mouseY >= mRowY && mouseY < mRowY + ROW_HEIGHT;
                    NammRenderer.drawRow(g, x, mRowY, width, ROW_HEIGHT, mHovered);
                    NammRenderer.drawCheckbox(g, x + 18, mRowY + 3, 10, macroActive);

                    String mName = truncate(mc, macro.getName(), width - 40);
                    NammRenderer.drawText(g, x + 32, mRowY + 4, mName, macroActive);
                }
            }
        }

        int newY = y + (rowIndex * ROW_HEIGHT);
        boolean hoverNew = mouseX >= x && mouseX < x + width
                && mouseY >= newY && mouseY < newY + ROW_HEIGHT;
        NammRenderer.drawRow(g, x, newY, width, ROW_HEIGHT, hoverNew);
        NammRenderer.drawTextCentered(g, x + width / 2, newY + 4, "+ New Profile", false);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        NammConfig cfg = NammConfig.getInstance();
        List<MacroProfile> profiles = cfg.getProfiles();
        List<Macro> allMacros = cfg.getMacros();

        int rowIndex = 0;

        if (creatingProfile) {
            int boxY = renderY + (rowIndex * ROW_HEIGHT);
            if (profileNameBox != null && mouseY >= boxY && mouseY < boxY + ROW_HEIGHT) {
                profileNameBox.setFocused(true);
                return true;
            }
            cancelProfileCreation();
            rowIndex++;
        }

        if (creatingProfile) rowIndex = 1;

        for (int i = 0; i < profiles.size(); i++) {
            MacroProfile profile = profiles.get(i);
            boolean isExpanded = expandedProfiles.contains(profile.getName());
            int rowY = renderY + (rowIndex * ROW_HEIGHT);
            rowIndex++;

            if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                if (button == 0) {
                    if (mouseX < renderX + 20) {
                        if (isExpanded) {
                            expandedProfiles.remove(profile.getName());
                        } else {
                            expandedProfiles.add(profile.getName());
                        }
                    } else {
                        String active = cfg.getActiveProfileName();
                        String newActive = profile.getName().equals(active) ? null : profile.getName();
                        cfg.setActiveProfileName(newActive);
                        cfg.save();
                        callback.profileSwitched(newActive);
                    }
                    return true;
                } else if (button == 1) {
                    callback.showContextMenu(i, mouseX, mouseY, WindowCallback.ContextMenuType.PROFILE);
                    return true;
                }
            }

            if (isExpanded) {
                for (int m = 0; m < allMacros.size(); m++) {
                    Macro macro = allMacros.get(m);
                    int mRowY = renderY + (rowIndex * ROW_HEIGHT);
                    rowIndex++;

                    if (mouseY >= mRowY && mouseY < mRowY + ROW_HEIGHT && button == 0) {
                        profile.setMacroActive(macro.getName(), !profile.isMacroActive(macro.getName()));
                        cfg.save();
                        return true;
                    }
                }
            }
        }

        int newY = renderY + (rowIndex * ROW_HEIGHT);
        if (button == 0 && mouseY >= newY && mouseY < newY + ROW_HEIGHT) {
            callback.addNewProfile();
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
        // Profile creation keyboard is handled by NammGuiScreen directly
        // because EditBox.keyPressed requires a KeyEvent object
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Profile creation charTyped is handled by NammGuiScreen directly
        // because EditBox.charTyped requires a CharacterEvent object
        return false;
    }

    @Override
    public void onCollapseChanged(Screen parentScreen, boolean collapsed) {
        if (collapsed && profileNameBox != null) {
            cancelProfileCreation();
        }
    }

    // --- Profile creation ---

    public void startProfileCreation(int winX, int contentTop, int winWidth) {
        creatingProfile = true;
        Minecraft mc = Minecraft.getInstance();
        profileNameBox = new EditBox(mc.font, winX + 4, contentTop + 1, winWidth - 8, ROW_HEIGHT - 2, Component.literal("Name"));
        profileNameBox.setValue("");
        profileNameBox.setFocused(true);
        callback.addWidget(profileNameBox);
    }

    public void commitProfileCreation() {
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

    public void cancelProfileCreation() {
        creatingProfile = false;
        if (profileNameBox != null) {
            callback.removeWidget(profileNameBox);
            profileNameBox = null;
        }
    }

    public boolean isCreatingProfile() { return creatingProfile; }
    public EditBox getProfileNameBox() { return profileNameBox; }

    private static String truncate(Minecraft mc, String text, int maxW) {
        if (mc.font.width(text) <= maxW) return text;
        while (mc.font.width(text + "..") > maxW && text.length() > 1)
            text = text.substring(0, text.length() - 1);
        return text + "..";
    }
}
