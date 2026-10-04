package com.seamlessdeconstructor.client.render;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ReverseDeconstructorBlockEntityRenderer implements BlockEntityRenderer<ReverseDeconstructorBlockEntity> {
    private static final float[][] OUTPUT_POSITIONS = {
        {-0.16F, -0.12F}, {0, -0.12F}, {0.16F, -0.12F},
        {-0.16F, 0.12F}, {0, 0.12F}, {0.16F, 0.12F}
    };
    private final ItemRenderer itemRenderer;

    public ReverseDeconstructorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ReverseDeconstructorBlockEntity blockEntity, float partialTick,
            PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        if (blockEntity.getLevel() == null) return;
        int itemLight = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above());
        ItemStack input = blockEntity.getRenderInputStack();
        if (!input.isEmpty()) {
            poses.pushPose();
            poses.translate(0.5, 1.0375, 0.5);
            poses.mulPose(Axis.XP.rotationDegrees(90));
            poses.scale(0.42F, 0.42F, 0.42F);
            itemRenderer.renderStatic(input, ItemDisplayContext.FIXED, itemLight,
                    OverlayTexture.NO_OVERLAY, poses, buffers, blockEntity.getLevel(), 0);
            poses.popPose();
        }
        Direction facing = blockEntity.getBlockState().getValue(ReverseDeconstructorBlock.FACING);
        for (int i = 0; i < OUTPUT_POSITIONS.length; i++) {
            ItemStack output = blockEntity.getRenderOutputStack(i);
            if (output.isEmpty()) continue;
            poses.pushPose();
            poses.translate(0.5, 0.275, 0.5);
            poses.mulPose(Axis.YP.rotationDegrees(yawForFacing(facing)));
            poses.translate(OUTPUT_POSITIONS[i][0], 0, OUTPUT_POSITIONS[i][1]);
            poses.mulPose(Axis.XP.rotationDegrees(90));
            poses.scale(0.24F, 0.24F, 0.24F);
            itemRenderer.renderStatic(output, ItemDisplayContext.FIXED, itemLight,
                    OverlayTexture.NO_OVERLAY, poses, buffers, blockEntity.getLevel(), i + 1);
            poses.popPose();
        }
    }

    @Override public boolean shouldRenderOffScreen(ReverseDeconstructorBlockEntity blockEntity) { return true; }

    private static float yawForFacing(Direction direction) {
        return switch (direction) {
            case NORTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
    }
}
