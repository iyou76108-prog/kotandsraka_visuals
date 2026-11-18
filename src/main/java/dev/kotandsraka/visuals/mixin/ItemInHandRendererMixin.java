package dev.kotandsraka.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.client.multiplayer.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.AxisAngle4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void kotandsraka$applyViewModel(float tickProgress, PoseStack poseStack,
                                             SubmitNodeCollector queue, LocalPlayer player,
                                             int light, CallbackInfo ci) {
        VisualConfig c = VisualConfig.get();
        if (!c.viewModel) return;

        if (c.viewModelX != 0.0f || c.viewModelY != 0.0f || c.viewModelZ != 0.0f) {
            poseStack.translate(c.viewModelX, c.viewModelY, c.viewModelZ);
        }
        if (c.viewModelScale != 1.0f) {
            poseStack.scale(c.viewModelScale, c.viewModelScale, c.viewModelScale);
        }
        if (c.viewModelRotX != 0.0f) {
            poseStack.mulPose(new AxisAngle4f((float) Math.toRadians(c.viewModelRotX), 1.0f, 0.0f, 0.0f));
        }
        if (c.viewModelRotY != 0.0f) {
            poseStack.mulPose(new AxisAngle4f((float) Math.toRadians(c.viewModelRotY), 0.0f, 1.0f, 0.0f));
        }
        if (c.viewModelRotZ != 0.0f) {
            poseStack.mulPose(new AxisAngle4f((float) Math.toRadians(c.viewModelRotZ), 0.0f, 0.0f, 1.0f));
        }
    }
}
