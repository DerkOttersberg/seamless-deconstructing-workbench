package com.seamlessdeconstructor.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class DeconstructionResolver {
    private static final Map<RecipeManager, Map<Item, DeconstructionPlan>> CACHE = new WeakHashMap<>();

    private DeconstructionResolver() {
    }

    public static Optional<DeconstructionPlan> resolve(ServerLevel world, Item item) {
        Map<Item, DeconstructionPlan> byItem;
        synchronized (CACHE) {
            byItem = CACHE.computeIfAbsent(world.getRecipeManager(), unused -> buildCache(world));
        }
        return Optional.ofNullable(byItem.get(item));
    }

    private static Map<Item, DeconstructionPlan> buildCache(ServerLevel world) {
        Map<Item, DeconstructionPlan> byOutputItem = new LinkedHashMap<>();

        List<Recipe<?>> recipes = world.getRecipeManager().getRecipes().stream()
            .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
            .toList();

        for (Recipe<?> recipe : recipes) {
            if (!(recipe instanceof CraftingRecipe craftingRecipe) || !(craftingRecipe instanceof ShapedRecipe shapedRecipe)) {
                continue;
            }

            ItemStack result;
            try {
                result = craftingRecipe.getResultItem(world.registryAccess());
            } catch (Exception ignored) {
                continue;
            }

            if (result.isEmpty()) {
                continue;
            }

            Map<Item, Integer> ingredientCount = new LinkedHashMap<>();
            for (Ingredient ingredient : shapedRecipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }

                ItemStack[] matching = ingredient.getItems();
                if (matching.length == 0 || matching[0].isEmpty()) {
                    continue;
                }

                ingredientCount.merge(matching[0].getItem(), 1, Integer::sum);
            }

            if (ingredientCount.isEmpty()) {
                continue;
            }

            int outputCount = Math.max(1, result.getCount());
            Map<Item, Double> perOutput = new LinkedHashMap<>();
            for (Map.Entry<Item, Integer> ingredientEntry : ingredientCount.entrySet()) {
                perOutput.put(ingredientEntry.getKey(), ingredientEntry.getValue() / (double) outputCount);
            }

            DeconstructionPlan candidatePlan = new DeconstructionPlan(recipe.getId(), perOutput);
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

        return candidate.totalUnitsPerOutput() > existing.totalUnitsPerOutput();
    }
}
