package org.teacon.exhibition_portal.client.framework.render.text;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import org.jspecify.annotations.NonNull;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

@EventBusSubscriber(Dist.CLIENT)
public final class EPTextRenderer extends PictureInPictureRenderer<EPTextRenderState> {
    @SubscribeEvent
    private static void on(RegisterPictureInPictureRenderersEvent event) {
        event.register(EPTextRenderState.class, EPTextRenderer::new);
    }

    private EPTextRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public @NonNull Class<EPTextRenderState> getRenderStateClass() {
        return EPTextRenderState.class;
    }

    private static final VarHandle TEXTURE_VIEW, DEPTH_TEXTURE_VIEW;
    private static final MethodHandle PREPARE_TEXTURES_AND_PROJECTION;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(PictureInPictureRenderer.class, MethodHandles.lookup());
            TEXTURE_VIEW = lookup.findVarHandle(PictureInPictureRenderer.class, "textureView", GpuTextureView.class);
            DEPTH_TEXTURE_VIEW = lookup.findVarHandle(PictureInPictureRenderer.class, "depthTextureView", GpuTextureView.class);
            PREPARE_TEXTURES_AND_PROJECTION = lookup.findVirtual(
                    PictureInPictureRenderer.class,
                    "prepareTexturesAndProjection",
                    MethodType.methodType(void.class, boolean.class, int.class, int.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void prepare(@NonNull EPTextRenderState state, @NonNull GuiRenderState guiRenderState, int guiScale) {
        GpuTextureView textureView = (GpuTextureView) TEXTURE_VIEW.get(this), depthTextureView = (GpuTextureView) DEPTH_TEXTURE_VIEW.get(this);
        guiScale *= EPTextRenderState.EXTRA_SCALE;

        int width = (state.x1() - state.x0()) * guiScale;
        int height = (state.y1() - state.y0()) * guiScale;
        if (width == 0 || height == 0) {
            return;
        }

        boolean needsAResize = textureView == null || textureView.getWidth(0) != width || textureView.getHeight(0) != height;
        if (needsAResize || !this.textureIsReadyToBlit(state)) {
            try {
                PREPARE_TEXTURES_AND_PROJECTION.invokeExact((PictureInPictureRenderer<EPTextRenderState>) this, needsAResize, width, height);
            } catch (Throwable e) {
                switch (e) {
                    case RuntimeException re -> throw re;
                    case Error ee -> throw ee;
                    default -> throw new RuntimeException(e);
                }
            }

            RenderSystem.outputColorTextureOverride = textureView;
            RenderSystem.outputDepthTextureOverride = depthTextureView;
            try {
                PoseStack poseStack = new PoseStack();
                poseStack.scale(guiScale, guiScale, -guiScale);
                poseStack.translate(EPTextRenderState.EXTRA_MARGIN, EPTextRenderState.EXTRA_MARGIN, 0);
                EPPreparedTextBuilder builder = new EPPreparedTextBuilder(
                        state.font(), new Rectangle(0, 0, state.area().w(), state.area().h()),
                        state.horizontalAlignment(), state.verticalAlignment(), state.fitMode()
                );
                state.text().accept(builder);
                builder.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, poseStack.last().pose(), Font.DisplayMode.NORMAL, 0xFFFFFFFF));
                this.bufferSource.endBatch();
            } finally {
                RenderSystem.outputColorTextureOverride = null;
                RenderSystem.outputDepthTextureOverride = null;
            }
        }

        this.blitTexture(state, guiRenderState);
    }

    @Override
    protected void blitTexture(@NonNull EPTextRenderState renderState, @NonNull GuiRenderState guiRenderState) {
        guiRenderState.addBlitToCurrentLayer(
                new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        TextureSetup.singleTexture((GpuTextureView) TEXTURE_VIEW.get(this), RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR)),
                        renderState.pose(),
                        renderState.x0(), renderState.y0(), renderState.x1(), renderState.y1(),
                        0.0F, 1.0F, 1.0F, 0.0F,
                        -1,
                        renderState.scissorArea(),
                        null
                )
        );
    }

    @Override
    protected void renderToTexture(@NonNull EPTextRenderState state, @NonNull PoseStack poseStack) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean textureIsReadyToBlit(@NonNull EPTextRenderState state) {
        return false;
    }

    @Override
    public boolean canBeReusedFor(@NonNull EPTextRenderState state, int width, int height) {
        return super.canBeReusedFor(state, width * EPTextRenderState.EXTRA_SCALE, height * EPTextRenderState.EXTRA_SCALE);
    }

    @Override
    protected @NonNull String getTextureLabel() {
        return "Exhibition Portal Text";
    }
}
