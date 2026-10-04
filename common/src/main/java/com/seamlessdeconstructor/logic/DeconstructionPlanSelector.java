package com.seamlessdeconstructor.logic;

import net.minecraft.resources.ResourceLocation;

/** Stable recipe-choice policy used when multiple recipes produce one item. */
public final class DeconstructionPlanSelector {
    private DeconstructionPlanSelector() {
    }

    public static boolean shouldReplace(
            ResourceLocation existingId,
            double existingUnits,
            ResourceLocation candidateId,
            double candidateUnits) {
        boolean existingVanilla = "minecraft".equals(existingId.getNamespace());
        boolean candidateVanilla = "minecraft".equals(candidateId.getNamespace());
        if (candidateVanilla != existingVanilla) {
            return candidateVanilla;
        }
        return candidateUnits > existingUnits;
    }
}
