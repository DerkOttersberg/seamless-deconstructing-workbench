package com.seamlessdeconstructor.screen;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModScreenHandlers {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, SeamlessDeconstructorMod.MOD_ID);

    public static final RegistryObject<MenuType<ReverseDeconstructorScreenHandler>> REVERSE_DECONSTRUCTOR_SCREEN_HANDLER =
        MENUS.register(
            "reverse_deconstructor",
            () -> IForgeMenuType.create((syncId, playerInventory, data) -> new ReverseDeconstructorScreenHandler(syncId, playerInventory))
        );

    private ModScreenHandlers() {
    }

    public static void initialize(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
