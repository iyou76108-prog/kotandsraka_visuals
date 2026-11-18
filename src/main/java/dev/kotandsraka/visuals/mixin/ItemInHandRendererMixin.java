package dev.kotandsraka.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.joml.AxisAngle4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyVariable(method = "renderHandsWithItems", at = @At("HEAD"), ordinal = 0)
    private PoseStack kotandsraka$transformViewModel(PoseStack poseStack) {
        VisualConfig c = VisualConfig.get();
        if (!c.viewModel) return poseStack;

        if (c.viewModelX != 0 || c.viewModelY != 0 || c.viewModelZ != 0)
            poseStack.translate(c.viewModelX, c.viewModelY, c.viewModelZ);
        if (c.viewModelScale != 1.0f)
            poseStack.scale(c.viewModelScale, c.viewModelScale, c.viewModelScale);
        if (c.viewModelRotX != 0)
            poseStack.mulPose(new AxisAngle4f((float)Math.toRadians(c.viewModelRotX), 1, 0, 0));
        if (c.viewModelRotY != 0)
            poseStack.mulPose(new AxisAngle4f((float)Math.toRadians(c.viewModelRotY), 0, 1, 0));
        if (c.viewModelRotZ != 0)
            poseStack.mulPose(new AxisAngle4f((float)Math.toRadians(c.viewModelRotZ), 0, 0, 1));
        return poseStack;
    }
}
