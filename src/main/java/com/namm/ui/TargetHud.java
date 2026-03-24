package com.namm.ui;

import com.namm.config.NammConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Target HUD panel showing health, armor, totem, and potion effects
 * for the player you're looking at or recently hit.
 * All data from client-side APIs synced by vanilla Minecraft.
 */
public class TargetHud {
	private static final TargetHud INSTANCE = new TargetHud();
	public static TargetHud get() { return INSTANCE; }

	private static final int PANEL_WIDTH = 130;
	private static final int HEAD_SIZE = 8;
	private static final int HEALTH_BAR_HEIGHT = 6;
	private static final int ARMOR_SLOT_SIZE = 6;
	private static final int PADDING = 4;
	private static final int ROW_GAP = 2;
	private static final long TARGET_TIMEOUT_MS = 3000;
	private static final int MAX_EFFECTS = 4;

	private Player currentTarget;
	private long lastTargetTime;

	// Drag state
	private boolean dragging = false;
	private double dragOffsetX, dragOffsetY;

	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			currentTarget = null;
			return;
		}

		// Check crosshair hit
		if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player target) {
			if (target != mc.player) {
				currentTarget = target;
				lastTargetTime = System.currentTimeMillis();
			}
		}

		// Clear if timed out, dead, or out of render distance
		if (currentTarget != null) {
			boolean timedOut = System.currentTimeMillis() - lastTargetTime > TARGET_TIMEOUT_MS;
			boolean dead = !currentTarget.isAlive();
			boolean removed = currentTarget.isRemoved();
			if (timedOut || dead || removed) {
				currentTarget = null;
			}
		}
	}

	public void onAttackEntity(Player target) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && target != mc.player) {
			currentTarget = target;
			lastTargetTime = System.currentTimeMillis();
		}
	}

	/** Render when menu is closed (static, no interaction). */
	public void render(GuiGraphics g, int screenW, int screenH) {
		if (!NammConfig.getInstance().isTargetHudEnabled()) return;
		if (currentTarget == null) return;
		int[] pos = resolvePosition(screenW, screenH);
		renderPanel(g, pos[0], pos[1], screenW, screenH, -1, -1);
	}

	/** Render when NammGuiScreen is open (shows hover, accepts drag). */
	public void renderDraggable(GuiGraphics g, int screenW, int screenH, int mouseX, int mouseY) {
		if (!NammConfig.getInstance().isTargetHudEnabled()) return;
		if (currentTarget == null) return;
		int[] pos = resolvePosition(screenW, screenH);
		renderPanel(g, pos[0], pos[1], screenW, screenH, mouseX, mouseY);
	}

	private int[] resolvePosition(int screenW, int screenH) {
		NammConfig cfg = NammConfig.getInstance();
		int px = cfg.getTargetHudX();
		int py = cfg.getTargetHudY();
		if (px < 0 || py < 0) {
			// Default: bottom-center
			px = (screenW - PANEL_WIDTH) / 2;
			py = screenH - 70;
		}
		// Clamp to screen
		px = Math.max(0, Math.min(px, screenW - PANEL_WIDTH));
		py = Math.max(0, Math.min(py, screenH - 20));
		return new int[]{px, py};
	}

	private void renderPanel(GuiGraphics g, int px, int py, int screenW, int screenH,
							 int mouseX, int mouseY) {
		NammTheme t = NammTheme.get();
		Player target = currentTarget;
		if (target == null) return;

		// Collect effects to determine panel height
		List<String> effects = collectEffects(target);
		boolean hasEffects = !effects.isEmpty();
		int panelH = PADDING + 10 + ROW_GAP + HEALTH_BAR_HEIGHT + ROW_GAP + ARMOR_SLOT_SIZE + PADDING;
		if (hasEffects) {
			panelH += ROW_GAP + 8;
		}

		// Background
		NammRenderer.drawRoundedRect(g, px, py, PANEL_WIDTH, panelH, NammRenderer.RADIUS, t.panelBg());
		NammRenderer.drawRoundedOutline(g, px, py, PANEL_WIDTH, panelH, NammRenderer.RADIUS, t.border());

		// Hover highlight when menu is open
		if (mouseX >= 0 && mouseX >= px && mouseX < px + PANEL_WIDTH
				&& mouseY >= py && mouseY < py + panelH) {
			NammRenderer.drawRoundedRect(g, px, py, PANEL_WIDTH, panelH, NammRenderer.RADIUS, t.hover());
		}

		int cx = px + PADDING;
		int cy = py + PADDING;

		// Row 1: Player head + name + health text
		renderTargetHead(g, target, cx, cy, HEAD_SIZE);
		int nameX = cx + HEAD_SIZE + 3;
		String name = target.getName().getString();
		// Truncate name if too long
		int maxNameW = PANEL_WIDTH - PADDING * 2 - HEAD_SIZE - 3 - 30;
		if (NammRenderer.fontWidth(name) > maxNameW) {
			while (name.length() > 1 && NammRenderer.fontWidth(name + "..") > maxNameW) {
				name = name.substring(0, name.length() - 1);
			}
			name = name + "..";
		}
		NammRenderer.drawText(g, nameX, cy + 1, name, true);

		// Health value right-aligned
		float health = target.getHealth();
		String healthStr = String.valueOf((int) Math.ceil(health));
		NammRenderer.drawTextColored(g, px + PANEL_WIDTH - PADDING - NammRenderer.fontWidth(healthStr),
				cy + 1, healthStr, t.accent());

		cy += 10 + ROW_GAP;

		// Row 2: Health bar
		float maxHealth = target.getMaxHealth();
		float ratio = maxHealth > 0 ? Math.min(health / maxHealth, 1.0f) : 0;
		int barW = PANEL_WIDTH - PADDING * 2;
		// Bar background
		g.fill(cx, cy, cx + barW, cy + HEALTH_BAR_HEIGHT, t.toggleOff());
		// Bar fill
		int fillW = (int) (barW * ratio);
		if (fillW > 0) {
			g.fill(cx, cy, cx + fillW, cy + HEALTH_BAR_HEIGHT, t.accent());
		}
		// Health text right of bar
		String healthFrac = (int) Math.ceil(health) + "/" + (int) maxHealth;
		NammRenderer.drawTextRight(g, px + PANEL_WIDTH - PADDING,
				cy - 1, healthFrac, false);

		cy += HEALTH_BAR_HEIGHT + ROW_GAP;

		// Row 3: Armor slots + totem indicator
		renderArmorSlots(g, target, cx, cy);

		// Totem indicator
		boolean hasTotem = target.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
		if (hasTotem) {
			String totemLabel = "totem";
			NammRenderer.drawTextColored(g, px + PANEL_WIDTH - PADDING - NammRenderer.fontWidth(totemLabel),
					cy - 1, totemLabel, t.accent());
		}

		cy += ARMOR_SLOT_SIZE + ROW_GAP;

		// Row 4: Active effects (if any)
		if (hasEffects) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < effects.size(); i++) {
				if (i > 0) sb.append(" \u00B7 ");
				sb.append(effects.get(i));
			}
			String effectStr = sb.toString();
			// Truncate if too wide
			int maxEffW = PANEL_WIDTH - PADDING * 2;
			if (NammRenderer.fontWidth(effectStr) > maxEffW) {
				while (effectStr.length() > 1 && NammRenderer.fontWidth(effectStr + "..") > maxEffW) {
					effectStr = effectStr.substring(0, effectStr.length() - 1);
				}
				effectStr = effectStr + "..";
			}
			NammRenderer.drawText(g, cx, cy, effectStr, false);
		}
	}

	private void renderTargetHead(GuiGraphics g, Player target, int x, int y, int size) {
		if (!(target instanceof AbstractClientPlayer clientPlayer)) return;
		Identifier skinTexture = clientPlayer.getSkin().body().texturePath();
		g.blit(RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 8.0f, 8.0f, size, size, 8, 8, 64, 64);
		g.blit(RenderPipelines.GUI_TEXTURED, skinTexture, x, y, 40.0f, 8.0f, size, size, 8, 8, 64, 64);
	}

	private void renderArmorSlots(GuiGraphics g, Player target, int x, int y) {
		NammTheme t = NammTheme.get();
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		String[] labels = {"H", "C", "L", "B"};
		int gap = 3;

		for (int i = 0; i < slots.length; i++) {
			int sx = x + i * (ARMOR_SLOT_SIZE + gap);
			boolean equipped = !target.getItemBySlot(slots[i]).isEmpty();
			if (equipped) {
				g.fill(sx, y, sx + ARMOR_SLOT_SIZE, y + ARMOR_SLOT_SIZE, t.textPrimary());
			} else {
				g.renderOutline(sx, y, ARMOR_SLOT_SIZE, ARMOR_SLOT_SIZE, t.toggleOff());
			}
		}
	}

	private List<String> collectEffects(Player target) {
		List<String> result = new ArrayList<>();
		Collection<MobEffectInstance> effects = target.getActiveEffects();
		int count = 0;
		for (MobEffectInstance effect : effects) {
			if (count >= MAX_EFFECTS) break;
			String name = abbreviateEffect(effect.getEffect().value().getDescriptionId());
			int amp = effect.getAmplifier();
			String level = amp > 0 ? " " + toRoman(amp + 1) : "";
			result.add(name + level);
			count++;
		}
		return result;
	}

	private String abbreviateEffect(String descriptionId) {
		// descriptionId is like "effect.minecraft.speed"
		String key = descriptionId;
		int lastDot = key.lastIndexOf('.');
		if (lastDot >= 0) key = key.substring(lastDot + 1);
		return switch (key) {
			case "speed" -> "Spd";
			case "slowness" -> "Slow";
			case "haste" -> "Haste";
			case "mining_fatigue" -> "Fatg";
			case "strength" -> "Str";
			case "instant_health" -> "Heal";
			case "instant_damage" -> "Harm";
			case "jump_boost" -> "Jump";
			case "nausea" -> "Naus";
			case "regeneration" -> "Regen";
			case "resistance" -> "Res";
			case "fire_resistance" -> "FRes";
			case "water_breathing" -> "WBre";
			case "invisibility" -> "Invis";
			case "blindness" -> "Blind";
			case "night_vision" -> "NVis";
			case "hunger" -> "Hung";
			case "weakness" -> "Weak";
			case "poison" -> "Pois";
			case "wither" -> "Wth";
			case "absorption" -> "Abs";
			case "saturation" -> "Sat";
			case "glowing" -> "Glow";
			case "levitation" -> "Lev";
			case "slow_falling" -> "SFall";
			default -> capitalize(key);
		};
	}

	private String toRoman(int num) {
		return switch (num) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> String.valueOf(num);
		};
	}

	private static String capitalize(String s) {
		if (s == null || s.isEmpty()) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	// --- Drag handling (only active when NammGuiScreen is open) ---

	public boolean mousePressed(double mouseX, double mouseY, int button) {
		if (button != 0 || currentTarget == null) return false;
		if (!NammConfig.getInstance().isTargetHudEnabled()) return false;

		Minecraft mc = Minecraft.getInstance();
		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();
		int[] pos = resolvePosition(screenW, screenH);
		int px = pos[0], py = pos[1];
		int panelH = calculatePanelHeight();

		if (mouseX >= px && mouseX < px + PANEL_WIDTH && mouseY >= py && mouseY < py + panelH) {
			dragging = true;
			dragOffsetX = mouseX - px;
			dragOffsetY = mouseY - py;
			return true;
		}
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY) {
		if (!dragging) return false;
		int nx = (int) (mouseX - dragOffsetX);
		int ny = (int) (mouseY - dragOffsetY);
		NammConfig.getInstance().setTargetHudPos(nx, ny);
		return true;
	}

	public boolean mouseReleased() {
		if (!dragging) return false;
		dragging = false;
		NammConfig.getInstance().save();
		return true;
	}

	private int calculatePanelHeight() {
		int panelH = PADDING + 10 + ROW_GAP + HEALTH_BAR_HEIGHT + ROW_GAP + ARMOR_SLOT_SIZE + PADDING;
		if (currentTarget != null && !currentTarget.getActiveEffects().isEmpty()) {
			panelH += ROW_GAP + 8;
		}
		return panelH;
	}
}
