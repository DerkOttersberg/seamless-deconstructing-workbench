package io.github.derkottersberg.seamlessdeconstructor.neoforge.gametest;

import com.seamlessdeconstructor.gametest.WorkbenchGameTestScenario;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.seamlessdeconstructor.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("seamlessdeconstructor")
@PrefixGameTestTemplate(false)
public final class SeamlessDeconstructorNeoForgeGameTests {
    public static void register(IEventBus bus) {
        bus.addListener((RegisterGameTestsEvent event) -> event.register(SeamlessDeconstructorNeoForgeGameTests.class));
    }
    @GameTest(template = "empty",timeoutTicks=700)
    public static void processesCraftingTableIntoIngredients(GameTestHelper helper) {
        WorkbenchGameTestScenario.processesCraftingTableIntoIngredients(helper);
    }
    @GameTest(template = "empty",timeoutTicks=700)
    public static void transfersEnchantmentsAndConsumesBookAtomically(GameTestHelper helper) {
        WorkbenchGameTestScenario.transfersEnchantmentsAndConsumesBookAtomically(helper);
    }
    @GameTest(template = "empty",timeoutTicks=700)
    public static void damagedInputUsesDurabilityAdjustedSalvage(GameTestHelper helper) {
        WorkbenchGameTestScenario.damagedInputUsesDurabilityAdjustedSalvage(helper);
    }
    @GameTest(template = "empty",timeoutTicks=700)
    public static void rejectsModifiedBooksAsEnchantmentCarriers(GameTestHelper helper) {
        WorkbenchGameTestScenario.rejectsModifiedBooksAsEnchantmentCarriers(helper);
    }
    @GameTest(template = "empty",timeoutTicks=700)
    public static void blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow(GameTestHelper helper) {
        WorkbenchGameTestScenario.blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow(helper);
    }
    @GameTest(template = "empty",timeoutTicks=700)
    public static void exposesStableSidedAutomationRules(GameTestHelper helper) {
        WorkbenchGameTestScenario.exposesStableSidedAutomationRules(helper);
    }
    @GameTest(template = "empty",timeoutTicks=700)
    public static void shiftClickRoutesBooksInputsAndOutputs(GameTestHelper helper) {
        WorkbenchGameTestScenario.shiftClickRoutesBooksInputsAndOutputs(helper);
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void exposesRegisteredItemHandlers(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, ModBlocks.REVERSE_DECONSTRUCTOR.get().defaultBlockState());
        ReverseDeconstructorBlockEntity blockEntity =
                (ReverseDeconstructorBlockEntity) helper.getBlockEntity(relativePos);

        IItemHandler unsided = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(relativePos), null);
        IItemHandler top = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(relativePos), Direction.UP);
        IItemHandler bottom = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(relativePos), Direction.DOWN);

        helper.assertTrue(unsided != null, "NeoForge unsided item handler was not exposed");
        helper.assertTrue(top != null, "NeoForge input-side item handler was not exposed");
        helper.assertTrue(bottom != null, "NeoForge output-side item handler was not exposed");
        helper.assertTrue(java.util.Objects.equals(unsided.getSlots(), 8), "NeoForge unsided handler slot count changed");
        helper.assertTrue(java.util.Objects.equals(top.getSlots(), 2), "NeoForge input-side handler slot count changed");
        helper.assertTrue(java.util.Objects.equals(bottom.getSlots(), 6), "NeoForge output-side handler slot count changed");

        ItemStack modifiedBook = new ItemStack(Items.BOOK);
        modifiedBook.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Modified"));
        ItemStack rejected = top.insertItem(1, modifiedBook, false);
        helper.assertTrue(java.util.Objects.equals(rejected.getCount(), 1), "NeoForge item handler accepted a modified book");
        helper.assertTrue(
                top.insertItem(0, new ItemStack(Items.CRAFTING_TABLE), false).isEmpty(),
                "NeoForge item handler did not accept a non-book input");
        ItemStack remainder = top.insertItem(1, new ItemStack(Items.BOOK, 5), false);
        helper.assertTrue(java.util.Objects.equals(remainder.getCount(), 4), "NeoForge item handler did not enforce one-book capacity");
        helper.assertTrue(java.util.Objects.equals(blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).getCount(), 1), "NeoForge item handler inserted the wrong number of books");
        blockEntity.setItem(
                ReverseDeconstructorBlockEntity.OUTPUT_START,
                new ItemStack(Items.OAK_PLANKS, 2));
        helper.assertTrue(java.util.Objects.equals(bottom.insertItem(0, new ItemStack(Items.STONE), false).getCount(), 1), "NeoForge output face accepted item insertion");
        helper.assertTrue(top.extractItem(0, 1, false).isEmpty(), "NeoForge input face allowed input extraction");
        helper.assertTrue(unsided.extractItem(0, 1, false).isEmpty(), "NeoForge unsided access allowed input extraction");
        helper.assertTrue(unsided.extractItem(1, 1, false).isEmpty(), "NeoForge unsided access allowed book extraction");
        helper.assertTrue(
                unsided.extractItem(ReverseDeconstructorBlockEntity.OUTPUT_START, 1, true).is(Items.OAK_PLANKS),
                "NeoForge unsided access could not extract an output");
        helper.assertTrue(bottom.extractItem(0, 1, true).is(Items.OAK_PLANKS),
                "NeoForge output face could not extract an output");
        helper.succeed();
    }
}
