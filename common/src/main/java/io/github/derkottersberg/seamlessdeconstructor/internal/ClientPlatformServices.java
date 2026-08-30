package io.github.derkottersberg.seamlessdeconstructor.internal;

import java.util.function.Supplier;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Explicit client registration bridge; implementations schedule loader events. */
public interface ClientPlatformServices {
    <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void registerScreen(
            Supplier<MenuType<M>> type,
            ScreenFactory<M, S> constructor);

    <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
            Supplier<BlockEntityType<T>> type,
            BlockEntityRendererProvider<T, S> provider);

    @FunctionalInterface
    interface ScreenFactory<M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> {
        S create(M menu, Inventory inventory, Component title);
    }
}
