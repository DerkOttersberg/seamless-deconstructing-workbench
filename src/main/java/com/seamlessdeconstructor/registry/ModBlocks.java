package com.seamlessdeconstructor.registry;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SeamlessDeconstructorMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SeamlessDeconstructorMod.MOD_ID);

    public static final RegistryObject<Block> REVERSE_DECONSTRUCTOR = BLOCKS.register(
        "reverse_deconstructor",
        () -> new ReverseDeconstructorBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)
            .noOcclusion()
            .strength(2.5f)
            .sound(SoundType.WOOD))
    );

    public static final RegistryObject<Item> REVERSE_DECONSTRUCTOR_ITEM = ITEMS.register(
        "reverse_deconstructor",
        () -> new BlockItem(REVERSE_DECONSTRUCTOR.get(), new Item.Properties())
    );

    private ModBlocks() {
    }

    public static void initialize(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
