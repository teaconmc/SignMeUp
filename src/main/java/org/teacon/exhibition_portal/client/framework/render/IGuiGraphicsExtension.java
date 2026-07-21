package org.teacon.exhibition_portal.client.framework.render;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.components.TextureMetadata;
import org.teacon.exhibition_portal.client.framework.components.UVSource;
import org.teacon.exhibition_portal.client.framework.render.text.EPTextRenderState;
import org.teacon.exhibition_portal.client.framework.render.text.TextFitMode;
import org.teacon.exhibition_portal.client.framework.render.text.TextHorizontalAlignment;
import org.teacon.exhibition_portal.client.framework.render.text.TextVerticalAlignment;

public interface IGuiGraphicsExtension {
    private GuiGraphicsExtractor self() {
        return (GuiGraphicsExtractor) this;
    }

    default void fill(Rectangle box, int col) {
        fill(box, col, col, col, col);
    }

    default void fill(Rectangle box, int col1, int col2, int col3, int col4) {
        fill(
                box.x1(), box.y1(), col1,
                box.x1(), box.y0(), col2,
                box.x0(), box.y0(), col3,
                box.x0(), box.y1(), col4
        );
    }

    default void fill(
            float x1, float y1, int col1,
            float x2, float y2, int col2,
            float x3, float y3, int col3,
            float x4, float y4, int col4
    ) {
        self().submitGuiElementRenderState(new ColoredQuadrangleRenderState(
                RenderPipelines.GUI, new Matrix3x2f(self().pose()),
                x1, y1, col1, x2, y2, col2, x3, y3, col3, x4, y4, col4,
                self().peekScissorStack()
        ));
    }

    default void blit(Identifier texture, GpuSampler sampler, Rectangle area) {
        blit(resolveTexture(texture), sampler, area, UVSource.FULL);
    }

    default void blit(Identifier texture, GpuSampler sampler, Rectangle area, UVSource uv) {
        blit(resolveTexture(texture), sampler, area, uv);
    }

    default void blit(GpuTextureView texture, GpuSampler sampler, Rectangle area) {
        blit(texture, sampler, area, UVSource.FULL);
    }

    default void blit(GpuTextureView textureView, GpuSampler sampler, Rectangle area, UVSource uv) {
        self().blit(
                textureView, sampler,
                Math.round(area.x0()), Math.round(area.y0()), Math.round(area.x1()), Math.round(area.y1()),
                uv.u0(), uv.u1(), uv.v0(), uv.v1()
        );
    }

    default void blit(TextureMetadata texture, GpuSampler sampler, Rectangle area) {
        blit(texture.textureView(), sampler, area, UVSource.FULL);
    }

    default void blit(TextureMetadata texture, GpuSampler sampler, Rectangle area, UVSource uv) {
        blit(texture.textureView(), sampler, area, uv);
    }

    private GpuTextureView resolveTexture(Identifier texture) {
        return Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView();
    }

    default void enableScissor(Rectangle box) {
        self().enableScissor(Math.round(box.x0()), Math.round(box.y0()), Math.round(box.x1()), Math.round(box.y1()));
    }

    default void renderString(
            Rectangle area, Component text,
            TextHorizontalAlignment horizontalAlignment, TextVerticalAlignment verticalAlignment, TextFitMode fitMode
    ) {
        renderString(area, text.getVisualOrderText(), horizontalAlignment, verticalAlignment, fitMode);
    }

    default void renderString(
            Rectangle area, FormattedCharSequence text,
            TextHorizontalAlignment horizontalAlignment, TextVerticalAlignment verticalAlignment, TextFitMode fitMode
    ) {
        self().submitPictureInPictureRenderState(new EPTextRenderState(area, Minecraft.getInstance().font, text, horizontalAlignment, verticalAlignment, fitMode, self().peekScissorStack()));
    }
}
