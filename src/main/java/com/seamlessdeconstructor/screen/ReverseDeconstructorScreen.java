package com.seamlessdeconstructor.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

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

        int panelTop = argb(255, 36, 34, 28);
        int panelBottom = argb(255, 24, 22, 18);
        context.fillGradient(x, y, x + this.backgroundWidth, y + this.backgroundHeight, panelTop, panelBottom);

        int border = argb(255, 110, 96, 74);
        drawStrokedRectangle(context, x, y, this.backgroundWidth, this.backgroundHeight, border);

        drawSlot(context, x + 29, y + 24, 18, 18);
        drawSlot(context, x + 29, y + 42, 18, 18);
        if (!this.handler.getSlot(1).hasStack()) {
            drawBookHint(context, x + 29, y + 42);
        }

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
        context.fill(arrowLeft, arrowTop, arrowLeft + 24, arrowTop + 10, argb(255, 56, 50, 40));

        if (handler.isProcessing()) {
            int progress = handler.getScaledProgress();
            context.fill(arrowLeft, arrowTop, arrowLeft + progress, arrowTop + 10, argb(255, 199, 173, 111));
        }
    }

    private static void drawBookHint(DrawContext context, int x, int y) {
        int outline = argb(170, 174, 156, 106);
        int page = argb(120, 220, 210, 182);
        int spine = argb(170, 126, 102, 72);

        context.drawVerticalLine(x + 7, y + 5, y + 12, spine);
        context.drawVerticalLine(x + 8, y + 5, y + 12, spine);
        drawStrokedRectangle(context, x + 6, y + 4, 7, 10, outline);
        context.fill(x + 9, y + 6, x + 12, y + 12, page);
    }

    private static void drawSlot(DrawContext context, int x, int y, int width, int height) {
        int outer = argb(255, 20, 18, 14);
        int inner = argb(255, 58, 54, 44);
        context.fill(x, y, x + width, y + height, outer);
        drawStrokedRectangle(context, x, y, width, height, inner);
    }

    private static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    private static void drawStrokedRectangle(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
