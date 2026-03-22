package com.seamlessdeconstructor.client.render;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class ReverseDeconstructorBlockEntityRenderer implements BlockEntityRenderer<ReverseDeconstructorBlockEntity, ReverseDeconstructorBlockEntityRenderer.State> {
    private final ItemModelManager itemModelManager;

    public ReverseDeconstructorBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemModelManager = context.itemModelManager();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(ReverseDeconstructorBlockEntity blockEntity, State state, float tickProgress, Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderer.super.updateRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);

        ItemStack input = blockEntity.getRenderInputStack();
        state.hasInput = !input.isEmpty();
        if (state.hasInput) {
            this.itemModelManager.clearAndUpdate(state.inputState, input, ItemDisplayContext.GROUND, blockEntity.getWorld(), null, 0);
        } else {
            state.inputState.clear();
        }

        ItemStack output = blockEntity.getRenderOutputStack();
        state.hasOutput = !output.isEmpty();
        if (state.hasOutput) {
            this.itemModelManager.clearAndUpdate(state.outputState, output, ItemDisplayContext.FIXED, blockEntity.getWorld(), null, 1);
        } else {
            state.outputState.clear();
        }

        state.facing = blockEntity.getCachedState().contains(ReverseDeconstructorBlock.FACING)
                ? blockEntity.getCachedState().get(ReverseDeconstructorBlock.FACING)
                : Direction.NORTH;
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        if (state.hasInput) {
            matrices.push();
            matrices.translate(0.5, 1.01, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.scale(0.42F, 0.42F, 0.42F);
            state.inputState.render(matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, -1);
            matrices.pop();
        }

        if (state.hasOutput) {
            matrices.push();
            matrices.translate(0.5, 0.55, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawForFacing(state.facing)));
            matrices.translate(0.0, 0.0, -0.18);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            matrices.scale(0.36F, 0.36F, 0.36F);
            state.outputState.render(matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, -1);
            matrices.pop();
        }
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

    public static class State extends BlockEntityRenderState {
        private final ItemRenderState inputState = new ItemRenderState();
        private final ItemRenderState outputState = new ItemRenderState();
        private Direction facing = Direction.NORTH;
        private boolean hasInput;
        private boolean hasOutput;
    }
}
