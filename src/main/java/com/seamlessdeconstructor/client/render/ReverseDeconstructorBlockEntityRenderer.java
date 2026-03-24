package com.seamlessdeconstructor.client.render;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
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

    public ReverseDeconstructorBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(ReverseDeconstructorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ItemStack input = entity.getRenderInputStack();
        if (!input.isEmpty()) {
            matrices.push();
            matrices.translate(0.5D, 1.0375D, 0.5D);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.scale(0.42F, 0.42F, 0.42F);
            MinecraftClient.getInstance().getItemRenderer().renderItem(input, net.minecraft.client.render.model.json.ModelTransformationMode.FIXED, light, overlay, matrices, vertexConsumers, entity.getWorld(), 0);
            matrices.pop();
        }

        Direction facing = entity.getCachedState().contains(ReverseDeconstructorBlock.FACING)
                ? entity.getCachedState().get(ReverseDeconstructorBlock.FACING)
                : Direction.NORTH;

        for (int i = 0; i < 6; i++) {
            ItemStack output = entity.getRenderOutputStack(i);
            if (output.isEmpty()) {
                continue;
            }

            float[] pos = OUTPUT_POSITIONS[i];
            matrices.push();
            matrices.translate(0.5D, 0.275D, 0.5D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawForFacing(facing)));
            matrices.translate(pos[0], 0.0D, pos[1]);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.scale(0.24F, 0.24F, 0.24F);
            MinecraftClient.getInstance().getItemRenderer().renderItem(output, net.minecraft.client.render.model.json.ModelTransformationMode.FIXED, light, overlay, matrices, vertexConsumers, entity.getWorld(), i + 1);
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
