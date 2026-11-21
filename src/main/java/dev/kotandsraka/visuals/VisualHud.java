package dev.kotandsraka.visuals;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Comparator;

import org.joml.Vector3f;

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
        if (cfg.hitSound && mc.player != null) {
            float pitch = critical ? 1.05f : 0.92f;
            mc.player.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.55f, pitch);
        }
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
        boolean editingChat = mc.screen instanceof ChatScreen;

        if (cfg.hitFlash && now < hitUntil) {
            int alpha = (int) Math.min(70, Math.max(0, (hitUntil - now) * 70 / Math.max(1, cfg.particleLifetimeMs)));
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0xFFFFFF);
        }
        if (cfg.hitText && now < hitUntil) {
            String text = KotCodeLanguage.text(critical ? "КРИТ" : "ПОПАДАНИЕ");
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
        if (cfg.enemyArmorHud) renderEnemyArmorHud(graphics, mc);
        if (cfg.keystrokes) renderKeystrokes(graphics, mc);
        if (cfg.comboHud && cfg.combo > 0) {
            graphics.drawCenteredString(mc.font, KotCodeLanguage.text("КОМБО") + "  " + cfg.combo,
                graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 38, VisualConfig.accentColor());
        }

        renderInfoHud(graphics, mc, cfg);
        if (cfg.potionHud && (!mc.player.getActiveEffects().isEmpty() || editingChat)) {
            renderEffectsHud(graphics, mc, cfg, editingChat);
        }
        renderActiveModules(graphics, mc, cfg);

        if (editingChat) {
            if (cfg.targetHud) HudLayoutEditor.drawFrame(graphics, cfg.targetHudX, cfg.targetHudY, 150, 42, KotCodeLanguage.text("ЦЕЛЬ"));
            if (cfg.armorHud) HudLayoutEditor.drawFrame(graphics, cfg.armorHudX, cfg.armorHudY, 78, 46, KotCodeLanguage.text("БРОНЯ"));
            if (cfg.keystrokes) HudLayoutEditor.drawFrame(graphics,
                HudLayoutEditor.keystrokesX(mc, cfg), HudLayoutEditor.keystrokesY(mc, cfg), 78, 68, KotCodeLanguage.text("КЛАВИШИ"));
            drawStatFrames(graphics, mc, cfg);
            if (cfg.activeModulesHud && activeModuleCount(cfg) > 0) {
                HudLayoutEditor.drawFrame(graphics, HudLayoutEditor.modulesX(mc, cfg), cfg.modulesHudY,
                    activeModulesWidth(mc, cfg), activeModulesHeight(cfg), KotCodeLanguage.text("МОДУЛИ"));
            }
            if (cfg.potionHud) {
                HudLayoutEditor.drawFrame(graphics, cfg.effectsHudX, cfg.effectsHudY,
                    effectsHudWidth(mc, cfg), effectsHudHeight(mc, cfg), KotCodeLanguage.text("ЭФФЕКТЫ"));
            }
            graphics.drawCenteredString(mc.font, KotCodeLanguage.text("ПЕРЕМЕЩЕНИЕ HUD  •  ЗАЖМИ ЛКМ НА ПАНЕЛИ И ПЕРЕТАЩИ ЕЁ"),
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
        g.drawCenteredString(Minecraft.getInstance().font, KotCodeLanguage.text("ПРЫЖОК"), x + 13, y + 28, VisualConfig.mutedColor());
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

    public static final int STAT_FPS = 0;
    public static final int STAT_CPS = 1;
    public static final int STAT_PING = 2;
    public static final int STAT_COORDS = 3;
    public static final int STAT_SPEED = 4;
    public static final int STAT_TOTEMS = 5;
    public static final int STAT_CLOCK = 6;
    public static final int STAT_DIRECTION = 7;

    private static final int[] STAT_IDS = {
        STAT_FPS, STAT_CPS, STAT_PING, STAT_COORDS,
        STAT_SPEED, STAT_TOTEMS, STAT_CLOCK, STAT_DIRECTION
    };

    private static void renderInfoHud(GuiGraphics graphics, Minecraft mc, VisualConfig cfg) {
        for (int id : STAT_IDS) {
            if (!isStatEnabled(cfg, id)) continue;
            String text = statText(mc, cfg, id);
            if (text == null) continue;

            int x = statX(cfg, id);
            int y = statY(cfg, id);
            int width = statWidth(mc, cfg, id);
            graphics.fill(x, y, x + width, y + 20, VisualConfig.panelColor());
            graphics.fill(x, y, x + 2, y + 20, VisualConfig.accentColor());
            graphics.fill(x + 2, y, x + width, y + 2, VisualConfig.accentColor());
            int split = text.indexOf("  ");
            if (split > 0) {
                graphics.drawString(mc.font, text.substring(0, split), x + 7, y + 6,
                    VisualConfig.mutedColor(), false);
                graphics.drawString(mc.font, text.substring(split + 2),
                    x + 7 + mc.font.width(text.substring(0, split)) + 6, y + 6,
                    0xFFFFFFFF, false);
            } else {
                graphics.drawString(mc.font, text, x + 7, y + 6, 0xFFFFFFFF, false);
            }
        }
    }

    private static void drawStatFrames(GuiGraphics graphics, Minecraft mc, VisualConfig cfg) {
        for (int id : STAT_IDS) {
            if (!isStatEnabled(cfg, id)) continue;
            HudLayoutEditor.drawFrame(graphics, statX(cfg, id), statY(cfg, id),
                statWidth(mc, cfg, id), 20, statFrameLabel(id));
        }
    }

    private static String statFrameLabel(int id) {
        return switch (id) {
            case STAT_FPS -> KotCodeLanguage.text("КАДРЫ");
            case STAT_CPS -> KotCodeLanguage.text("КЛИКИ");
            case STAT_PING -> KotCodeLanguage.text("ПИНГ");
            case STAT_COORDS -> "XYZ";
            case STAT_SPEED -> KotCodeLanguage.text("СКОРОСТЬ");
            case STAT_TOTEMS -> KotCodeLanguage.text("ТОТЕМЫ");
            case STAT_CLOCK -> KotCodeLanguage.text("ВРЕМЯ");
            case STAT_DIRECTION -> KotCodeLanguage.text("НАПРАВЛЕНИЕ");
            default -> "HUD";
        };
    }

    public static boolean isStatEnabled(VisualConfig cfg, int id) {
        return switch (id) {
            case STAT_FPS -> cfg.fpsHud;
            case STAT_CPS -> cfg.cpsHud;
            case STAT_PING -> cfg.pingHud;
            case STAT_COORDS -> cfg.coordsHud;
            case STAT_SPEED -> cfg.speedHud;
            case STAT_TOTEMS -> cfg.totemHud;
            case STAT_CLOCK -> cfg.clockHud;
            case STAT_DIRECTION -> cfg.compassHud;
            default -> false;
        };
    }

    public static int statX(VisualConfig cfg, int id) {
        return switch (id) {
            case STAT_FPS -> cfg.fpsHudX;
            case STAT_CPS -> cfg.cpsHudX;
            case STAT_PING -> cfg.pingHudX;
            case STAT_COORDS -> cfg.coordsHudX;
            case STAT_SPEED -> cfg.speedHudX;
            case STAT_TOTEMS -> cfg.totemHudX;
            case STAT_CLOCK -> cfg.clockHudX;
            case STAT_DIRECTION -> cfg.compassHudX;
            default -> 0;
        };
    }

    public static int statY(VisualConfig cfg, int id) {
        return switch (id) {
            case STAT_FPS -> cfg.fpsHudY;
            case STAT_CPS -> cfg.cpsHudY;
            case STAT_PING -> cfg.pingHudY;
            case STAT_COORDS -> cfg.coordsHudY;
            case STAT_SPEED -> cfg.speedHudY;
            case STAT_TOTEMS -> cfg.totemHudY;
            case STAT_CLOCK -> cfg.clockHudY;
            case STAT_DIRECTION -> cfg.compassHudY;
            default -> 0;
        };
    }

    public static void setStatPosition(VisualConfig cfg, int id, int x, int y) {
        switch (id) {
            case STAT_FPS -> { cfg.fpsHudX = x; cfg.fpsHudY = y; }
            case STAT_CPS -> { cfg.cpsHudX = x; cfg.cpsHudY = y; }
            case STAT_PING -> { cfg.pingHudX = x; cfg.pingHudY = y; }
            case STAT_COORDS -> { cfg.coordsHudX = x; cfg.coordsHudY = y; }
            case STAT_SPEED -> { cfg.speedHudX = x; cfg.speedHudY = y; }
            case STAT_TOTEMS -> { cfg.totemHudX = x; cfg.totemHudY = y; }
            case STAT_CLOCK -> { cfg.clockHudX = x; cfg.clockHudY = y; }
            case STAT_DIRECTION -> { cfg.compassHudX = x; cfg.compassHudY = y; }
            default -> { }
        }
    }

    public static String statText(Minecraft mc, VisualConfig cfg, int id) {
        if (mc.player == null) return "";
        return switch (id) {
            case STAT_FPS -> KotCodeLanguage.text("КАДРЫ") + "  " + mc.getFps();
            case STAT_CPS -> KotCodeLanguage.text("КЛИКИ") + "  " + LEFT_CLICKS.size() + " / " + RIGHT_CLICKS.size();
            case STAT_PING -> {
                int ping = 0;
                if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
                    ping = mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency();
                }
                yield KotCodeLanguage.text("ПИНГ") + "  " + ping + KotCodeLanguage.text(" мс");
            }
            case STAT_COORDS -> String.format("XYZ  %d  %d  %d",
                mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
            case STAT_SPEED -> String.format(KotCodeLanguage.text("СКОРОСТЬ") + "  %.2f" + KotCodeLanguage.text(" бл/с"),
                mc.player.getDeltaMovement().horizontalDistance() * 20.0);
            case STAT_TOTEMS -> KotCodeLanguage.text("ТОТЕМЫ") + "  " + countTotems(mc);
            case STAT_CLOCK -> KotCodeLanguage.text("ВРЕМЯ") + "  " + worldClock(mc);
            case STAT_DIRECTION -> KotCodeLanguage.text("НАПРАВЛЕНИЕ") + "  " + mc.player.getDirection().getName().toUpperCase();
            default -> "";
        };
    }

    public static int statWidth(Minecraft mc, VisualConfig cfg, int id) {
        String text = statText(mc, cfg, id);
        return Math.max(48, mc.font.width(text) + 14);
    }

    public static int statHeight() {
        return 20;
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
        g.drawString(mc.font, "KOTCODE", x + 6, y + 6, VisualConfig.accentColor(), false);
        for (int i = 0; i < modules.size(); i++) {
            g.drawString(mc.font, modules.get(i), x + 6, y + 19 + i * 11, 0xFFEAF5FF, false);
        }
    }

    public static int activeModuleCount(VisualConfig c) {
        return activeModules(c).size();
    }

    private static List<String> activeModules(VisualConfig c) {
        List<String> list = new ArrayList<>();
        if (c.hitParticles) list.add(KotCodeLanguage.text("Частицы при ударе"));
        if (c.damageNumbers) list.add(KotCodeLanguage.text("Числа урона"));
        if (c.killEffect) list.add(KotCodeLanguage.text("Эффект убийства"));
        if (c.targetEsp) list.add(KotCodeLanguage.text("Обводка цели"));
        if (c.hitRange) list.add(KotCodeLanguage.text("Дальность удара"));
        if (c.customCrosshair) list.add(KotCodeLanguage.text("Настраиваемый прицел"));
        if (c.playerTrail) list.add(KotCodeLanguage.text("След игрока"));
        if (c.pearlTrail) list.add(KotCodeLanguage.text("След жемчуга"));
        if (c.trajectory) list.add(KotCodeLanguage.text("Траектория"));
        if (c.chinaHat) list.add(KotCodeLanguage.text("Шляпа"));
        if (c.bodyGlow) list.add(KotCodeLanguage.text("Свечение тела"));
        if (c.worldParticles) list.add(KotCodeLanguage.text("Частицы мира"));
        if (c.autoSprint) list.add(KotCodeLanguage.text("Авто-бег"));
        if (c.skullJump) list.add(KotCodeLanguage.text("Череп при прыжке"));
        if (c.comboHud) list.add(KotCodeLanguage.text("Счётчик комбо"));
        if (c.viewModel) list.add(KotCodeLanguage.text("Положение рук"));
        if (c.customSwing) list.add(KotCodeLanguage.text("Настройка удара"));
        if (c.customFire) list.add(KotCodeLanguage.text("Огонь от первого лица"));
        if (c.playerGlow) list.add(KotCodeLanguage.text("Обводка силуэта игроков"));
        if (c.friendGlow) list.add(KotCodeLanguage.text("Обводка друзей"));
        if (c.chestGlow) list.add(KotCodeLanguage.text("Свечение сундуков"));
        if (c.enemyArmorHud) list.add(KotCodeLanguage.text("Броня противников"));
        if (c.fpsHud) list.add(KotCodeLanguage.text("Кадры"));
        if (c.cpsHud) list.add(KotCodeLanguage.text("Клики"));
        if (c.pingHud) list.add(KotCodeLanguage.text("Пинг"));
        if (c.coordsHud) list.add(KotCodeLanguage.text("Координаты"));
        if (c.speedHud) list.add(KotCodeLanguage.text("Скорость"));
        if (c.totemHud) list.add(KotCodeLanguage.text("Тотемы"));
        if (c.clockHud) list.add(KotCodeLanguage.text("Время"));
        if (c.compassHud) list.add(KotCodeLanguage.text("Направление"));
        return list;
    }

    public static int activeModulesWidth(Minecraft mc, VisualConfig cfg) {
        List<String> modules = activeModules(cfg);
        if (modules.isEmpty()) return 0;
        int width = mc.font.width("KOTCODE");
        for (String module : modules) width = Math.max(width, mc.font.width(module));
        return width + 14;
    }

    public static int activeModulesHeight(VisualConfig cfg) {
        return activeModules(cfg).size() * 12 + 20;
    }

    private static List<String> effectLines(Minecraft mc, boolean preview) {
        List<String> lines = new ArrayList<>();
        if (mc.player == null) return preview ? List.of(KotCodeLanguage.text("Нет активных эффектов")) : lines;

        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        effects.sort(Comparator.comparing(effect -> effect.getEffect().value().getDisplayName().getString()));
        for (MobEffectInstance effect : effects) {
            String name = effect.getEffect().value().getDisplayName().getString();
            String level = effectLevel(effect.getAmplifier() + 1);
            String duration = effect.isInfiniteDuration() ? "∞" : formatEffectDuration(effect.getDuration());
            lines.add(name + " " + level + "   " + duration);
        }
        if (lines.isEmpty() && preview) lines.add(KotCodeLanguage.text("Нет активных эффектов"));
        return lines;
    }

    private static String effectLevel(int level) {
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level <= roman.length ? roman[level - 1] : KotCodeLanguage.text("ур. ") + level;
    }

    private static String formatEffectDuration(int ticks) {
        if (ticks < 0) return "∞";
        int seconds = ticks / 20;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public static int effectsHudWidth(Minecraft mc, VisualConfig cfg) {
        List<String> lines = effectLines(mc, true);
        int width = mc.font.width(KotCodeLanguage.text("ЭФФЕКТЫ"));
        for (String line : lines) width = Math.max(width, mc.font.width(line));
        return Math.max(118, width + 16);
    }

    public static int effectsHudHeight(Minecraft mc, VisualConfig cfg) {
        return effectLines(mc, true).size() * 12 + 22;
    }

    private static void renderEffectsHud(GuiGraphics graphics, Minecraft mc, VisualConfig cfg, boolean preview) {
        List<String> lines = effectLines(mc, preview);
        if (lines.isEmpty()) return;

        int x = cfg.effectsHudX;
        int y = cfg.effectsHudY;
        int width = effectsHudWidth(mc, cfg);
        int height = effectsHudHeight(mc, cfg);
        graphics.fill(x, y, x + width, y + height, VisualConfig.panelColor());
        graphics.fill(x, y, x + width, y + 2, VisualConfig.accentColor());
        graphics.fill(x, y + 2, x + 2, y + height, VisualConfig.accentColor());
        graphics.drawString(mc.font, KotCodeLanguage.text("ЭФФЕКТЫ"), x + 7, y + 6, VisualConfig.accentColor(), true);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(mc.font, lines.get(i), x + 7, y + 19 + i * 12,
                0xFFF0F0F4, false);
        }
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

    /**
     * Draws the four equipped armor items above each other player's head.
     * Positions are projected from world space into the scaled HUD viewport.
     */
    private static void renderEnemyArmorHud(GuiGraphics g, Minecraft mc) {
        if (mc.level == null || mc.player == null || g.guiHeight() <= 0) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(camera.rotation());
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(camera.rotation());
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(camera.rotation());

        double fov = Math.toRadians(Math.max(30.0, Math.min(110.0, mc.options.fov().get())));
        double tanHalfFov = Math.tan(fov * 0.5);
        double aspect = (double) g.guiWidth() / (double) g.guiHeight();
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] slotNames = {KotCodeLanguage.text("Шлем"), KotCodeLanguage.text("Нагрудник"), KotCodeLanguage.text("Поножи"), KotCodeLanguage.text("Ботинки")};

        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive() || player.isInvisibleTo(mc.player)) continue;
            if (player.distanceToSqr(mc.player) > 48.0 * 48.0) continue;

            ItemStack[] armor = new ItemStack[slots.length];
            boolean hasArmor = false;
            List<String> enchantRows = new ArrayList<>();
            for (int i = 0; i < slots.length; i++) {
                armor[i] = player.getItemBySlot(slots[i]);
                if (armor[i].isEmpty()) continue;
                hasArmor = true;

                List<String> enchantNames = new ArrayList<>();
                for (var entry : armor[i].getEnchantments().entrySet()) {
                    // getFullname supplies the translated enchantment name and Roman-numeral level.
                    enchantNames.add(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
                }
                if (!enchantNames.isEmpty()) {
                    enchantRows.add(slotNames[i] + ": " + String.join(", ", enchantNames));
                }
            }
            if (!hasArmor) continue;

            double worldX = player.getX();
            double worldY = player.getY() + player.getBbHeight() + 0.30;
            double worldZ = player.getZ();
            double dx = worldX - cameraPos.x;
            double dy = worldY - cameraPos.y;
            double dz = worldZ - cameraPos.z;

            double depth = dx * forward.x + dy * forward.y + dz * forward.z;
            if (depth <= 0.05) continue;

            double cameraRight = dx * right.x + dy * right.y + dz * right.z;
            double cameraUp = dx * up.x + dy * up.y + dz * up.z;
            double ndcX = cameraRight / (depth * tanHalfFov * aspect);
            double ndcY = cameraUp / (depth * tanHalfFov);
            if (ndcX < -1.15 || ndcX > 1.15 || ndcY < -1.15 || ndcY > 1.15) continue;

            int centerX = (int) Math.round((ndcX * 0.5 + 0.5) * g.guiWidth());
            int centerY = (int) Math.round((0.5 - ndcY * 0.5) * g.guiHeight());

            int panelWidth = 78;
            for (String row : enchantRows) {
                panelWidth = Math.max(panelWidth, Math.min(220, mc.font.width(row) + 10));
            }
            int textWidth = panelWidth - 10;
            List<FormattedCharSequence> enchantLines = new ArrayList<>();
            for (String row : enchantRows) {
                enchantLines.addAll(mc.font.split(Component.literal(row), textWidth));
            }

            int panelHeight = 24 + (enchantLines.isEmpty() ? 0 : 3 + enchantLines.size() * 9);
            int x = centerX - panelWidth / 2;
            // Keep the complete panel above the player's head, including wrapped enchantment lines.
            int y = centerY - panelHeight - 2;

            g.fill(x, y, x + panelWidth, y + panelHeight, 0xCC090C12);
            g.fill(x, y, x + panelWidth, y + 1, VisualConfig.accentColor());

            int iconStartX = x + (panelWidth - 78) / 2;
            for (int i = 0; i < armor.length; i++) {
                int iconX = iconStartX + 3 + i * 18;
                int iconY = y + 3;
                ItemStack stack = armor[i];
                g.fill(iconX, iconY, iconX + 16, iconY + 16, 0x66545B69);
                if (stack.isEmpty()) continue;

                g.renderItem(stack, iconX, iconY);
                if (stack.isDamageableItem()) {
                    int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
                    int barWidth = Math.max(0, Math.min(16,
                        (int) Math.round(16.0 * remaining / Math.max(1, stack.getMaxDamage()))));
                    g.fill(iconX, y + 20, iconX + 16, y + 22, 0xFF292D36);
                    if (barWidth > 0) {
                        g.fill(iconX, y + 20, iconX + barWidth, y + 22, VisualConfig.accentColor());
                    }
                }
            }

            int textY = y + 26;
            for (FormattedCharSequence line : enchantLines) {
                g.drawString(mc.font, line, x + 5, textY, 0xFFF0F0F4);
                textY += 9;
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
        String name = target == null ? KotCodeLanguage.text("Предпросмотр HUD цели") : target.getName().getString();
        g.drawString(mc.font, name, x + 7, y + 6, 0xFFFFFF, true);
        float health = target == null ? 0.75f : Math.max(0.0F, target.getHealth());
        float max = target == null ? 1.0f : Math.max(1.0F, target.getMaxHealth());
        int bar = (int) ((w - 14) * Math.min(1.0F, health / max));
        g.fill(x + 7, y + 20, x + w - 7, y + 27, 0xFF333333);
        g.fill(x + 7, y + 20, x + 7 + bar, y + 27, VisualConfig.accentColor());
        g.drawString(mc.font, target == null ? KotCodeLanguage.text("Перетащите мышью") : String.format("%.1f / %.1f", health, max),
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
        drawKey(g, mc, KotCodeLanguage.text("ЛКМ"), x, y + 48, mc.options.keyAttack.isDown());
        drawKey(g, mc, KotCodeLanguage.text("ПКМ"), x + 52, y + 48, mc.options.keyUse.isDown());
    }

    private static void drawKey(GuiGraphics g, Minecraft mc, String label, int x, int y, boolean down) {
        g.fill(x, y, x + 26, y + 20, down ? VisualConfig.accentColor() : VisualConfig.widgetColor());
        g.drawString(mc.font, label, x + (26 - mc.font.width(label)) / 2, y + 6, 0xFFFFFFFF, false);
    }
}
