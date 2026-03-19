package com.namm.ui;

import com.namm.config.NammConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ToastManager {
    private static final ToastManager INSTANCE = new ToastManager();
    public static ToastManager get() { return INSTANCE; }

    private static final int MAX_VISIBLE = 4;
    private static final long FADE_IN_MS = 200;
    private static final long HOLD_MS = 3000;
    private static final long FADE_OUT_MS = 300;
    private static final int TOAST_WIDTH = 180;
    private static final int TOAST_HEIGHT = 24;
    private static final int TOAST_PADDING = 4;

    public enum Category { MACRO_TOGGLED, CHAT_COMMAND, PROFILE_SWITCHED, IMPORT_EXPORT, ERROR }
    public enum ToastType { SUCCESS, ERROR, INFO }

    private record ToastEvent(String message, ToastType type, Category category) {}

    private static class ActiveToast {
        final String message;
        final ToastType type;
        final long createdAt;
        ActiveToast(String message, ToastType type) {
            this.message = message; this.type = type; this.createdAt = System.currentTimeMillis();
        }
        float getAlpha() {
            long age = System.currentTimeMillis() - createdAt;
            if (age < FADE_IN_MS) return (float) age / FADE_IN_MS;
            if (age < FADE_IN_MS + HOLD_MS) return 1.0f;
            long fadeAge = age - FADE_IN_MS - HOLD_MS;
            if (fadeAge < FADE_OUT_MS) return 1.0f - (float) fadeAge / FADE_OUT_MS;
            return 0.0f;
        }
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > FADE_IN_MS + HOLD_MS + FADE_OUT_MS;
        }
    }

    private final ConcurrentLinkedQueue<ToastEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final List<ActiveToast> activeToasts = new ArrayList<>();

    public void post(String message, ToastType type, Category category) {
        pendingEvents.add(new ToastEvent(message, type, category));
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        NammConfig cfg = NammConfig.getInstance();

        ToastEvent event;
        while ((event = pendingEvents.poll()) != null) {
            if (cfg.isNotificationsMuted()) continue;
            if (!isCategoryEnabled(cfg, event.category)) continue;
            if (activeToasts.size() >= MAX_VISIBLE) activeToasts.remove(0);
            activeToasts.add(new ActiveToast(event.message, event.type));
        }

        activeToasts.removeIf(ActiveToast::isExpired);

        int x = screenWidth - TOAST_WIDTH - 8;
        int baseY = screenHeight - 8;
        NammTheme t = NammTheme.get();

        for (int i = activeToasts.size() - 1; i >= 0; i--) {
            ActiveToast toast = activeToasts.get(i);
            float alpha = toast.getAlpha();
            if (alpha <= 0) continue;

            int index = activeToasts.size() - 1 - i;
            int ty = baseY - (index + 1) * (TOAST_HEIGHT + TOAST_PADDING);
            int alphaInt = (int)(alpha * 255) << 24;

            int bg = (t.panelBg() & 0x00FFFFFF) | alphaInt;
            g.fill(x, ty, x + TOAST_WIDTH, ty + TOAST_HEIGHT, bg);

            int dotColor = switch (toast.type) {
                case SUCCESS -> t.toastSuccess();
                case ERROR -> t.toastError();
                case INFO -> t.toastInfo();
            };
            dotColor = (dotColor & 0x00FFFFFF) | alphaInt;
            int dotY = ty + (TOAST_HEIGHT - 6) / 2;
            g.fill(x + 8, dotY, x + 14, dotY + 6, dotColor);

            int textColor = (t.textPrimary() & 0x00FFFFFF) | alphaInt;
            g.drawString(Minecraft.getInstance().font, toast.message, x + 20, ty + 8, textColor, false);
        }
    }

    private boolean isCategoryEnabled(NammConfig cfg, Category category) {
        return switch (category) {
            case MACRO_TOGGLED -> cfg.isNotifMacroToggled();
            case CHAT_COMMAND -> cfg.isNotifChatCommand();
            case PROFILE_SWITCHED -> cfg.isNotifProfileSwitched();
            case IMPORT_EXPORT -> cfg.isNotifImportExport();
            case ERROR -> cfg.isNotifErrors();
        };
    }
}
