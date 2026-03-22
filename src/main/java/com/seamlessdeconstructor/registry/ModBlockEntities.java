package com.seamlessdeconstructor.registry;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    public static final BlockEntityType<ReverseDeconstructorBlockEntity> REVERSE_DECONSTRUCTOR_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(SeamlessDeconstructorMod.MOD_ID, "reverse_deconstructor"),
                    FabricBlockEntityTypeBuilder.create(
                            ReverseDeconstructorBlockEntity::new,
                            ModBlocks.REVERSE_DECONSTRUCTOR
                    ).build()
            );

    private ModBlockEntities() {
    }

    public static void initialize() {
    }
}
