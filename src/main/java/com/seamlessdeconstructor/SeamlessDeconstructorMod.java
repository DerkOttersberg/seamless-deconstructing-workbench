package com.seamlessdeconstructor;

import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.registry.ModBlocks;
import com.seamlessdeconstructor.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeamlessDeconstructorMod implements ModInitializer {
    public static final String MOD_ID = "seamlessdeconstructor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.load();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModScreenHandlers.initialize();

        LOGGER.info("Seamless Deconstructor initialized.");
    }
}
