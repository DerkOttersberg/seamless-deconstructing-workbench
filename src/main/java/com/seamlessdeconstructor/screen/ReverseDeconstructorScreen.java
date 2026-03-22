package com.seamlessdeconstructor.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

public class ReverseDeconstructorScreen extends HandledScreen<ReverseDeconstructorScreenHandler> {
    public ReverseDeconstructorScreen(ReverseDeconstructorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        int panelTop = ColorHelper.getArgb(255, 36, 34, 28);
        int panelBottom = ColorHelper.getArgb(255, 24, 22, 18);
        context.fillGradient(x, y, x + this.backgroundWidth, y + this.backgroundHeight, panelTop, panelBottom);

        int border = ColorHelper.getArgb(255, 110, 96, 74);
        context.drawStrokedRectangle(x, y, this.backgroundWidth, this.backgroundHeight, border);

        drawSlot(context, x + 29, y + 33, 18, 18);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(context, x + 97 + col * 18, y + 24 + row * 18, 18, 18);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(context, x + 7 + col * 18, y + 83 + row * 18, 18, 18);
            }
        }

        for (int col = 0; col < 9; col++) {
            drawSlot(context, x + 7 + col * 18, y + 141, 18, 18);
        }

        int arrowLeft = x + 58;
        int arrowTop = y + 37;
        context.fill(arrowLeft, arrowTop, arrowLeft + 24, arrowTop + 10, ColorHelper.getArgb(255, 56, 50, 40));

        if (handler.isProcessing()) {
            int progress = handler.getScaledProgress();
            context.fill(arrowLeft, arrowTop, arrowLeft + progress, arrowTop + 10, ColorHelper.getArgb(255, 199, 173, 111));
        }
    }

    private static void drawSlot(DrawContext context, int x, int y, int width, int height) {
        int outer = ColorHelper.getArgb(255, 20, 18, 14);
        int inner = ColorHelper.getArgb(255, 58, 54, 44);
        context.fill(x, y, x + width, y + height, outer);
        context.drawStrokedRectangle(x, y, width, height, inner);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
