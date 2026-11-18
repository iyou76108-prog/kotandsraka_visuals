package dev.kotandsraka.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kotandsraka.visuals.VisualConfig;

/**
 * Compile-safe render transform helper.
 * The actual first-person injection is kept disabled until the loader/mixin target
 * is verified against the exact runtime mapping.
 */
public final class ItemInHandRendererMixin {
    private ItemInHandRendererMixin() {}

    public static void applyViewModel(PoseStack poseStack) {
        VisualConfig c = VisualConfig.get();
        if (!c.viewModel) return;
        if (c.viewModelX != 0 || c.viewModelY != 0 || c.viewModelZ != 0) {
            poseStack.translate(c.viewModelX, c.viewModelY, c.viewModelZ);
        }
        if (c.viewModelScale != 1.0f) {
            poseStack.scale(c.viewModelScale, c.viewModelScale, c.viewModelScale);
        }
    }
}
