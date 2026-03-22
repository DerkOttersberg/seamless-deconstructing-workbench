package com.seamlessdeconstructor.client.render;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.WorldRenderer;
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
    private static final float[][] OUTPUT_POSITIONS = new float[][]{
            {-0.16F, -0.12F},
            {0.0F, -0.12F},
            {0.16F, -0.12F},
            {-0.16F, 0.12F},
            {0.0F, 0.12F},
            {0.16F, 0.12F}
    };

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
            this.itemModelManager.clearAndUpdate(state.inputState, input, ItemDisplayContext.FIXED, blockEntity.getWorld(), null, 0);
        } else {
            state.inputState.clear();
        }

        state.hasOutput = false;
        for (int i = 0; i < 6; i++) {
            ItemStack output = blockEntity.getRenderOutputStack(i);
            state.hasOutputs[i] = !output.isEmpty();
            if (state.hasOutputs[i]) {
                this.itemModelManager.clearAndUpdate(state.outputStates[i], output, ItemDisplayContext.FIXED, blockEntity.getWorld(), null, i + 1);
                state.hasOutput = true;
            } else {
                state.outputStates[i].clear();
            }
        }

        state.facing = blockEntity.getCachedState().contains(ReverseDeconstructorBlock.FACING)
                ? blockEntity.getCachedState().get(ReverseDeconstructorBlock.FACING)
                : Direction.NORTH;

        state.itemLightCoords = blockEntity.getWorld() != null
            ? WorldRenderer.getLightmapCoordinates(blockEntity.getWorld(), blockEntity.getPos().up())
            : state.lightmapCoordinates;
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        if (state.hasInput) {
            matrices.push();
            matrices.translate(0.5, 1.0375, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.scale(0.42F, 0.42F, 0.42F);
            state.inputState.render(matrices, queue, state.itemLightCoords, OverlayTexture.DEFAULT_UV, 0);
            matrices.pop();
        }

        if (state.hasOutput) {
            for (int i = 0; i < 6; i++) {
                if (!state.hasOutputs[i]) {
                    continue;
                }

                float[] pos = OUTPUT_POSITIONS[i];
                matrices.push();
                matrices.translate(0.5, 0.275, 0.5);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawForFacing(state.facing)));
                matrices.translate(pos[0], 0.0, pos[1]);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                matrices.scale(0.24F, 0.24F, 0.24F);
                state.outputStates[i].render(matrices, queue, state.itemLightCoords, OverlayTexture.DEFAULT_UV, 0);
                matrices.pop();
            }
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox() {
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

    public static class State extends BlockEntityRenderState {
        private final ItemRenderState inputState = new ItemRenderState();
        private final ItemRenderState[] outputStates = new ItemRenderState[]{
                new ItemRenderState(),
                new ItemRenderState(),
                new ItemRenderState(),
                new ItemRenderState(),
                new ItemRenderState(),
                new ItemRenderState()
        };
        private final boolean[] hasOutputs = new boolean[6];
        private Direction facing = Direction.NORTH;
        private int itemLightCoords;
        private boolean hasInput;
        private boolean hasOutput;
    }
}
