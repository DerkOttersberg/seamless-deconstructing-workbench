package com.seamlessdeconstructor.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Inventory;

public class ReverseDeconstructorScreen extends AbstractContainerScreen<ReverseDeconstructorScreenHandler> {
    public ReverseDeconstructorScreen(ReverseDeconstructorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int panelTop = FastColor.ARGB32.color(255, 36, 34, 28);
        int panelBottom = FastColor.ARGB32.color(255, 24, 22, 18);
        graphics.fillGradient(x, y, x + this.imageWidth, y + this.imageHeight, panelTop, panelBottom);

        int border = FastColor.ARGB32.color(255, 110, 96, 74);
        graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, border);

        drawSlot(graphics, x + 29, y + 24, 18, 18);
        drawSlot(graphics, x + 29, y + 42, 18, 18);
        if (!this.menu.getSlot(1).hasItem()) {
            drawBookHint(graphics, x + 29, y + 42);
        }

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(graphics, x + 97 + col * 18, y + 24 + row * 18, 18, 18);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 7 + col * 18, y + 83 + row * 18, 18, 18);
            }
        }

        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 7 + col * 18, y + 141, 18, 18);
        }

        int arrowLeft = x + 58;
        int arrowTop = y + 37;
        graphics.fill(arrowLeft, arrowTop, arrowLeft + 24, arrowTop + 10, FastColor.ARGB32.color(255, 56, 50, 40));

        if (menu.isProcessing()) {
            int progress = menu.getScaledProgress();
            graphics.fill(arrowLeft, arrowTop, arrowLeft + progress, arrowTop + 10, FastColor.ARGB32.color(255, 199, 173, 111));
        }
    }

    private static void drawBookHint(GuiGraphics graphics, int x, int y) {
        int outline = FastColor.ARGB32.color(170, 174, 156, 106);
        int page = FastColor.ARGB32.color(120, 220, 210, 182);
        int spine = FastColor.ARGB32.color(170, 126, 102, 72);

        graphics.vLine(x + 7, y + 5, y + 12, spine);
        graphics.vLine(x + 8, y + 5, y + 12, spine);
        graphics.renderOutline(x + 6, y + 4, 7, 10, outline);
        graphics.fill(x + 9, y + 6, x + 12, y + 12, page);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int width, int height) {
        int outer = FastColor.ARGB32.color(255, 20, 18, 14);
        int inner = FastColor.ARGB32.color(255, 58, 54, 44);
        graphics.fill(x, y, x + width, y + height, outer);
        graphics.renderOutline(x, y, width, height, inner);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
