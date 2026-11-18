package dev.kotandsraka.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VisualConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("kotandsraka_visuals.json");

    public boolean hitParticles = true;
    public boolean criticalParticles = true;
    public boolean targetHud = true;
    public boolean keystrokes = true;
    public boolean hitFlash = true;
    public boolean hitText = true;
    public boolean armorHud = true;
    public boolean playerTrail = true;
    public boolean jumpRings = true;
    public boolean autoSprint = true;

    public boolean damageNumbers = true;
    public boolean killEffect = true;
    public boolean targetEsp = true;
    public boolean customCrosshair = true;
    public boolean hitRange = true;
    public boolean customSwing = true;
    public boolean viewModel = true;
    public boolean pearlTrail = true;
    public boolean trajectory = true;
    public boolean chinaHat = true;
    public boolean bodyGlow = true;
    public boolean worldParticles = true;
    public boolean customFire = true;
    public boolean motionBlur = false;

    public boolean fpsHud = true;
    public boolean cpsHud = true;
    public boolean pingHud = true;
    public boolean coordsHud = true;
    public boolean speedHud = true;
    public boolean comboHud = true;
    public boolean totemHud = true;
    public boolean potionHud = true;
    public boolean clockHud = true;
    public boolean compassHud = true;

    public boolean glassMenu = true;
    public boolean roundedMenu = true;
    public boolean animatedMenu = true;
    public boolean chromaTheme = false;
    public float menuScale = 1.0f;
    public float hudOpacity = 0.72f;

    public float particleScale = 1.0f;
    public int particleCount = 8;
    public int particleLifetimeMs = 450;
    public int armorHudX = 8;
    public int armorHudY = 58;
    public int trailIntensity = 2;
    public int crosshairSize = 6;
    public int crosshairGap = 3;
    public int crosshairThickness = 2;
    public int hudX = 8;
    public int hudY = 110;
    public int combo = 0;

    public float viewModelX = 0.0f;
    public float viewModelY = 0.0f;
    public float viewModelZ = 0.0f;
    public float viewModelScale = 1.0f;
    public float viewModelRotX = 0.0f;
    public float viewModelRotY = 0.0f;
    public float viewModelRotZ = 0.0f;
    public float trailWidth = 2.5f;
    public int trailLength = 24;
    public int trailAlpha = 190;
    public boolean trailRainbow = false;

    private static VisualConfig INSTANCE;

    public static void load() {
        try {
            if (Files.exists(FILE)) INSTANCE = GSON.fromJson(Files.readString(FILE), VisualConfig.class);
        } catch (Exception ignored) {}
        if (INSTANCE == null) INSTANCE = new VisualConfig();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(INSTANCE == null ? new VisualConfig() : INSTANCE));
        } catch (IOException ignored) {}
    }

    public static void reset() {
        INSTANCE = new VisualConfig();
        save();
    }

    public static VisualConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }
}
