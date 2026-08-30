package com.seamlessdeconstructor;

import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.registry.ModBlocks;
import com.seamlessdeconstructor.screen.ModScreenHandlers;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SeamlessDeconstructorMod {
    public static final String MOD_ID = "seamlessdeconstructor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static PlatformServices platform;

    private SeamlessDeconstructorMod() {
    }

    public static synchronized void initialize(PlatformServices services) {
        Objects.requireNonNull(services, "services");
        if (platform != null) {
            throw new IllegalStateException("Seamless Deconstructor was initialized twice");
        }

        platform = services;
        ModConfig.load(services.configDirectory());
        ModBlocks.initialize(services);
        ModBlockEntities.initialize(services);
        ModScreenHandlers.initialize(services);
        LOGGER.info("Seamless Deconstructing Workbench initialized on {}", services.loaderName());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
