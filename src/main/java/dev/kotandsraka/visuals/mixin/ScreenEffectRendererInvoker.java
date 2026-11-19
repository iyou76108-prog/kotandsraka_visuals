package dev.kotandsraka.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenEffectRenderer.class)
public interface ScreenEffectRendererInvoker {
    @Invoker("renderFire")
    static void kotandsraka$invokeRenderFire(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite) {
        throw new AssertionError("Mixin invoker was not transformed");
    }
}
