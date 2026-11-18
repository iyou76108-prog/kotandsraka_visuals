package dev.kotandsraka.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VisualConfigScreen extends Screen {
    private final Screen parent;

    public VisualConfigScreen(Screen parent) {
        super(Component.literal("KotAndSraka Visuals"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        VisualConfig cfg = VisualConfig.get();
        int left = this.width / 2 - 105;
        int right = this.width / 2 + 5;
        int y = 48;

        addRenderableWidget(toggle("Hit Particles", cfg.hitParticles, left, y, 100, v -> { cfg.hitParticles = !cfg.hitParticles; VisualConfig.save(); rebuild(); }));
        addRenderableWidget(toggle("Critical Particles", cfg.criticalParticles, right, y, 100, v -> { cfg.criticalParticles = !cfg.criticalParticles; VisualConfig.save(); rebuild(); }));
        y += 30;
        addRenderableWidget(toggle("Target HUD", cfg.targetHud, left, y, 100, v -> { cfg.targetHud = !cfg.targetHud; VisualConfig.save(); rebuild(); }));
        addRenderableWidget(toggle("Keystrokes", cfg.keystrokes, right, y, 100, v -> { cfg.keystrokes = !cfg.keystrokes; VisualConfig.save(); rebuild(); }));
        y += 30;
        addRenderableWidget(toggle("Hit Flash", cfg.hitFlash, left, y, 100, v -> { cfg.hitFlash = !cfg.hitFlash; VisualConfig.save(); rebuild(); }));
        addRenderableWidget(toggle("Hit Text", cfg.hitText, right, y, 100, v -> { cfg.hitText = !cfg.hitText; VisualConfig.save(); rebuild(); }));

        y += 42;
        addRenderableWidget(Button.builder(Component.literal("Particle Count: " + cfg.particleCount), b -> {
            cfg.particleCount = cfg.particleCount >= 24 ? 2 : cfg.particleCount + 2;
            VisualConfig.save(); rebuild();
        }).bounds(left, y, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Particle Size: " + String.format("%.1f", cfg.particleScale)), b -> {
            cfg.particleScale = cfg.particleScale >= 2.0f ? 0.5f : cfg.particleScale + 0.25f;
            VisualConfig.save(); rebuild();
        }).bounds(right, y, 100, 20).build());

        y += 30;
        addRenderableWidget(Button.builder(Component.literal("Lifetime: " + cfg.particleLifetimeMs + " ms"), b -> {
            cfg.particleLifetimeMs = cfg.particleLifetimeMs >= 1000 ? 150 : cfg.particleLifetimeMs + 50;
            VisualConfig.save(); rebuild();
        }).bounds(left, y, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            VisualConfig.reset();
            rebuild();
        }).bounds(right, y, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(this.width / 2 - 50, this.height - 32, 100, 20).build());
    }

    private Button toggle(String name, boolean enabled, int x, int y, int w, java.util.function.Consumer<Button> action) {
        return Button.builder(Component.literal(name + ": " + (enabled ? "ON" : "OFF")), action::accept)
            .bounds(x, y, w, 20).build();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        this.renderBackground(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.literal("Right Shift opens this menu"), this.width / 2, 32, 0xFFAAAAAA);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
