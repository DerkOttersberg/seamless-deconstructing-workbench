package com.seamlessdeconstructor.logic;

import net.minecraft.resources.Identifier;

/** Stable recipe-choice policy used when multiple recipes produce one item. */
public final class DeconstructionPlanSelector {
    private DeconstructionPlanSelector() {
    }

    public static boolean shouldReplace(
            Identifier existingId,
            double existingUnits,
            Identifier candidateId,
            double candidateUnits) {
        boolean existingVanilla = "minecraft".equals(existingId.getNamespace());
        boolean candidateVanilla = "minecraft".equals(candidateId.getNamespace());
        if (candidateVanilla != existingVanilla) {
            return candidateVanilla;
        }
        return candidateUnits > existingUnits;
    }
}
