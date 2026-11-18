package dev.kotandsraka.visuals;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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
        boolean editingChat = cfg.hudEditMode && mc.screen instanceof ChatScreen;

        if (target != null && (!target.isAlive() || now > hitUntil + 1500L)) target = null;

        if (cfg.hitFlash && now < hitUntil) {
            int alpha = (int) Math.min(70, Math.max(0, (hitUntil - now) * 70 / Math.max(1, cfg.particleLifetimeMs)));
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0xFFFFFF);
        }
        if (cfg.hitText && now < hitUntil) {
            Component text = Component.literal(critical ? "CRITICAL" : "HIT");
            graphics.drawString(mc.font, text, (graphics.guiWidth() - mc.font.width(text)) / 2,
                graphics.guiHeight() / 2 - 32, 0xFFFFFF, true);
        }

        if (cfg.targetHud && (target != null || editingChat)) renderTargetHud(graphics, mc, editingChat);
        if (cfg.armorHud) renderArmorHud(graphics, mc);
        if (cfg.keystrokes) renderKeystrokes(graphics, mc);

        if (editingChat) {
            if (cfg.targetHud) HudLayoutEditor.drawFrame(graphics, cfg.targetHudX, cfg.targetHudY, 150, 42, "TARGET");
            if (cfg.armorHud) HudLayoutEditor.drawFrame(graphics, cfg.armorHudX, cfg.armorHudY, 78, 46, "ARMOR");
            if (cfg.keystrokes) HudLayoutEditor.drawFrame(graphics,
                HudLayoutEditor.keystrokesX(mc, cfg), HudLayoutEditor.keystrokesY(mc, cfg), 78, 68, "KEYS");
            graphics.drawCenteredString(mc.font, "HUD EDIT  •  HOLD RIGHT MOUSE TO MOVE  •  RIGHT SHIFT TO FINISH",
                graphics.guiWidth() / 2, 5, 0xFF68DFFF);
        }
    }

    private static void renderArmorHud(GuiGraphics g, Minecraft mc) {
        VisualConfig cfg = VisualConfig.get();
        int x = cfg.armorHudX;
        int y = cfg.armorHudY;
        g.fill(x - 3, y - 3, x + 75, y + 43, 0xA0101010);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(slots[i]);
            g.renderItem(stack, x + i * 18, y);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int damage = stack.getMaxDamage() - stack.getDamageValue();
                int max = Math.max(1, stack.getMaxDamage());
                int width = Math.max(1, (int) (16.0 * damage / max));
                g.fill(x + i * 18, y + 18, x + i * 18 + 16, y + 20, 0xFF333333);
                g.fill(x + i * 18, y + 18, x + i * 18 + width, y + 20, 0xFF55DD55);
            }
        }
    }

    private static void renderTargetHud(GuiGraphics g, Minecraft mc, boolean editPreview) {
        VisualConfig cfg = VisualConfig.get();
        int x = cfg.targetHudX;
        int y = cfg.targetHudY;
        int w = 150;
        g.fill(x, y, x + w, y + 42, 0xB0101010);
        String name = target == null ? "Target HUD preview" : target.getName().getString();
        g.drawString(mc.font, name, x + 7, y + 6, 0xFFFFFF, true);
        float health = target == null ? 0.75f : Math.max(0.0F, target.getHealth());
        float max = target == null ? 1.0f : Math.max(1.0F, target.getMaxHealth());
        int bar = (int) ((w - 14) * Math.min(1.0F, health / max));
        g.fill(x + 7, y + 20, x + w - 7, y + 27, 0xFF333333);
        g.fill(x + 7, y + 20, x + 7 + bar, y + 27, 0xFF55DD55);
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
        g.fill(x, y, x + 26, y + 20, down ? 0xFF606060 : 0xB0202020);
        g.drawString(mc.font, label, x + (26 - mc.font.width(label)) / 2, y + 6, 0xFFFFFFFF, false);
    }
}
