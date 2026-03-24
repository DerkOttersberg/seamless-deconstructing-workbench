package com.seamlessdeconstructor.screen;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    public static final ScreenHandlerType<ReverseDeconstructorScreenHandler> REVERSE_DECONSTRUCTOR_SCREEN_HANDLER =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    new Identifier(SeamlessDeconstructorMod.MOD_ID, "reverse_deconstructor"),
                    new ScreenHandlerType<ReverseDeconstructorScreenHandler>(
                            ReverseDeconstructorScreenHandler::new,
                            FeatureFlags.VANILLA_FEATURES
                    )
            );

    private ModScreenHandlers() {
    }

    public static void initialize() {
    }
}
