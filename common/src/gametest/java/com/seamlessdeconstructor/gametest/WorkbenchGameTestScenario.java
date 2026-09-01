package com.seamlessdeconstructor.gametest;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.logic.DeconstructionResolver;
import com.seamlessdeconstructor.logic.PendingDeconstructionOperation;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.registry.ModBlocks;
import com.seamlessdeconstructor.screen.ReverseDeconstructorScreenHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

public final class WorkbenchGameTestScenario {
    private static final BlockPos WORKBENCH_POS = new BlockPos(1, 1, 1);

    private WorkbenchGameTestScenario() {
    }

    public static void processesCraftingTableIntoIngredients(GameTestHelper helper) {
        var workbench = ModBlocks.REVERSE_DECONSTRUCTOR.get();
        helper.assertValueEqual(
                BuiltInRegistries.BLOCK.getKey(workbench),
                SeamlessDeconstructorMod.id("reverse_deconstructor"),
                "The preserved workbench block ID changed");
        helper.assertValueEqual(
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY.get()),
                SeamlessDeconstructorMod.id("reverse_deconstructor"),
                "The preserved workbench block-entity ID changed");
        helper.assertTrue(
                DeconstructionResolver.resolve(helper.getLevel(), Items.CRAFTING_TABLE).isPresent(),
                "The live recipe manager did not resolve the crafting-table recipe");

        ReverseDeconstructorBlockEntity blockEntity = placeWorkbench(helper);
        blockEntity.setItem(ReverseDeconstructorBlockEntity.INPUT_SLOT, new ItemStack(Items.CRAFTING_TABLE));

