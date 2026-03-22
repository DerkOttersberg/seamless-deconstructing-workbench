package com.seamlessdeconstructor.client;

import com.seamlessdeconstructor.client.render.ReverseDeconstructorBlockEntityRenderer;
import com.seamlessdeconstructor.registry.ModBlockEntities;
import com.seamlessdeconstructor.screen.ModScreenHandlers;
import com.seamlessdeconstructor.screen.ReverseDeconstructorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class SeamlessDeconstructorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.REVERSE_DECONSTRUCTOR_SCREEN_HANDLER, ReverseDeconstructorScreen::new);
        BlockEntityRendererFactories.register(ModBlockEntities.REVERSE_DECONSTRUCTOR_BLOCK_ENTITY, ReverseDeconstructorBlockEntityRenderer::new);
    }
}
