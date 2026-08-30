package com.seamlessdeconstructor.logic;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

public final class DeconstructionPlan {
    private final Identifier recipeId;
    private final Map<Item, Double> unitsPerOutput;
    private final boolean damageScalingEnabled;

    public DeconstructionPlan(Identifier recipeId, Map<Item, Double> unitsPerOutput) {
        this(recipeId, unitsPerOutput, true);
    }

    public DeconstructionPlan(Identifier recipeId, Map<Item, Double> unitsPerOutput, boolean damageScalingEnabled) {
        this.recipeId = recipeId;
        this.unitsPerOutput = Collections.unmodifiableMap(new LinkedHashMap<>(unitsPerOutput));
        this.damageScalingEnabled = damageScalingEnabled;
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

    public boolean damageScalingEnabled() {
        return damageScalingEnabled;
    }

    public Map<Item, Integer> maxRollPerOperation() {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Item, Double> entry : unitsPerOutput.entrySet()) {
            result.put(entry.getKey(), Math.max(1, (int) Math.ceil(entry.getValue())));
        }
        return result;
    }

    public Map<Item, Integer> rollOutput(RandomSource random, double minLoss, double maxLoss) {
        return FractionalOutputRoller.roll(unitsPerOutput, random, minLoss, maxLoss);
    }
}
