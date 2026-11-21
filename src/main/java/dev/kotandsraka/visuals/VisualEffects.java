package dev.kotandsraka.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

public final class VisualEffects {
    private VisualEffects() {}

    public static void targetRing(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        double y = target.getY() + 0.05;
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2 * i / 28.0;
            double r = 0.65 + target.getBbWidth() * 0.25;
            mc.level.addParticle(ParticleTypes.END_ROD, target.getX() + Math.cos(a) * r, y,
                target.getZ() + Math.sin(a) * r, 0, 0.015, 0);
        }
    }

    public static void killEffect(LivingEntity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48.0;
            double r = 0.15 + (i % 6) * 0.09;
            mc.level.addParticle(i % 2 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.CRIT,
                target.getX() + Math.cos(a) * r, target.getY() + target.getBbHeight() * 0.5,
                target.getZ() + Math.sin(a) * r, Math.cos(a) * 0.08,
                0.12 + (i % 4) * 0.03, Math.sin(a) * 0.08);
        }
    }

    public static void worldAmbience() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        VisualConfig cfg = VisualConfig.get();
        if (!cfg.worldParticles || mc.player.tickCount % 6 != 0) return;

        int density = Math.max(1, Math.min(8, cfg.worldParticleDensity));
        int mode = Math.floorMod(cfg.worldParticleMode, 4);
        double radius = Math.max(3.0, Math.min(12.0, cfg.worldParticleRadius));

        for (int i = 0; i < density; i++) {
            int effect = mode == 3 ? mc.level.random.nextInt(3) : mode;
            double x = mc.player.getX() + (mc.level.random.nextDouble() - 0.5) * radius * 2.0;
            double z = mc.player.getZ() + (mc.level.random.nextDouble() - 0.5) * radius * 2.0;
            double y = mc.player.getY() + 0.65 + mc.level.random.nextDouble() * 2.4;
            double driftX = (mc.level.random.nextDouble() - 0.5) * 0.025;
            double driftZ = (mc.level.random.nextDouble() - 0.5) * 0.025;

            switch (effect) {
                case 1 -> {
                    double snowY = mc.player.getY() + 2.2 + mc.level.random.nextDouble() * 3.0;
                    mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, snowY, z,
                        driftX, -0.035 - mc.level.random.nextDouble() * 0.035, driftZ);
                }
                case 2 -> {
                    float hue = (mc.player.tickCount * 5.0f + i * (360.0f / density)) % 360.0f;
                    int rgb = VisualConfig.hsvToRgb(hue, 1.0f, 1.0f) & 0x00FFFFFF;
                    DustParticleOptions rainbowDust = new DustParticleOptions(rgb, 1.0f);
                    mc.level.addParticle(rainbowDust, x, y, z, driftX, 0.012, driftZ);
                }
                default -> {
                    mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                        driftX, 0.012, driftZ);
                    if ((i & 1) == 0) {
                        mc.level.addParticle(ParticleTypes.END_ROD, x, y + 0.06, z,
                            driftX * 0.4, 0.018, driftZ * 0.4);
                    }
                }
            }
        }
    }
}
