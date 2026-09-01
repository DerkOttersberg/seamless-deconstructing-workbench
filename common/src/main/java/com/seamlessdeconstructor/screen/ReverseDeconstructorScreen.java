package com.seamlessdeconstructor.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class ReverseDeconstructorScreen extends AbstractContainerScreen<ReverseDeconstructorScreenHandler> {
    public ReverseDeconstructorScreen(ReverseDeconstructorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;

        int panelTop = ARGB.color(255, 36, 34, 28);
        int panelBottom = ARGB.color(255, 24, 22, 18);
        context.fillGradient(x, y, x + this.imageWidth, y + this.imageHeight, panelTop, panelBottom);

        int border = ARGB.color(255, 110, 96, 74);
        context.outline(x, y, this.imageWidth, this.imageHeight, border);

        drawSlot(context, x + 29, y + 23, 18, 18);
        drawSlot(context, x + 29, y + 41, 18, 18);
        if (!this.menu.getSlot(1).hasItem()) {
            drawBookHint(context, x + 29, y + 41);
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
        context.fill(arrowLeft, arrowTop, arrowLeft + 24, arrowTop + 10, ARGB.color(255, 56, 50, 40));

        if (menu.isProcessing() || menu.isBlocked()) {
            int progress = menu.getScaledProgress();
            int progressColor = menu.isBlocked()
                    ? ARGB.color(255, 190, 91, 70)
                    : ARGB.color(255, 199, 173, 111);
            context.fill(arrowLeft, arrowTop, arrowLeft + progress, arrowTop + 10, progressColor);
        }

        int statusColor = menu.isBlocked()
                ? ARGB.color(255, 226, 126, 100)
                : ARGB.color(255, 199, 185, 151);
        context.centeredText(this.font, menu.getStatusText(), x + this.imageWidth / 2, y + 65, statusColor);

        super.extractContents(context, mouseX, mouseY, delta);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);
        if (isHovering(58, 37, 24, 10, mouseX, mouseY)) {
            context.setTooltipForNextFrame(this.font, menu.getStatusText(), mouseX, mouseY);
        }
    }

    private static void drawBookHint(GuiGraphicsExtractor context, int x, int y) {
        int outline = ARGB.color(170, 174, 156, 106);
        int page = ARGB.color(120, 220, 210, 182);
        int spine = ARGB.color(170, 126, 102, 72);

        context.verticalLine(x + 7, y + 5, y + 12, spine);
        context.verticalLine(x + 8, y + 5, y + 12, spine);
        context.outline(x + 6, y + 4, 7, 10, outline);
        context.fill(x + 9, y + 6, x + 12, y + 12, page);
    }

    private static void drawSlot(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        int outer = ARGB.color(255, 20, 18, 14);
        int inner = ARGB.color(255, 58, 54, 44);
        context.fill(x, y, x + width, y + height, outer);
        context.outline(x, y, width, height, inner);
    }
}
