package com.seamlessdeconstructor.client;

import com.seamlessdeconstructor.screen.ModScreenHandlers;
import com.seamlessdeconstructor.screen.ReverseDeconstructorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class SeamlessDeconstructorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.REVERSE_DECONSTRUCTOR_SCREEN_HANDLER, ReverseDeconstructorScreen::new);
    }
}
