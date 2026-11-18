package dev.kotandsraka.visuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

public final class KotAndSrakaVisualsClient implements ClientModInitializer {
    public static KeyMapping OPEN_MENU;
    private static boolean wasOnGround = true;

    @Override
    public void onInitializeClient() {
        VisualConfig.load();
        WorldTrailRenderer.register();
        OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.kotandsraka_visuals.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("kotandsraka_visuals", "main"))
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU.consumeClick()) if (client.screen == null) client.setScreen(new VisualConfigScreen(null));
            if (client.player == null || client.level == null) return;
            WorldTrailRenderer.tick();
            boolean ground = client.player.onGround();
            VisualConfig cfg = VisualConfig.get();
            if (cfg.autoSprint && client.player.isAlive() && client.player.onGround() && client.player.zza > 0.0F && !client.player.isSprinting()) {
                client.player.setSprinting(true);
            }
            if (cfg.playerTrail && client.player.getDeltaMovement().horizontalDistanceSqr() > 0.0025) {
                for (int i = 0; i < cfg.trailIntensity; i++) {
                    double ox = (client.level.random.nextDouble() - .5) * .35;
                    double oy = client.level.random.nextDouble() * .9;
                    double oz = (client.level.random.nextDouble() - .5) * .35;
                    client.level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        client.player.getX()+ox, client.player.getY()+oy, client.player.getZ()+oz, 0, .01, 0);
                }
            }
            if (cfg.jumpRings && !ground && wasOnGround) {
                for (int i=0;i<24;i++) {
                    double a = Math.PI*2*i/24.0, r=.7;
                    client.level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        client.player.getX()+Math.cos(a)*r, client.player.getY()+.05, client.player.getZ()+Math.sin(a)*r, 0, .08, 0);
                }
            }
            wasOnGround = ground;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && entity instanceof LivingEntity living) { HitParticles.spawn(living); VisualHud.onHit(living); }
            return InteractionResult.PASS;
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("kotandsraka_visuals","hud"), VisualHud::render);
    }
}
