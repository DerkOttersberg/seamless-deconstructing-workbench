package com.seamlessdeconstructor.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
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
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int panelTop = argb(255, 36, 34, 28);
        int panelBottom = argb(255, 24, 22, 18);
        context.fillGradient(x, y, x + this.imageWidth, y + this.imageHeight, panelTop, panelBottom);

        int border = argb(255, 110, 96, 74);
        context.renderOutline(x, y, this.imageWidth, this.imageHeight, border);

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
        context.fill(arrowLeft, arrowTop, arrowLeft + 24, arrowTop + 10, argb(255, 56, 50, 40));

        if (menu.isProcessing() || menu.isBlocked()) {
            int progress = menu.getScaledProgress();
            int progressColor = menu.isBlocked()
                    ? argb(255, 190, 91, 70)
                    : argb(255, 199, 173, 111);
            context.fill(arrowLeft, arrowTop, arrowLeft + progress, arrowTop + 10, progressColor);
        }

        int statusColor = menu.isBlocked()
                ? argb(255, 226, 126, 100)
                : argb(255, 199, 185, 151);
        context.drawCenteredString(this.font, menu.getStatusText(), x + this.imageWidth / 2, y + 65, statusColor);

    }

    @Override
    protected void renderTooltip(GuiGraphics context, int mouseX, int mouseY) {
        super.renderTooltip(context, mouseX, mouseY);
        if (!this.menu.getSlot(1).hasItem() && isHovering(29, 41, 18, 18, mouseX, mouseY)) {
            var lines = java.util.List.of(
                    Component.translatable("screen.seamlessdeconstructor.book_slot.title").withStyle(net.minecraft.ChatFormatting.GOLD),
                    Component.translatable("screen.seamlessdeconstructor.book_slot.help"),
                    Component.translatable("screen.seamlessdeconstructor.book_slot.optional").withStyle(net.minecraft.ChatFormatting.GRAY))
                    .stream().flatMap(line -> this.font.split(line, Math.min(230, this.width - 24)).stream()).toList();
            context.renderTooltip(this.font, lines, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, mouseX, mouseY);
        } else if (!this.menu.getSlot(0).hasItem() && isHovering(29, 23, 18, 18, mouseX, mouseY)) {
            context.renderTooltip(this.font,
                    Component.translatable("screen.seamlessdeconstructor.input_slot.help"), mouseX, mouseY);
        }
        if (isHovering(58, 37, 24, 10, mouseX, mouseY)) {
            context.renderTooltip(this.font, menu.getStatusText(), mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        renderTooltip(context, mouseX, mouseY);
    }

    private static int argb(int a, int r, int g, int b) { return (a << 24) | (r << 16) | (g << 8) | b; }

    private static void drawBookHint(GuiGraphics context, int x, int y) {
        int outline = argb(170, 174, 156, 106);
        int page = argb(120, 220, 210, 182);
        int spine = argb(170, 126, 102, 72);

        context.vLine(x + 7, y + 5, y + 12, spine);
        context.vLine(x + 8, y + 5, y + 12, spine);
        context.renderOutline(x + 6, y + 4, 7, 10, outline);
        context.fill(x + 9, y + 6, x + 12, y + 12, page);
    }

    private static void drawSlot(GuiGraphics context, int x, int y, int width, int height) {
        int outer = argb(255, 20, 18, 14);
        int inner = argb(255, 58, 54, 44);
        context.fill(x, y, x + width, y + height, outer);
        context.renderOutline(x, y, width, height, inner);
    }
}
