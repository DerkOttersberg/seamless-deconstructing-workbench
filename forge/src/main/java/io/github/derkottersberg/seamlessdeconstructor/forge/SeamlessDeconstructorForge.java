package io.github.derkottersberg.seamlessdeconstructor.forge;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.registry.ModBlocks;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices;
import java.nio.file.Path;
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
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(SeamlessDeconstructorMod.MOD_ID)
public final class SeamlessDeconstructorForge {
    public SeamlessDeconstructorForge(FMLJavaModLoadingContext context) {
        ForgePlatformServices services = new ForgePlatformServices(context);
        SeamlessDeconstructorMod.initialize(services);
        BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
                event.accept(ModBlocks.REVERSE_DECONSTRUCTOR_ITEM.get());
            }
        });
        if (FMLEnvironment.dist.isClient()) {
            SeamlessDeconstructorForgeClient.initialize(context);
        }
    }

    private static final class ForgePlatformServices implements PlatformServices {
        private final DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, SeamlessDeconstructorMod.MOD_ID);
        private final DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, SeamlessDeconstructorMod.MOD_ID);
        private final DeferredRegister<BlockEntityType<?>> blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SeamlessDeconstructorMod.MOD_ID);
        private final DeferredRegister<MenuType<?>> menus = DeferredRegister.create(Registries.MENU, SeamlessDeconstructorMod.MOD_ID);

        ForgePlatformServices(FMLJavaModLoadingContext context) {
            blocks.register(context.getModBusGroup());
            items.register(context.getModBusGroup());
            blockEntities.register(context.getModBusGroup());
            menus.register(context.getModBusGroup());
        }

        @Override
        public String loaderName() {
            return "Forge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public <T extends Block> RegistryHandle<T> registerBlock(String path, Supplier<T> factory) {
            RegistryObject<T> holder = blocks.register(path, factory);
            return holder::get;
        }

        @Override
        public <T extends Item> RegistryHandle<T> registerItem(String path, Supplier<T> factory) {
            RegistryObject<T> holder = items.register(path, factory);
            return holder::get;
        }

        @Override
        public <T extends BlockEntity> RegistryHandle<BlockEntityType<T>> registerBlockEntityType(
                String path,
                Supplier<BlockEntityType<T>> factory) {
            RegistryObject<BlockEntityType<T>> holder = blockEntities.register(path, factory);
            return holder::get;
        }

        @Override
        public <T extends AbstractContainerMenu> RegistryHandle<MenuType<T>> registerMenuType(
                String path,
                MenuFactory<T> factory) {
            RegistryObject<MenuType<T>> holder = menus.register(
                    path,
                    () -> new MenuType<>(factory::create, FeatureFlags.DEFAULT_FLAGS));
            return holder::get;
        }
    }
}
