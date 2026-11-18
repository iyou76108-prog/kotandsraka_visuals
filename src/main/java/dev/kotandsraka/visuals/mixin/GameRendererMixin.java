package dev.kotandsraka.visuals.mixin;

import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private static Identifier BLUR_ID;
    @Shadow private void setPostProcessor(Identifier id) {}

    @Inject(method = "render", at = @At("HEAD"))
    private void kotandsraka$motionBlurHook(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        GameRenderer renderer = (GameRenderer) (Object) this;
        if (VisualConfig.get().motionBlur) {
            setPostProcessor(BLUR_ID);
        } else if (BLUR_ID.equals(renderer.getPostProcessorId())) {
            renderer.clearPostProcessor();
        }
    }
}
