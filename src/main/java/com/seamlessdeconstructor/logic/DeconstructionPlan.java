package com.seamlessdeconstructor.logic;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DeconstructionPlan {
    private final Identifier recipeId;
    private final Map<Item, Double> unitsPerOutput;

    public DeconstructionPlan(Identifier recipeId, Map<Item, Double> unitsPerOutput) {
        this.recipeId = recipeId;
        this.unitsPerOutput = Map.copyOf(unitsPerOutput);
    }

    public Identifier recipeId() {
        return recipeId;
    }

    public double totalUnitsPerOutput() {
        double total = 0.0;
        for (double value : unitsPerOutput.values()) {
            total += value;
        }
        return total;
    }

    public Map<Item, Integer> maxRollPerOperation() {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Item, Double> entry : unitsPerOutput.entrySet()) {
            result.put(entry.getKey(), Math.max(1, (int) Math.ceil(entry.getValue())));
        }
        return result;
    }

    public Map<Item, Integer> rollOutput(Random random, double minLoss, double maxLoss) {
        double lower = Math.min(minLoss, maxLoss);
        double upper = Math.max(minLoss, maxLoss);
        double loss = lower + random.nextDouble() * (upper - lower);

        Map<Item, Integer> result = new LinkedHashMap<>();

        for (Map.Entry<Item, Double> entry : unitsPerOutput.entrySet()) {
            double rawUnits = entry.getValue();
            int whole = (int) Math.floor(rawUnits);
            double fraction = rawUnits - whole;

            int trials = whole + (random.nextDouble() < fraction ? 1 : 0);
            int kept = 0;

            for (int i = 0; i < trials; i++) {
                if (random.nextDouble() >= loss) {
                    kept++;
                }
            }

            if (kept > 0) {
                result.put(entry.getKey(), kept);
            }
        }

        return result;
    }
}
