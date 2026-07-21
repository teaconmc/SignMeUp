package org.teacon.exhibition_portal.client.framework.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

public record ColoredQuadrangleRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x1, float y1, int col1,
        float x2, float y2, int col2,
        float x3, float y3, int col3,
        float x4, float y4, int col4,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColoredQuadrangleRenderState(
            RenderPipeline pipeline,
            Matrix3x2fc pose,
            float x1, float y1, int col1,
            float x2, float y2, int col2,
            float x3, float y3, int col3,
            float x4, float y4, int col4,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                pipeline, TextureSetup.noTexture(), pose,
                x1, y1, col1, x2, y2, col2, x3, y3, col3, x4, y4, col4,
                scissorArea,
                getBounds(min(x1, x2, x3, x4), min(y1, y2, y3, y4), max(x1, x2, x3, x4), max(y1, y2, y3, y4), pose, scissorArea)
        );
    }

    private static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x1, y1).setColor(col1);
        vertexConsumer.addVertexWith2DPose(pose, x2, y2).setColor(col2);
        vertexConsumer.addVertexWith2DPose(pose, x3, y3).setColor(col3);
        vertexConsumer.addVertexWith2DPose(pose, x4, y4).setColor(col4);
    }

    private static @Nullable ScreenRectangle getBounds(float x0, float y0, float x1, float y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        Vector2f topLeft = pose.transformPosition(x0, y0, new Vector2f());
        Vector2f topRight = pose.transformPosition(x1, y0, new Vector2f());
        Vector2f bottomLeft = pose.transformPosition(x0, y1, new Vector2f());
        Vector2f bottomRight = pose.transformPosition(x1, y1, new Vector2f());
        float minX = Math.min(Math.min(topLeft.x(), bottomLeft.x()), Math.min(topRight.x(), bottomRight.x()));
        float maxX = Math.max(Math.max(topLeft.x(), bottomLeft.x()), Math.max(topRight.x(), bottomRight.x()));
        float minY = Math.min(Math.min(topLeft.y(), bottomLeft.y()), Math.min(topRight.y(), bottomRight.y()));
        float maxY = Math.max(Math.max(topLeft.y(), bottomLeft.y()), Math.max(topRight.y(), bottomRight.y()));
        
        ScreenRectangle bounds = new ScreenRectangle(Mth.floor(minX), Mth.floor(minY), Mth.ceil(maxX - minX), Mth.ceil(maxY - minY));
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}
