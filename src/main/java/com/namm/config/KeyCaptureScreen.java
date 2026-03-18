package com.namm.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public class KeyCaptureScreen extends Screen {
	private final Screen parent;
	private final BiConsumer<Integer, Boolean> callback;

	public KeyCaptureScreen(Screen parent, BiConsumer<Integer, Boolean> callback) {
		super(Component.translatable("namm.key_capture.title"));
		this.parent = parent;
		this.callback = callback;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0xC0101010);
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(
				this.font,
				"Press the key you want to trigger this macro",
				this.width / 2,
				this.height / 2 - 10,
				0xFFFFFF);
		graphics.drawCenteredString(
				this.font,
				"(keyboard or mouse button)",
				this.width / 2,
				this.height / 2 + 6,
				0x999999);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		callback.accept(keyEvent.key(), false);
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		callback.accept(event.button(), true);
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
