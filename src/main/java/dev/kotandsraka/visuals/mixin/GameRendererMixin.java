package dev.kotandsraka.visuals.mixin;

import dev.kotandsraka.visuals.VisualConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void kotandsraka$renderLevelHook(CallbackInfo ci) {
        // Render hook reserved for world-space visuals that need the camera/level render phase.
        // Feature toggles live in VisualConfig and are consumed by future world render passes.
        if (VisualConfig.get().motionBlur) {
            // Kept intentionally lightweight: no framebuffer mutation on unsupported render paths.
        }
    }
}
