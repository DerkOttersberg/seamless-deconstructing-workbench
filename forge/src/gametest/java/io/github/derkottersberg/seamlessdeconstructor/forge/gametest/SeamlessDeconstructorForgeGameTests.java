package io.github.derkottersberg.seamlessdeconstructor.forge.gametest;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.seamlessdeconstructor.gametest.WorkbenchGameTestScenario;
import com.seamlessdeconstructor.registry.ModBlocks;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.DeferredRegister;

public final class SeamlessDeconstructorForgeGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, SeamlessDeconstructorMod.MOD_ID);

    static {
        TEST_FUNCTIONS.register(
                "processes_crafting_table",
                () -> WorkbenchGameTestScenario::processesCraftingTableIntoIngredients);
        TEST_FUNCTIONS.register(
                "enchantment_book_atomicity",
                () -> WorkbenchGameTestScenario::transfersEnchantmentsAndConsumesBookAtomically);
        TEST_FUNCTIONS.register(
                "damaged_input",
                () -> WorkbenchGameTestScenario::damagedInputUsesDurabilityAdjustedSalvage);
        TEST_FUNCTIONS.register(
                "modified_book_rejected",
                () -> WorkbenchGameTestScenario::rejectsModifiedBooksAsEnchantmentCarriers);
        TEST_FUNCTIONS.register(
                "pending_save_reload",
                () -> WorkbenchGameTestScenario::blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow);
        TEST_FUNCTIONS.register(
                "sided_automation",
                () -> WorkbenchGameTestScenario::exposesStableSidedAutomationRules);
        TEST_FUNCTIONS.register(
                "automation_capability",
                () -> SeamlessDeconstructorForgeGameTests::exposesRegisteredItemHandlers);
        TEST_FUNCTIONS.register(
                "screen_shift_click",
                () -> WorkbenchGameTestScenario::shiftClickRoutesBooksInputsAndOutputs);
    }

    private SeamlessDeconstructorForgeGameTests() {
    }

    public static void register(BusGroup modBusGroup) {
        TEST_FUNCTIONS.register(modBusGroup);
    }

    private static void exposesRegisteredItemHandlers(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, ModBlocks.REVERSE_DECONSTRUCTOR.get().defaultBlockState());
        ReverseDeconstructorBlockEntity blockEntity =
                helper.getBlockEntity(relativePos, ReverseDeconstructorBlockEntity.class);

        IItemHandler unsided = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        IItemHandler top = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
        IItemHandler bottom = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).resolve().orElse(null);

        helper.assertTrue(unsided != null, "Forge unsided item handler was not exposed");
        helper.assertTrue(top != null, "Forge input-side item handler was not exposed");
        helper.assertTrue(bottom != null, "Forge output-side item handler was not exposed");
        helper.assertValueEqual(unsided.getSlots(), 8, "Forge unsided handler slot count changed");
        helper.assertValueEqual(top.getSlots(), 2, "Forge input-side handler slot count changed");
        helper.assertValueEqual(bottom.getSlots(), 6, "Forge output-side handler slot count changed");

        ItemStack modifiedBook = new ItemStack(Items.BOOK);
        modifiedBook.set(DataComponents.CUSTOM_NAME, Component.literal("Modified"));
        ItemStack rejected = top.insertItem(1, modifiedBook, false);
        helper.assertValueEqual(rejected.getCount(), 1, "Forge item handler accepted a modified book");
        helper.assertTrue(
                top.insertItem(0, new ItemStack(Items.CRAFTING_TABLE), false).isEmpty(),
                "Forge item handler did not accept a non-book input");
        ItemStack remainder = top.insertItem(1, new ItemStack(Items.BOOK, 5), false);
        helper.assertValueEqual(remainder.getCount(), 4, "Forge item handler did not enforce one-book capacity");
        helper.assertValueEqual(
                blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).getCount(),
                1,
                "Forge item handler inserted the wrong number of books");
        blockEntity.setItem(
                ReverseDeconstructorBlockEntity.OUTPUT_START,
                new ItemStack(Items.OAK_PLANKS, 2));
        helper.assertValueEqual(
                bottom.insertItem(0, new ItemStack(Items.STONE), false).getCount(),
                1,
                "Forge output face accepted item insertion");
        helper.assertTrue(top.extractItem(0, 1, false).isEmpty(), "Forge input face allowed input extraction");
        helper.assertTrue(unsided.extractItem(0, 1, false).isEmpty(), "Forge unsided access allowed input extraction");
        helper.assertTrue(unsided.extractItem(1, 1, false).isEmpty(), "Forge unsided access allowed book extraction");
        helper.assertTrue(
                unsided.extractItem(ReverseDeconstructorBlockEntity.OUTPUT_START, 1, true).is(Items.OAK_PLANKS),
                "Forge unsided access could not extract an output");
        helper.assertTrue(bottom.extractItem(0, 1, true).is(Items.OAK_PLANKS),
                "Forge output face could not extract an output");
        helper.succeed();
    }
}
