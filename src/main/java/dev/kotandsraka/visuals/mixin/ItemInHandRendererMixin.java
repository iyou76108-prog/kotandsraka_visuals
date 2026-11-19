package dev.kotandsraka.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void kotandsraka$applyViewModel(AbstractClientPlayer player, float tickProgress, float pitch,
                                            InteractionHand hand, float swingProgress, ItemStack item,
                                            float equipProgress, PoseStack poseStack,
                                            SubmitNodeCollector collector, int packedLight,
                                            CallbackInfo ci) {
        VisualConfig cfg = VisualConfig.get();
        if (!cfg.viewModel && !cfg.customSwing) return;

        poseStack.pushPose();

        if (cfg.viewModel) {
            float scale = clamp(cfg.viewModelScale, 0.25f, 2.0f);
            float side = hand == InteractionHand.OFF_HAND ? -1.0f : 1.0f;
            poseStack.translate(cfg.viewModelX * side, cfg.viewModelY, cfg.viewModelZ);
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Axis.XP.rotationDegrees(cfg.viewModelRotX));
            poseStack.mulPose(Axis.YP.rotationDegrees(cfg.viewModelRotY));
            poseStack.mulPose(Axis.ZP.rotationDegrees(cfg.viewModelRotZ));
        }

        if (cfg.customSwing) {
            float progress = clamp(swingProgress, 0.0f, 1.0f);
            float wave = (float) Math.sin(progress * Math.PI);
            float side = hand == InteractionHand.OFF_HAND ? -1.0f : 1.0f;
            poseStack.mulPose(Axis.XP.rotationDegrees(cfg.swingRotationX * wave));
            poseStack.mulPose(Axis.YP.rotationDegrees(cfg.swingRotationY * wave * side));
            poseStack.mulPose(Axis.ZP.rotationDegrees(cfg.swingRotationZ * wave * side));
        }
    }

    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float kotandsraka$customSwingProgress(float original) {
        VisualConfig cfg = VisualConfig.get();
        if (!cfg.customSwing) return original;
        float speed = clamp(cfg.swingSpeed, 0.25f, 3.0f);
        float strength = clamp(cfg.swingStrength, 0.1f, 1.5f);
        float progressed = clamp(original * speed, 0.0f, 1.0f);
        float eased = (float) Math.sin(progressed * Math.PI * 0.5);
        return clamp(eased * strength, 0.0f, 1.0f);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void kotandsraka$restoreViewModel(AbstractClientPlayer player, float tickProgress, float pitch,
                                               InteractionHand hand, float swingProgress, ItemStack item,
                                               float equipProgress, PoseStack poseStack,
                                               SubmitNodeCollector collector, int packedLight,
                                               CallbackInfo ci) {
        VisualConfig cfg = VisualConfig.get();
        if (cfg.viewModel || cfg.customSwing) poseStack.popPose();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
