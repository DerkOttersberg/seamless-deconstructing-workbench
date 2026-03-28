package com.seamlessdeconstructor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("seamlessdeconstructor.json");

    private static ModConfigData data = new ModConfigData();

    private ModConfig() {
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(PATH)) {
            ModConfigData loaded = GSON.fromJson(reader, ModConfigData.class);
            if (loaded != null) {
                data = loaded;
            }
            sanitize();
        } catch (Exception ignored) {
            data = new ModConfigData();
        }
    }

    public static void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
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
        public int minLossPercent = 0;
        public int maxLossPercent = 0;
    }
}
