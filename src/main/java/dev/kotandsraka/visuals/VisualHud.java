package dev.kotandsraka.visuals;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;

public final class VisualHud {
    private static LivingEntity target;
    private static long hitUntil;
    private static boolean critical;

    private VisualHud() {}

    public static void onHit(LivingEntity living) {
        target = living;
        hitUntil = System.currentTimeMillis() + VisualConfig.get().particleLifetimeMs;
        Minecraft mc = Minecraft.getInstance();
        critical = mc.player != null && mc.player.fallDistance > 0.0F && !mc.player.onGround();
    }

    public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        VisualConfig cfg = VisualConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        if (target != null && (!target.isAlive() || now > hitUntil + 1500L)) target = null;

        if (cfg.hitFlash && now < hitUntil) {
            int alpha = (int)Math.min(70, Math.max(0, (hitUntil - now) * 70 / Math.max(1, cfg.particleLifetimeMs)));
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0xFFFFFF);
        }
        if (cfg.hitText && now < hitUntil) {
            Component text = Component.literal(critical ? "CRITICAL" : "HIT");
            graphics.drawString(mc.font, text, (graphics.guiWidth() - mc.font.width(text)) / 2,
                graphics.guiHeight() / 2 - 32, 0xFFFFFF, true);
        }
        if (cfg.targetHud && target != null) renderTargetHud(graphics, mc);
        if (cfg.keystrokes) renderKeystrokes(graphics, mc);
    }

    private static void renderTargetHud(GuiGraphics g, Minecraft mc) {
        int x = 8, y = 8, w = 150, h = 42;
        g.fill(x, y, x + w, y + h, 0xB0101010);
        g.drawString(mc.font, target.getName().getString(), x + 7, y + 6, 0xFFFFFF, true);
        float health = Math.max(0.0F, target.getHealth());
        float max = Math.max(1.0F, target.getMaxHealth());
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
