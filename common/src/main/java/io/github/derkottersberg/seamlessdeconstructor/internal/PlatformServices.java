package io.github.derkottersberg.seamlessdeconstructor.internal;

import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Explicit loader bridge used by the common bootstrap. */
public interface PlatformServices {
    String loaderName();

    Path configDirectory();

    <T extends Block> RegistryHandle<T> registerBlock(String path, Supplier<T> factory);

    <T extends Item> RegistryHandle<T> registerItem(String path, Supplier<T> factory);

    <T extends BlockEntity> RegistryHandle<BlockEntityType<T>> registerBlockEntityType(
            String path,
            Supplier<BlockEntityType<T>> factory);

    <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenuType(
            String path,
            MenuFactory<T> factory);

    @FunctionalInterface
    interface RegistryHandle<T> extends Supplier<T> {
    }

    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory inventory);
    }
}
