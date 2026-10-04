package io.github.derkottersberg.seamlessdeconstructor.forge;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.registry.ModBlocks;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
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
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(SeamlessDeconstructorMod.MOD_ID)
public final class SeamlessDeconstructorForge {
    public SeamlessDeconstructorForge() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        ForgePlatformServices services = new ForgePlatformServices(context);
        SeamlessDeconstructorMod.initialize(services);
        context.getModEventBus().addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
                event.accept(ModBlocks.REVERSE_DECONSTRUCTOR_ITEM.get());
            }
        });
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, (AttachCapabilitiesEvent<BlockEntity> event) -> {
            if (event.getObject() instanceof ReverseDeconstructorBlockEntity workbench) {
                WorkbenchCapabilityProvider provider = new WorkbenchCapabilityProvider(workbench);
                event.addCapability(SeamlessDeconstructorMod.id("item_handler"), provider);
                event.addListener(provider::invalidate);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.OnDatapackSyncEvent event) ->
                com.seamlessdeconstructor.logic.DeconstructionResolver.invalidateCache());
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
            blocks.register(context.getModEventBus());
            items.register(context.getModEventBus());
            blockEntities.register(context.getModEventBus());
            menus.register(context.getModEventBus());
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
