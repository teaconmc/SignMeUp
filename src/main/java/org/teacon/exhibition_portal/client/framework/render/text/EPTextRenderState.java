package org.teacon.exhibition_portal.client.framework.render.text;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;

public record EPTextRenderState(
        Rectangle area, Font font, FormattedCharSequence text,
        TextHorizontalAlignment horizontalAlignment, TextVerticalAlignment verticalAlignment, TextFitMode fitMode,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public static final int EXTRA_SCALE = 2;

    public EPTextRenderState(
            Rectangle area, Font font, FormattedCharSequence text,
            TextHorizontalAlignment horizontalAlignment, TextVerticalAlignment verticalAlignment, TextFitMode fitMode,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                area, font, text,
                horizontalAlignment, verticalAlignment, fitMode,
                scissorArea, PictureInPictureRenderState.getBounds((int) area.x0(), (int) area.y0(), (int) area.x1(), (int) area.y1(), scissorArea)
        );
    }

    @Override
    public int x0() {
        return (int) area.x0();
    }

    @Override
    public int x1() {
        return (int) area.x1();
    }

    @Override
    public int y0() {
        return (int) area.y0();
    }

    @Override
    public int y1() {
        return (int) area.y1();
    }

    @Override
    public float scale() {
        throw new UnsupportedOperationException();
    }
}
