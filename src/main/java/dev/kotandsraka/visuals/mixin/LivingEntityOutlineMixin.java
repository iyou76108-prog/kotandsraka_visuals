package dev.kotandsraka.visuals.mixin;

import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds player outlines directly to the 1.21.11 entity render state.
 * This avoids changing the entity's synced glowing flag or touching server state.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityOutlineMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void kotcode$applyPlayerOutline(LivingEntity entity, LivingEntityRenderState state,
                                            float partialTick, CallbackInfo ci) {
        if (!(entity instanceof Player player) || player == Minecraft.getInstance().player
            || !player.isAlive()) return;

        VisualConfig cfg = VisualConfig.get();
        boolean friend = cfg.friendGlow && VisualConfig.isFriend(player.getName().getString());
        if (friend) {
            state.outlineColor = 0xFF000000 | (VisualConfig.friendAccentColor() & 0x00FFFFFF);
        } else if (cfg.playerGlow) {
            state.outlineColor = 0xFF000000 | (VisualConfig.accentColor() & 0x00FFFFFF);
        }
    }
}
