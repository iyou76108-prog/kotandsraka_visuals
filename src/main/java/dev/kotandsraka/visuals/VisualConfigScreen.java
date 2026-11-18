package dev.kotandsraka.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VisualConfigScreen extends Screen {
    private final Screen parent;
    private int tab = 0;

    public VisualConfigScreen(Screen parent) {
        super(Component.literal("KotAndSraka Visuals"));
        this.parent = parent;
    }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets();
        VisualConfig cfg = VisualConfig.get();
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(Component.literal("COMBAT"), b -> { tab = 0; rebuild(); }).bounds(cx - 155, 40, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("HUD"), b -> { tab = 1; rebuild(); }).bounds(cx - 50, 40, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("MOVEMENT"), b -> { tab = 2; rebuild(); }).bounds(cx + 55, 40, 95, 20).build());

        int left = cx - 105, right = cx + 5, y = 72;
        if (tab == 0) {
            addRenderableWidget(toggle("Hit Particles", cfg.hitParticles, left, y, v -> { cfg.hitParticles=!cfg.hitParticles; VisualConfig.save(); rebuild(); }));
            addRenderableWidget(toggle("Critical", cfg.criticalParticles, right, y, v -> { cfg.criticalParticles=!cfg.criticalParticles; VisualConfig.save(); rebuild(); }));
            y += 30;
            addRenderableWidget(toggle("Hit Flash", cfg.hitFlash, left, y, v -> { cfg.hitFlash=!cfg.hitFlash; VisualConfig.save(); rebuild(); }));
            addRenderableWidget(toggle("Hit Text", cfg.hitText, right, y, v -> { cfg.hitText=!cfg.hitText; VisualConfig.save(); rebuild(); }));
            y += 42;
            addRenderableWidget(Button.builder(Component.literal("Particle Count: " + cfg.particleCount), b -> { cfg.particleCount=cfg.particleCount>=24?2:cfg.particleCount+2; VisualConfig.save(); rebuild(); }).bounds(left,y,100,20).build());
            addRenderableWidget(Button.builder(Component.literal("Particle Size: " + String.format("%.1f",cfg.particleScale)), b -> { cfg.particleScale=cfg.particleScale>=2f?0.5f:cfg.particleScale+0.25f; VisualConfig.save(); rebuild(); }).bounds(right,y,100,20).build());
            y += 30;
            addRenderableWidget(Button.builder(Component.literal("Lifetime: " + cfg.particleLifetimeMs + " ms"), b -> { cfg.particleLifetimeMs=cfg.particleLifetimeMs>=1000?150:cfg.particleLifetimeMs+50; VisualConfig.save(); rebuild(); }).bounds(left,y,100,20).build());
        } else if (tab == 1) {
            addRenderableWidget(toggle("Target HUD", cfg.targetHud, left, y, v -> { cfg.targetHud=!cfg.targetHud; VisualConfig.save(); rebuild(); }));
            addRenderableWidget(toggle("Keystrokes", cfg.keystrokes, right, y, v -> { cfg.keystrokes=!cfg.keystrokes; VisualConfig.save(); rebuild(); }));
            y += 30;
            addRenderableWidget(toggle("My Armor", cfg.armorHud, left, y, v -> { cfg.armorHud=!cfg.armorHud; VisualConfig.save(); rebuild(); }));
            addRenderableWidget(Button.builder(Component.literal("Armor X: " + cfg.armorHudX), b -> { cfg.armorHudX=cfg.armorHudX>=200?0:cfg.armorHudX+10; VisualConfig.save(); rebuild(); }).bounds(right,y,100,20).build());
            y += 30;
            addRenderableWidget(Button.builder(Component.literal("Armor Y: " + cfg.armorHudY), b -> { cfg.armorHudY=cfg.armorHudY>=150?0:cfg.armorHudY+10; VisualConfig.save(); rebuild(); }).bounds(left,y,100,20).build());
            addRenderableWidget(Button.builder(Component.literal("Reset"), b -> { VisualConfig.reset(); rebuild(); }).bounds(right,y,100,20).build());
        } else {
            addRenderableWidget(toggle("Player Trail", cfg.playerTrail, left, y, v -> { cfg.playerTrail=!cfg.playerTrail; VisualConfig.save(); rebuild(); }));
            y += 30;
            addRenderableWidget(toggle("Auto Sprint", cfg.autoSprint, left, y, v -> { cfg.autoSprint=!cfg.autoSprint; VisualConfig.save(); rebuild(); }));
            addRenderableWidget(toggle("Jump Rings", cfg.jumpRings, right, y, v -> { cfg.jumpRings=!cfg.jumpRings; VisualConfig.save(); rebuild(); }));
            y += 30;
            addRenderableWidget(Button.builder(Component.literal("Trail Intensity: " + cfg.trailIntensity), b -> { cfg.trailIntensity=cfg.trailIntensity>=5?1:cfg.trailIntensity+1; VisualConfig.save(); rebuild(); }).bounds(left,y,100,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(cx-50,this.height-32,100,20).build());
    }

    private Button toggle(String name, boolean enabled, int x, int y, java.util.function.Consumer<Button> action) {
        return Button.builder(Component.literal(name + ": " + (enabled ? "ON" : "OFF")), action::accept).bounds(x,y,100,20).build();
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.renderBackground(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width/2, 20, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "Tabs: Combat / HUD / Movement", this.width/2, 30, 0xFFAAAAAA);
        super.render(g,mouseX,mouseY,delta);
    }

    @Override public void onClose() { if (this.minecraft != null) this.minecraft.setScreen(parent); }
    @Override public boolean shouldCloseOnEsc() { return true; }
}
