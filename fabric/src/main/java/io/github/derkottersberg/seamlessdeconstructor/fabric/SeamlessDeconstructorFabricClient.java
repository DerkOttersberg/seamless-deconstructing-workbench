package io.github.derkottersberg.seamlessdeconstructor.fabric;

import com.seamlessdeconstructor.client.SeamlessDeconstructorClientBootstrap;
import io.github.derkottersberg.seamlessdeconstructor.internal.ClientPlatformServices;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SeamlessDeconstructorFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SeamlessDeconstructorClientBootstrap.initialize(new FabricClientPlatformServices());
    }

    private static final class FabricClientPlatformServices implements ClientPlatformServices {
        @Override
        public <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void registerScreen(
                Supplier<MenuType<M>> type,
                ScreenFactory<M, S> constructor) {
            MenuScreens.register(type.get(), constructor::create);
        }

        @Override
        public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
                Supplier<BlockEntityType<T>> type,
                BlockEntityRendererProvider<T, S> provider) {
            BlockEntityRenderers.register(type.get(), provider);
        }
    }
}
