package com.seamlessdeconstructor.logic;

import com.derko.seamlessapi.DeconstructionAPI;
import com.derko.seamlessapi.api.deconstruction.DeconstructionRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
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
            byItem = CACHE.computeIfAbsent(world.recipeAccess(), unused -> buildCache(world));
        }
        return Optional.ofNullable(byItem.get(item));
    }

    static Map<Item, DeconstructionPlan> buildCache(ServerLevel world) {
        Map<Item, DeconstructionPlan> byOutputItem = new LinkedHashMap<>();
        RecipeManager recipeManager = world.recipeAccess();

        List<RecipeHolder<?>> recipes = new ArrayList<>(recipeManager.getRecipes());
        recipes.sort(Comparator.comparing(entry -> entry.id().identifier().toString()));

        for (RecipeHolder<?> recipeEntry : recipes) {
            if (!(recipeEntry.value() instanceof CraftingRecipe craftingRecipe) || !(craftingRecipe instanceof ShapedRecipe shapedRecipe)) {
                continue;
            }

            ItemStack result;
            try {
                result = shapedRecipe.assemble(buildRepresentativeInput(shapedRecipe));
            } catch (Exception ignored) {
                continue;
            }

            if (result.isEmpty()) {
                continue;
            }

            Map<Item, Integer> ingredientCount = new LinkedHashMap<>();

            for (Optional<Ingredient> maybeIngredient : shapedRecipe.getIngredients()) {
                if (maybeIngredient.isEmpty() || maybeIngredient.get().isEmpty()) {
                    continue;
                }

                Ingredient ingredient = maybeIngredient.get();

                Optional<Item> candidate = ingredient.items().findFirst().map(holder -> holder.value());
                if (candidate.isEmpty()) {
                    continue;
                }

                ingredientCount.merge(candidate.get(), 1, Integer::sum);
            }

            if (ingredientCount.isEmpty()) {
                continue;
            }

            int outputCount = Math.max(1, result.getCount());
            Map<Item, Double> perOutput = new LinkedHashMap<>();
            for (Map.Entry<Item, Integer> ingredientEntry : ingredientCount.entrySet()) {
                perOutput.put(ingredientEntry.getKey(), ingredientEntry.getValue() / (double) outputCount);
            }

            DeconstructionPlan candidatePlan = new DeconstructionPlan(recipeEntry.id().identifier(), perOutput);
            DeconstructionPlan existingPlan = byOutputItem.get(result.getItem());
            if (existingPlan == null || shouldReplace(existingPlan, candidatePlan)) {
                byOutputItem.put(result.getItem(), candidatePlan);
            }
        }

        mergeApiRegistrations(byOutputItem);

        return byOutputItem;
    }

    private static CraftingInput buildRepresentativeInput(ShapedRecipe recipe) {
        int width = Math.max(1, recipe.getWidth());
        int height = Math.max(1, recipe.getHeight());

        List<ItemStack> inputStacks = new ArrayList<>(width * height);
        for (Optional<Ingredient> maybeIngredient : recipe.getIngredients()) {
            if (maybeIngredient.isEmpty() || maybeIngredient.get().isEmpty()) {
                inputStacks.add(ItemStack.EMPTY);
                continue;
            }

            ItemStack stack = maybeIngredient.get()
                    .items()
                    .findFirst()
                    .map(holder -> new ItemStack(holder.value()))
                    .orElse(ItemStack.EMPTY);
            inputStacks.add(stack);
        }

        while (inputStacks.size() < width * height) {
            inputStacks.add(ItemStack.EMPTY);
        }

        return CraftingInput.of(width, height, inputStacks);
    }

    private static void mergeApiRegistrations(Map<Item, DeconstructionPlan> byOutputItem) {
        DeconstructionAPI.freezeAndGetAll().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Identifier inputId;
                    try {
                        inputId = Identifier.parse(entry.getKey());
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    Item input = BuiltInRegistries.ITEM.getValue(inputId);
                    if (input == null) {
                        return;
                    }

                    DeconstructionRegistration registration = entry.getValue();
                    Map<Item, Double> ingredients = new LinkedHashMap<>();
                    registration.ingredientUnits().forEach((ingredientId, units) -> {
                        try {
                            Item ingredient = BuiltInRegistries.ITEM.getValue(Identifier.parse(ingredientId));
                            if (ingredient != null && units != null && units > 0.0D) {
                                ingredients.put(ingredient, units);
                            }
                        } catch (RuntimeException ignored) {
                        }
                    });

                    if (!ingredients.isEmpty()) {
                        Identifier registrationId = Identifier.fromNamespaceAndPath(
                                "seamlessapi",
                                "registered/" + inputId.getNamespace() + "/" + inputId.getPath());
                        byOutputItem.put(
                                input,
                                new DeconstructionPlan(
                                        registrationId,
                                        ingredients,
                                        registration.damageScalingEnabled()));
                    }
                });
    }

    private static boolean shouldReplace(DeconstructionPlan existing, DeconstructionPlan candidate) {
        return DeconstructionPlanSelector.shouldReplace(
                existing.recipeId(),
                existing.totalUnitsPerOutput(),
                candidate.recipeId(),
                candidate.totalUnitsPerOutput());
    }
}
