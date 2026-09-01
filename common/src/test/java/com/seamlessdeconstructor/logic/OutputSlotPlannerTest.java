package com.seamlessdeconstructor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OutputSlotPlannerTest {
    private static final int OUTPUT_START = 2;
    private static final int OUTPUT_END = 7;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.ENDER_PEARL, 16);
        bindTestComponents(Items.DIAMOND, 64);
        bindTestComponents(Items.COBBLESTONE, 64);
        bindTestComponents(Items.STONE, 64);
    }

    @Test
    void splitsAndPlansUsingTheItemsActualMaximumStackSize() {
        NonNullList<ItemStack> inventory = emptyInventory();
        ItemStack oversizedPearls = new ItemStack(Items.ENDER_PEARL, 17);

        List<ItemStack> split = OutputSlotPlanner.splitToMaxStackSize(oversizedPearls);
        assertEquals(List.of(16, 1), split.stream().map(ItemStack::getCount).toList());

        List<ItemStack> planned = OutputSlotPlanner.plan(
                        inventory,
                        OUTPUT_START,
                        OUTPUT_END,
                        split)
                .orElseThrow();
        assertEquals(16, planned.get(OUTPUT_START).getCount());
        assertEquals(1, planned.get(OUTPUT_START + 1).getCount());
    }

    @Test
    void componentBearingStacksKeepTheirIdentityAndDoNotMerge() {
        NonNullList<ItemStack> inventory = emptyInventory();
        ItemStack existing = namedDiamond("Existing");
        inventory.set(OUTPUT_START, existing.copy());
        ItemStack requested = namedDiamond("Requested");

        List<ItemStack> planned = OutputSlotPlanner.plan(
                        inventory,
                        OUTPUT_START,
                        OUTPUT_END,
                        List.of(requested))
                .orElseThrow();

        assertEquals(1, planned.get(OUTPUT_START).getCount());
        assertEquals(Component.literal("Existing"), planned.get(OUTPUT_START).get(DataComponents.CUSTOM_NAME));
        assertEquals(1, planned.get(OUTPUT_START + 1).getCount());
        assertEquals(Component.literal("Requested"), planned.get(OUTPUT_START + 1).get(DataComponents.CUSTOM_NAME));
    }

    @Test
    void capacityFailureIsAtomicAndLeavesTheSourceInventoryUntouched() {
        NonNullList<ItemStack> inventory = emptyInventory();
        for (int slot = OUTPUT_START; slot <= OUTPUT_END; slot++) {
            inventory.set(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        assertTrue(OutputSlotPlanner.plan(
                        inventory,
                        OUTPUT_START,
                        OUTPUT_END,
                        List.of(new ItemStack(Items.STONE)))
                .isEmpty());
        for (int slot = OUTPUT_START; slot <= OUTPUT_END; slot++) {
            assertTrue(inventory.get(slot).is(Items.COBBLESTONE));
            assertEquals(64, inventory.get(slot).getCount());
        }
    }

    @Test
    void pendingOperationRoundTripsExactInputAndOutputsThroughNbt() {
        ItemStack input = namedDiamond("Input identity");
        ItemStack outputStack = namedDiamond("Exact output");
        PendingDeconstructionOperation operation = new PendingDeconstructionOperation(
                input,
                true,
                List.of(outputStack));

        RegistryAccess.Frozen lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
        operation.save(output);
        CompoundTag tag = output.buildResult();
        ValueInput inputView = TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
        PendingDeconstructionOperation restored = PendingDeconstructionOperation.load(inputView).orElseThrow();

        assertTrue(restored.consumesBook());
        assertTrue(restored.matchesInput(input.copyWithCount(32)));
        assertFalse(restored.matchesInput(namedDiamond("Different identity")));
        assertEquals(1, restored.outputs().size());
        assertEquals(
                Component.literal("Exact output"),
                restored.outputs().getFirst().get(DataComponents.CUSTOM_NAME));
        assertEquals(1, restored.outputs().getFirst().getCount());
    }

    private static NonNullList<ItemStack> emptyInventory() {
        return NonNullList.withSize(8, ItemStack.EMPTY);
    }

    private static ItemStack namedDiamond(String name) {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void bindTestComponents(Item item, int maxStackSize) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, maxStackSize)
                    .build());
        }
    }
}
