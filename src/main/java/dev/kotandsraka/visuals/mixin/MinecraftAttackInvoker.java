package dev.kotandsraka.visuals.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accesses Minecraft's own attack-start routine so auto-clicks follow the
 * normal client attack path (including hit detection and game-mode cooldowns).
 */
@Mixin(Minecraft.class)
public interface MinecraftAttackInvoker {
    @Invoker("startAttack")
    boolean kotcode$startAttack();
}
