package dev.kotandsraka.visuals;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VisualConfigScreen extends Screen {
    private static final int PINK = 0xFFFF347D;
    private static final int BG = 0xFF111319;
    private static final int SIDEBAR = 0xFF181B23;
    private static final int MODULES_BG = 0xFF20232C;
    private static final int SETTINGS_BG = 0xFF242730;
    private static final int TEXT = 0xFFF3F1F5;
    private static final int MUTED = 0xFF9A9DA8;

    private final Screen parent;
    private String category = "Combat";
    private String selectedId = "hitParticles";
    private int moduleRowHeight = 27;

    private record Feature(String id, String name, String description, boolean enabled, Runnable toggle) {}

    public VisualConfigScreen(Screen parent) {
        super(Component.literal("KotAndSraka Visuals"));
        this.parent = parent;
    }

    private static final String[] CATEGORIES = {
        "Combat", "Movement", "World", "Render", "Player",
        "Exploit", "Other", "Friends", "Configs"
    };

    private int panelWidth() {
        return Math.max(320, Math.min(930, this.width - 20));
    }

    private int panelHeight() {
        return Math.max(220, Math.min(610, this.height - 16));
    }

    private int left() {
        return (this.width - panelWidth()) / 2;
    }

    private int top() {
        return (this.height - panelHeight()) / 2;
    }

    private int modulesX() {
        return left() + 140;
    }

    private int modulesWidth() {
        return 220;
    }

    private int settingsX() {
        return modulesX() + modulesWidth() + 12;
    }

    private int settingsWidth() {
        return left() + panelWidth() - 14 - settingsX();
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        VisualConfig cfg = VisualConfig.get();
        int l = left();
        int t = top();
        int h = panelHeight();

        for (int i = 0; i < CATEGORIES.length; i++) {
            String name = CATEGORIES[i];
            int y = t + 65 + i * 29;
            addRenderableWidget(Button.builder(Component.literal(name), b -> {
                category = name;
                List<Feature> next = featuresForCategory(VisualConfig.get());
                selectedId = next.isEmpty() ? "" : next.get(0).id();
                rebuild();
            }).bounds(l + 10, y, 118, 24).build());
        }

        List<Feature> features = featuresForCategory(cfg);
        if (!features.isEmpty() && features.stream().noneMatch(f -> f.id().equals(selectedId))) {
            selectedId = features.get(0).id();
        }

        int listTop = t + 66;
        int listBottom = t + h - 45;
        moduleRowHeight = Math.max(20, Math.min(29,
            (listBottom - listTop) / Math.max(1, features.size())));

        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);
            int y = listTop + i * moduleRowHeight;
            int rowHeight = Math.max(18, moduleRowHeight - 3);
            final String id = feature.id();

            addRenderableWidget(Button.builder(Component.literal(feature.name()), b -> {
                selectedId = id;
                rebuild();
            }).bounds(modulesX() + 6, y, modulesWidth() - 57, rowHeight).build());

            if (feature.toggle() != null) {
                addRenderableWidget(Button.builder(Component.literal(feature.enabled() ? "ON" : "OFF"), b -> {
                    feature.toggle().run();
                    VisualConfig.save();
                    rebuild();
                }).bounds(modulesX() + modulesWidth() - 46, y, 39, rowHeight).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("..."), b -> {
                    selectedId = id;
                    rebuild();
                }).bounds(modulesX() + modulesWidth() - 46, y, 39, rowHeight).build());
            }
        }

        buildSettings(cfg, findSelected(features), l, t, h);

        addRenderableWidget(Button.builder(Component.literal("RESET ALL"), b -> {
            VisualConfig.reset();
            selectedId = category.equals("Configs") ? "theme" : selectedId;
            rebuild();
        }).bounds(l + 12, t + h - 34, 112, 24).build());

        addRenderableWidget(Button.builder(Component.literal("DONE"), b -> onClose())
            .bounds(l + panelWidth() - 124, t + h - 34, 110, 24).build());
    }

    private Feature findSelected(List<Feature> features) {
        for (Feature feature : features) {
            if (feature.id().equals(selectedId)) return feature;
        }
        return null;
    }

    private List<Feature> featuresForCategory(VisualConfig c) {
        List<Feature> list = new ArrayList<>();
        switch (category) {
            case "Combat" -> {
                add(list, "hitParticles", "Hit Particles", "Particle burst when your attack connects", c.hitParticles, () -> c.hitParticles = !c.hitParticles);
                add(list, "criticalParticles", "Critical Particles", "Extra particles on critical hits", c.criticalParticles, () -> c.criticalParticles = !c.criticalParticles);
                add(list, "hitSound", "Hit Sound", "Play a sound when you hit an entity", c.hitSound, () -> c.hitSound = !c.hitSound);
                add(list, "hitFlash", "Hit Flash", "Brief screen flash after a hit", c.hitFlash, () -> c.hitFlash = !c.hitFlash);
                add(list, "hitText", "Hit Marker", "Show HIT / CRITICAL text", c.hitText, () -> c.hitText = !c.hitText);
                add(list, "damageNumbers", "Damage Numbers", "Show damage dealt to the target", c.damageNumbers, () -> c.damageNumbers = !c.damageNumbers);
                add(list, "killEffect", "Kill Effect", "Particle burst when the target dies", c.killEffect, () -> c.killEffect = !c.killEffect);
                add(list, "targetRing", "Target Ring", "Ring effect on the attacked target", c.targetRing, () -> c.targetRing = !c.targetRing);
                add(list, "targetEsp", "Target ESP", "Ring outline around the last attacked target", c.targetEsp, () -> c.targetEsp = !c.targetEsp);
                add(list, "hitRange", "Hit Range", "Show a 3-block range circle", c.hitRange, () -> c.hitRange = !c.hitRange);
                add(list, "comboHud", "Combo Counter", "Count consecutive hits", c.comboHud, () -> c.comboHud = !c.comboHud);
            }
            case "Movement" -> {
                add(list, "autoSprint", "Auto Sprint", "Automatically sprint while moving forward", c.autoSprint, () -> c.autoSprint = !c.autoSprint);
                add(list, "playerTrail", "Player Trail", "Leave a trail behind your player", c.playerTrail, () -> c.playerTrail = !c.playerTrail);
                add(list, "jumpRings", "Jump Rings", "Particle ring when jumping", c.jumpRings, () -> c.jumpRings = !c.jumpRings);
                add(list, "skullJump", "Skull Jump", "Show a small skull near the crosshair on jump", c.skullJump, () -> c.skullJump = !c.skullJump);
            }
            case "World" -> {
                add(list, "worldParticles", "World Particles", "Ambient particles around the player", c.worldParticles, () -> c.worldParticles = !c.worldParticles);
                add(list, "pearlTrail", "Pearl Tracer", "Trail behind your own ender pearls", c.pearlTrail, () -> c.pearlTrail = !c.pearlTrail);
                add(list, "trajectory", "Trajectory", "Ballistic guide along your look direction", c.trajectory, () -> c.trajectory = !c.trajectory);
                add(list, "arrowTrajectory", "Arrow Prediction", "Predict the path of your fired arrows", c.arrowTrajectory, () -> c.arrowTrajectory = !c.arrowTrajectory);
                add(list, "chinaHat", "China Hat", "Ring above your head in third person", c.chinaHat, () -> c.chinaHat = !c.chinaHat);
                add(list, "bodyGlow", "Body Glow", "Orbiting cosmetic particles around you", c.bodyGlow, () -> c.bodyGlow = !c.bodyGlow);
                add(list, "trailRainbow", "Rainbow Trail", "Cycle trail colors", c.trailRainbow, () -> c.trailRainbow = !c.trailRainbow);
            }
            case "Render" -> {
                add(list, "customCrosshair", "Custom Crosshair", "Accent-colored crosshair overlay", c.customCrosshair, () -> c.customCrosshair = !c.customCrosshair);
                add(list, "targetEsp", "Target ESP", "Ring outline around your last target", c.targetEsp, () -> c.targetEsp = !c.targetEsp);
                add(list, "hitRange", "Hit Range", "Show the 3-block range circle", c.hitRange, () -> c.hitRange = !c.hitRange);
                add(list, "hitFlash", "Hit Flash", "Brief screen flash after a hit", c.hitFlash, () -> c.hitFlash = !c.hitFlash);
            }
            case "Player" -> {
                add(list, "viewModel", "ViewModel", "Change first-person hand position and rotation", c.viewModel, () -> c.viewModel = !c.viewModel);
                add(list, "customSwing", "Custom Swing", "Adjust swing speed, strength and angles", c.customSwing, () -> c.customSwing = !c.customSwing);
            }
            case "Other" -> {
                add(list, "targetHud", "Target HUD", "Target name and health bar", c.targetHud, () -> c.targetHud = !c.targetHud);
                add(list, "armorHud", "Armor HUD", "Armor slots and durability", c.armorHud, () -> c.armorHud = !c.armorHud);
                add(list, "keystrokes", "Keystrokes", "WASD and mouse key display", c.keystrokes, () -> c.keystrokes = !c.keystrokes);
                add(list, "fpsHud", "FPS", "Frames per second", c.fpsHud, () -> c.fpsHud = !c.fpsHud);
                add(list, "cpsHud", "CPS", "Clicks per second", c.cpsHud, () -> c.cpsHud = !c.cpsHud);
                add(list, "pingHud", "Ping", "Server latency", c.pingHud, () -> c.pingHud = !c.pingHud);
                add(list, "coordsHud", "Coordinates", "XYZ position", c.coordsHud, () -> c.coordsHud = !c.coordsHud);
                add(list, "speedHud", "Speed", "Movement speed in blocks per second", c.speedHud, () -> c.speedHud = !c.speedHud);
                add(list, "totemHud", "Totem Counter", "Totems in inventory and offhand", c.totemHud, () -> c.totemHud = !c.totemHud);
                add(list, "potionHud", "Potion Effects", "Count of active potion effects", c.potionHud, () -> c.potionHud = !c.potionHud);
                add(list, "clockHud", "Clock", "Minecraft world time", c.clockHud, () -> c.clockHud = !c.clockHud);
                add(list, "compassHud", "Compass", "Facing direction", c.compassHud, () -> c.compassHud = !c.compassHud);
                add(list, "activeModulesHud", "Active Modules", "List of enabled modules", c.activeModulesHud, () -> c.activeModulesHud = !c.activeModulesHud);
                add(list, "hudEditMode", "HUD Editor", "Move HUD panels while chat is open", c.hudEditMode, () -> c.hudEditMode = !c.hudEditMode);
                add(list, "comboHud", "Combo Counter", "Show consecutive hit count", c.comboHud, () -> c.comboHud = !c.comboHud);
            }
            case "Configs" -> {
                list.add(new Feature("theme", "Theme", "Accent color and menu appearance", false, null));
                list.add(new Feature("hudLayout", "HUD Layout", "Reset the positions of movable HUD panels", false, null));
                list.add(new Feature("resetConfig", "Reset Config", "Restore all module settings to defaults", false, null));
            }
            case "Exploit", "Friends" -> {
                // These categories are shown to mirror the reference layout; no unsupported modules are fabricated.
            }
            default -> {
            }
        }
        return list;
    }

    private static void add(List<Feature> list, String id, String name, String description,
                            boolean enabled, Runnable toggle) {
        list.add(new Feature(id, name, description, enabled, toggle));
    }

    private void buildSettings(VisualConfig c, Feature feature, int l, int t, int h) {
        if (feature == null) return;
        int x = settingsX() + 12;
        int colWidth = Math.max(105, (settingsWidth() - 34) / 2);
        int x2 = x + colWidth + 8;
        int firstY = t + 132;

        if (feature.toggle() != null) {
            addRenderableWidget(Button.builder(Component.literal(feature.enabled() ? "ENABLED  ●" : "DISABLED  ○"), b -> {
                feature.toggle().run();
                VisualConfig.save();
                rebuild();
            }).bounds(settingsX() + settingsWidth() - 132, t + 63, 118, 24).build());
        }

        switch (feature.id()) {
            case "hitParticles", "criticalParticles" -> {
                addSetting("PARTICLE COUNT: " + c.particleCount, x, firstY, colWidth,
                    () -> c.particleCount = c.particleCount >= 24 ? 2 : c.particleCount + 2);
                addSetting("SHAPE: " + particleShapeName(c.particleShape), x2, firstY,
                    colWidth, () -> c.particleShape = (c.particleShape + 1) % 3);
                addSetting("SPREAD: " + fmt(c.particleSpread), x, firstY + 34, colWidth,
                    () -> c.particleSpread = stepFloat(c.particleSpread, 0.10f, 1.25f, 0.05f));
                addSetting("SPEED: " + fmt(c.particleSpeed), x2, firstY + 34, colWidth,
                    () -> c.particleSpeed = stepFloat(c.particleSpeed, 0.02f, 0.50f, 0.02f));
                addSetting("HEIGHT: " + fmt(c.particleHeight), x, firstY + 68, colWidth,
                    () -> c.particleHeight = stepFloat(c.particleHeight, 0.05f, 1.50f, 0.05f));
                addSetting("LIFETIME: " + c.particleLifetimeMs + " ms", x2, firstY + 68, colWidth,
                    () -> c.particleLifetimeMs = c.particleLifetimeMs >= 1200 ? 200 : c.particleLifetimeMs + 100);
                addSettingToggle("CRITICAL PARTICLES: " + onOff(c.criticalParticles),
                    x, firstY + 102, colWidth, () -> c.criticalParticles = !c.criticalParticles);
            }
            case "playerTrail", "pearlTrail", "trailRainbow" -> {
                addSetting("TRAIL WIDTH: " + fmt(c.trailWidth), x, firstY, colWidth,
                    () -> c.trailWidth = stepFloat(c.trailWidth, 1.0f, 5.0f, 0.5f));
                addSetting("TRAIL LENGTH: " + c.trailLength, x2, firstY, colWidth,
                    () -> c.trailLength = c.trailLength >= 80 ? 8 : c.trailLength + 4);
                addSetting("TRAIL DENSITY: " + c.trailIntensity, x, firstY + 34, colWidth,
                    () -> c.trailIntensity = c.trailIntensity >= 8 ? 1 : c.trailIntensity + 1);
                addSettingToggle("RAINBOW: " + onOff(c.trailRainbow), x2, firstY + 34,
                    colWidth, () -> c.trailRainbow = !c.trailRainbow);
            }
            case "viewModel" -> {
                addSetting("POSITION X: " + fmt(c.viewModelX), x, firstY, colWidth,
                    () -> c.viewModelX = stepFloat(c.viewModelX, -0.50f, 0.50f, 0.05f));
                addSetting("POSITION Y: " + fmt(c.viewModelY), x2, firstY, colWidth,
                    () -> c.viewModelY = stepFloat(c.viewModelY, -0.50f, 0.50f, 0.05f));
                addSetting("POSITION Z: " + fmt(c.viewModelZ), x, firstY + 34, colWidth,
                    () -> c.viewModelZ = stepFloat(c.viewModelZ, -0.50f, 0.50f, 0.05f));
                addSetting("SCALE: " + fmt(c.viewModelScale), x2, firstY + 34, colWidth,
                    () -> c.viewModelScale = stepFloat(c.viewModelScale, 0.50f, 1.50f, 0.05f));
                addSetting("ROTATION X: " + fmt(c.viewModelRotX), x, firstY + 68, colWidth,
                    () -> c.viewModelRotX = stepFloat(c.viewModelRotX, -180f, 180f, 10f));
                addSetting("ROTATION Y: " + fmt(c.viewModelRotY), x2, firstY + 68, colWidth,
                    () -> c.viewModelRotY = stepFloat(c.viewModelRotY, -180f, 180f, 10f));
                addSetting("ROTATION Z: " + fmt(c.viewModelRotZ), x, firstY + 102, colWidth,
                    () -> c.viewModelRotZ = stepFloat(c.viewModelRotZ, -180f, 180f, 10f));
            }
            case "customSwing" -> {
                addSetting("SWING SPEED: " + fmt(c.swingSpeed), x, firstY, colWidth,
                    () -> c.swingSpeed = stepFloat(c.swingSpeed, 0.50f, 2.50f, 0.25f));
                addSetting("SWING STRENGTH: " + fmt(c.swingStrength), x2, firstY, colWidth,
                    () -> c.swingStrength = stepFloat(c.swingStrength, 0.25f, 1.50f, 0.10f));
                addSetting("ROTATION X: " + fmt(c.swingRotationX), x, firstY + 34, colWidth,
                    () -> c.swingRotationX = stepFloat(c.swingRotationX, -90f, 90f, 5f));
                addSetting("ROTATION Y: " + fmt(c.swingRotationY), x2, firstY + 34, colWidth,
                    () -> c.swingRotationY = stepFloat(c.swingRotationY, -90f, 90f, 5f));
                addSetting("ROTATION Z: " + fmt(c.swingRotationZ), x, firstY + 68, colWidth,
                    () -> c.swingRotationZ = stepFloat(c.swingRotationZ, -90f, 90f, 5f));
            }
            case "customCrosshair" -> {
                addSetting("SIZE: " + c.crosshairSize, x, firstY, colWidth,
                    () -> c.crosshairSize = c.crosshairSize >= 16 ? 3 : c.crosshairSize + 1);
                addSetting("GAP: " + c.crosshairGap, x2, firstY, colWidth,
                    () -> c.crosshairGap = c.crosshairGap >= 12 ? 1 : c.crosshairGap + 1);
                addSetting("THICKNESS: " + c.crosshairThickness, x, firstY + 34, colWidth,
                    () -> c.crosshairThickness = c.crosshairThickness >= 6 ? 1 : c.crosshairThickness + 1);
            }
            case "theme" -> {
                String[] palettes = {"CYAN", "VIOLET", "RED", "MINT", "GOLD", "PINK"};
                addSetting("ACCENT: " + palettes[Math.floorMod(c.colorPreset, palettes.length)],
                    x, firstY, colWidth, () -> c.colorPreset = (c.colorPreset + 1) % palettes.length);
                addSetting("HUD OPACITY: " + Math.round(c.hudOpacity * 100) + "%",
                    x2, firstY, colWidth, () -> c.hudOpacity = stepFloat(c.hudOpacity, 0.25f, 1.0f, 0.05f));
                addSettingToggle("CHROMA: " + onOff(c.chromaTheme), x, firstY + 34,
                    colWidth, () -> c.chromaTheme = !c.chromaTheme);
                addSettingToggle("GLASS MENU: " + onOff(c.glassMenu), x2, firstY + 34,
                    colWidth, () -> c.glassMenu = !c.glassMenu);
                addSettingToggle("ANIMATED ACCENT: " + onOff(c.animatedMenu), x, firstY + 68,
                    colWidth, () -> c.animatedMenu = !c.animatedMenu);
            }
            case "hudLayout" -> {
                addSetting("RESET HUD POSITIONS", x, firstY, colWidth, VisualConfig::resetHudPositions);
                addSetting("HUD OPACITY: " + Math.round(c.hudOpacity * 100) + "%",
                    x2, firstY, colWidth, () -> c.hudOpacity = stepFloat(c.hudOpacity, 0.25f, 1.0f, 0.05f));
            }
            case "resetConfig" -> addSetting("RESTORE DEFAULT CONFIG", x, firstY,
                colWidth, VisualConfig::reset);
            default -> {
                // Features without numeric options keep their controls simple and safe.
            }
        }
    }

    private void addSetting(String label, int x, int y, int width, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            action.run();
            VisualConfig.save();
            rebuild();
        }).bounds(x, y, width, 25).build());
    }

    private void addSettingToggle(String label, int x, int y, int width, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            action.run();
            VisualConfig.save();
            rebuild();
        }).bounds(x, y, width, 25).build());
    }

    private static String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private static String fmt(float value) {
        return String.format("%.2f", value);
    }

    private static float stepFloat(float value, float min, float max, float step) {
        float next = value + step;
        return next > max + 0.0001f ? min : Math.round(next * 100.0f) / 100.0f;
    }

    private static String particleShapeName(int shape) {
        return switch (shape) {
            case 1 -> "RING";
            case 2 -> "UPWARD";
            default -> "BURST";
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int l = left();
        int t = top();
        int w = panelWidth();
        int h = panelHeight();
        int sx = settingsX();
        int sw = settingsWidth();

        g.fill(0, 0, this.width, this.height, 0xB800050A);
        g.fill(l, t, l + w, t + h, BG);
        g.fill(l, t, l + 130, t + h, SIDEBAR);
        g.fill(l + 136, t + 52, l + 136 + modulesWidth() + 2, t + h - 43, MODULES_BG);
        g.fill(sx, t + 52, sx + sw, t + h - 43, SETTINGS_BG);

        g.fill(l, t, l + w, t + 3, PINK);
        g.fill(l, t, l + 3, t + h, PINK);
        g.fill(sx, t + 52, sx + sw, t + 53, PINK & 0x99FFFFFF);
        g.fill(l + 12, t + h - 40, l + w - 12, t + h - 39, 0x554D5360);

        g.drawString(this.font, "DeadCode", l + 18, t + 17, PINK, true);
        g.drawString(this.font, "KOTAND SRAKA  /  VISUALS", l + 148, t + 16, TEXT, true);
        g.drawString(this.font, "CLIENT SETTINGS", l + 148, t + 31, MUTED, false);
        g.drawString(this.font, category, modulesX() + 7, t + 59, TEXT, true);

        List<Feature> features = featuresForCategory(VisualConfig.get());
        if (features.isEmpty()) {
            g.drawString(this.font, "No modules in this category yet.", modulesX() + 12, t + 83, MUTED, false);
        } else {
            int listTop = t + 66;
            for (int i = 0; i < features.size(); i++) {
                Feature feature = features.get(i);
                int y = listTop + i * moduleRowHeight;
                if (feature.id().equals(selectedId)) {
                    g.fill(modulesX() + 3, y - 1, modulesX() + modulesWidth() - 3,
                        y + moduleRowHeight - 2, 0xAA4A2435);
                    g.fill(modulesX() + 3, y - 1, modulesX() + 6,
                        y + moduleRowHeight - 2, PINK);
                }
            }
        }

        Feature selected = findSelected(features);
        if (selected != null) {
            g.drawString(this.font, category.toUpperCase() + "  /  " + selected.name(),
                sx + 12, t + 62, TEXT, true);
            g.drawString(this.font, selected.description(), sx + 12, t + 83, MUTED, false);
            g.fill(sx + 12, t + 99, sx + sw - 12, t + 100, 0x664D5360);
            g.drawString(this.font, "SETTINGS", sx + 12, t + 111, PINK, true);
            if (selected.toggle() == null) {
                g.drawString(this.font, "Choose an option below.", sx + 12, t + 128, MUTED, false);
            } else if (selected.id().equals("targetEsp")) {
                g.drawString(this.font, "Draws a ring at the last attacked living entity.", sx + 12, t + 133, MUTED, false);
            } else if (selected.id().equals("trajectory") || selected.id().equals("arrowTrajectory")) {
                g.drawString(this.font, "Prediction is rendered in the 3D world.", sx + 12, t + 133, MUTED, false);
            } else if (selected.id().equals("hitRange")) {
                g.drawString(this.font, "The range ring is fixed at three blocks.", sx + 12, t + 133, MUTED, false);
            } else if (selected.id().equals("resetConfig")) {
                g.drawString(this.font, "Restore the module toggles and values to defaults.", sx + 12, t + 133, MUTED, false);
            }
        } else if (category.equals("Exploit") || category.equals("Friends")) {
            g.drawString(this.font, "Nothing here yet", sx + 16, t + 80, MUTED, false);
            g.drawString(this.font, "This category is reserved for future modules.", sx + 16, t + 99, MUTED, false);
        }

        g.drawString(this.font, "v1.0.4", l + 18, t + h - 55, MUTED, false);
        g.drawString(this.font, "RIGHT SHIFT  •  SELECT A MODULE  •  CHANGE ITS SETTINGS",
            l + 148, t + h - 28, MUTED, false);

        super.render(g, mouseX, mouseY, delta);

        // Thin pink edges keep the controls consistent with the reference skin.
        for (var child : this.children()) {
            if (child instanceof Button button && button.visible) {
                int bx = button.getX();
                int by = button.getY();
                int bw = button.getWidth();
                int bh = button.getHeight();
                g.fill(bx, by, bx + bw, by + 1, 0xAAFF78A5);
                g.fill(bx, by + bh - 1, bx + bw, by + bh, 0xAAFF78A5);
            }
        }
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
