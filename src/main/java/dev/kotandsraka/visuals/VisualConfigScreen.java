package dev.kotandsraka.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VisualConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 306;
    private final Screen parent;
    private int tab = 0;

    public VisualConfigScreen(Screen parent) {
        super(Component.literal("KotAndSraka Visuals"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private void rebuild() {
        clearWidgets();
        VisualConfig cfg = VisualConfig.get();
        int left = panelLeft();
        int top = panelTop();
        int sideX = left + 16;
        int contentX = left + 132;
        int col2 = contentX + 164;
        int row1 = top + 78;
        int row2 = top + 128;
        int row3 = top + 178;
        int row4 = top + 228;

        addTab("COMBAT", 0, sideX, top + 72);
        addTab("HUD", 1, sideX, top + 106);
        addTab("MOVEMENT", 2, sideX, top + 140);
        addTab("VISUALS", 3, sideX, top + 174);

        if (tab == 0) {
            addToggle("Hit Particles", cfg.hitParticles, contentX, row1, () -> cfg.hitParticles = !cfg.hitParticles);
            addToggle("Critical Particles", cfg.criticalParticles, col2, row1, () -> cfg.criticalParticles = !cfg.criticalParticles);
            addToggle("Hit Flash", cfg.hitFlash, contentX, row2, () -> cfg.hitFlash = !cfg.hitFlash);
            addToggle("Hit Text", cfg.hitText, col2, row2, () -> cfg.hitText = !cfg.hitText);
            addCycle("Particle Count: " + cfg.particleCount, contentX, row3,
                () -> cfg.particleCount = cfg.particleCount >= 24 ? 2 : cfg.particleCount + 2);
            addCycle("Particle Size: " + String.format("%.2f", cfg.particleScale), col2, row3,
                () -> cfg.particleScale = cfg.particleScale >= 2.0f ? 0.5f : cfg.particleScale + 0.25f);
            addCycle("Lifetime: " + cfg.particleLifetimeMs + " ms", contentX, row4,
                () -> cfg.particleLifetimeMs = cfg.particleLifetimeMs >= 1000 ? 150 : cfg.particleLifetimeMs + 50);
        } else if (tab == 1) {
            addToggle("Target HUD", cfg.targetHud, contentX, row1, () -> cfg.targetHud = !cfg.targetHud);
            addToggle("Armor HUD", cfg.armorHud, col2, row1, () -> cfg.armorHud = !cfg.armorHud);
            addToggle("Keystrokes", cfg.keystrokes, contentX, row2, () -> cfg.keystrokes = !cfg.keystrokes);
            addToggle("Edit HUD in Chat: " + (cfg.hudEditMode ? "ON" : "OFF"), cfg.hudEditMode, col2, row2,
                () -> cfg.hudEditMode = !cfg.hudEditMode);
            addCycle("Reset HUD Positions", contentX, row3, VisualConfig::resetHudPositions);
        } else if (tab == 2) {
            addToggle("Player Trail", cfg.playerTrail, contentX, row1, () -> cfg.playerTrail = !cfg.playerTrail);
            addToggle("Auto Sprint", cfg.autoSprint, col2, row1, () -> cfg.autoSprint = !cfg.autoSprint);
            addToggle("Jump Rings", cfg.jumpRings, contentX, row2, () -> cfg.jumpRings = !cfg.jumpRings);
            addCycle("Trail Intensity: " + cfg.trailIntensity, col2, row2,
                () -> cfg.trailIntensity = cfg.trailIntensity >= 5 ? 1 : cfg.trailIntensity + 1);
        } else {
            addToggle("China Hat", cfg.chinaHat, contentX, row1, () -> cfg.chinaHat = !cfg.chinaHat);
            addToggle("Rainbow Trail", cfg.trailRainbow, col2, row1, () -> cfg.trailRainbow = !cfg.trailRainbow);
            addCycle("Trail Width: " + String.format("%.1f", cfg.trailWidth), contentX, row2,
                () -> cfg.trailWidth = cfg.trailWidth >= 5.0f ? 1.0f : cfg.trailWidth + 0.5f);
        }

        addRenderableWidget(Button.builder(Component.literal("RESET ALL"), b -> {
            VisualConfig.reset();
            rebuild();
        }).bounds(left + 16, top + PANEL_HEIGHT - 42, 104, 26).build());

        addRenderableWidget(Button.builder(Component.literal("DONE"), b -> onClose())
            .bounds(left + PANEL_WIDTH - 120, top + PANEL_HEIGHT - 42, 104, 26).build());
    }

    private void addTab(String label, int tabId, int x, int y) {
        boolean active = tab == tabId;
        addRenderableWidget(Button.builder(Component.literal((active ? "◆  " : "   ") + label), b -> {
            tab = tabId;
            rebuild();
        }).bounds(x, y, 100, 26).build());
    }

    private void addToggle(String label, boolean enabled, int x, int y, Runnable action) {
        String state = enabled ? "  [ON]" : "  [OFF]";
        addRenderableWidget(Button.builder(Component.literal(label + state), b -> {
            action.run();
            VisualConfig.save();
            rebuild();
        }).bounds(x, y, 154, 34).build());
    }

    private void addCycle(String label, int x, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            action.run();
            VisualConfig.save();
            rebuild();
        }).bounds(x, y, 154, 34).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int left = panelLeft();
        int top = panelTop();

        g.fill(0, 0, this.width, this.height, 0xA800060F);
        g.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF00C1422);
        g.fill(left, top, left + PANEL_WIDTH, top + 3, 0xFF56D8FF);
        g.fill(left, top + 3, left + 5, top + PANEL_HEIGHT, 0xFF247BBA);
        g.fill(left + 12, top + 58, left + 118, top + 250, 0xA90A101C);
        g.fill(left + 126, top + 58, left + PANEL_WIDTH - 12, top + 250, 0xA90A101C);
        g.fill(left + 12, top + 264, left + PANEL_WIDTH - 12, top + 265, 0x553E89B8);

        g.drawString(this.font, "KOTAND SRAKA", left + 17, top + 14, 0xFFFFFFFF, false);
        g.drawString(this.font, "VISUAL CLIENT  /  PULSE-STYLE MENU", left + 17, top + 31, 0xFF83A6C3, false);
        g.drawString(this.font, tabTitle(), left + 136, top + 62, 0xFF71D9FF, false);

        if (tab == 1) {
            g.drawString(this.font, "Open chat, hold RIGHT MOUSE and drag a HUD panel.", left + 136, top + 216, 0xFFB8C8D8, false);
            g.drawString(this.font, "Turn this mode off here when you're done.", left + 136, top + 231, 0xFF8199AE, false);
        } else if (tab == 0) {
            g.drawString(this.font, "Combat feedback and hit particle controls", left + 136, top + 216, 0xFF8199AE, false);
        } else if (tab == 2) {
            g.drawString(this.font, "Movement visuals and sprint helpers", left + 136, top + 216, 0xFF8199AE, false);
        } else {
            g.drawString(this.font, "World and trail appearance", left + 136, top + 216, 0xFF8199AE, false);
        }

        g.drawString(this.font, "RIGHT SHIFT  •  CLOSE / OPEN", left + 136, top + 278, 0xFF66839C, false);
        super.render(g, mouseX, mouseY, delta);
    }

    private String tabTitle() {
        return switch (tab) {
            case 0 -> "COMBAT SETTINGS";
            case 1 -> "HUD LAYOUT";
            case 2 -> "MOVEMENT";
            default -> "VISUALS";
        };
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
