package org.teacon.exhibition_portal.client.interaction;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.ExhibitionPortal;

@EventBusSubscriber(Dist.CLIENT)
public class StampingCounterBlockEntityRenderer implements BlockEntityRenderer<StampingCounterBlockEntity, StampingCounterBlockEntityRenderer.RenderState> {
    public StampingCounterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public StampingCounterBlockEntityRenderer.@NonNull RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
            @NonNull StampingCounterBlockEntity blockEntity,
            @NonNull RenderState state, float partialTicks,
            @NonNull Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
    }

    @Override
    public void submit(
            StampingCounterBlockEntityRenderer.@NonNull RenderState state,
            @NonNull PoseStack pose,
            @NonNull SubmitNodeCollector collector,
            @NonNull CameraRenderState camera
    ) {
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ExhibitionPortal.STAMPING_COUNTER_BE.get(),
                StampingCounterBlockEntityRenderer::new
        );
    }

    public static class RenderState extends BlockEntityRenderState {
    }
}
