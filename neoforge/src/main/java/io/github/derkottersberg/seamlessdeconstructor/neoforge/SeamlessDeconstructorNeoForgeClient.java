package io.github.derkottersberg.seamlessdeconstructor.neoforge;

import com.seamlessdeconstructor.client.SeamlessDeconstructorClientBootstrap;
import io.github.derkottersberg.seamlessdeconstructor.internal.ClientPlatformServices;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class SeamlessDeconstructorNeoForgeClient {
    private SeamlessDeconstructorNeoForgeClient() {
    }

    static void initialize(IEventBus modEventBus) {
        SeamlessDeconstructorClientBootstrap.initialize(new NeoForgeClientPlatformServices(modEventBus));
    }

    private static final class NeoForgeClientPlatformServices implements ClientPlatformServices {
        private final IEventBus modEventBus;

        NeoForgeClientPlatformServices(IEventBus modEventBus) {
            this.modEventBus = modEventBus;
        }

        @Override
        public <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void registerScreen(
                Supplier<MenuType<M>> type,
                ScreenFactory<M, S> constructor) {
            modEventBus.addListener((RegisterMenuScreensEvent event) ->
                    event.register(type.get(), constructor::create));
        }

        @Override
        public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
                Supplier<BlockEntityType<T>> type,
                BlockEntityRendererProvider<T, S> provider) {
            modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                    event.registerBlockEntityRenderer(type.get(), provider));
        }
    }
}
