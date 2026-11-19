package dev.kotandsraka.visuals.mixin;

import com.mojang.logging.LogUtils;
import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean motionBlurFailed = false;

    @Shadow @Final private Minecraft minecraft;
    @Shadow public abstract void renderBlur();

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void kotandsraka$movementBlur(DeltaTracker tickCounter, CallbackInfo ci) {
        VisualConfig cfg = VisualConfig.get();
        if (!cfg.motionBlur || motionBlurFailed || minecraft.player == null || minecraft.level == null) return;

        // Uses the built-in 1.21.11 blur post effect while the player is moving.
        // This is a lightweight screen-space blur, not accumulated frame-history blur.
        if (minecraft.player.getDeltaMovement().horizontalDistanceSqr() < 0.0125) return;
        try {
            renderBlur();
        } catch (RuntimeException exception) {
            motionBlurFailed = true;
            cfg.motionBlur = false;
            VisualConfig.save();
            LOGGER.error("KotAndSraka Visuals disabled movement blur after a renderer error", exception);
        }
    }
}
