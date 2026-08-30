package com.seamlessdeconstructor.client;

import com.seamlessdeconstructor.client.render.ReverseDeconstructorBlockEntityRenderer;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.screen.ModScreenHandlers;
import com.seamlessdeconstructor.screen.ReverseDeconstructorScreen;
import io.github.derkottersberg.seamlessdeconstructor.internal.ClientPlatformServices;
import java.util.Objects;

public final class SeamlessDeconstructorClientBootstrap {
    private static boolean initialized;

    private SeamlessDeconstructorClientBootstrap() {
    }

    public static synchronized void initialize(ClientPlatformServices services) {
        Objects.requireNonNull(services, "services");
        if (initialized) {
            throw new IllegalStateException("Seamless Deconstructor client was initialized twice");
        }
        initialized = true;

        services.registerScreen(
                ModScreenHandlers.REVERSE_DECONSTRUCTOR_SCREEN_HANDLER,
                ReverseDeconstructorScreen::new);
        services.registerBlockEntityRenderer(
                ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY,
                ReverseDeconstructorBlockEntityRenderer::new);
    }
}
