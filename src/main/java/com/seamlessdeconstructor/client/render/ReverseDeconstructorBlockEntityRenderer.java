package com.seamlessdeconstructor.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ReverseDeconstructorBlockEntityRenderer implements BlockEntityRenderer<ReverseDeconstructorBlockEntity> {
    private static final float[][] OUTPUT_POSITIONS = new float[][]{
        {-0.16F, -0.12F},
        {0.0F, -0.12F},
        {0.16F, -0.12F},
        {-0.16F, 0.12F},
        {0.0F, 0.12F},
        {0.16F, 0.12F}
    };

    private final ItemRenderer itemRenderer;

    public ReverseDeconstructorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ReverseDeconstructorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack input = blockEntity.getRenderInputStack();
        if (!input.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5D, 1.0375D, 0.5D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.42F, 0.42F, 0.42F);
            itemRenderer.renderStatic(input, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, blockEntity.getLevel(), 0);
            poseStack.popPose();
        }

        Direction facing = blockEntity.getBlockState().hasProperty(ReverseDeconstructorBlock.FACING)
            ? blockEntity.getBlockState().getValue(ReverseDeconstructorBlock.FACING)
            : Direction.NORTH;

        for (int i = 0; i < 6; i++) {
            ItemStack output = blockEntity.getRenderOutputStack(i);
            if (output.isEmpty()) {
                continue;
            }

            float[] pos = OUTPUT_POSITIONS[i];
            poseStack.pushPose();
            poseStack.translate(0.5D, 0.275D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(yawForFacing(facing)));
            poseStack.translate(pos[0], 0.0D, pos[1]);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.24F, 0.24F, 0.24F);
            itemRenderer.renderStatic(output, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, blockEntity.getLevel(), i + 1);
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ReverseDeconstructorBlockEntity blockEntity) {
        return true;
    }

    private static float yawForFacing(Direction direction) {
        return switch (direction) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }
}
