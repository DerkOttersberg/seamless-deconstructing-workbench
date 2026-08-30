package io.github.derkottersberg.seamlessdeconstructor.forge;

import com.seamlessdeconstructor.client.SeamlessDeconstructorClientBootstrap;
import io.github.derkottersberg.seamlessdeconstructor.internal.ClientPlatformServices;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

final class SeamlessDeconstructorForgeClient {
    private SeamlessDeconstructorForgeClient() {
    }

    static void initialize(FMLJavaModLoadingContext context) {
        SeamlessDeconstructorClientBootstrap.initialize(new ForgeClientPlatformServices(context));
    }

    private static final class ForgeClientPlatformServices implements ClientPlatformServices {
        private final FMLJavaModLoadingContext context;

        ForgeClientPlatformServices(FMLJavaModLoadingContext context) {
            this.context = context;
        }

        @Override
        public <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void registerScreen(
                Supplier<MenuType<M>> type,
                ScreenFactory<M, S> constructor) {
            FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event ->
                    event.enqueueWork(() -> MenuScreens.register(type.get(), constructor::create)));
        }

        @Override
        public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
                Supplier<BlockEntityType<T>> type,
                BlockEntityRendererProvider<T, S> provider) {
            EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                    event.registerBlockEntityRenderer(type.get(), provider));
        }
    }
}
