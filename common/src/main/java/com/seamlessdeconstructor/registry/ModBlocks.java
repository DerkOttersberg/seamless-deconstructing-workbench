package com.seamlessdeconstructor.registry;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices.RegistryHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static RegistryHandle<ReverseDeconstructorBlock> REVERSE_DECONSTRUCTOR;
    public static RegistryHandle<BlockItem> REVERSE_DECONSTRUCTOR_ITEM;

    private ModBlocks() {
    }

    public static void initialize(PlatformServices platform) {
        if (REVERSE_DECONSTRUCTOR != null) {
            throw new IllegalStateException("Workbench blocks were initialized twice");
        }

        String path = "reverse_deconstructor";
        Identifier id = SeamlessDeconstructorMod.id(path);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties blockProperties = BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
                .noOcclusion()
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .isValidSpawn((state, level, pos, entityType) -> false)
                .strength(2.5F)
                .sound(SoundType.WOOD)
                .setId(blockKey);

        REVERSE_DECONSTRUCTOR = platform.registerBlock(
                path,
                () -> new ReverseDeconstructorBlock(blockProperties));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Item.Properties itemProperties = new Item.Properties()
                .useBlockDescriptionPrefix()
                .setId(itemKey);
        REVERSE_DECONSTRUCTOR_ITEM = platform.registerItem(
                path,
                () -> new BlockItem(REVERSE_DECONSTRUCTOR.get(), itemProperties));
    }
}
