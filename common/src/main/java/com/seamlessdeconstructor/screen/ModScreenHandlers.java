package com.seamlessdeconstructor.screen;

import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices;
import io.github.derkottersberg.seamlessdeconstructor.internal.PlatformServices.RegistryHandle;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {
    public static RegistryHandle<MenuType<ReverseDeconstructorScreenHandler>> REVERSE_DECONSTRUCTOR_SCREEN_HANDLER;

    private ModScreenHandlers() {
    }

    public static void initialize(PlatformServices platform) {
        if (REVERSE_DECONSTRUCTOR_SCREEN_HANDLER != null) {
            throw new IllegalStateException("Workbench menus were initialized twice");
        }

        REVERSE_DECONSTRUCTOR_SCREEN_HANDLER = platform.registerMenuType(
                "reverse_deconstructor",
                ReverseDeconstructorScreenHandler::new);
    }
}
