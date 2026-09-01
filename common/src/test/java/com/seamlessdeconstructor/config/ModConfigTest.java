package com.seamlessdeconstructor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        Path backup = directory.resolve(ModConfig.LEGACY_FILE_NAME + ".bak");
        assertTrue(Files.exists(backup));
        assertEquals(Files.readString(legacy), Files.readString(backup));
        assertTrue(Files.exists(legacy));
        assertEquals(600, ModConfig.processTicks());
        assertEquals(0.20D, ModConfig.minLossFraction());
        assertEquals(0.80D, ModConfig.maxLossFraction());
    }

    @Test
    void invalidCanonicalConfigIsBackedUpAndReplacedWithDefaults() throws Exception {
        Path canonical = directory.resolve(ModConfig.CANONICAL_FILE_NAME);
        Files.writeString(canonical, "{not valid json");

        ModConfig.load(directory);

        Path backup = directory.resolve(ModConfig.CANONICAL_FILE_NAME + ModConfig.INVALID_BACKUP_SUFFIX);
        assertTrue(Files.exists(backup));
        assertEquals("{not valid json", Files.readString(backup));
        assertTrue(Files.readString(canonical).contains("\"processTicks\": 100"));
        assertEquals(100, ModConfig.processTicks());
        assertEquals(0.0D, ModConfig.minLossFraction());
        assertEquals(0.0D, ModConfig.maxLossFraction());
    }

    @Test
    void canonicalConfigTakesPrecedenceOverLegacyConfig() throws Exception {
        Files.writeString(directory.resolve(ModConfig.CANONICAL_FILE_NAME), """
                {"processTicks": 40, "minLossPercent": 3, "maxLossPercent": 7}
                """);
        Files.writeString(directory.resolve(ModConfig.LEGACY_FILE_NAME), """
                {"processTicks": 500, "minLossPercent": 30, "maxLossPercent": 70}
                """);

        ModConfig.load(directory);

        assertEquals(40, ModConfig.processTicks());
        assertEquals(0.03D, ModConfig.minLossFraction());
        assertEquals(0.07D, ModConfig.maxLossFraction());
        assertFalse(Files.exists(directory.resolve(ModConfig.LEGACY_FILE_NAME + ".bak")));
    }
}
