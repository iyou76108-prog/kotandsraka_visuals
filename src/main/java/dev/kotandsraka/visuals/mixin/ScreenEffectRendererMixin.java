package dev.kotandsraka.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Redirect(
        method = "renderScreenEffect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
        ),
        require = 0
    )
    private void kotandsraka$customFire(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite) {
        VisualConfig cfg = VisualConfig.get();
        if (!cfg.customFire) {
            ScreenEffectRendererInvoker.kotandsraka$invokeRenderFire(poseStack, buffers, sprite);
            return;
        }

        float height = Math.max(0.12f, Math.min(1.0f, cfg.fireHeight));
        poseStack.pushPose();
        poseStack.translate(0.0, -0.20 * (1.0 - height), 0.0);
        poseStack.scale(1.0f, height, 1.0f);
        ScreenEffectRendererInvoker.kotandsraka$invokeRenderFire(poseStack, buffers, sprite);
        poseStack.popPose();
    }
}
