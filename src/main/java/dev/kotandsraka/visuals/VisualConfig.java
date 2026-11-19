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

    // Movement
    public boolean autoSprint = false;

    // Theme
    public boolean glassMenu = false;
    public boolean roundedMenu = false;
    public boolean animatedMenu = false;
    public boolean chromaTheme = false;
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
        if (cfg.chromaTheme || cfg.animatedMenu) {
            float phase = (System.currentTimeMillis() % 5000L) / 5000.0f;
            float red = (float) (Math.sin(phase * Math.PI * 2.0) * 0.5 + 0.5);
            float green = (float) (Math.sin((phase + 0.3333f) * Math.PI * 2.0) * 0.5 + 0.5);
            float blue = (float) (Math.sin((phase + 0.6666f) * Math.PI * 2.0) * 0.5 + 0.5);
            return 0xFF000000 | ((int) (red * 255) << 16) | ((int) (green * 255) << 8) | (int) (blue * 255);
        }
        return switch (cfg.colorPreset) {
            case 1 -> 0xFFB58CFF;
            case 2 -> 0xFFFF657A;
            case 3 -> 0xFF62E6A7;
            case 4 -> 0xFFFFC45C;
            case 5 -> 0xFFFF82C8;
            default -> 0xFF56D8FF;
        };
    }

    public static int panelColor() {
        VisualConfig cfg = get();
        int rgb = switch (cfg.colorPreset) {
            case 1 -> 0x00181329;
            case 2 -> 0x00241319;
            case 3 -> 0x0010231D;
            case 4 -> 0x00241D10;
            case 5 -> 0x00251421;
            default -> 0x000C1724;
        };
        return ((cfg.glassMenu ? 0xB8 : 0xEE) << 24) | rgb;
    }

    public static int widgetColor() {
        return switch (get().colorPreset) {
            case 1 -> 0xCC29203D;
            case 2 -> 0xCC3A2029;
            case 3 -> 0xCC1B382D;
            case 4 -> 0xCC3A2E1C;
            case 5 -> 0xCC3B2033;
            default -> 0xCC183247;
        };
    }

    public static int mutedColor() {
        return switch (get().colorPreset) {
            case 1 -> 0xFFBBA8D7;
            case 2 -> 0xFFE0A3AB;
            case 3 -> 0xFF9CCDB8;
            case 4 -> 0xFFE2C38B;
            case 5 -> 0xFFE0A9CB;
            default -> 0xFF9BB5C9;
        };
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
        cfg.hudX = 8;
        cfg.hudY = 110;
        save();
    }

    public static VisualConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }
}
