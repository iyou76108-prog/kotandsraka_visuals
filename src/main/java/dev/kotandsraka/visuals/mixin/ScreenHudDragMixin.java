package dev.kotandsraka.visuals.mixin;

import dev.kotandsraka.visuals.HudLayoutEditor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ScreenHudDragMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void kotandsraka$beginHudDrag(MouseButtonEvent click, boolean doubled,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
            && HudLayoutEditor.beginDrag(click.x(), click.y())) {
            cir.setReturnValue(true);
        }
    }
}
