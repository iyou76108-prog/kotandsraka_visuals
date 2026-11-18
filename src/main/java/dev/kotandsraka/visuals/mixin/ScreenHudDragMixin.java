package dev.kotandsraka.visuals.mixin;

import dev.kotandsraka.visuals.HudLayoutEditor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenHudDragMixin {
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void kotandsraka$beginHudDrag(double mouseX, double mouseY, int button,
                                          CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ChatScreen && button == 1 && HudLayoutEditor.beginDrag(mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void kotandsraka$moveHud(double mouseX, double mouseY, int button, double dragX, double dragY,
                                     CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ChatScreen && button == 1 && HudLayoutEditor.isDragging()) {
            HudLayoutEditor.drag(mouseX, mouseY, width, height);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void kotandsraka$finishHudDrag(double mouseX, double mouseY, int button,
                                           CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ChatScreen && button == 1 && HudLayoutEditor.endDrag()) {
            cir.setReturnValue(true);
        }
    }
}
