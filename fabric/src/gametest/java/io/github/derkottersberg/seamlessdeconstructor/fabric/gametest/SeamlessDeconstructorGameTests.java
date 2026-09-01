package io.github.derkottersberg.seamlessdeconstructor.fabric.gametest;

import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.seamlessdeconstructor.gametest.WorkbenchGameTestScenario;
import com.seamlessdeconstructor.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SeamlessDeconstructorGameTests {
    public SeamlessDeconstructorGameTests() {
    }

    @GameTest(maxTicks = 650)
    public void processesCraftingTableIntoIngredients(GameTestHelper helper) {
        WorkbenchGameTestScenario.processesCraftingTableIntoIngredients(helper);
    }

    @GameTest(maxTicks = 650)
    public void transfersEnchantmentsAndConsumesBookAtomically(GameTestHelper helper) {
        WorkbenchGameTestScenario.transfersEnchantmentsAndConsumesBookAtomically(helper);
    }

    @GameTest(maxTicks = 200)
    public void rejectsModifiedBooksAsEnchantmentCarriers(GameTestHelper helper) {
        WorkbenchGameTestScenario.rejectsModifiedBooksAsEnchantmentCarriers(helper);
    }

    @GameTest(maxTicks = 700)
    public void blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow(GameTestHelper helper) {
        WorkbenchGameTestScenario.blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow(helper);
    }

    @GameTest(maxTicks = 100)
    public void exposesStableSidedAutomationRules(GameTestHelper helper) {
        WorkbenchGameTestScenario.exposesStableSidedAutomationRules(helper);
    }

    @GameTest(maxTicks = 100)
    public void shiftClickRoutesBooksInputsAndOutputs(GameTestHelper helper) {
        WorkbenchGameTestScenario.shiftClickRoutesBooksInputsAndOutputs(helper);
    }

    @GameTest(maxTicks = 100)
    public void exposesRegisteredFabricTransferAdapters(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, ModBlocks.REVERSE_DECONSTRUCTOR.get().defaultBlockState());
        ReverseDeconstructorBlockEntity blockEntity =
                helper.getBlockEntity(relativePos, ReverseDeconstructorBlockEntity.class);
        BlockPos absolutePos = helper.absolutePos(relativePos);

        Storage<ItemVariant> unsided = ItemStorage.SIDED.find(
                helper.getLevel(), absolutePos, blockEntity.getBlockState(), blockEntity, null);
        Storage<ItemVariant> top = ItemStorage.SIDED.find(
                helper.getLevel(), absolutePos, blockEntity.getBlockState(), blockEntity, Direction.UP);
        Storage<ItemVariant> bottom = ItemStorage.SIDED.find(
                helper.getLevel(), absolutePos, blockEntity.getBlockState(), blockEntity, Direction.DOWN);

        helper.assertTrue(unsided != null, "Fabric unsided item storage was not exposed");
        helper.assertTrue(top != null, "Fabric input-side item storage was not exposed");
        helper.assertTrue(bottom != null, "Fabric output-side item storage was not exposed");

        ItemStack modifiedBook = new ItemStack(Items.BOOK);
        modifiedBook.set(DataComponents.CUSTOM_NAME, Component.literal("Modified"));
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(
                    top.insert(ItemVariant.of(modifiedBook), 1, transaction),
                    0L,
                    "Fabric Transfer API accepted a modified book");
        }
        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(
                    top.insert(ItemVariant.of(Items.BOOK), 5, transaction),
                    1L,
                    "Fabric Transfer API did not enforce the one-book capacity");
            transaction.commit();
        }
        helper.assertValueEqual(
                blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).getCount(),
                1,
                "Fabric Transfer API inserted the wrong number of books");
        helper.succeed();
    }
}
