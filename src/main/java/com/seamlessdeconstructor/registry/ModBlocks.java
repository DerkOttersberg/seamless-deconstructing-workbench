package com.seamlessdeconstructor.registry;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public final class ModBlocks {
    public static final Block REVERSE_DECONSTRUCTOR = registerBlock(
            "reverse_deconstructor",
        id -> new ReverseDeconstructorBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE)
            .nonOpaque()
            .solidBlock((state, world, pos) -> false)
            .suffocates((state, world, pos) -> false)
            .blockVision((state, world, pos) -> false)
            .allowsSpawning((state, world, pos, type) -> false)
            .strength(2.5f)
                .sounds(BlockSoundGroup.WOOD))
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
            new BlockItem(block, new Item.Settings())
        );
        return block;
    }

    public static void initialize() {
    }
}
