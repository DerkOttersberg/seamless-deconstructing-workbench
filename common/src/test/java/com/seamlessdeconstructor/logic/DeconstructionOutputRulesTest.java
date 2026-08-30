package com.seamlessdeconstructor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeconstructionOutputRulesTest {
    private static Map<String, Integer> fullOutput() {
        Map<String, Integer> output = new LinkedHashMap<>();
        output.put("planks", 3);
        output.put("sticks", 2);
        return output;
    }

    @Test
    void badlyDamagedItemsReturnOneDeterministicIngredient() {
        assertEquals(
                Map.of("planks", 1),
                DeconstructionOutputRules.applyDurability(fullOutput(), true, 100, 80));
    }

    @Test
    void middleDurabilityBandsScaleWithoutDroppingBelowTheirMinimum() {
        assertEquals(
                Map.of("planks", 1, "sticks", 1),
                DeconstructionOutputRules.applyDurability(fullOutput(), true, 100, 60));
        assertEquals(
                Map.of("planks", 2, "sticks", 1),
                DeconstructionOutputRules.applyDurability(fullOutput(), true, 100, 30));
    }

    @Test
    void healthyOrNonDamageableItemsKeepTheirOutput() {
        Map<String, Integer> output = fullOutput();
        assertSame(output, DeconstructionOutputRules.applyDurability(output, false, 0, 0));
        assertSame(output, DeconstructionOutputRules.applyDurability(output, true, 100, 10));
    }
}
