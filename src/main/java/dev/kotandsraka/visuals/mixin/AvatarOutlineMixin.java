package dev.kotandsraka.visuals.mixin;

import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies KotCode player/friend outlines to Minecraft 1.21.11's Avatar renderer.
 * Players are rendered by AvatarRenderer in this version, so this hook runs
 * after the player-specific render state has been extracted.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarOutlineMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void kotcode$applyAvatarOutline(Avatar entity, AvatarRenderState state,
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
