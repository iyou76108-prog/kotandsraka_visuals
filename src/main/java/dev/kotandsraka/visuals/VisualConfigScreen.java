package dev.kotandsraka.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VisualConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 590;
    private static final int PANEL_HEIGHT = 390;
    private static final int VISIBLE_ROWS = 6;
    private final Screen parent;
    private int tab = 0;
    private int page = 0;

    private record Feature(String name, String description, boolean enabled, Runnable toggle) {}

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

        String[] tabs = {"COMBAT", "RENDER", "HUD", "MOVE", "HANDS", "THEME"};
        int tabWidth = 92;
        for (int i = 0; i < tabs.length; i++) {
            final int id = i;
            addRenderableWidget(Button.builder(Component.literal((tab == i ? "◆ " : "") + tabs[i]), b -> {
                tab = id;
                page = 0;
                rebuild();
            }).bounds(left + 12 + i * 96, top + 54, tabWidth, 25).build());
        }

        if (tab == 5) {
            buildThemeTab(cfg, left, top);
        } else if (tab == 4) {
            buildHandsTab(cfg, left, top);
        } else {
            List<Feature> features = featuresForTab(cfg);
            int pageCount = Math.max(1, (features.size() + VISIBLE_ROWS * 2 - 1) / (VISIBLE_ROWS * 2));
            page = Math.max(0, Math.min(page, pageCount - 1));
            int start = page * VISIBLE_ROWS * 2;
            for (int visible = 0; visible < VISIBLE_ROWS * 2 && start + visible < features.size(); visible++) {
                Feature feature = features.get(start + visible);
                int column = visible % 2;
                int row = visible / 2;
                int x = left + 22 + column * 276;
                int y = top + 94 + row * 38;
                addFeature(feature, x, y);
            }

            if (pageCount > 1) {
                addRenderableWidget(Button.builder(Component.literal("< PREV"), b -> {
                    page = Math.max(0, page - 1);
                    rebuild();
                }).bounds(left + 22, top + 330, 94, 24).build());
                addRenderableWidget(Button.builder(Component.literal("NEXT >"), b -> {
                    page = Math.min(pageCount - 1, page + 1);
                    rebuild();
                }).bounds(left + 122, top + 330, 94, 24).build());
            }

            addRenderableWidget(Button.builder(Component.literal("PAGE " + (page + 1) + " / " + pageCount),
                b -> {}).bounds(left + 224, top + 330, 110, 24).build());
            addRenderableWidget(Button.builder(Component.literal("PARTICLE COUNT: " + cfg.particleCount), b -> {
                cfg.particleCount = cfg.particleCount >= 24 ? 2 : cfg.particleCount + 2;
                VisualConfig.save();
                rebuild();
            }).bounds(left + 346, top + 330, 110, 24).build());
            addRenderableWidget(Button.builder(Component.literal("TRAIL WIDTH: " + String.format("%.1f", cfg.trailWidth)), b -> {
                cfg.trailWidth = cfg.trailWidth >= 5.0f ? 1.0f : cfg.trailWidth + 0.5f;
                VisualConfig.save();
                rebuild();
            }).bounds(left + 462, top + 330, 108, 24).build());
        }

        addRenderableWidget(Button.builder(Component.literal("RESET ALL"), b -> {
            VisualConfig.reset();
            rebuild();
        }).bounds(left + 18, top + 356, 108, 25).build());
        addRenderableWidget(Button.builder(Component.literal("DONE"), b -> onClose())
            .bounds(left + PANEL_WIDTH - 126, top + 356, 108, 25).build());
    }

    private List<Feature> featuresForTab(VisualConfig c) {
        List<Feature> list = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                add(list, "Hit Particles", "Particles on hit", c.hitParticles, () -> c.hitParticles = !c.hitParticles);
                add(list, "Critical Particles", "Extra particles on critical hits", c.criticalParticles, () -> c.criticalParticles = !c.criticalParticles);
                add(list, "Hit Flash", "Brief screen flash after hitting", c.hitFlash, () -> c.hitFlash = !c.hitFlash);
                add(list, "Hit Marker", "HIT / CRITICAL label on screen", c.hitText, () -> c.hitText = !c.hitText);
                add(list, "Damage Numbers", "Displays damage dealt to a target", c.damageNumbers, () -> c.damageNumbers = !c.damageNumbers);
                add(list, "Kill Effect", "Particle burst when your target dies", c.killEffect, () -> c.killEffect = !c.killEffect);
                add(list, "Target Ring", "Ring burst on the attacked target", c.targetRing, () -> c.targetRing = !c.targetRing);
                add(list, "Target ESP", "Continuous outline ring around target", c.targetEsp, () -> c.targetEsp = !c.targetEsp);
                add(list, "Hit Range", "Shows the 3-block hit-range circle", c.hitRange, () -> c.hitRange = !c.hitRange);
                add(list, "Custom Crosshair", "Accent-colored crosshair overlay", c.customCrosshair, () -> c.customCrosshair = !c.customCrosshair);
                add(list, "Combo Counter", "Counts consecutive hits", c.comboHud, () -> c.comboHud = !c.comboHud);
            }
            case 1 -> {
                add(list, "Player Trail", "Third-person movement trail", c.playerTrail, () -> c.playerTrail = !c.playerTrail);
                add(list, "Rainbow Trail", "Cycles trail colors", c.trailRainbow, () -> c.trailRainbow = !c.trailRainbow);
                add(list, "China Hat", "Ring above the player head", c.chinaHat, () -> c.chinaHat = !c.chinaHat);
                add(list, "Body Glow", "Orbiting cosmetic particles", c.bodyGlow, () -> c.bodyGlow = !c.bodyGlow);
                add(list, "World Particles", "Ambient particles around you", c.worldParticles, () -> c.worldParticles = !c.worldParticles);
                add(list, "Pearl Tracer", "Trail behind your ender pearls", c.pearlTrail, () -> c.pearlTrail = !c.pearlTrail);
                add(list, "Trajectory", "Projectile path prediction guide", c.trajectory, () -> c.trajectory = !c.trajectory);
                add(list, "Jump Rings", "Particle ring when you jump", c.jumpRings, () -> c.jumpRings = !c.jumpRings);
                add(list, "Skull Jump", "Pixel skull appears near crosshair on jump", c.skullJump, () -> c.skullJump = !c.skullJump);
                add(list, "Movement Blur", "Built-in screen blur while moving", c.motionBlur, () -> c.motionBlur = !c.motionBlur);
            }
            case 2 -> {
                add(list, "Target HUD", "Target name and health bar", c.targetHud, () -> c.targetHud = !c.targetHud);
                add(list, "Armor HUD", "Armor slots and durability", c.armorHud, () -> c.armorHud = !c.armorHud);
                add(list, "Keystrokes", "WASD and mouse key display", c.keystrokes, () -> c.keystrokes = !c.keystrokes);
                add(list, "FPS", "Frames per second", c.fpsHud, () -> c.fpsHud = !c.fpsHud);
                add(list, "CPS", "Clicks per second counter", c.cpsHud, () -> c.cpsHud = !c.cpsHud);
                add(list, "Ping", "Server latency", c.pingHud, () -> c.pingHud = !c.pingHud);
                add(list, "Coordinates", "XYZ position", c.coordsHud, () -> c.coordsHud = !c.coordsHud);
                add(list, "Speed", "Movement speed in blocks/second", c.speedHud, () -> c.speedHud = !c.speedHud);
                add(list, "Totem Counter", "Totems in inventory", c.totemHud, () -> c.totemHud = !c.totemHud);
                add(list, "Potion Effects", "Count of active effects", c.potionHud, () -> c.potionHud = !c.potionHud);
                add(list, "Clock", "Minecraft world time", c.clockHud, () -> c.clockHud = !c.clockHud);
                add(list, "Compass", "Facing direction", c.compassHud, () -> c.compassHud = !c.compassHud);
                add(list, "Active Modules", "Colored list of enabled functions", c.activeModulesHud, () -> c.activeModulesHud = !c.activeModulesHud);
                add(list, "Edit HUD in Chat", "Hold right mouse on a panel to move it", c.hudEditMode, () -> c.hudEditMode = !c.hudEditMode);
                add(list, "Reset HUD Positions", "Returns panels to their default positions", false, VisualConfig::resetHudPositions);
            }
            case 3 -> {
                add(list, "Auto Sprint", "Automatically sprint while moving forward", c.autoSprint, () -> c.autoSprint = !c.autoSprint);
            }
            default -> {
            }
        }
        return list;
    }

    private static void add(List<Feature> list, String name, String description, boolean enabled, Runnable toggle) {
        list.add(new Feature(name, description, enabled, toggle));
    }

    private void addFeature(Feature feature, int x, int y) {
        String state = feature.enabled ? "  ● ON" : "  ○ OFF";
        addRenderableWidget(Button.builder(Component.literal(feature.name + state), b -> {
            feature.toggle.run();
            VisualConfig.save();
            rebuild();
        }).bounds(x, y, 260, 30).build());
    }

    private void buildHandsTab(VisualConfig cfg, int left, int top) {
        int x1 = left + 20;
        int x2 = left + 208;
        int x3 = left + 396;
        int y0 = top + 94;
        int y1 = top + 134;
        int y2 = top + 174;
        int y3 = top + 214;
        int y4 = top + 254;
        int y5 = top + 294;

        addFeature(new Feature("ViewModel", "First-person hand position and rotation", cfg.viewModel,
            () -> cfg.viewModel = !cfg.viewModel), x1, y0);
        addFeature(new Feature("Custom Swing", "Custom swing timing and angles", cfg.customSwing,
            () -> cfg.customSwing = !cfg.customSwing), x2, y0);
        addFeature(new Feature("Custom Fire", "Lower first-person fire overlay", cfg.customFire,
            () -> cfg.customFire = !cfg.customFire), x3, y0);

        addHandCycle("VM X: " + fmt(cfg.viewModelX), x1, y1,
            () -> cfg.viewModelX = stepFloat(cfg.viewModelX, -0.5f, 0.5f, 0.05f));
        addHandCycle("VM Y: " + fmt(cfg.viewModelY), x2, y1,
            () -> cfg.viewModelY = stepFloat(cfg.viewModelY, -0.5f, 0.5f, 0.05f));
        addHandCycle("VM Z: " + fmt(cfg.viewModelZ), x3, y1,
            () -> cfg.viewModelZ = stepFloat(cfg.viewModelZ, -0.5f, 0.5f, 0.05f));

        addHandCycle("VM SCALE: " + fmt(cfg.viewModelScale), x1, y2,
            () -> cfg.viewModelScale = stepFloat(cfg.viewModelScale, 0.5f, 1.5f, 0.05f));
        addHandCycle("VM ROT X: " + fmt(cfg.viewModelRotX), x2, y2,
            () -> cfg.viewModelRotX = stepFloat(cfg.viewModelRotX, -180f, 180f, 10f));
        addHandCycle("VM ROT Y: " + fmt(cfg.viewModelRotY), x3, y2,
            () -> cfg.viewModelRotY = stepFloat(cfg.viewModelRotY, -180f, 180f, 10f));

        addHandCycle("VM ROT Z: " + fmt(cfg.viewModelRotZ), x1, y3,
            () -> cfg.viewModelRotZ = stepFloat(cfg.viewModelRotZ, -180f, 180f, 10f));
        addHandCycle("SWING SPEED: " + fmt(cfg.swingSpeed), x2, y3,
            () -> cfg.swingSpeed = stepFloat(cfg.swingSpeed, 0.5f, 2.5f, 0.25f));
        addHandCycle("SWING STRENGTH: " + fmt(cfg.swingStrength), x3, y3,
            () -> cfg.swingStrength = stepFloat(cfg.swingStrength, 0.25f, 1.5f, 0.1f));

        addHandCycle("SWING ROT X: " + fmt(cfg.swingRotationX), x1, y4,
            () -> cfg.swingRotationX = stepFloat(cfg.swingRotationX, -90f, 90f, 5f));
        addHandCycle("SWING ROT Y: " + fmt(cfg.swingRotationY), x2, y4,
            () -> cfg.swingRotationY = stepFloat(cfg.swingRotationY, -90f, 90f, 5f));
        addHandCycle("SWING ROT Z: " + fmt(cfg.swingRotationZ), x3, y4,
            () -> cfg.swingRotationZ = stepFloat(cfg.swingRotationZ, -90f, 90f, 5f));

        addHandCycle("FIRE HEIGHT: " + Math.round(cfg.fireHeight * 100) + "%", x1, y5,
            () -> cfg.fireHeight = cfg.fireHeight >= 1.0f ? 0.2f : Math.min(1.0f, cfg.fireHeight + 0.1f));
    }

    private void addHandCycle(String label, int x, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            action.run();
            VisualConfig.save();
            rebuild();
        }).bounds(x, y, 174, 30).build());
    }

    private static String fmt(float value) {
        return String.format("%.2f", value);
    }

    private static float stepFloat(float value, float min, float max, float step) {
        float next = value + step;
        return next > max + 0.0001f ? min : Math.round(next * 100.0f) / 100.0f;
    }

    private void buildThemeTab(VisualConfig cfg, int left, int top) {
        String[] names = {"CYAN", "VIOLET", "RED", "MINT", "GOLD", "PINK"};
        addRenderableWidget(Button.builder(Component.literal("COLOR PALETTE: " + names[Math.floorMod(cfg.colorPreset, names.length)]), b -> {
            cfg.colorPreset = (cfg.colorPreset + 1) % names.length;
            VisualConfig.save();
            rebuild();
        }).bounds(left + 24, top + 100, 250, 32).build());

        addRenderableWidget(Button.builder(Component.literal("CHROMA ACCENT: " + (cfg.chromaTheme ? "ON" : "OFF")), b -> {
            cfg.chromaTheme = !cfg.chromaTheme;
            VisualConfig.save();
            rebuild();
        }).bounds(left + 290, top + 100, 250, 32).build());

        addRenderableWidget(Button.builder(Component.literal("HUD OPACITY: " + Math.round(cfg.hudOpacity * 100) + "%"), b -> {
            cfg.hudOpacity = cfg.hudOpacity >= 1.0f ? 0.25f : cfg.hudOpacity + 0.15f;
            VisualConfig.save();
            rebuild();
        }).bounds(left + 24, top + 148, 250, 32).build());

        addRenderableWidget(Button.builder(Component.literal("RESET HUD POSITIONS"), b -> {
            VisualConfig.resetHudPositions();
            rebuild();
        }).bounds(left + 290, top + 148, 250, 32).build());

        addRenderableWidget(Button.builder(Component.literal("GLASS MENU: " + (cfg.glassMenu ? "ON" : "OFF")), b -> {
            cfg.glassMenu = !cfg.glassMenu;
            VisualConfig.save();
            rebuild();
        }).bounds(left + 24, top + 196, 250, 32).build());

        addRenderableWidget(Button.builder(Component.literal("ANIMATED MENU: " + (cfg.animatedMenu ? "ON" : "OFF")), b -> {
            cfg.animatedMenu = !cfg.animatedMenu;
            VisualConfig.save();
            rebuild();
        }).bounds(left + 290, top + 196, 250, 32).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int left = panelLeft();
        int top = panelTop();
        int accent = VisualConfig.accentColor();

        g.fill(0, 0, this.width, this.height, 0xB800060D);
        g.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, VisualConfig.panelColor());
        g.fill(left, top, left + PANEL_WIDTH, top + 4, accent);
        g.fill(left, top + 4, left + 4, top + PANEL_HEIGHT, accent);
        g.fill(left + 14, top + 85, left + PANEL_WIDTH - 14, top + 86, accent & 0x55FFFFFF);
        g.fill(left + 14, top + 346, left + PANEL_WIDTH - 14, top + 347, accent & 0x55FFFFFF);

        g.drawString(this.font, "KOTAND SRAKA  /  VISUAL CLIENT", left + 20, top + 14, 0xFFFFFFFF, false);
        g.drawString(this.font, "RIGHT SHIFT  •  FEATURES  •  CUSTOM HUD  •  COLOR THEMES",
            left + 20, top + 31, VisualConfig.mutedColor(), false);
        g.drawString(this.font, tabTitle(), left + 22, top + 82, accent, false);

        if (tab == 5) {
            g.drawString(this.font, "Palette changes the panel, active modules and HUD accent.", left + 24, top + 260,
                VisualConfig.mutedColor(), false);
            g.fill(left + 24, top + 281, left + 540, top + 286, accent);
        } else if (tab == 4) {
            g.drawString(this.font, "ViewModel and swing changes are local to first-person hands.", left + 22, top + 320,
                VisualConfig.mutedColor(), false);
        } else {
            g.drawString(this.font, "Select a feature to enable it. All features start OFF.", left + 22, top + 316,
                VisualConfig.mutedColor(), false);
        }

        super.render(g, mouseX, mouseY, delta);

        // Accent outlines make the whole function list follow the selected palette.
        for (var widget : this.children()) {
            if (widget instanceof Button button && button.visible) {
                int x = button.getX();
                int y = button.getY();
                int w = button.getWidth();
                int h = button.getHeight();
                g.fill(x, y, x + w, y + 1, accent & 0xAAFFFFFF);
                g.fill(x, y + h - 1, x + w, y + h, accent & 0xAAFFFFFF);
            }
        }
    }

    private String tabTitle() {
        return switch (tab) {
            case 0 -> "COMBAT MODULES";
            case 1 -> "WORLD / RENDER MODULES";
            case 2 -> "HUD MODULES";
            case 3 -> "MOVEMENT";
            case 4 -> "VIEWMODEL / SWING / FIRE";
            default -> "THEME / COLOR PALETTE";
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
