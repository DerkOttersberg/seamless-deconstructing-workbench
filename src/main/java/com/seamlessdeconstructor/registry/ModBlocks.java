package com.seamlessdeconstructor.registry;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public final class ModBlocks {
    public static final Block REVERSE_DECONSTRUCTOR = registerBlock(
            "reverse_deconstructor",
        id -> new ReverseDeconstructorBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE)
            .strength(2.5f)
            .sounds(BlockSoundGroup.WOOD)
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)))
    );

    private ModBlocks() {
    }

    private static Block registerBlock(String name, Function<Identifier, Block> factory) {
        Identifier id = Identifier.of(SeamlessDeconstructorMod.MOD_ID, name);
        Block block = factory.apply(id);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(
                Registries.ITEM,
                id,
                new BlockItem(block, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)))
        );
        return block;
    }

    public static void initialize() {
    }
}
