package com.seamlessdeconstructor.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class DeconstructionPlanSelectorTest {
    @Test
    void vanillaRecipeWinsOverModdedRecipe() {
        assertTrue(DeconstructionPlanSelector.shouldReplace(
                Identifier.parse("example:table"),
                20.0D,
                Identifier.parse("minecraft:table"),
                2.0D));
        assertFalse(DeconstructionPlanSelector.shouldReplace(
                Identifier.parse("minecraft:table"),
                2.0D,
                Identifier.parse("example:table"),
                20.0D));
    }

    @Test
    void sameNamespaceClassPrefersMoreIngredientUnits() {
        assertTrue(DeconstructionPlanSelector.shouldReplace(
                Identifier.parse("minecraft:cheap"),
                2.0D,
                Identifier.parse("minecraft:expensive"),
                3.0D));
        assertFalse(DeconstructionPlanSelector.shouldReplace(
                Identifier.parse("minecraft:expensive"),
                3.0D,
                Identifier.parse("minecraft:cheap"),
                2.0D));
    }
}
