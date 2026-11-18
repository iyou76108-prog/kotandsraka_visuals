package dev.kotandsraka.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

public final class KotAndSrakaVisualsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        VisualConfig.load();
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && entity instanceof LivingEntity living) {
                HitParticles.spawn(living);
                VisualHud.onHit(living);
            }
            return InteractionResult.PASS;
        });
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("kotandsraka_visuals", "hud"),
            VisualHud::render
        );
    }
}
