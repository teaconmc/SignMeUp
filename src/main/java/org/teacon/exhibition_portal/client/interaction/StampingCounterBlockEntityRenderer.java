package org.teacon.exhibition_portal.client.interaction;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.EPClient;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.components.Exhibition;

import java.util.Optional;
import java.util.UUID;

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
    }
}
