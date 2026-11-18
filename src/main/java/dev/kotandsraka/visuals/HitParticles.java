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
        double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.55, z = target.getZ();
        for (int i = 0; i < Math.max(1, cfg.particleCount); i++) {
            double dx = (mc.level.random.nextDouble() - 0.5) * 0.45 * cfg.particleScale;
            double dy = mc.level.random.nextDouble() * 0.35 * cfg.particleScale;
            double dz = (mc.level.random.nextDouble() - 0.5) * 0.45 * cfg.particleScale;
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, dx, dy, dz);
            if (cfg.criticalParticles) {
                mc.level.addParticle(ParticleTypes.CRIT, x, y, z, dx * 0.5, dy + 0.05, dz * 0.5);
            }
        }
    }
}
