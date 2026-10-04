package com.seamlessdeconstructor.client;

import com.seamlessdeconstructor.config.ModConfig;
import com.seamlessdeconstructor.config.ModConfig.Settings;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SeamlessDeconstructorConfigScreen extends Screen {
    private final Screen parent;
    private EditBox processSecondsField;
    private EditBox minLossField;
    private EditBox maxLossField;
    private Component errorText = Component.empty();

    public SeamlessDeconstructorConfigScreen(Screen parent) {
        super(Component.translatable("config.seamlessdeconstructor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Settings settings = ModConfig.snapshot();
        int centerX = this.width / 2;
        int fieldX = centerX + 20;
        int y = Math.max(62, this.height / 2 - 64);

        this.processSecondsField = addField(fieldX, y, formatSeconds(settings.processTicks()));
        this.minLossField = addField(fieldX, y + 34, Integer.toString(settings.minLossPercent()));
        this.maxLossField = addField(fieldX, y + 68, Integer.toString(settings.maxLossPercent()));

        int buttonY = y + 112;
        this.addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), button -> resetDefaults())
            .bounds(centerX - 155, buttonY, 100, 20)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
            .bounds(centerX - 50, buttonY, 100, 20)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose())
            .bounds(centerX + 55, buttonY, 100, 20)
            .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xCC101014);
        int centerX = this.width / 2;
        int y = Math.max(62, this.height / 2 - 64);
        graphics.centeredText(this.font, this.title, centerX, 20, 0xFFFFFF);
        graphics.centeredText(
            this.font,
            Component.literal("Settings apply to local and integrated-server play"),
            centerX,
            38,
            0xAFAFAF
        );
        graphics.text(this.font, Component.translatable("config.seamlessdeconstructor.process_ticks"), centerX - 155, y + 6, 0xFFFFFF, true);
        graphics.text(this.font, Component.literal("seconds (1-30)"), centerX + 126, y + 6, 0xAFAFAF, true);
        graphics.text(this.font, Component.translatable("config.seamlessdeconstructor.min_loss"), centerX - 155, y + 40, 0xFFFFFF, true);
        graphics.text(this.font, Component.literal("percent (0-90)"), centerX + 126, y + 40, 0xAFAFAF, true);
        graphics.text(this.font, Component.translatable("config.seamlessdeconstructor.max_loss"), centerX - 155, y + 74, 0xFFFFFF, true);
        graphics.text(this.font, Component.literal("percent (0-90)"), centerX + 126, y + 74, 0xAFAFAF, true);
        if (!this.errorText.getString().isEmpty()) {
            graphics.centeredText(this.font, this.errorText, centerX, y + 96, 0xFF6B6B);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private EditBox addField(int x, int y, String value) {
        EditBox field = new EditBox(this.font, x, y, 96, 20, Component.empty());
        field.setValue(value);
        this.addRenderableWidget(field);
        return field;
    }

    private void resetDefaults() {
        Settings defaults = Settings.defaults();
        this.processSecondsField.setValue(formatSeconds(defaults.processTicks()));
        this.minLossField.setValue(Integer.toString(defaults.minLossPercent()));
        this.maxLossField.setValue(Integer.toString(defaults.maxLossPercent()));
        this.errorText = Component.empty();
    }

    private void saveAndClose() {
        try {
            int processTicks = parseSecondsToTicks(this.processSecondsField.getValue());
            int minLoss = parsePercentage(this.minLossField.getValue(), "Minimum loss");
            int maxLoss = parsePercentage(this.maxLossField.getValue(), "Maximum loss");
            if (minLoss > maxLoss) {
                throw new IllegalArgumentException("Minimum loss cannot exceed maximum loss.");
            }
            ModConfig.update(new Settings(processTicks, minLoss, maxLoss));
            onClose();
        } catch (IllegalArgumentException exception) {
            this.errorText = Component.literal(exception.getMessage());
        }
    }

    private static int parseSecondsToTicks(String raw) {
        try {
            double seconds = Double.parseDouble(raw.trim());
            if (!Double.isFinite(seconds) || seconds < 1.0D || seconds > 30.0D) {
                throw new NumberFormatException();
            }
            return (int) Math.round(seconds * 20.0D);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Process time must be between 1 and 30 seconds.");
        }
    }

    private static int parsePercentage(String raw, String label) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0 || value > 90) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number from 0 to 90.");
        }
    }

    private static String formatSeconds(int ticks) {
        double seconds = ticks / 20.0D;
        return seconds == Math.rint(seconds)
            ? Integer.toString((int) seconds)
            : String.format(Locale.ROOT, "%.2f", seconds).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
