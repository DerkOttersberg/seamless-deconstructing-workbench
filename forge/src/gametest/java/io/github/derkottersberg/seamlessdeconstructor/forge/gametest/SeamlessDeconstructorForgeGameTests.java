package io.github.derkottersberg.seamlessdeconstructor.forge.gametest;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.seamlessdeconstructor.gametest.WorkbenchGameTestScenario;
import com.seamlessdeconstructor.registry.ModBlocks;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.RegisterEvent;

@GameTestHolder("seamlessdeconstructor")
@PrefixGameTestTemplate(false)
public final class SeamlessDeconstructorForgeGameTests {
    @GameTest(template = "empty", timeoutTicks = 700)
    public static void processesCraftingTableIntoIngredients(GameTestHelper helper) {
        WorkbenchGameTestScenario.processesCraftingTableIntoIngredients(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void transfersEnchantmentsAndConsumesBookAtomically(GameTestHelper helper) {
        WorkbenchGameTestScenario.transfersEnchantmentsAndConsumesBookAtomically(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void damagedInputUsesDurabilityAdjustedSalvage(GameTestHelper helper) {
        WorkbenchGameTestScenario.damagedInputUsesDurabilityAdjustedSalvage(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void rejectsModifiedBooksAsEnchantmentCarriers(GameTestHelper helper) {
        WorkbenchGameTestScenario.rejectsModifiedBooksAsEnchantmentCarriers(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow(GameTestHelper helper) {
        WorkbenchGameTestScenario.blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void exposesStableSidedAutomationRules(GameTestHelper helper) {
        WorkbenchGameTestScenario.exposesStableSidedAutomationRules(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void shiftClickRoutesBooksInputsAndOutputs(GameTestHelper helper) {
        WorkbenchGameTestScenario.shiftClickRoutesBooksInputsAndOutputs(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void exposesRegisteredItemHandlers(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, ModBlocks.REVERSE_DECONSTRUCTOR.get().defaultBlockState());
        ReverseDeconstructorBlockEntity blockEntity =
                (ReverseDeconstructorBlockEntity) helper.getBlockEntity(relativePos);

        IItemHandler unsided = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        IItemHandler top = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
        IItemHandler bottom = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).resolve().orElse(null);

        helper.assertTrue(unsided != null, "Forge unsided item handler was not exposed");
        helper.assertTrue(top != null, "Forge input-side item handler was not exposed");
        helper.assertTrue(bottom != null, "Forge output-side item handler was not exposed");
        helper.assertTrue(java.util.Objects.equals(unsided.getSlots(), 8), "Forge unsided handler slot count changed");
        helper.assertTrue(java.util.Objects.equals(top.getSlots(), 2), "Forge input-side handler slot count changed");
        helper.assertTrue(java.util.Objects.equals(bottom.getSlots(), 6), "Forge output-side handler slot count changed");

        ItemStack modifiedBook = new ItemStack(Items.BOOK);
        modifiedBook.setHoverName(Component.literal("Modified"));
        ItemStack rejected = top.insertItem(1, modifiedBook, false);
        helper.assertTrue(java.util.Objects.equals(rejected.getCount(), 1), "Forge item handler accepted a modified book");
        helper.assertTrue(
                top.insertItem(0, new ItemStack(Items.CRAFTING_TABLE), false).isEmpty(),
                "Forge item handler did not accept a non-book input");
        ItemStack remainder = top.insertItem(1, new ItemStack(Items.BOOK, 5), false);
        helper.assertTrue(java.util.Objects.equals(remainder.getCount(), 4), "Forge item handler did not enforce one-book capacity");
        helper.assertTrue(java.util.Objects.equals(blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).getCount(), 1), "Forge item handler inserted the wrong number of books");
        blockEntity.setItem(
                ReverseDeconstructorBlockEntity.OUTPUT_START,
                new ItemStack(Items.OAK_PLANKS, 2));
        helper.assertTrue(java.util.Objects.equals(bottom.insertItem(0, new ItemStack(Items.STONE), false).getCount(), 1), "Forge output face accepted item insertion");
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
