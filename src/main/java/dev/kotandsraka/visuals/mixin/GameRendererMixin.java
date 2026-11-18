package dev.kotandsraka.visuals.mixin;

import dev.kotandsraka.visuals.VisualConfig;

/**
 * Compile-safe hook helper. The runtime post-processing injection is disabled
 * until the exact loader target is verified.
 */
public final class GameRendererMixin {
    private GameRendererMixin() {}

    public static boolean motionBlurEnabled() {
        return VisualConfig.get().motionBlur;
    }
}
