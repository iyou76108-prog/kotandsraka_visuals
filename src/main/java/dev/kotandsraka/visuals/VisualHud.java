package dev.kotandsraka.visuals;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayDeque;
import java.util.Deque;

public final class VisualHud {
    private static LivingEntity target;
    private static long hitUntil;
    private static boolean critical;
    private static final Deque<DamagePop> DAMAGE = new ArrayDeque<>();
    private static int cps;
    private static long cpsWindow;
    private static long lastHit;
    private static int combo;

    private record DamagePop(String text, long until) {}

    private VisualHud() {}

    public static void onHit(LivingEntity living) {
        VisualConfig cfg = VisualConfig.get();
        target = living;
        hitUntil = System.currentTimeMillis() + cfg.particleLifetimeMs;
        Minecraft mc = Minecraft.getInstance();
        critical = mc.player != null && mc.player.fallDistance > 0.0F && !mc.player.onGround();

        long now = System.currentTimeMillis();
        cps = now - cpsWindow > 1000 ? 1 : cps + 1;
        cpsWindow = now;
        combo = now - lastHit < 900 ? combo + 1 : 1;
        lastHit = now;
        cfg.combo = combo;

        if (cfg.damageNumbers) DAMAGE.addLast(new DamagePop(critical ? "-CRIT" : "-HIT", now + 650));
        if (cfg.targetEsp) VisualEffects.targetRing(living);
    }

    public static void onTick() {
        long now = System.currentTimeMillis();
        if (target != null && !target.isAlive()) {
            if (VisualConfig.get().killEffect) VisualEffects.killEffect(target);
            target = null;
        }
        while (!DAMAGE.isEmpty() && DAMAGE.peekFirst().until < now) DAMAGE.removeFirst();
        if (now - lastHit > 1200) VisualConfig.get().combo = 0;
    }

