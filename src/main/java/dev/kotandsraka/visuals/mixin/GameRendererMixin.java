package dev.kotandsraka.visuals.mixin;

/**
 * Reserved for a future verified post-processing hook.
 * Motion blur is not exposed as an active feature until rendering support is implemented.
 */
public final class GameRendererMixin {
    private GameRendererMixin() {}

    public static boolean motionBlurEnabled() {
        return false;
    }
}
