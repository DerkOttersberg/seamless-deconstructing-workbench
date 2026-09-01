package io.github.derkottersberg.seamlessdeconstructor.fabric;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.registry.ModBlocks;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SeamlessDeconstructorFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SeamlessDeconstructorMod.initialize(new FabricPlatformServices());
        ItemStorage.SIDED.registerForBlockEntity(
                (blockEntity, side) -> ContainerStorage.of(blockEntity, side),
                ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY.get());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(ModBlocks.REVERSE_DECONSTRUCTOR_ITEM.get()));
    }

    private static final class FabricPlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public Path configDirectory() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public <T extends Block> RegistryHandle<T> registerBlock(String path, Supplier<T> factory) {
            T block = Registry.register(BuiltInRegistries.BLOCK, SeamlessDeconstructorMod.id(path), factory.get());
            return () -> block;
        }

        @Override
        public <T extends Item> RegistryHandle<T> registerItem(String path, Supplier<T> factory) {
            T item = Registry.register(BuiltInRegistries.ITEM, SeamlessDeconstructorMod.id(path), factory.get());
            return () -> item;
        }

        @Override
        public <T extends BlockEntity> RegistryHandle<BlockEntityType<T>> registerBlockEntityType(
                String path,
                Supplier<BlockEntityType<T>> factory) {
            BlockEntityType<T> type = Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    SeamlessDeconstructorMod.id(path),
                    factory.get());
            return () -> type;
        }

        @Override
        public <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenuType(
                String path,
                MenuFactory<T> factory) {
            MenuType<T> type = new MenuType<>(factory::create, FeatureFlags.DEFAULT_FLAGS);
            MenuType<T> registered = Registry.register(BuiltInRegistries.MENU, SeamlessDeconstructorMod.id(path), type);
            return () -> registered;
        }
    }
}
