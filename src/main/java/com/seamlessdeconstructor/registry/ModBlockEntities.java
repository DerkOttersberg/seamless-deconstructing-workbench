package com.seamlessdeconstructor.registry;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SeamlessDeconstructorMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ReverseDeconstructorBlockEntity>> REVERSE_DECONSTRUCTOR_BLOCK_ENTITY =
        BLOCK_ENTITIES.register(
            "reverse_deconstructor",
            () -> BlockEntityType.Builder.of(ReverseDeconstructorBlockEntity::new, ModBlocks.REVERSE_DECONSTRUCTOR.get()).build(null)
        );

    private ModBlockEntities() {
    }

    public static void initialize(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
