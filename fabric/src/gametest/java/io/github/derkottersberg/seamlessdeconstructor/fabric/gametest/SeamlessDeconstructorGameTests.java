package io.github.derkottersberg.seamlessdeconstructor.fabric.gametest;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.logic.DeconstructionResolver;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SeamlessDeconstructorGameTests {
    private static final BlockPos WORKBENCH_POS = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 650)
    public void processesCraftingTableIntoIngredients(GameTestHelper helper) {
        var workbench = ModBlocks.REVERSE_DECONSTRUCTOR.get();
        helper.assertValueEqual(
            BuiltInRegistries.BLOCK.getKey(workbench),
            SeamlessDeconstructorMod.id("reverse_deconstructor"),
            "The preserved workbench block ID changed"
        );
        helper.assertValueEqual(
            BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY.get()),
            SeamlessDeconstructorMod.id("reverse_deconstructor"),
            "The preserved workbench block-entity ID changed"
        );

        helper.assertTrue(
            DeconstructionResolver.resolve(helper.getLevel(), Items.CRAFTING_TABLE).isPresent(),
            "The live recipe manager did not resolve the crafting-table recipe"
        );

        helper.setBlock(WORKBENCH_POS, workbench.defaultBlockState());
        ReverseDeconstructorBlockEntity blockEntity = helper.getBlockEntity(
            WORKBENCH_POS,
            ReverseDeconstructorBlockEntity.class
        );
        blockEntity.setItem(0, new ItemStack(Items.CRAFTING_TABLE));

        helper.runAfterDelay(ModConfig.processTicks() + 10L, () -> {
            helper.assertTrue(blockEntity.getItem(0).isEmpty(), "The workbench did not consume one input item");

            int outputCount = 0;
            for (int slot = 2; slot <= 7; slot++) {
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
}
