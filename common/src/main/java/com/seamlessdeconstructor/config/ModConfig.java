package com.seamlessdeconstructor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ModConfig {
    static final String CANONICAL_FILE_NAME = "seamless-deconstructing-workbench.json";
    static final String LEGACY_FILE_NAME = "seamlessdeconstructor.json";
    static final String INVALID_BACKUP_SUFFIX = ".invalid.bak";

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
            if (loaded == null) {
                throw new IOException("Config contained no JSON object");
            }
            data = loaded;
            sanitize();
            save();
        } catch (Exception exception) {
            recoverInvalidConfig(exception);
        }
    }

    private static void recoverInvalidConfig(Exception cause) {
        Path backup = path.resolveSibling(path.getFileName() + INVALID_BACKUP_SUFFIX);
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            SeamlessDeconstructorMod.LOGGER.warn(
                    "Backed up invalid workbench config {} to {}",
                    path,
                    backup);
        } catch (IOException backupException) {
            cause.addSuppressed(backupException);
            SeamlessDeconstructorMod.LOGGER.error(
                    "Could not back up invalid workbench config {} to {}",
                    path,
                    backup,
                    backupException);
        }

        SeamlessDeconstructorMod.LOGGER.warn(
                "Workbench config {} was invalid; restoring sanitized defaults",
                path,
                cause);
        data = new ModConfigData();
        sanitize();
        save();
    }

    public static synchronized void save() {
        if (path == null) {
            throw new IllegalStateException("Config must be loaded before it can be saved");
        }

        sanitize();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                // Windows can report AccessDeniedException when ATOMIC_MOVE replaces an existing
                // file even though an ordinary replace is permitted. Retry without the atomic
                // option; if this is a real permission problem, the outer catch still logs it.
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            SeamlessDeconstructorMod.LOGGER.error("Could not save workbench config {}", path, exception);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException exception) {
                SeamlessDeconstructorMod.LOGGER.warn(
                        "Could not remove temporary workbench config {}", temporary, exception);
            }
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
            SeamlessDeconstructorMod.LOGGER.info(
                    "Migrated legacy workbench config {} to {}; backup retained at {}",
                    legacy,
                    path,
                    backup);
        } catch (IOException exception) {
            SeamlessDeconstructorMod.LOGGER.error(
                    "Could not migrate legacy workbench config {} to {}",
                    legacy,
                    path,
                    exception);
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