        helper.runAfterDelay(ModConfig.processTicks() + 10L, () -> {
            helper.assertTrue(blockEntity.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).isEmpty(),
                    "The workbench did not consume one input item");

            int outputCount = 0;
            for (int slot = ReverseDeconstructorBlockEntity.OUTPUT_START;
                    slot <= ReverseDeconstructorBlockEntity.OUTPUT_END;
                    slot++) {
                ItemStack output = blockEntity.getItem(slot);
                if (!output.isEmpty()) {
                    helper.assertTrue(output.is(ItemTags.PLANKS), "Crafting-table salvage produced a non-plank item");
                    outputCount += output.getCount();
                }
            }

            helper.assertValueEqual(outputCount, 4, "Crafting-table salvage did not conserve its four ingredients");
            helper.succeed();
        });
    }

    public static void transfersEnchantmentsAndConsumesBookAtomically(GameTestHelper helper) {
        ReverseDeconstructorBlockEntity blockEntity = placeWorkbench(helper);
        ItemStack input = new ItemStack(Items.IRON_PICKAXE);
        var efficiency = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY);
        input.enchant(efficiency, 3);
        ItemEnchantments expectedEnchantments = input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        blockEntity.setItem(ReverseDeconstructorBlockEntity.INPUT_SLOT, input);
        blockEntity.setItem(ReverseDeconstructorBlockEntity.BOOK_SLOT, new ItemStack(Items.BOOK));
        for (int slot = ReverseDeconstructorBlockEntity.OUTPUT_START;
                slot <= ReverseDeconstructorBlockEntity.OUTPUT_END;
                slot++) {
            blockEntity.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        helper.runAfterDelay(ModConfig.processTicks() + 10L, () -> {
            helper.assertValueEqual(
                    blockEntity.getBlockReason(),
                    ReverseDeconstructorBlockEntity.BLOCK_REASON_OUTPUT_FULL,
                    "Enchantment extraction did not wait for atomic output capacity");
            helper.assertValueEqual(
                    blockEntity.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).getCount(),
                    1,
                    "A blocked enchantment operation consumed its input");
            helper.assertValueEqual(
                    blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).getCount(),
                    1,
                    "A blocked enchantment operation consumed its plain book");
            helper.assertTrue(
                    findOutput(blockEntity, Items.ENCHANTED_BOOK).isEmpty(),
                    "A blocked operation committed only its enchanted-book output");

            for (int slot = ReverseDeconstructorBlockEntity.OUTPUT_START;
                    slot <= ReverseDeconstructorBlockEntity.OUTPUT_END;
                    slot++) {
                blockEntity.setItem(slot, ItemStack.EMPTY);
            }

            helper.runAfterDelay(5L, () -> {
                helper.assertTrue(blockEntity.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).isEmpty(),
                        "Enchanted input was not consumed after capacity became available");
                helper.assertTrue(blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).isEmpty(),
                        "Plain book was not consumed with the committed operation");

                ItemStack enchantedBook = findOutput(blockEntity, Items.ENCHANTED_BOOK);
                helper.assertFalse(enchantedBook.isEmpty(), "No enchanted book was produced");
                helper.assertValueEqual(
                        enchantedBook.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY),
                        expectedEnchantments,
                        "The enchanted book did not preserve the input enchantments exactly");
                helper.succeed();
            });
        });
    }

    public static void damagedInputUsesDurabilityAdjustedSalvage(GameTestHelper helper) {
        ReverseDeconstructorBlockEntity blockEntity = placeWorkbench(helper);
        ItemStack input = new ItemStack(Items.IRON_PICKAXE);
        input.setDamageValue(input.getMaxDamage() - 1);
        helper.assertTrue(
                DeconstructionResolver.resolve(helper.getLevel(), input.getItem()).isPresent(),
                "The live recipe manager did not resolve the damaged pickaxe recipe");
        blockEntity.setItem(ReverseDeconstructorBlockEntity.INPUT_SLOT, input);

        helper.runAfterDelay(ModConfig.processTicks() + 10L, () -> {
            helper.assertTrue(
                    blockEntity.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).isEmpty(),
                    "The damaged input was not consumed");
            int total = 0;
            for (int slot = ReverseDeconstructorBlockEntity.OUTPUT_START;
                    slot <= ReverseDeconstructorBlockEntity.OUTPUT_END;
                    slot++) {
                ItemStack output = blockEntity.getItem(slot);
                if (!output.isEmpty()) {
                    helper.assertTrue(
                            output.is(Items.IRON_INGOT) || output.is(Items.STICK),
                            "Damaged pickaxe salvage produced an unrelated item");
                    total += output.getCount();
                }
            }
            helper.assertValueEqual(
                    total,
                    1,
                    "Near-broken input did not use the minimum durability-adjusted salvage result");
            helper.succeed();
        });
    }

    public static void rejectsModifiedBooksAsEnchantmentCarriers(GameTestHelper helper) {
        ReverseDeconstructorBlockEntity blockEntity = placeWorkbench(helper);
        ItemStack input = new ItemStack(Items.IRON_PICKAXE);
        var efficiency = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY);
        input.enchant(efficiency, 3);
        ItemStack modifiedBook = new ItemStack(Items.BOOK);
        modifiedBook.set(DataComponents.CUSTOM_NAME, Component.literal("Do not consume"));

        blockEntity.setItem(ReverseDeconstructorBlockEntity.INPUT_SLOT, input);
        blockEntity.setItem(ReverseDeconstructorBlockEntity.BOOK_SLOT, modifiedBook);

        helper.runAfterDelay(ModConfig.processTicks() + 10L, () -> {
            helper.assertValueEqual(
                    blockEntity.getMachineState(),
                    ReverseDeconstructorBlockEntity.MACHINE_BLOCKED,
                    "A modified book did not block enchanted-item processing");
            helper.assertValueEqual(
                    blockEntity.getBlockReason(),
                    ReverseDeconstructorBlockEntity.BLOCK_REASON_MISSING_BOOK,
                    "The screen did not synchronize the plain-book requirement");
            helper.assertFalse(
                    blockEntity.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).isEmpty(),
                    "A modified book allowed the enchanted input to be consumed");
            ItemStack retainedBook = blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT);
            helper.assertTrue(
                    retainedBook.getCount() == 1
                            && ItemStack.isSameItemSameComponents(modifiedBook, retainedBook),
                    "The modified book was changed or consumed");
            helper.assertTrue(
                    findOutput(blockEntity, Items.ENCHANTED_BOOK).isEmpty(),
                    "A modified book was converted into an enchanted book");
            helper.succeed();
        });
    }

    public static void blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow(GameTestHelper helper) {
        ReverseDeconstructorBlockEntity blockEntity = placeWorkbench(helper);
        ItemStack randomizedInput = new ItemStack(Items.RAIL);
        var efficiency = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY);
        randomizedInput.enchant(efficiency, 1);
        var randomizedPlan = DeconstructionResolver.resolve(helper.getLevel(), randomizedInput.getItem());
        helper.assertTrue(randomizedPlan.isPresent(), "The live recipe manager did not resolve the rail recipe");
        helper.assertTrue(
                randomizedPlan.get().totalUnitsPerOutput() > 0.0D
                        && randomizedPlan.get().totalUnitsPerOutput() < 1.0D,
                "The pending-operation fixture no longer exercises a fractional randomized salvage roll");
        blockEntity.setItem(ReverseDeconstructorBlockEntity.INPUT_SLOT, randomizedInput);
        blockEntity.setItem(ReverseDeconstructorBlockEntity.BOOK_SLOT, new ItemStack(Items.BOOK));
        for (int slot = ReverseDeconstructorBlockEntity.OUTPUT_START;
                slot <= ReverseDeconstructorBlockEntity.OUTPUT_END;
                slot++) {
            blockEntity.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        var legacySaved = blockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
        legacySaved.putInt("Progress", 37);
        legacySaved.putInt("MaxProgress", 100);
        legacySaved.remove("MachineState");
        legacySaved.remove("BlockReason");
        legacySaved.remove("PendingOperation");
        BlockEntity legacyLoaded = BlockEntity.loadStatic(
                helper.absolutePos(WORKBENCH_POS),
                blockEntity.getBlockState(),
                legacySaved,
                helper.getLevel().registryAccess());
        helper.assertTrue(
                legacyLoaded instanceof ReverseDeconstructorBlockEntity,
                "Historical workbench NBT did not load through the preserved block-entity ID");
        ReverseDeconstructorBlockEntity legacyWorkbench = (ReverseDeconstructorBlockEntity) legacyLoaded;
        helper.assertValueEqual(
                legacyWorkbench.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).getCount(),
                1,
                "Historical Items NBT was not retained");
        helper.assertValueEqual(
                legacyWorkbench.getMachineState(),
                ReverseDeconstructorBlockEntity.MACHINE_PROCESSING,
                "Historical Progress NBT was not migrated into the synchronized machine state");
        ServerPlayer legacyPlayer = helper.makeMockServerPlayerInLevel();
        ReverseDeconstructorScreenHandler legacyMenu = (ReverseDeconstructorScreenHandler) legacyWorkbench.createMenu(
                2,
                legacyPlayer.getInventory(),
                legacyPlayer);
        helper.assertValueEqual(
                legacyMenu.getScaledProgress(),
                8,
                "Historical Progress/MaxProgress NBT did not retain its scaled value");
        legacyMenu.removed(legacyPlayer);

        helper.runAfterDelay(ModConfig.processTicks() + 10L, () -> {
            helper.assertValueEqual(
                    blockEntity.getMachineState(),
                    ReverseDeconstructorBlockEntity.MACHINE_BLOCKED,
                    "Full outputs did not put the machine into its synchronized blocked state");
            helper.assertValueEqual(
                    blockEntity.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).getCount(),
                    1,
                    "A blocked operation consumed its input");

            var saved = blockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(saved.contains("PendingOperation"), "The exact pending operation was not persisted");
            PendingDeconstructionOperation expectedPending = PendingDeconstructionOperation.load(
                            TagValueInput.create(
                                    ProblemReporter.DISCARDING,
                                    helper.getLevel().registryAccess(),
                                    saved))
                    .orElseThrow(() -> new AssertionError("The randomized pending operation could not be decoded"));

            BlockPos absolutePos = helper.absolutePos(WORKBENCH_POS);
            BlockEntity reloaded = BlockEntity.loadStatic(
                    absolutePos,
                    blockEntity.getBlockState(),
                    saved,
                    helper.getLevel().registryAccess());
            helper.assertTrue(
                    reloaded instanceof ReverseDeconstructorBlockEntity,
                    "The saved workbench did not reload as its preserved block-entity ID");

            helper.getLevel().removeBlockEntity(absolutePos);
            helper.getLevel().setBlockEntity(reloaded);
            ReverseDeconstructorBlockEntity reloadedWorkbench = (ReverseDeconstructorBlockEntity) reloaded;

            helper.runAfterDelay(2L, () -> {
                var stillBlocked = reloadedWorkbench.saveWithFullMetadata(helper.getLevel().registryAccess());
                PendingDeconstructionOperation reloadedPending = PendingDeconstructionOperation.load(
                                TagValueInput.create(
                                        ProblemReporter.DISCARDING,
                                        helper.getLevel().registryAccess(),
                                        stillBlocked))
                        .orElseThrow(() -> new AssertionError("Reload discarded the randomized pending operation"));
                assertExactStackLists(
                        helper,
                        expectedPending.outputs(),
                        reloadedPending.outputs(),
                        "Reload rerolled or changed the exact pending output");
                for (int slot = ReverseDeconstructorBlockEntity.OUTPUT_START;
                        slot <= ReverseDeconstructorBlockEntity.OUTPUT_END;
                        slot++) {
                    reloadedWorkbench.setItem(slot, ItemStack.EMPTY);
                }

                helper.runAfterDelay(5L, () -> {
                    helper.assertTrue(
                            reloadedWorkbench.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).isEmpty(),
                            "Reloaded pending operation did not commit after capacity became available");
                    helper.assertTrue(
                            reloadedWorkbench.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).isEmpty(),
                            "Reloaded pending operation did not consume its exact plain book");
                    List<ItemStack> committed = new java.util.ArrayList<>();
                    for (int slot = ReverseDeconstructorBlockEntity.OUTPUT_START;
                            slot <= ReverseDeconstructorBlockEntity.OUTPUT_END;
                            slot++) {
                        ItemStack output = reloadedWorkbench.getItem(slot);
                        if (!output.isEmpty()) {
                            committed.add(output.copy());
                        }
                    }
                    assertExactStackLists(
                            helper,
                            expectedPending.outputs(),
                            committed,
                            "Committed output differs from the randomized result persisted before reload");
                    helper.succeed();
                });
            });
        });
    }

    public static void exposesStableSidedAutomationRules(GameTestHelper helper) {
        ReverseDeconstructorBlockEntity blockEntity = placeWorkbench(helper);

        helper.assertValueEqual(blockEntity.getSlotsForFace(null).length, 8, "Unsided automation cannot see all slots");
        helper.assertValueEqual(blockEntity.getSlotsForFace(Direction.DOWN).length, 6, "Bottom automation cannot see outputs");
        helper.assertValueEqual(blockEntity.getSlotsForFace(Direction.UP).length, 2, "Input automation slot view changed");
        helper.assertTrue(
                blockEntity.canPlaceItemThroughFace(
                        ReverseDeconstructorBlockEntity.BOOK_SLOT,
                        new ItemStack(Items.BOOK),
                        Direction.UP),
                "Automation cannot insert the required plain book");
        ItemStack modifiedBook = new ItemStack(Items.BOOK);
        modifiedBook.set(DataComponents.CUSTOM_NAME, Component.literal("Modified"));
        helper.assertFalse(
                blockEntity.canPlaceItemThroughFace(
                        ReverseDeconstructorBlockEntity.BOOK_SLOT,
                        modifiedBook,
                        Direction.UP),
                "Automation accepted a component-bearing book as a plain enchantment carrier");
        helper.assertFalse(
                blockEntity.canPlaceItemThroughFace(
                        ReverseDeconstructorBlockEntity.OUTPUT_START,
                        new ItemStack(Items.STONE),
                        Direction.UP),
                "Automation can insert into an output slot");
        helper.succeed();
    }

    public static void shiftClickRoutesBooksInputsAndOutputs(GameTestHelper helper) {
        ReverseDeconstructorBlockEntity blockEntity = placeWorkbench(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ReverseDeconstructorScreenHandler menu = (ReverseDeconstructorScreenHandler) blockEntity.createMenu(
                1,
                player.getInventory(),
                player);

        helper.assertValueEqual(menu.getSlot(0).x, 30, "Input slot horizontal hitbox moved");
        helper.assertValueEqual(menu.getSlot(0).y, 24, "Input slot vertical hitbox moved");
        helper.assertValueEqual(menu.getSlot(1).x, 30, "Book slot horizontal hitbox moved");
        helper.assertValueEqual(menu.getSlot(1).y, 42, "Book slot vertical hitbox moved");
        helper.assertValueEqual(menu.getSlot(2).x, 98, "First output slot horizontal hitbox moved");
        helper.assertValueEqual(menu.getSlot(2).y, 25, "First output slot vertical hitbox moved");
        helper.assertValueEqual(menu.getSlot(7).x, 134, "Last output slot horizontal hitbox moved");
        helper.assertValueEqual(menu.getSlot(7).y, 43, "Last output slot vertical hitbox moved");

        player.getInventory().setItem(9, new ItemStack(Items.BOOK, 5));
        ItemStack movedBook = menu.quickMoveStack(player, 8);
        helper.assertFalse(movedBook.isEmpty(), "Shift-clicking a plain book did nothing");
        helper.assertValueEqual(
                blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).getCount(),
                1,
                "Shift-click bypassed the one-book slot limit");
        helper.assertValueEqual(
                player.getInventory().getItem(9).getCount(),
                4,
                "Shift-clicking a book consumed more than one item");

        ItemStack modifiedBook = new ItemStack(Items.BOOK);
        modifiedBook.set(DataComponents.CUSTOM_NAME, Component.literal("Modified"));
        player.getInventory().setItem(11, modifiedBook.copy());
        helper.assertTrue(
                menu.quickMoveStack(player, 10).isEmpty(),
                "Shift-click routed a component-bearing book as a plain book");
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(player.getInventory().getItem(11), modifiedBook),
                "Shift-click changed or consumed a component-bearing book");

        player.getInventory().setItem(10, new ItemStack(Items.CRAFTING_TABLE));
        ItemStack movedInput = menu.quickMoveStack(player, 9);
        helper.assertFalse(movedInput.isEmpty(), "Shift-clicking a valid input did nothing");
        helper.assertTrue(
                blockEntity.getItem(ReverseDeconstructorBlockEntity.INPUT_SLOT).is(Items.CRAFTING_TABLE),
                "Shift-click routed a non-book away from the input slot");

        blockEntity.setItem(
                ReverseDeconstructorBlockEntity.OUTPUT_START,
                new ItemStack(Items.OAK_PLANKS, 4));
        ItemStack movedOutput = menu.quickMoveStack(player, ReverseDeconstructorBlockEntity.OUTPUT_START);
        helper.assertFalse(movedOutput.isEmpty(), "Shift-clicking an output did nothing");
        helper.assertTrue(
                blockEntity.getItem(ReverseDeconstructorBlockEntity.OUTPUT_START).isEmpty(),
                "Shift-clicking did not clear the machine output slot");

        int plankCount = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.OAK_PLANKS)) {
                plankCount += stack.getCount();
            }
        }
        helper.assertValueEqual(plankCount, 4, "Shift-clicking an output lost items");
        menu.removed(player);
        helper.succeed();
    }

    private static ReverseDeconstructorBlockEntity placeWorkbench(GameTestHelper helper) {
        helper.setBlock(WORKBENCH_POS, ModBlocks.REVERSE_DECONSTRUCTOR.get().defaultBlockState());
        return helper.getBlockEntity(WORKBENCH_POS, ReverseDeconstructorBlockEntity.class);
    }

    private static ItemStack findOutput(ReverseDeconstructorBlockEntity blockEntity, Item item) {
        for (int slot = ReverseDeconstructorBlockEntity.OUTPUT_START;
                slot <= ReverseDeconstructorBlockEntity.OUTPUT_END;
                slot++) {
            ItemStack stack = blockEntity.getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void assertExactStackLists(
            GameTestHelper helper,
            List<ItemStack> expected,
            List<ItemStack> actual,
            String message) {
        helper.assertValueEqual(actual.size(), expected.size(), message + " (stack count)");
        for (int index = 0; index < expected.size(); index++) {
            ItemStack expectedStack = expected.get(index);
            ItemStack actualStack = actual.get(index);
            helper.assertTrue(
                    ItemStack.isSameItemSameComponents(expectedStack, actualStack)
                            && expectedStack.getCount() == actualStack.getCount(),
                    message + " (stack " + index + ")");
        }
    }
}
