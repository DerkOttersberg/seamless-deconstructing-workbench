package com.seamlessdeconstructor.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DeconstructionPlanSelectorTest {
    @Test
    void vanillaRecipeWinsOverModdedRecipe() {
        assertTrue(DeconstructionPlanSelector.shouldReplace(
                new ResourceLocation("example:table"),
                20.0D,
                new ResourceLocation("minecraft:table"),
                2.0D));
        assertFalse(DeconstructionPlanSelector.shouldReplace(
                new ResourceLocation("minecraft:table"),
                2.0D,
                new ResourceLocation("example:table"),
                20.0D));
    }

    @Test
    void sameNamespaceClassPrefersMoreIngredientUnits() {
        assertTrue(DeconstructionPlanSelector.shouldReplace(
                new ResourceLocation("minecraft:cheap"),
                2.0D,
                new ResourceLocation("minecraft:expensive"),
                3.0D));
        assertFalse(DeconstructionPlanSelector.shouldReplace(
                new ResourceLocation("minecraft:expensive"),
                3.0D,
                new ResourceLocation("minecraft:cheap"),
                2.0D));
    }
}
