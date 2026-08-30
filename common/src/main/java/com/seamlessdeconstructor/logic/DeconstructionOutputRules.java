package com.seamlessdeconstructor.logic;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pure durability scaling rules shared by runtime code and unit tests. */
public final class DeconstructionOutputRules {
    private DeconstructionOutputRules() {
    }

    public static <T> Map<T, Integer> applyDurability(
            Map<T, Integer> fullOutput,
            boolean damageable,
            int maxDamage,
            int damage) {
        if (fullOutput.isEmpty() || !damageable || maxDamage <= 0) {
            return fullOutput;
        }

        double durabilityFraction = (maxDamage - damage) / (double) maxDamage;
        if (durabilityFraction < 0.25D) {
            return minimumSingleOutput(fullOutput);
        }
        if (durabilityFraction < 0.50D) {
            int fullTotal = fullOutput.values().stream().mapToInt(Integer::intValue).sum();
            return scaleOutputWithMinimum(fullOutput, 0.60D, fullTotal >= 2 ? 2 : 1);
        }
        if (durabilityFraction < 0.75D) {
            return scaleOutputWithMinimum(fullOutput, 0.70D, 1);
        }
        return fullOutput;
    }

    private static <T> Map<T, Integer> minimumSingleOutput(Map<T, Integer> fullOutput) {
        Map<T, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<T, Integer> entry : fullOutput.entrySet()) {
            if (entry.getValue() > 0) {
                result.put(entry.getKey(), 1);
                break;
            }
        }
        return result;
    }

    private static <T> Map<T, Integer> scaleOutputWithMinimum(
            Map<T, Integer> fullOutput,
            double multiplier,
            int minimumTotal) {
        Map<T, Integer> scaled = new LinkedHashMap<>();
        int total = 0;
        for (Map.Entry<T, Integer> entry : fullOutput.entrySet()) {
            int scaledCount = (int) Math.floor(entry.getValue() * multiplier);
            if (scaledCount > 0) {
                scaled.put(entry.getKey(), scaledCount);
                total += scaledCount;
            }
        }

        for (Map.Entry<T, Integer> entry : fullOutput.entrySet()) {
            if (total >= minimumTotal) {
                break;
            }
            int current = scaled.getOrDefault(entry.getKey(), 0);
            if (current < entry.getValue()) {
                scaled.put(entry.getKey(), current + 1);
                total++;
            }
        }
        return scaled;
    }
}
