package dev.kotandsraka.visuals.mixin;

import dev.kotandsraka.visuals.VisualConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla's model-silhouette outline use KotCode's current accent color.
 */
@Mixin(Entity.class)
public abstract class EntityOutlineColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void kotcode$accentPlayerOutline(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Player player) {
            VisualConfig config = VisualConfig.get();
            if (config.friendGlow && VisualConfig.isFriend(player.getName().getString())) {
                cir.setReturnValue(VisualConfig.friendAccentColor() & 0x00FFFFFF);
            } else if (config.playerGlow) {
                cir.setReturnValue(VisualConfig.accentColor() & 0x00FFFFFF);
            }
        }
    }
}