    public static void render(GuiGraphics g, DeltaTracker tickCounter) {
        VisualConfig cfg = VisualConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        if (target != null && now > hitUntil + 1500L) target = null;

        if (cfg.hitFlash && now < hitUntil) {
            int alpha = (int)Math.min(70, Math.max(0, (hitUntil - now) * 70 / Math.max(1, cfg.particleLifetimeMs)));
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | 0xFFFFFF);
        }
        if (cfg.hitText && now < hitUntil) {
            Component text = Component.literal(critical ? "CRITICAL" : "HIT");
            g.drawString(mc.font, text, (g.guiWidth() - mc.font.width(text)) / 2,
                g.guiHeight() / 2 - 32, 0xFFFFFF, true);
        }
        if (cfg.customCrosshair) renderCrosshair(g);
        if (cfg.damageNumbers) renderDamage(g, mc);
        if (cfg.targetHud && target != null) renderTargetHud(g, mc);
        if (cfg.armorHud) renderArmorHud(g, mc);
        if (cfg.keystrokes) renderKeystrokes(g, mc);
        renderStats(g, mc);
    }

    private static void renderCrosshair(GuiGraphics g) {
        VisualConfig c = VisualConfig.get();
        int cx = g.guiWidth()/2, cy = g.guiHeight()/2, s = c.crosshairSize, gap = c.crosshairGap, t = c.crosshairThickness;
        g.fill(cx-gap-s, cy-t/2, cx-gap, cy+(t+1)/2, 0xFFFFFFFF);
        g.fill(cx+gap, cy-t/2, cx+gap+s, cy+(t+1)/2, 0xFFFFFFFF);
        g.fill(cx-t/2, cy-gap-s, cx+(t+1)/2, cy-gap, 0xFFFFFFFF);
        g.fill(cx-t/2, cy+gap, cx+(t+1)/2, cy+gap+s, 0xFFFFFFFF);
    }

    private static void renderDamage(GuiGraphics g, Minecraft mc) {
        int i = 0;
        long now = System.currentTimeMillis();
        for (DamagePop pop : DAMAGE) {
            int alpha = (int)Math.max(0, Math.min(255, (pop.until - now) * 255 / 650));
            g.drawCenteredString(mc.font, pop.text, g.guiWidth()/2 + i++ * 14, g.guiHeight()/2 - 48 - i*3, (alpha << 24) | 0xFFFFFF);
        }
    }

    private static void renderStats(GuiGraphics g, Minecraft mc) {
        VisualConfig c = VisualConfig.get();
        int x = c.hudX, y = c.hudY, line = 11, shown = 0;
        if (c.fpsHud) g.drawString(mc.font, "FPS: " + mc.getFps(), x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.cpsHud) g.drawString(mc.font, "CPS: " + cps, x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.pingHud && mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null)
            g.drawString(mc.font, "PING: " + mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency(), x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.coordsHud) g.drawString(mc.font, String.format("XYZ: %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()), x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.speedHud) g.drawString(mc.font, String.format("SPEED: %.2f", mc.player.getDeltaMovement().horizontalDistance()*20), x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.comboHud) g.drawString(mc.font, "COMBO: " + c.combo, x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.totemHud) g.drawString(mc.font, "TOTEM: " + countTotems(mc), x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.potionHud) g.drawString(mc.font, "POTIONS: " + mc.player.getActiveEffects().size(), x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.clockHud) g.drawString(mc.font, "TIME: " + ((mc.level.getDayTime()/1000)%24) + ":" + String.format("%02d",(mc.level.getDayTime()%1000)*60/1000), x, y + shown++*line, 0xFFFFFFFF, true);
        if (c.compassHud) g.drawString(mc.font, "FACING: " + mc.player.getDirection().getName(), x, y + shown*line, 0xFFFFFFFF, true);
    }

    private static int countTotems(Minecraft mc) {
        int count = 0;
        for (ItemStack stack : mc.player.getInventory().items)
            if (stack.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING) count += stack.getCount();
        return count;
    }

    private static void renderArmorHud(GuiGraphics g, Minecraft mc) {
        VisualConfig cfg = VisualConfig.get();
        int x = cfg.armorHudX, y = cfg.armorHudY;
        g.fill(x - 3, y - 3, x + 75, y + 43, 0xA0101010);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(slots[i]);
            g.renderItem(stack, x + i * 18, y);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int damage = stack.getMaxDamage() - stack.getDamageValue(), max = Math.max(1, stack.getMaxDamage());
                int width = Math.max(1, (int)(16.0 * damage / max));
                g.fill(x + i * 18, y + 18, x + i * 18 + 16, y + 20, 0xFF333333);
                g.fill(x + i * 18, y + 18, x + i * 18 + width, y + 20, 0xFF55DD55);
            }
        }
    }

    private static void renderTargetHud(GuiGraphics g, Minecraft mc) {
        int x = 8, y = 8, w = 150, h = 42;
        g.fill(x, y, x + w, y + h, 0xB0101010);
        g.drawString(mc.font, target.getName().getString(), x + 7, y + 6, 0xFFFFFF, true);
        float health = Math.max(0.0F, target.getHealth()), max = Math.max(1.0F, target.getMaxHealth());
        int bar = (int)((w - 14) * Math.min(1.0F, health / max));
        g.fill(x + 7, y + 20, x + w - 7, y + 27, 0xFF333333);
        g.fill(x + 7, y + 20, x + 7 + bar, y + 27, 0xFF55DD55);
        g.drawString(mc.font, String.format("%.1f / %.1f", health, max), x + 7, y + 30, 0xFFE0E0E0, true);
    }

    private static void renderKeystrokes(GuiGraphics g, Minecraft mc) {
        int x = g.guiWidth() - 82, y = g.guiHeight() - 70;
        drawKey(g, mc, "W", x + 26, y, mc.options.keyUp.isDown());
        drawKey(g, mc, "A", x, y + 24, mc.options.keyLeft.isDown());
        drawKey(g, mc, "S", x + 26, y + 24, mc.options.keyDown.isDown());
        drawKey(g, mc, "D", x + 52, y + 24, mc.options.keyRight.isDown());
        drawKey(g, mc, "LMB", x, y + 48, mc.options.keyAttack.isDown());
        drawKey(g, mc, "RMB", x + 52, y + 48, mc.options.keyUse.isDown());
    }

    private static void drawKey(GuiGraphics g, Minecraft mc, String label, int x, int y, boolean down) {
        g.fill(x, y, x + 26, y + 20, down ? 0xFF606060 : 0xB0202020);
        g.drawString(mc.font, label, x + (26 - mc.font.width(label)) / 2, y + 6, 0xFFFFFFFF, false);
    }
}
