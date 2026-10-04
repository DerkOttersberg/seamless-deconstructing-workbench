package com.seamlessdeconstructor.client;

import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.config.ModConfig.Settings;
import io.github.derkottersberg.seamlessdeconstructor.internal.client.SettingsScreen;
import java.util.Locale;
import net.minecraft.client.gui.screens.Screen;

public final class SeamlessDeconstructorConfigScreen extends SettingsScreen {
    private String seconds, minimum, maximum;

    public SeamlessDeconstructorConfigScreen(Screen parent) {
        super(parent, "Deconstructing Workbench Settings", "Singleplayer gameplay. Multiplayer uses the server's configuration.");
        setDraft(ModConfig.snapshot());
    }

    private void setDraft(Settings settings) {
        this.seconds = String.format(Locale.ROOT, "%.2f", settings.processTicks() / 20.0)
            .replaceAll("0+$", "").replaceAll("\\.$", "");
        this.minimum = Integer.toString(settings.minLossPercent());
        this.maximum = Integer.toString(settings.maxLossPercent());
    }

    @Override
    protected void buildSettings() {
        textSetting("Salvaging", "Process time (seconds)", "1-30 seconds to salvage one item. Lower values make the workbench faster.",
            this.seconds, v -> this.seconds = v);
        textSetting("Ingredient loss", "Minimum loss (%)", "0-90%. Least material lost when salvaging. Set both loss values to 0 for no random loss.",
            this.minimum, v -> this.minimum = v);
        textSetting("Ingredient loss", "Maximum loss (%)", "0-90%. Most material lost. Must be at least the minimum. Damaged items can still yield less.",
            this.maximum, v -> this.maximum = v);
    }

    @Override
    protected void resetDraft() { setDraft(Settings.defaults()); }

    @Override
    protected void saveDraft() {
        int ticks = (int) Math.round(decimal(this.seconds, 1, 30, "Process time (seconds)") * 20);
        int min = integer(this.minimum, 0, 90, "Minimum loss (%)");
        int max = integer(this.maximum, 0, 90, "Maximum loss (%)");
        if (min > max) throw new IllegalArgumentException("Minimum loss must be less than or equal to maximum loss.");
        ModConfig.update(new Settings(ticks, min, max));
    }

    @Override
    protected String summary() { return "Example: 20% loss returns about 80% of the recipe materials."; }
}
