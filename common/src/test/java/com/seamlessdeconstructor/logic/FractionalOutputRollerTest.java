package com.seamlessdeconstructor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class FractionalOutputRollerTest {
    @Test
    void wholeUnitsAreConservedWithoutConfiguredLoss() {
        Map<String, Integer> output = FractionalOutputRoller.roll(
                Map.of("minecraft:oak_planks", 3.0D),
                RandomSource.create(42L),
                0.0D,
                0.0D);

        assertEquals(Map.of("minecraft:oak_planks", 3), output);
    }

    @Test
    void totalLossDropsEveryTrial() {
        Map<String, Integer> output = FractionalOutputRoller.roll(
                Map.of("minecraft:iron_ingot", 4.0D),
                RandomSource.create(42L),
                1.0D,
                1.0D);

        assertTrue(output.isEmpty());
    }

    @Test
    void fractionalUnitsAreProbabilisticInsteadOfAlwaysRoundedUp() {
        Map<String, Double> units = new LinkedHashMap<>();
        units.put("minecraft:stick", 0.5D);
        int returned = 0;
        for (int seed = 0; seed < 256; seed++) {
            returned += FractionalOutputRoller.roll(
                    units,
                    RandomSource.create(seed),
                    0.0D,
                    0.0D).getOrDefault("minecraft:stick", 0);
        }

        assertTrue(returned > 80 && returned < 176, "half-unit roll count was " + returned);
    }
}
