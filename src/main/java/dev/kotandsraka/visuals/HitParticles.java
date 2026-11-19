package dev.kotandsraka.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

public final class HitParticles {
    private HitParticles() {}

    public static void spawn(LivingEntity target) {
        VisualConfig cfg = VisualConfig.get();
        if (!cfg.hitParticles) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.55;
        double z = target.getZ();
        boolean crit = mc.player != null && mc.player.fallDistance > 0.0F && !mc.player.onGround();

        int count = Math.max(1, cfg.particleCount);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            double radius = 0.08 + mc.level.random.nextDouble() * cfg.particleSpread;
            double dx;
            double dy;
            double dz;
            if (cfg.particleShape == 1) {
                dx = Math.cos(angle) * radius * cfg.particleSpeed;
                dz = Math.sin(angle) * radius * cfg.particleSpeed;
                dy = 0.015;
            } else if (cfg.particleShape == 2) {
                dx = (mc.level.random.nextDouble() - 0.5) * 0.12;
                dz = (mc.level.random.nextDouble() - 0.5) * 0.12;
                dy = cfg.particleSpeed + mc.level.random.nextDouble() * cfg.particleHeight;
            } else {
                dx = (mc.level.random.nextDouble() - 0.5) * cfg.particleSpread * cfg.particleScale;
                dz = (mc.level.random.nextDouble() - 0.5) * cfg.particleSpread * cfg.particleScale;
                dy = mc.level.random.nextDouble() * cfg.particleHeight * cfg.particleScale;
            }
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, dx, dy, dz);
            if (cfg.criticalParticles && crit) {
                mc.level.addParticle(ParticleTypes.CRIT, x, y, z, dx * 0.5, dy + 0.05, dz * 0.5);
            }
        }
    }
}
