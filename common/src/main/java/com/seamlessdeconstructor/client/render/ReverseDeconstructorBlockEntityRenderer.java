package com.seamlessdeconstructor.client.render;

import com.seamlessdeconstructor.block.ReverseDeconstructorBlock;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ReverseDeconstructorBlockEntityRenderer implements BlockEntityRenderer<ReverseDeconstructorBlockEntity, ReverseDeconstructorBlockEntityRenderer.State> {
    private static final float[][] OUTPUT_POSITIONS = new float[][]{
            {-0.16F, -0.12F},
            {0.0F, -0.12F},
            {0.16F, -0.12F},
            {-0.16F, 0.12F},
            {0.0F, 0.12F},
            {0.16F, 0.12F}
    };

    private final ItemModelResolver itemModelResolver;

    public ReverseDeconstructorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ReverseDeconstructorBlockEntity blockEntity, State state, float tickProgress, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);

        ItemStack input = blockEntity.getRenderInputStack();
        state.hasInput = !input.isEmpty();
        if (state.hasInput) {
            this.itemModelResolver.updateForTopItem(state.inputState, input, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
        } else {
            state.inputState.clear();
        }

        state.hasOutput = false;
        for (int i = 0; i < 6; i++) {
            ItemStack output = blockEntity.getRenderOutputStack(i);
            state.hasOutputs[i] = !output.isEmpty();
            if (state.hasOutputs[i]) {
                this.itemModelResolver.updateForTopItem(state.outputStates[i], output, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, i + 1);
                state.hasOutput = true;
            } else {
                state.outputStates[i].clear();
            }
        }

        state.facing = blockEntity.getBlockState().hasProperty(ReverseDeconstructorBlock.FACING)
                ? blockEntity.getBlockState().getValue(ReverseDeconstructorBlock.FACING)
                : Direction.NORTH;

        state.itemLightCoords = blockEntity.getLevel() != null
                ? LightCoordsUtil.getLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos().above())
                : state.lightCoords;
    }

    @Override
    public void submit(State state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (state.hasInput) {
            matrices.pushPose();
            matrices.translate(0.5, 1.0375, 0.5);
            matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
            matrices.scale(0.42F, 0.42F, 0.42F);
            state.inputState.submit(matrices, queue, state.itemLightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }

        if (state.hasOutput) {
            for (int i = 0; i < 6; i++) {
                if (!state.hasOutputs[i]) {
                    continue;
                }

                float[] pos = OUTPUT_POSITIONS[i];
                matrices.pushPose();
                matrices.translate(0.5, 0.275, 0.5);
                matrices.mulPose(Axis.YP.rotationDegrees(yawForFacing(state.facing)));
                matrices.translate(pos[0], 0.0, pos[1]);
                matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
                matrices.scale(0.24F, 0.24F, 0.24F);
                state.outputStates[i].submit(matrices, queue, state.itemLightCoords, OverlayTexture.NO_OVERLAY, 0);
                matrices.popPose();
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
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
        private final ItemStackRenderState inputState = new ItemStackRenderState();
        private final ItemStackRenderState[] outputStates = new ItemStackRenderState[]{
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState()
        };
        private final boolean[] hasOutputs = new boolean[6];
        private Direction facing = Direction.NORTH;
        private int itemLightCoords;
        private boolean hasInput;
        private boolean hasOutput;
    }
}
