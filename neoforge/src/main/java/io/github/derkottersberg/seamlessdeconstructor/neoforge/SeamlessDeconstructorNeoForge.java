package io.github.derkottersberg.seamlessdeconstructor.neoforge;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.registry.ModBlocks;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(SeamlessDeconstructorMod.MOD_ID)
public final class SeamlessDeconstructorNeoForge {
    public SeamlessDeconstructorNeoForge(IEventBus modEventBus) {
        registerDevelopmentGameTests(modEventBus);
        NeoForgePlatformServices services = new NeoForgePlatformServices(modEventBus);
        SeamlessDeconstructorMod.initialize(services);
        modEventBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
                event.accept(ModBlocks.REVERSE_DECONSTRUCTOR_ITEM.get());
            }
        });
        modEventBus.addListener((RegisterCapabilitiesEvent event) -> event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY.get(),
                WorkbenchTransferHandler::new));
        if (FMLEnvironment.getDist().isClient()) {
            SeamlessDeconstructorNeoForgeClient.initialize(modEventBus);
        }
    }

    private static void registerDevelopmentGameTests(IEventBus modEventBus) {
        try {
            Class<?> bootstrap = Class.forName(
                    "io.github.derkottersberg.seamlessdeconstructor.neoforge.gametest.SeamlessDeconstructorNeoForgeGameTests");
            bootstrap.getMethod("register", IEventBus.class).invoke(null, modEventBus);
        } catch (ClassNotFoundException ignored) {
            // Expected in production jars and normal development launches.
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not register Workbench NeoForge GameTests", exception);
        }
    }

    private static final class NeoForgePlatformServices implements PlatformServices {
        private final DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, SeamlessDeconstructorMod.MOD_ID);
        private final DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, SeamlessDeconstructorMod.MOD_ID);
        private final DeferredRegister<BlockEntityType<?>> blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SeamlessDeconstructorMod.MOD_ID);
        private final DeferredRegister<MenuType<?>> menus = DeferredRegister.create(Registries.MENU, SeamlessDeconstructorMod.MOD_ID);

        NeoForgePlatformServices(IEventBus modEventBus) {
            blocks.register(modEventBus);
            items.register(modEventBus);
            blockEntities.register(modEventBus);
            menus.register(modEventBus);
        }

        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public <T extends Block> RegistryHandle<T> registerBlock(String path, Supplier<T> factory) {
            DeferredHolder<Block, T> holder = blocks.register(path, factory);
            return holder::get;
        }

        @Override
        public <T extends Item> RegistryHandle<T> registerItem(String path, Supplier<T> factory) {
            DeferredHolder<Item, T> holder = items.register(path, factory);
            return holder::get;
        }

        @Override
        public <T extends BlockEntity> RegistryHandle<BlockEntityType<T>> registerBlockEntityType(
                String path,
                Supplier<BlockEntityType<T>> factory) {
            DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> holder = blockEntities.register(path, factory);
            return holder::get;
        }

        @Override
        public <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenuType(
                String path,
                MenuFactory<T> factory) {
            DeferredHolder<MenuType<?>, MenuType<T>> holder = menus.register(
                    path,
                    () -> new MenuType<>(factory::create, FeatureFlags.DEFAULT_FLAGS));
            return holder::get;
        }
    }
}
