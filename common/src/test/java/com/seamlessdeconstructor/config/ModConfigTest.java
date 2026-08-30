package com.seamlessdeconstructor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModConfigTest {
    @TempDir
    Path directory;

    @Test
    void legacyNameMigratesToCanonicalFileAndKeepsBackup() throws Exception {
        Path legacy = directory.resolve(ModConfig.LEGACY_FILE_NAME);
        Files.writeString(legacy, """
                {
                  "processTicks": 900,
                  "minLossPercent": 80,
                  "maxLossPercent": 20
                }
                """);

        ModConfig.load(directory);

        assertTrue(Files.exists(directory.resolve(ModConfig.CANONICAL_FILE_NAME)));
        assertTrue(Files.exists(directory.resolve(ModConfig.LEGACY_FILE_NAME + ".bak")));
        assertTrue(Files.exists(legacy));
        assertEquals(600, ModConfig.processTicks());
        assertEquals(0.20D, ModConfig.minLossFraction());
        assertEquals(0.80D, ModConfig.maxLossFraction());
    }
}
