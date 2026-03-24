package com.seamlessdeconstructor.client.render;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

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

    public ReverseDeconstructorBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ReverseDeconstructorBlockEntity blockEntity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (blockEntity.getWorld() == null) {
            return;
        }

        Direction facing = blockEntity.getCachedState().contains(ReverseDeconstructorBlock.FACING)
                ? blockEntity.getCachedState().get(ReverseDeconstructorBlock.FACING)
                : Direction.NORTH;

        int itemLight = WorldRenderer.getLightmapCoordinates(blockEntity.getWorld(), blockEntity.getPos().up());

        ItemStack input = blockEntity.getRenderInputStack();
        if (!input.isEmpty()) {
            matrices.push();
            matrices.translate(0.5, 1.0375, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.scale(0.42F, 0.42F, 0.42F);
            this.itemRenderer.renderItem(input, ModelTransformationMode.FIXED, itemLight, OverlayTexture.DEFAULT_UV,
                    matrices, vertexConsumers, blockEntity.getWorld(), 0);
            matrices.pop();
        }

        for (int i = 0; i < 6; i++) {
            ItemStack output = blockEntity.getRenderOutputStack(i);
            if (output.isEmpty()) {
                continue;
            }

            float[] pos = OUTPUT_POSITIONS[i];
            matrices.push();
            matrices.translate(0.5, 0.275, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawForFacing(facing)));
            matrices.translate(pos[0], 0.0, pos[1]);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.scale(0.24F, 0.24F, 0.24F);
            this.itemRenderer.renderItem(output, ModelTransformationMode.FIXED, itemLight, OverlayTexture.DEFAULT_UV,
                    matrices, vertexConsumers, blockEntity.getWorld(), i + 1);
            matrices.pop();
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(ReverseDeconstructorBlockEntity blockEntity) {
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
