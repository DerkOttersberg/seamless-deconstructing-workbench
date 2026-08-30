package com.seamlessdeconstructor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ModConfig {
    static final String CANONICAL_FILE_NAME = "seamless-deconstructing-workbench.json";
    static final String LEGACY_FILE_NAME = "seamlessdeconstructor.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfigData data = new ModConfigData();
    private static Path path;

    private ModConfig() {
    }

    public static synchronized void load(Path configDirectory) {
        path = configDirectory.resolve(CANONICAL_FILE_NAME);
        migrateLegacyConfig(configDirectory);

        if (!Files.exists(path)) {
            data = new ModConfigData();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ModConfigData loaded = GSON.fromJson(reader, ModConfigData.class);
            data = loaded != null ? loaded : new ModConfigData();
            sanitize();
        } catch (Exception ignored) {
            data = new ModConfigData();
        }
    }

    public static synchronized void save() {
        if (path == null) {
            throw new IllegalStateException("Config must be loaded before it can be saved");
        }

        sanitize();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static int processTicks() {
        return data.processTicks;
    }

    public static double minLossFraction() {
        return data.minLossPercent / 100.0;
    }

    public static double maxLossFraction() {
        return data.maxLossPercent / 100.0;
    }

    private static void migrateLegacyConfig(Path configDirectory) {
        Path legacy = configDirectory.resolve(LEGACY_FILE_NAME);
        if (Files.exists(path) || !Files.exists(legacy)) {
            return;
        }

        Path backup = configDirectory.resolve(LEGACY_FILE_NAME + ".bak");
        try {
            Files.createDirectories(configDirectory);
            if (!Files.exists(backup)) {
                Files.copy(legacy, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
            Files.copy(legacy, path, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException ignored) {
        }
    }

    private static void sanitize() {
        data.processTicks = clamp(data.processTicks, 20, 600);
        data.minLossPercent = clamp(data.minLossPercent, 0, 90);
        data.maxLossPercent = clamp(data.maxLossPercent, 0, 90);
        if (data.minLossPercent > data.maxLossPercent) {
            int temp = data.minLossPercent;
            data.minLossPercent = data.maxLossPercent;
            data.maxLossPercent = temp;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class ModConfigData {
        public int processTicks = 100;
        public int minLossPercent;
        public int maxLossPercent;
    }
}
