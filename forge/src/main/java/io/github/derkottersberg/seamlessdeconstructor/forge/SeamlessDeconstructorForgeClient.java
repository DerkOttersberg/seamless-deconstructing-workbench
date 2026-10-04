package io.github.derkottersberg.seamlessdeconstructor.forge;

import com.seamlessdeconstructor.client.SeamlessDeconstructorClientBootstrap;
import com.seamlessdeconstructor.client.SeamlessDeconstructorConfigScreen;
import io.github.derkottersberg.seamlessdeconstructor.internal.ClientPlatformServices;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

final class SeamlessDeconstructorForgeClient {
    private SeamlessDeconstructorForgeClient() {
    }

    static void initialize(FMLJavaModLoadingContext context) {
        SeamlessDeconstructorClientBootstrap.initialize(new ForgeClientPlatformServices(context));
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new SeamlessDeconstructorConfigScreen(parent)));
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
            context.getModEventBus().addListener((FMLClientSetupEvent event) ->
                    event.enqueueWork(() -> MenuScreens.register(type.get(), constructor::create)));
        }

        @Override
        public <T extends BlockEntity> void registerBlockEntityRenderer(
                Supplier<BlockEntityType<T>> type,
                BlockEntityRendererProvider<T> provider) {
            context.getModEventBus().addListener((EntityRenderersEvent.RegisterRenderers event) ->
                    event.registerBlockEntityRenderer(type.get(), provider));
        }
    }
}
