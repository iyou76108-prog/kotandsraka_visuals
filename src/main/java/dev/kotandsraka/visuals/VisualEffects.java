package dev.kotandsraka.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

public final class VisualEffects {
    private VisualEffects() {}

    public static void targetRing(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        double y = target.getY() + 0.05;
        for (int i = 0; i < 20; i++) {
            double a = Math.PI * 2 * i / 20.0;
            double r = 0.65 + target.getBbWidth() * 0.25;
            mc.level.addParticle(ParticleTypes.END_ROD, target.getX() + Math.cos(a) * r, y,
                target.getZ() + Math.sin(a) * r, 0, 0.015, 0);
        }
    }

    public static void killEffect(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2 * i / 40.0;
            double r = 0.15 + (i % 5) * 0.08;
            mc.level.addParticle(i % 2 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.CRIT,
                target.getX() + Math.cos(a) * r, target.getY() + target.getBbHeight() * 0.5,
                target.getZ() + Math.sin(a) * r, Math.cos(a) * 0.08,
                0.12 + (i % 4) * 0.03, Math.sin(a) * 0.08);
        }
    }

    public static void playerCosmetics() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        VisualConfig cfg = VisualConfig.get();
        if (cfg.bodyGlow) {
            for (int i = 0; i < 3; i++) {
                double a = mc.player.tickCount * 0.12 + i * 2.1;
                mc.level.addParticle(ParticleTypes.END_ROD,
                    mc.player.getX() + Math.cos(a) * 0.38,
                    mc.player.getY() + 0.55 + Math.sin(a * 1.7) * 0.45,
                    mc.player.getZ() + Math.sin(a) * 0.38, 0, 0.005, 0);
            }
        }
        if (cfg.chinaHat) {
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16.0, r = 0.72;
                mc.level.addParticle(ParticleTypes.END_ROD,
                    mc.player.getX() + Math.cos(a) * r,
                    mc.player.getY() + mc.player.getBbHeight() + 0.08,
                    mc.player.getZ() + Math.sin(a) * r, 0, 0.002, 0);
            }
        }
    }

    public static void worldAmbience() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !VisualConfig.get().worldParticles) return;
        if (mc.player.tickCount % 8 != 0) return;
        double x = mc.player.getX() + (mc.level.random.nextDouble() - .5) * 10;
        double z = mc.player.getZ() + (mc.level.random.nextDouble() - .5) * 10;
        mc.level.addParticle(ParticleTypes.ENCHANT, x,
            mc.player.getY() + 1 + mc.level.random.nextDouble() * 2, z, 0, .01, 0);
    }
}
