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
    public float particleScale = 1.0f;
    public int particleCount = 8;
    public int particleLifetimeMs = 450;
    public int armorHudX = 8;
    public int armorHudY = 58;
    public int trailIntensity = 2;
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
