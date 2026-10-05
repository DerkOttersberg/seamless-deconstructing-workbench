package com.seamlessdeconstructor.logic;

import com.derko.seamlessapi.DeconstructionAPI;
import com.derko.seamlessapi.api.deconstruction.DeconstructionRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.CraftingInput;
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
            byItem = CACHE.computeIfAbsent(world.getRecipeManager(), unused -> buildCache(world));
        }
        return Optional.ofNullable(byItem.get(item));
    }

    static Map<Item, DeconstructionPlan> buildCache(ServerLevel world) {
        Map<Item, DeconstructionPlan> byOutputItem = new LinkedHashMap<>();
        RecipeManager recipeManager = world.getRecipeManager();

        List<RecipeHolder<?>> recipes = new ArrayList<>(recipeManager.getRecipes());
        recipes.sort(Comparator.comparing(entry -> entry.id().toString()));

        for (RecipeHolder<?> recipeEntry : recipes) {
            if (!(recipeEntry.value() instanceof ShapedRecipe shapedRecipe)) {
                continue;
            }

            ItemStack result;
            try {
                result = shapedRecipe.assemble(buildRepresentativeInput(shapedRecipe), world.registryAccess());
            } catch (Exception ignored) {
                continue;
            }

            if (result.isEmpty()) {
                continue;
            }

            Map<Item, Integer> ingredientCount = new LinkedHashMap<>();

            for (Ingredient ingredient : shapedRecipe.getIngredients()) {
                if (ingredient.isEmpty() || ingredient.getItems().length == 0) continue;
                ingredientCount.merge(ingredient.getItems()[0].getItem(), 1, Integer::sum);
            }

            if (ingredientCount.isEmpty()) {
                continue;
            }

            int outputCount = Math.max(1, result.getCount());
            Map<Item, Double> perOutput = new LinkedHashMap<>();
            for (Map.Entry<Item, Integer> ingredientEntry : ingredientCount.entrySet()) {
                perOutput.put(ingredientEntry.getKey(), ingredientEntry.getValue() / (double) outputCount);
            }

            DeconstructionPlan candidatePlan = new DeconstructionPlan(recipeEntry.id(), perOutput);
            DeconstructionPlan existingPlan = byOutputItem.get(result.getItem());
            if (existingPlan == null || shouldReplace(existingPlan, candidatePlan)) {
                byOutputItem.put(result.getItem(), candidatePlan);
            }
        }

        mergeApiRegistrations(byOutputItem);

        return byOutputItem;
    }

    public static void invalidateCache() {
        synchronized (CACHE) { CACHE.clear(); }
    }

    private static CraftingInput buildRepresentativeInput(ShapedRecipe recipe) {
        int width = Math.max(1, recipe.getWidth());
        int height = Math.max(1, recipe.getHeight());
        AbstractContainerMenu owner = new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return false; }
        };
        CraftingContainer input = new TransientCraftingContainer(owner, width, height);
        for (int i = 0; i < recipe.getIngredients().size() && i < input.getContainerSize(); i++) {
            Ingredient ingredient = recipe.getIngredients().get(i);
            if (!ingredient.isEmpty() && ingredient.getItems().length != 0)
                input.setItem(i, ingredient.getItems()[0].copy());
        }
        return input.asCraftInput();
    }

    private static void mergeApiRegistrations(Map<Item, DeconstructionPlan> byOutputItem) {
        DeconstructionAPI.freezeAndGetAll().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    ResourceLocation inputId;
                    try {
                        inputId = ResourceLocation.parse(entry.getKey());
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    Item input = BuiltInRegistries.ITEM.get(inputId);
                    if (input == null) {
                        return;
                    }

                    DeconstructionRegistration registration = entry.getValue();
                    Map<Item, Double> ingredients = new LinkedHashMap<>();
                    registration.ingredientUnits().forEach((ingredientId, units) -> {
                        try {
                            Item ingredient = BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingredientId));
                            if (ingredient != null && units != null && units > 0.0D) {
                                ingredients.put(ingredient, units);
                            }
                        } catch (RuntimeException ignored) {
                        }
                    });

                    if (!ingredients.isEmpty()) {
                        ResourceLocation registrationId = ResourceLocation.fromNamespaceAndPath(
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
