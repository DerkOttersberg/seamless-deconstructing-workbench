package io.github.derkottersberg.seamlessdeconstructor.fabric;

import com.seamlessdeconstructor.client.SeamlessDeconstructorConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class SeamlessDeconstructorModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SeamlessDeconstructorConfigScreen::new;
    }
}
