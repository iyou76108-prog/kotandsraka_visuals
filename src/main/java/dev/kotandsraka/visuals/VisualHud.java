package dev.kotandsraka.visuals;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class VisualHud {
    private static LivingEntity target;
    private static long hitUntil;
    private static long damageUntil;
    private static long skullUntil;
    private static long lastComboHit;
    private static float targetHealthBeforeHit;
    private static float lastDamage;
    private static boolean critical;
    private static boolean killHandled;
    private static boolean attackWasDown;
    private static boolean useWasDown;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();

    private VisualHud() {}

    public static void onHit(LivingEntity living) {
        VisualConfig cfg = VisualConfig.get();
        target = living;
        targetHealthBeforeHit = living.getHealth();
        hitUntil = System.currentTimeMillis() + cfg.particleLifetimeMs;
        killHandled = false;

        long now = System.currentTimeMillis();
        cfg.combo = now - lastComboHit <= 1200L ? cfg.combo + 1 : 1;
        lastComboHit = now;

        Minecraft mc = Minecraft.getInstance();
        critical = mc.player != null && mc.player.fallDistance > 0.0F && !mc.player.onGround();
        if (cfg.targetRing) VisualEffects.targetRing(living);
    }

    public static LivingEntity getTarget() {
        return target;
    }

    public static void onJump() {
        skullUntil = System.currentTimeMillis() + 950L;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        boolean attackDown = mc.options.keyAttack.isDown();
        boolean useDown = mc.options.keyUse.isDown();
        if (attackDown && !attackWasDown) LEFT_CLICKS.addLast(now);
        if (useDown && !useWasDown) RIGHT_CLICKS.addLast(now);
        attackWasDown = attackDown;
        useWasDown = useDown;
        trimClicks(LEFT_CLICKS, now);
        trimClicks(RIGHT_CLICKS, now);

        if (now - lastComboHit > 1200L) VisualConfig.get().combo = 0;

        if (target != null) {
            float health = Math.max(0.0f, target.getHealth());
            if (health < targetHealthBeforeHit) {
                lastDamage = targetHealthBeforeHit - health;
                damageUntil = now + 900L;
            }
            targetHealthBeforeHit = health;
            if (!target.isAlive() && !killHandled) {
                killHandled = true;
                if (VisualConfig.get().killEffect) VisualEffects.killEffect(target);
            }
            if (now > hitUntil + 2500L) target = null;
        }
    }

    private static void trimClicks(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) clicks.removeFirst();
    }

    public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        VisualConfig cfg = VisualConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        boolean editingChat = cfg.hudEditMode && mc.screen instanceof ChatScreen;

        if (cfg.hitFlash && now < hitUntil) {
            int alpha = (int) Math.min(70, Math.max(0, (hitUntil - now) * 70 / Math.max(1, cfg.particleLifetimeMs)));
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0xFFFFFF);
        }
        if (cfg.hitText && now < hitUntil) {
            String text = critical ? "CRITICAL" : "HIT";
            graphics.drawCenteredString(mc.font, text, graphics.guiWidth() / 2,
                graphics.guiHeight() / 2 - 34, VisualConfig.accentColor());
        }
        if (cfg.damageNumbers && now < damageUntil) {
            String damage = String.format("-%.1f", lastDamage);
            int bob = (int) ((damageUntil - now) / 75L);
            graphics.drawCenteredString(mc.font, damage, graphics.guiWidth() / 2 + 28,
                graphics.guiHeight() / 2 - 47 - bob, 0xFFFF6B6B);
        }
        if (cfg.skullJump && now < skullUntil) {
            drawSkull(graphics, graphics.guiWidth() / 2 - 16, graphics.guiHeight() / 2 + 25);
        }
        if (cfg.customCrosshair) renderCrosshair(graphics, mc);

        if (cfg.targetHud && (target != null || editingChat)) renderTargetHud(graphics, mc);
        if (cfg.armorHud) renderArmorHud(graphics, mc);
        if (cfg.keystrokes) renderKeystrokes(graphics, mc);
        if (cfg.comboHud && cfg.combo > 0) {
            graphics.drawCenteredString(mc.font, "COMBO  " + cfg.combo,
                graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 38, VisualConfig.accentColor());
        }

        renderInfoHud(graphics, mc, cfg);
        renderActiveModules(graphics, mc, cfg);

        if (editingChat) {
            if (cfg.targetHud) HudLayoutEditor.drawFrame(graphics, cfg.targetHudX, cfg.targetHudY, 150, 42, "TARGET");
            if (cfg.armorHud) HudLayoutEditor.drawFrame(graphics, cfg.armorHudX, cfg.armorHudY, 78, 46, "ARMOR");
            if (cfg.keystrokes) HudLayoutEditor.drawFrame(graphics,
                HudLayoutEditor.keystrokesX(mc, cfg), HudLayoutEditor.keystrokesY(mc, cfg), 78, 68, "KEYS");
            int statRows = infoHudLineCount(cfg);
            if (statRows > 0) HudLayoutEditor.drawFrame(graphics, cfg.hudX - 5, cfg.hudY - 5, 225, statRows * 12 + 12, "INFO");
            int moduleCount = activeModuleCount(cfg);
            if (cfg.activeModulesHud && moduleCount > 0) {
                HudLayoutEditor.drawFrame(graphics, HudLayoutEditor.modulesX(mc, cfg), cfg.modulesHudY,
                    112, moduleCount * 11 + 22, "MODULES");
            }
            graphics.drawCenteredString(mc.font, "HUD EDIT  •  HOLD RIGHT MOUSE TO MOVE  •  DISABLE EDIT MODE TO FINISH",
                graphics.guiWidth() / 2, 5, VisualConfig.accentColor());
        }
    }

    private static void drawSkull(GuiGraphics g, int x, int y) {
        // Compact pixel-art skull icon that pops up beside the crosshair after a jump.
        String[] pixels = {
            "001111100",
            "011111110",
            "111111111",
            "110110011",
            "111111111",
            "011111110",
            "001010100",
            "001010100"
        };
        int size = 3;
        int color = VisualConfig.accentColor();
        int shadow = 0xCC08111A;
        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[row].length(); col++) {
                if (pixels[row].charAt(col) == '1') {
                    int px = x + col * size;
                    int py = y + row * size;
                    g.fill(px + 1, py + 1, px + size + 1, py + size + 1, shadow);
                    g.fill(px, py, px + size, py + size, color);
                }
            }
        }
        g.drawCenteredString(Minecraft.getInstance().font, "JUMP", x + 13, y + 28, VisualConfig.mutedColor());
    }

    private static void renderCrosshair(GuiGraphics g, Minecraft mc) {
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        int gap = Math.max(2, VisualConfig.get().crosshairGap);
        int thickness = Math.max(1, VisualConfig.get().crosshairThickness);
        int length = Math.max(3, VisualConfig.get().crosshairSize);
        int color = VisualConfig.accentColor();
        int dark = 0xB9000000;
        g.fill(cx - length - gap - 1, cy - thickness / 2 - 1, cx - gap + 1, cy + (thickness + 1) / 2, dark);
        g.fill(cx + gap - 1, cy - thickness / 2 - 1, cx + length + gap + 1, cy + (thickness + 1) / 2, dark);
        g.fill(cx - thickness / 2 - 1, cy - length - gap - 1, cx + (thickness + 1) / 2, cy - gap + 1, dark);
        g.fill(cx - thickness / 2 - 1, cy + gap - 1, cx + (thickness + 1) / 2, cy + length + gap + 1, dark);
        g.fill(cx - length - gap, cy - thickness / 2, cx - gap, cy + (thickness + 1) / 2, color);
        g.fill(cx + gap, cy - thickness / 2, cx + length + gap, cy + (thickness + 1) / 2, color);
        g.fill(cx - thickness / 2, cy - length - gap, cx + (thickness + 1) / 2, cy - gap, color);
        g.fill(cx - thickness / 2, cy + gap, cx + (thickness + 1) / 2, cy + length + gap, color);
    }

    private static void renderInfoHud(GuiGraphics g, Minecraft mc, VisualConfig cfg) {
        List<String> lines = new ArrayList<>();
        if (cfg.fpsHud) lines.add("FPS  " + mc.getFps());
        if (cfg.cpsHud) lines.add("CPS  " + LEFT_CLICKS.size() + " / " + RIGHT_CLICKS.size());
        if (cfg.pingHud) {
            int ping = 0;
            if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
                ping = mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency();
            }
            lines.add("PING  " + ping + " ms");
        }
        if (cfg.coordsHud) lines.add(String.format("XYZ  %d  %d  %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()));
        if (cfg.speedHud) lines.add(String.format("SPEED  %.2f b/s", mc.player.getDeltaMovement().horizontalDistance() * 20.0));
        if (cfg.totemHud) lines.add("TOTEMS  " + countTotems(mc));
        if (cfg.potionHud) lines.add("EFFECTS  " + mc.player.getActiveEffects().size());
        if (cfg.clockHud) lines.add("TIME  " + worldClock(mc));
        if (cfg.compassHud) {
            Direction direction = mc.player.getDirection();
            lines.add("FACING  " + direction.getName().toUpperCase());
        }
        if (lines.isEmpty()) return;

        int x = cfg.hudX;
        int y = cfg.hudY;
        int width = 0;
        for (String line : lines) width = Math.max(width, mc.font.width(line));
        int alpha = Math.max(48, Math.min(230, (int) (cfg.hudOpacity * 255)));
        int background = (alpha << 24) | (VisualConfig.panelColor() & 0x00FFFFFF);
        g.fill(x - 5, y - 5, x + width + 14, y + lines.size() * 12 + 2, background);
        g.fill(x - 5, y - 5, x - 2, y + lines.size() * 12 + 2, VisualConfig.accentColor());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int split = line.indexOf("  ");
            if (split > 0) {
                g.drawString(mc.font, line.substring(0, split), x, y + i * 12, VisualConfig.mutedColor(), false);
                g.drawString(mc.font, line.substring(split + 2), x + mc.font.width(line.substring(0, split)) + 7,
                    y + i * 12, 0xFFFFFFFF, false);
            } else {
                g.drawString(mc.font, line, x, y + i * 12, 0xFFFFFFFF, false);
            }
        }
    }

    private static int countTotems(Minecraft mc) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.is(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
        }
        ItemStack offhand = mc.player.getOffhandItem();
        if (offhand.is(Items.TOTEM_OF_UNDYING)) count += offhand.getCount();
        return count;
    }

    private static String worldClock(Minecraft mc) {
        long ticks = Math.floorMod(mc.level.getDayTime() + 6000L, 24000L);
        int hours = (int) (ticks / 1000L);
        int minutes = (int) ((ticks % 1000L) * 60L / 1000L);
        return String.format("%02d:%02d", hours, minutes);
    }

    private static void renderActiveModules(GuiGraphics g, Minecraft mc, VisualConfig cfg) {
        if (!cfg.activeModulesHud) return;
        List<String> modules = activeModules(cfg);
        if (modules.isEmpty()) return;

        int x = HudLayoutEditor.modulesX(mc, cfg);
        int y = cfg.modulesHudY;
        int width = 0;
        for (String module : modules) width = Math.max(width, mc.font.width(module));
        int panelWidth = width + 14;
        g.fill(x, y, x + panelWidth, y + modules.size() * 12 + 20, VisualConfig.panelColor());
        g.fill(x, y, x + panelWidth, y + 2, VisualConfig.accentColor());
        g.drawString(mc.font, "KOTAND SRAKA", x + 6, y + 6, VisualConfig.accentColor(), false);
        for (int i = 0; i < modules.size(); i++) {
            g.drawString(mc.font, modules.get(i), x + 6, y + 19 + i * 11, 0xFFEAF5FF, false);
        }
    }

    public static int infoHudLineCount(VisualConfig c) {
        int count = 0;
        if (c.fpsHud) count++;
        if (c.cpsHud) count++;
        if (c.pingHud) count++;
        if (c.coordsHud) count++;
        if (c.speedHud) count++;
        if (c.totemHud) count++;
        if (c.potionHud) count++;
        if (c.clockHud) count++;
        if (c.compassHud) count++;
        return count;
    }

    public static int activeModuleCount(VisualConfig c) {
        return activeModules(c).size();
    }

    private static List<String> activeModules(VisualConfig c) {
        List<String> list = new ArrayList<>();
        if (c.hitParticles) list.add("Hit Particles");
        if (c.damageNumbers) list.add("Damage Numbers");
        if (c.killEffect) list.add("Kill Effect");
        if (c.targetEsp) list.add("Target ESP");
        if (c.hitRange) list.add("Hit Range");
        if (c.customCrosshair) list.add("Custom Crosshair");
        if (c.playerTrail) list.add("Player Trail");
        if (c.pearlTrail) list.add("Pearl Tracer");
        if (c.trajectory) list.add("Trajectory");
        if (c.chinaHat) list.add("China Hat");
        if (c.bodyGlow) list.add("Body Glow");
        if (c.worldParticles) list.add("World Particles");
        if (c.autoSprint) list.add("Auto Sprint");
        if (c.skullJump) list.add("Skull Jump");
        if (c.comboHud) list.add("Combo Counter");
        if (c.motionBlur) list.add("Movement Blur");
        if (c.viewModel) list.add("ViewModel");
        if (c.customSwing) list.add("Custom Swing");
        if (c.customFire) list.add("Custom Fire");
        return list;
    }

    private static void renderArmorHud(GuiGraphics g, Minecraft mc) {
        VisualConfig cfg = VisualConfig.get();
        int x = cfg.armorHudX;
        int y = cfg.armorHudY;
        g.fill(x - 3, y - 3, x + 75, y + 43, VisualConfig.panelColor());
        g.fill(x - 3, y - 3, x + 75, y - 2, VisualConfig.accentColor());
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(slots[i]);
            g.renderItem(stack, x + i * 18, y);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int damage = stack.getMaxDamage() - stack.getDamageValue();
                int max = Math.max(1, stack.getMaxDamage());
                int width = Math.max(1, (int) (16.0 * damage / max));
                g.fill(x + i * 18, y + 18, x + i * 18 + 16, y + 20, 0xFF333333);
                g.fill(x + i * 18, y + 18, x + i * 18 + width, y + 20, VisualConfig.accentColor());
            }
        }
    }

    private static void renderTargetHud(GuiGraphics g, Minecraft mc) {
        VisualConfig cfg = VisualConfig.get();
        int x = cfg.targetHudX;
        int y = cfg.targetHudY;
        int w = 150;
        g.fill(x, y, x + w, y + 42, VisualConfig.panelColor());
        g.fill(x, y, x + w, y + 2, VisualConfig.accentColor());
        String name = target == null ? "Target HUD preview" : target.getName().getString();
        g.drawString(mc.font, name, x + 7, y + 6, 0xFFFFFF, true);
        float health = target == null ? 0.75f : Math.max(0.0F, target.getHealth());
        float max = target == null ? 1.0f : Math.max(1.0F, target.getMaxHealth());
        int bar = (int) ((w - 14) * Math.min(1.0F, health / max));
        g.fill(x + 7, y + 20, x + w - 7, y + 27, 0xFF333333);
        g.fill(x + 7, y + 20, x + 7 + bar, y + 27, VisualConfig.accentColor());
        g.drawString(mc.font, target == null ? "Drag to move" : String.format("%.1f / %.1f", health, max),
            x + 7, y + 30, 0xFFE0E0E0, true);
    }

    private static void renderKeystrokes(GuiGraphics g, Minecraft mc) {
        VisualConfig cfg = VisualConfig.get();
        int x = HudLayoutEditor.keystrokesX(mc, cfg);
        int y = HudLayoutEditor.keystrokesY(mc, cfg);
        drawKey(g, mc, "W", x + 26, y, mc.options.keyUp.isDown());
        drawKey(g, mc, "A", x, y + 24, mc.options.keyLeft.isDown());
        drawKey(g, mc, "S", x + 26, y + 24, mc.options.keyDown.isDown());
        drawKey(g, mc, "D", x + 52, y + 24, mc.options.keyRight.isDown());
        drawKey(g, mc, "LMB", x, y + 48, mc.options.keyAttack.isDown());
        drawKey(g, mc, "RMB", x + 52, y + 48, mc.options.keyUse.isDown());
    }

    private static void drawKey(GuiGraphics g, Minecraft mc, String label, int x, int y, boolean down) {
        g.fill(x, y, x + 26, y + 20, down ? VisualConfig.accentColor() : VisualConfig.widgetColor());
        g.drawString(mc.font, label, x + (26 - mc.font.width(label)) / 2, y + 6, 0xFFFFFFFF, false);
    }
}
