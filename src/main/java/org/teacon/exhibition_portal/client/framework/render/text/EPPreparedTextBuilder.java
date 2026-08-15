package org.teacon.exhibition_portal.client.framework.render.text;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public class EPPreparedTextBuilder implements Font.PreparedText, FormattedCharSink {
    private static final MethodHandle FONT_GET_GLYPH;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Font.class, MethodHandles.lookup());

            FONT_GET_GLYPH = lookup.findVirtual(Font.class, "getGlyph", MethodType.methodType(BakedGlyph.class, int.class, Style.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final int GLYPH_Y = 20;

    private final Font font;

    private final Rectangle area;
    private final TextHorizontalAlignment horizontalAlignment;
    private final TextVerticalAlignment verticalAlignment;
    private final TextFitMode fitMode;

    private float left, top, right, bottom;

    private record GlyphRecord(TextRenderable.Styled glyph, float x, float advance) {
    }

    private final List<GlyphRecord> glyphs;

    public EPPreparedTextBuilder(Font font, Rectangle area, TextHorizontalAlignment horizontalAlignment, TextVerticalAlignment verticalAlignment, TextFitMode fitMode) {
        this.font = font;
        this.area = area;
        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
        this.fitMode = fitMode;
        this.glyphs = new ArrayList<>();
    }

    private void expand(float left, float top, float right, float bottom) {
        this.left = Math.min(this.left, left);
        this.top = Math.min(this.top, top);
        this.right = Math.max(this.right, right);
        this.bottom = Math.max(this.bottom, bottom);
    }

    @Override
    public boolean accept(int position, @NonNull Style style, int c) {
        BakedGlyph bakedGlyph;
        try {
            bakedGlyph = (BakedGlyph) FONT_GET_GLYPH.invokeExact(font, c, style);
        } catch (Throwable t) {
            throw t instanceof RuntimeException re ? re : new RuntimeException(t);
        }
        return this.accept(style, bakedGlyph);
    }

    private float x;

    private boolean accept(Style style, BakedGlyph glyph) {
        GlyphInfo glyphInfo = glyph.info();
        int textColor = this.getTextColor(style);
        int shadowColor = this.getShadowColor(style, textColor);
        float advance = glyphInfo.getAdvance(style.isBold());
        float shadowOffset = glyphInfo.getShadowOffset();
        float boldOffset = style.isBold() ? glyphInfo.getBoldOffset() : 0.0F;

        TextRenderable.Styled instance = glyph.createGlyph(this.x, GLYPH_Y, textColor, shadowColor, style, boldOffset, shadowOffset);
        if (instance != null) {
            // FIXME: Display the following styles.
//            if (style.isStrikethrough()) {
//                this.addEffect(getProvider().effect()
//                        .createEffect(effectX0, this.y + 4.5F - 1.0F, this.x + advance, this.y + 4.5F, 0.01F, textColor, shadowColor, shadowOffset)
//                );
//            }
//
//            if (style.isUnderlined()) {
//                this.addEffect(getProvider().effect()
//                        .createEffect(effectX0, this.y + 9.0F - 1.0F, this.x + advance, this.y + 9.0F, 0.01F, textColor, shadowColor, shadowOffset)
//                );
//            }
            this.glyphs.add(new GlyphRecord(instance, this.x, advance));
            this.expand(instance.left(), instance.top(), instance.right(), instance.bottom());
        }

        this.x += advance;
        return true;
    }

    @Override
    public void visit(Font.@NonNull GlyphVisitor visitor) {
        Rectangle bound = computeBound();
//        visitor.acceptEffect(getProvider().effect()
//                .createEffect(bound.x0(), bound.y0(), bound.x1(), bound.y1(), 0.01f, 0x80FF00FF, 0, 1));

        for (GlyphRecord gr : this.glyphs) {
            TextRenderable.Styled glyph = gr.glyph();

            float glyphHeight = (glyph.activeBottom() - GLYPH_Y) * bound.w() / (right - left);
            Rectangle target = new Rectangle(
                    bound.x() + gr.x * bound.w() / (right - left),
                    switch (verticalAlignment) {
                        case TOP -> bound.y();
                        case CENTER -> bound.y() + bound.h() / 2f - glyphHeight / 2f;
                        case BOTTOM -> bound.y1() - glyphHeight;
                    },
                    gr.advance * bound.w() / (right - left),
                    glyphHeight
            );
            Rectangle source = new Rectangle(gr.x, GLYPH_Y, gr.advance, glyph.activeBottom() - GLYPH_Y);

//            visitor.acceptEffect(getProvider().effect()
//                    .createEffect(target.x0(), target.y0(), target.x1(), target.y1(), 0.01f, 0x80FFFF00, 0, 1));

            visitor.acceptGlyph(new TextRenderable.Styled() {
                public float activeLeft() {
                    return glyph.activeLeft();
                }

                public float activeTop() {
                    return glyph.activeTop();
                }

                public float activeRight() {
                    return glyph.activeRight();
                }

                public float activeBottom() {
                    return glyph.activeBottom();
                }

                @Override
                public void render(@NonNull Matrix4fc pose, @NonNull VertexConsumer buffer, int packedLightCoords, boolean flat) {
                    Matrix4f matrix = new Matrix4f(pose);
                    matrix.translate(target.x(), target.y(), 0)
                            .scale(target.w() / source.w(), target.w() / source.w(), 1)
                            .translate(-source.x(), -source.y(), 0);

                    glyph.render(matrix, buffer, packedLightCoords, flat);
                }

                public @NonNull RenderType renderType(Font.@NonNull DisplayMode displayMode, boolean blur) {
                    return glyph.renderType(displayMode, blur);
                }

                @Deprecated
                public @NonNull RenderType renderType(Font.@NonNull DisplayMode displayMode) {
                    return glyph.renderType(displayMode);
                }

                public @NonNull GpuTextureView textureView() {
                    return glyph.textureView();
                }

                public @NonNull RenderPipeline guiPipeline() {
                    return glyph.guiPipeline();
                }

                public float left() {
                    return glyph.left();
                }

                public float top() {
                    return glyph.top();
                }

                public float right() {
                    return glyph.right();
                }

                public float bottom() {
                    return glyph.bottom();
                }

                public @NonNull Style style() {
                    return glyph.style();
                }
            });
        }
    }

    private @NonNull Rectangle computeBound() {
        Rectangle bound = switch (fitMode) {
            case FIT_HEIGHT -> new Rectangle(0, 0, area.h() * (right - left) / (bottom - GLYPH_Y), area.h());
        };
        bound = new Rectangle(switch (horizontalAlignment) {
            case LEFT, SCROLL -> area.x();
            case MIDDLE -> area.x() + area.w() / 2f - bound.w() / 2f;
            case RIGHT -> area.x1() - bound.w();
        }, bound.y(), bound.w(), bound.h());
        bound = new Rectangle(bound.x(), switch (verticalAlignment) {
            case TOP -> area.y();
            case CENTER -> area.y() + area.h() / 2f - bound.h() / 2f;
            case BOTTOM -> area.y1() - bound.h();
        }, bound.w(), bound.h());
        return bound;
    }

    private int getTextColor(Style style) {
        TextColor textColor = style.getColor();
        return ARGB.color(0xFF, textColor != null ? textColor.getValue() : 0xFFFFFF);
    }

    private int getShadowColor(Style style, int textColor) {
        Integer shadow = style.getShadowColor();
        if (shadow != null) {
            float textAlpha = ARGB.alphaFloat(textColor);
            return textAlpha != 1.0F ? ARGB.color(ARGB.as8BitChannel(textAlpha * ARGB.alphaFloat(shadow)), shadow) : shadow;
        } else {
            return 0;
        }
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        if (!(this.left >= this.right) && !(this.top >= this.bottom)) {
            int left = Mth.floor(this.left);
            int top = Mth.floor(this.top);
            int right = Mth.ceil(this.right);
            int bottom = Mth.ceil(this.bottom);
            return new ScreenRectangle(Math.round(this.area.x() + left), Math.round(this.area.y() + top - GLYPH_Y), right - left, bottom - top);
        } else {
            return null;
        }
    }
}
