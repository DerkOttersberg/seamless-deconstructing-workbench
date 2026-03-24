package com.seamlessdeconstructor.logic;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Optional;

public final class DeconstructionResolver {
    private static final Map<RecipeManager, Map<Item, DeconstructionPlan>> CACHE = new WeakHashMap<>();

    private DeconstructionResolver() {
    }

    public static Optional<DeconstructionPlan> resolve(ServerWorld world, Item item) {
        Map<Item, DeconstructionPlan> byItem;
        synchronized (CACHE) {
            byItem = CACHE.computeIfAbsent(world.getRecipeManager(), unused -> buildCache(world));
        }
        return Optional.ofNullable(byItem.get(item));
    }

    private static Map<Item, DeconstructionPlan> buildCache(ServerWorld world) {
        Map<Item, DeconstructionPlan> byOutputItem = new LinkedHashMap<>();
        RecipeManager recipeManager = world.getRecipeManager();

        List<ShapedRecipe> recipes = new ArrayList<>();
        for (var recipe : recipeManager.values()) {
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                recipes.add(shapedRecipe);
            }
        }
        recipes.sort(Comparator.comparing(recipe -> recipe.getId().toString()));

        for (ShapedRecipe shapedRecipe : recipes) {
            ItemStack result = shapedRecipe.getOutput(world.getRegistryManager()).copy();

            if (result.isEmpty()) {
                continue;
            }

            Map<Item, Integer> ingredientCount = new LinkedHashMap<>();

            for (Ingredient ingredient : shapedRecipe.getIngredients()) {
                if (ingredient == null || ingredient.isEmpty()) {
                    continue;
                }

                ItemStack[] matches = ingredient.getMatchingStacks();
                if (matches.length == 0 || matches[0].isEmpty()) {
                    continue;
                }

                ingredientCount.merge(matches[0].getItem(), 1, Integer::sum);
            }

            if (ingredientCount.isEmpty()) {
                continue;
            }

            int outputCount = Math.max(1, result.getCount());
            Map<Item, Double> perOutput = new LinkedHashMap<>();
            for (Map.Entry<Item, Integer> ingredientEntry : ingredientCount.entrySet()) {
                perOutput.put(ingredientEntry.getKey(), ingredientEntry.getValue() / (double) outputCount);
            }

            Identifier recipeId = shapedRecipe.getId();
            DeconstructionPlan candidatePlan = new DeconstructionPlan(recipeId, perOutput);
            DeconstructionPlan existingPlan = byOutputItem.get(result.getItem());
            if (existingPlan == null || shouldReplace(existingPlan, candidatePlan)) {
                byOutputItem.put(result.getItem(), candidatePlan);
            }
        }

        return byOutputItem;
    }

    private static boolean shouldReplace(DeconstructionPlan existing, DeconstructionPlan candidate) {
        boolean existingVanilla = "minecraft".equals(existing.recipeId().getNamespace());
        boolean candidateVanilla = "minecraft".equals(candidate.recipeId().getNamespace());

        if (candidateVanilla && !existingVanilla) {
            return true;
        }
        if (existingVanilla && !candidateVanilla) {
            return false;
        }

        // Prefer the plan that returns more total ingredient units per output item.
        return candidate.totalUnitsPerOutput() > existing.totalUnitsPerOutput();
    }
}
