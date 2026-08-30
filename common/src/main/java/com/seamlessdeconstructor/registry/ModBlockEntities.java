package com.seamlessdeconstructor.registry;

import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices.RegistryHandle;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static RegistryHandle<BlockEntityType<ReverseDeconstructorBlockEntity>> REVERSE_DECONSTRUCTOR_BLOCK_ENTITY;

    private ModBlockEntities() {
    }

    public static void initialize(PlatformServices platform) {
        if (REVERSE_DECONSTRUCTOR_BLOCK_ENTITY != null) {
            throw new IllegalStateException("Workbench block entities were initialized twice");
        }

        REVERSE_DECONSTRUCTOR_BLOCK_ENTITY = platform.registerBlockEntityType(
                "reverse_deconstructor",
                () -> new BlockEntityType<>(
                        ReverseDeconstructorBlockEntity::new,
                        Set.of(ModBlocks.REVERSE_DECONSTRUCTOR.get())));
    }
}
