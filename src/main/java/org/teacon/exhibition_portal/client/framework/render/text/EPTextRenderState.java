package org.teacon.exhibition_portal.client.framework.render.text;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;

public record EPTextRenderState(
        Rectangle area, Font font, FormattedCharSequence text,
        TextHorizontalAlignment horizontalAlignment, TextVerticalAlignment verticalAlignment, TextFitMode fitMode,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public static final int EXTRA_SCALE = 2, EXTRA_MARGIN = 5;

    public EPTextRenderState(
            Rectangle area, Font font, FormattedCharSequence text,
            TextHorizontalAlignment horizontalAlignment, TextVerticalAlignment verticalAlignment, TextFitMode fitMode,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                area, font, text,
                horizontalAlignment, verticalAlignment, fitMode,
                scissorArea, PictureInPictureRenderState.getBounds(
                        Mth.floor(area.x0()), Mth.floor(area.y0()), Mth.ceil(area.x1()), Mth.ceil(area.y1()), scissorArea)
        );
    }

    @Override
    public int x0() {
        return Mth.floor(area.x0()) - EXTRA_MARGIN;
    }

    @Override
    public int x1() {
        return Mth.ceil(area.x1()) + EXTRA_MARGIN;
    }

    @Override
    public int y0() {
        return Mth.floor(area.y0()) - EXTRA_MARGIN;
    }

    @Override
    public int y1() {
        return Mth.ceil(area.y1()) + EXTRA_MARGIN;
    }

    @Override
    public float scale() {
        throw new UnsupportedOperationException();
    }
}
