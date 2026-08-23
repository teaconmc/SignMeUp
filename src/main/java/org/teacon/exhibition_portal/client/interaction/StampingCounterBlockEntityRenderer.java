package org.teacon.exhibition_portal.client.interaction;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.EPClient;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.components.Exhibition;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@EventBusSubscriber(Dist.CLIENT)
public class StampingCounterBlockEntityRenderer implements BlockEntityRenderer<StampingCounterBlockEntity, StampingCounterBlockEntityRenderer.RenderState> {
    private static final Identifier MARKER = ExhibitionPortal.id("textures/stamping_table_mark.png");

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

        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        boolean visible = player != null && level != null
                && AABB.ofSize(blockEntity.getBlockPos().getCenter(), 15, 3, 15).contains(player.position())
                && level.getBlockState(blockEntity.getBlockPos().above(3)).isAir();
        if (visible != blockEntity.visible) {
            blockEntity.visible = visible;
            if (visible) {
                blockEntity.visibleSinceNs = Util.getNanos();
            }
        }

        state.visible = visible;
        if (visible) {
            state.visibleSinceNs = blockEntity.visibleSinceNs;
        }
    }

    @Override
    public void submit(
            StampingCounterBlockEntityRenderer.@NonNull RenderState state,
            @NonNull PoseStack pose,
            @NonNull SubmitNodeCollector collector,
            @NonNull CameraRenderState camera
    ) {
        if (!state.visible) {
            return;
        }

        long durationNs = Util.getNanos() - state.visibleSinceNs;
        pose.pushPose();
        pose.translate(0, 2.5, 0);

        float scale;
        if (durationNs <= TimeUnit.MILLISECONDS.toNanos(500)) {
            scale = durationNs / (float) TimeUnit.MILLISECONDS.toNanos(500);
        } else {
            scale = 1;
        }
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -0.5, -0.5);

        float step = durationNs % TimeUnit.MILLISECONDS.toNanos(1500) / (float) TimeUnit.MILLISECONDS.toNanos(1500);
        pose.translate(0, Mth.sin(step * Mth.TWO_PI) * 0.1f, 0);

        pose.rotateAround(camera.orientation, 0.5f, 0.5f, 0.5f);
        pose.rotateAround(new Quaternionf().rotateX(Mth.HALF_PI), 0.5f, 0.5f, 0.5f);
        pose.translate(0, 0.5f, 0);

        collector.submitCustomGeometry(
                pose,
                RenderType.create(
                        "StampingCounter",
                        RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
                                .withTexture("Sampler0", MARKER)
                                .useLightmap()
                                .createRenderSetup()
                ),
                (p, buffer) -> {
                    buffer.addVertex(p, 0, 0, 1)
                            .setColor(0xFFFFFFFF)
                            .setUv(0, 1)
                            .setLight(LightCoordsUtil.pack(15, 15))
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setNormal(p, 0, 1, 0);
                    buffer.addVertex(p, 1, 0, 1)
                            .setColor(0xFFFFFFFF)
                            .setUv(1, 1)
                            .setLight(LightCoordsUtil.pack(15, 15))
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setNormal(p, 0, 1, 0);
                    buffer.addVertex(p, 1, 0, 0)
                            .setColor(0xFFFFFFFF)
                            .setUv(1, 0)
                            .setLight(LightCoordsUtil.pack(15, 15))
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setNormal(p, 0, 1, 0);
                    buffer.addVertex(p, 0, 0, 0)
                            .setColor(0xFFFFFFFF)
                            .setUv(0, 0)
                            .setLight(LightCoordsUtil.pack(15, 15))
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setNormal(p, 0, 1, 0);
                });
        pose.popPose();
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(StampingCounterBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, 4, 0);
    }

    @SubscribeEvent
    private static void on(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ExhibitionPortal.STAMPING_COUNTER_BE.get(),
                StampingCounterBlockEntityRenderer::new
        );
    }

    @SubscribeEvent
    private static void on(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof StampingCounterBlock)) {
            return;
        }

        TypedEntityData<BlockEntityType<?>> data = event.getItemStack().getComponents().get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return;
        }

        CompoundTag tag = data.copyTagWithoutId();
        Optional<int[]> exhibition = tag.getIntArray("exhibition");
        Optional<String> stampID = tag.getString("stamp_id");
        Optional<Boolean> isRectangleStamp = tag.getBoolean("rectangle");
        if (exhibition.isEmpty() || exhibition.get().length != 4 || stampID.isEmpty() || isRectangleStamp.isEmpty()) {
            return;
        }

        Exhibition e = RenderAccess.get(EPClient.GALLERY_LOOKUP).get(UUIDUtil.uuidFromIntArray(exhibition.get()));
        event.getToolTip().add(Component.translatable("exhibition_portal.stamping_counter.tooltip", e.metadata().name(), stampID.get()));
    }

    public static class RenderState extends BlockEntityRenderState {
        private boolean visible;
        private long visibleSinceNs = 0;
    }
}
