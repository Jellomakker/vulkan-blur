package dev.jello.vmb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Plain JSON config stored in config/vulkanmotionblur.json. */
public final class BlurConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vulkanmotionblur.json");

    /** Master switch (also flipped by the toggle key). */
    public boolean enabled = true;
    /** 0..1. With FPS scaling this maps to a blur time window of 0..60 ms. */
    public double strength = 0.10;
    /** Keep the blur length identical at any frame rate (frame-time weighted). */
    public boolean fpsScaling = true;
    /** Only blur while the camera is turning. */
    public boolean onlyWhileTurning = false;
    /** Turn speed (deg/s) at which blur reaches full strength when onlyWhileTurning is on. */
    public double turnSpeedForFull = 240.0;

    private static BlurConfig instance;

    public static BlurConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    instance = GSON.fromJson(r, BlurConfig.class);
                }
            }
        } catch (Exception e) {
            VulkanMotionBlur.LOGGER.warn("Could not read config, using defaults", e);
        }
        boolean fresh = instance == null;
        if (fresh) instance = new BlurConfig();
        instance.clamp();
        if (fresh || !Files.exists(FILE)) instance.save(); // write defaults so the file is editable
    }

    public void clamp() {
        strength = Math.max(0.0, Math.min(1.0, strength));
        turnSpeedForFull = Math.max(30.0, Math.min(720.0, turnSpeedForFull));
    }

    public void save() {
        clamp();
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            VulkanMotionBlur.LOGGER.warn("Could not save config", e);
        }
    }
}
