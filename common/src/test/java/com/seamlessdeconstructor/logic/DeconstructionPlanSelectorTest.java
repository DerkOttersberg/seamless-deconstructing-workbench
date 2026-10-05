package com.seamlessdeconstructor.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DeconstructionPlanSelectorTest {
    @Test
    void vanillaRecipeWinsOverModdedRecipe() {
        assertTrue(DeconstructionPlanSelector.shouldReplace(
                ResourceLocation.parse("example:table"),
                20.0D,
                ResourceLocation.parse("minecraft:table"),
                2.0D));
        assertFalse(DeconstructionPlanSelector.shouldReplace(
                ResourceLocation.parse("minecraft:table"),
                2.0D,
                ResourceLocation.parse("example:table"),
                20.0D));
    }

    @Test
    void sameNamespaceClassPrefersMoreIngredientUnits() {
        assertTrue(DeconstructionPlanSelector.shouldReplace(
                ResourceLocation.parse("minecraft:cheap"),
                2.0D,
                ResourceLocation.parse("minecraft:expensive"),
                3.0D));
        assertFalse(DeconstructionPlanSelector.shouldReplace(
                ResourceLocation.parse("minecraft:expensive"),
                3.0D,
                ResourceLocation.parse("minecraft:cheap"),
                2.0D));
    }
}
