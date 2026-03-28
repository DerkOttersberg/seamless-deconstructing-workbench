package com.seamlessdeconstructor;

import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.registry.ModBlocks;
import com.seamlessdeconstructor.screen.ModScreenHandlers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SeamlessDeconstructorMod.MOD_ID)
public class SeamlessDeconstructorMod {
    public static final String MOD_ID = "seamlessdeconstructor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SeamlessDeconstructorMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModConfig.load();
        ModBlocks.initialize(modEventBus);
        ModBlockEntities.initialize(modEventBus);
        ModScreenHandlers.initialize(modEventBus);
        modEventBus.addListener(this::onBuildCreativeTabContents);

        LOGGER.info("Salvage Workbench initialized.");
    }

    private void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.REVERSE_DECONSTRUCTOR_ITEM);
        }
    }
}
