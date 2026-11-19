package dev.kotandsraka.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VisualConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("kotandsraka_visuals.json");
    private static final int CURRENT_CONFIG_VERSION = 4;

    // All visual/gameplay toggles start disabled. The first launch after this update
    // also resets old "enabled by default" config files once.
    public Integer configVersion = CURRENT_CONFIG_VERSION;

    // Combat
    public boolean hitParticles = false;
    public boolean criticalParticles = false;
    public boolean hitFlash = false;
    public boolean hitText = false;
    public boolean damageNumbers = false;
    public boolean killEffect = false;
    public boolean targetRing = false;
    public boolean targetEsp = false;
    public boolean hitRange = false;
    public boolean customCrosshair = false;
    public boolean comboHud = false;
    public boolean hitSound = false;

    // Held-item rendering
    public boolean viewModel = false;
    public boolean customSwing = false;
    public boolean customFire = false;
    // Motion blur needs temporal framebuffer history; deliberately left disabled until
    // the custom post-processing pass is added and verified.
    public boolean motionBlur = false;

    // World / render
    public boolean playerTrail = false;
    public boolean jumpRings = false;
    public boolean skullJump = false;
    public boolean chinaHat = false;
    public boolean bodyGlow = false;
    public boolean worldParticles = false;
    public boolean pearlTrail = false;
    public boolean trajectory = false;
    public boolean arrowTrajectory = false;
    public boolean trailRainbow = false;

    // HUD
    public boolean targetHud = false;
    public boolean keystrokes = false;
    public boolean armorHud = false;
    public boolean fpsHud = false;
    public boolean cpsHud = false;
    public boolean pingHud = false;
    public boolean coordsHud = false;
    public boolean speedHud = false;
    public boolean totemHud = false;
    public boolean potionHud = false;
    public boolean clockHud = false;
    public boolean compassHud = false;
    public boolean activeModulesHud = false;
    public boolean playerGlow = false;
    public boolean chestGlow = false;

    // Movement
    public boolean autoSprint = false;

    // Theme
    public boolean glassMenu = false;
    public boolean roundedMenu = false;
    public boolean animatedMenu = false;
    public boolean chromaTheme = false;
    // HSV color picker values (hue in degrees, saturation/value in 0..1 range).
    public float colorHue = 337.0f;
    public float colorSaturation = 0.78f;
    public float colorValue = 1.0f;
    // Kept for compatibility with older configuration files.
    public int colorPreset = 0;
    public float menuScale = 1.0f;
    public float hudOpacity = 0.72f;

    // Numeric settings
    public float particleScale = 1.0f;
    public int particleCount = 8;
    public int particleShape = 0;
    public float particleSpread = 0.45f;
    public float particleSpeed = 0.12f;
    public float particleHeight = 0.35f;
    public int particleLifetimeMs = 450;
    public int armorHudX = 8;
    public int armorHudY = 58;
    public int targetHudX = 8;
    public int targetHudY = 8;
    public int keystrokesHudX = -1;
    public int keystrokesHudY = -1;
    public boolean hudEditMode = false;
    public int trailIntensity = 2;
    public int crosshairSize = 6;
    public int crosshairGap = 3;
    public int crosshairThickness = 2;
    public int hudX = 8;
    public int hudY = 110;
    public int modulesHudX = -1;
    public int modulesHudY = 8;
    public int effectsHudX = 8;
    public int effectsHudY = 185;
    public int combo = 0;

    public float viewModelX = 0.0f;
    public float viewModelY = 0.0f;
    public float viewModelZ = 0.0f;
    public float viewModelScale = 1.0f;
    public float viewModelRotX = 0.0f;
    public float viewModelRotY = 0.0f;
    public float viewModelRotZ = 0.0f;
    public float swingSpeed = 1.0f;
    public float swingStrength = 1.0f;
    public float swingRotationX = 0.0f;
    public float swingRotationY = 0.0f;
    public float swingRotationZ = 0.0f;
    public float fireHeight = 0.35f;
    public float trailWidth = 2.5f;
    public int trailLength = 24;
    public int trailAlpha = 190;

    public static int accentColor() {
        VisualConfig cfg = get();
        return hsvToRgb(cfg.colorHue, cfg.colorSaturation, cfg.colorValue);
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        float h = ((hue % 360.0f) + 360.0f) % 360.0f;
        float s = Math.max(0.0f, Math.min(1.0f, saturation));
        float v = Math.max(0.0f, Math.min(1.0f, value));
        float chroma = v * s;
        float x = chroma * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - chroma;
        float r;
        float g;
        float b;
        if (h < 60.0f) { r = chroma; g = x; b = 0.0f; }
        else if (h < 120.0f) { r = x; g = chroma; b = 0.0f; }
        else if (h < 180.0f) { r = 0.0f; g = chroma; b = x; }
        else if (h < 240.0f) { r = 0.0f; g = x; b = chroma; }
        else if (h < 300.0f) { r = x; g = 0.0f; b = chroma; }
        else { r = chroma; g = 0.0f; b = x; }
        int red = Math.round((r + m) * 255.0f);
        int green = Math.round((g + m) * 255.0f);
        int blue = Math.round((b + m) * 255.0f);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public static int panelColor() {
        return 0xEE11141B;
    }

    public static int widgetColor() {
        return 0xCC262B36;
    }

    public static int mutedColor() {
        return 0xFFA0A5B2;
    }

    private static VisualConfig INSTANCE;

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE);
                boolean legacyConfig = !JsonParser.parseString(json).getAsJsonObject().has("configVersion");
                INSTANCE = legacyConfig ? new VisualConfig() : GSON.fromJson(json, VisualConfig.class);
            }
        } catch (Exception ignored) {
            INSTANCE = null;
        }

        if (INSTANCE == null || INSTANCE.configVersion == null
            || INSTANCE.configVersion < CURRENT_CONFIG_VERSION) {
            INSTANCE = new VisualConfig();
        }
        INSTANCE.configVersion = CURRENT_CONFIG_VERSION;
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(INSTANCE == null ? new VisualConfig() : INSTANCE));
        } catch (IOException ignored) {
        }
    }

    public static void reset() {
        INSTANCE = new VisualConfig();
        save();
    }

    public static void resetHudPositions() {
        VisualConfig cfg = get();
        cfg.targetHudX = 8;
        cfg.targetHudY = 8;
        cfg.armorHudX = 8;
        cfg.armorHudY = 58;
        cfg.keystrokesHudX = -1;
        cfg.keystrokesHudY = -1;
        cfg.modulesHudX = -1;
        cfg.modulesHudY = 8;
        cfg.effectsHudX = 8;
        cfg.effectsHudY = 185;
        cfg.hudX = 8;
        cfg.hudY = 110;
        save();
    }

    public static VisualConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }
}
