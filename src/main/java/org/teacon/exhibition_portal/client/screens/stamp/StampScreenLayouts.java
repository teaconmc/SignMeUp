package org.teacon.exhibition_portal.client.screens.stamp;

import com.google.gson.annotations.SerializedName;
import com.mojang.util.UndashedUuid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.framework.DragContext;
import org.teacon.exhibition_portal.client.framework.Layers;
import org.teacon.exhibition_portal.client.framework.binding.LayoutBinding;
import org.teacon.exhibition_portal.client.framework.binding.LayoutFailureException;
import org.teacon.exhibition_portal.client.framework.binding.LayoutParameter;
import org.teacon.exhibition_portal.client.framework.binding.LayoutResource;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Pos;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.components.TextureMetadata;
import org.teacon.exhibition_portal.client.screens.LayerPriority;
import org.teacon.exhibition_portal.client.screens.MapLayouts;
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.components.ExhibitionStamp;
import org.teacon.exhibition_portal.network.UpdateExhibitionStampPacket;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.teacon.exhibition_portal.client.EPClient.GALLERY_LOOKUP;
import static org.teacon.exhibition_portal.client.framework.GeneralLayouts.WINDOW_HEIGHT;
import static org.teacon.exhibition_portal.client.framework.GeneralLayouts.WINDOW_WIDTH;

public class StampScreenLayouts {
    public static final LayoutBinding<Rectangle> MAP_BOX = LayoutBinding.of(
            () -> List.of(WINDOW_WIDTH, WINDOW_HEIGHT),
            context -> new Rectangle(0, 0, context.get(WINDOW_WIDTH), context.get(WINDOW_HEIGHT))
    );

    private record HandleConfig(Rectangle rotate, Rectangle move, Rectangle scale) {
        private static final LayoutResource.ResourceReader<HandleConfig> READER = id -> {
            try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(id)) {
                record H(@SerializedName("rotate") float[] rotate, @SerializedName("move") float[] move,
                         @SerializedName("scale") float[] scale) {
                }

                H h = ExhibitionPortal.GSON.fromJson(reader, H.class);
                return new HandleConfig(
                        new Rectangle(h.rotate[0], h.rotate[1], h.rotate[2], h.rotate[3]),
                        new Rectangle(h.move[0], h.move[1], h.move[2], h.move[3]),
                        new Rectangle(h.scale[0], h.scale[1], h.scale[2], h.scale[3])
                );
            }
        };
    }

    public static final LayoutBinding<TextureMetadata> STAMP_HANDLE_RECT = TextureMetadata.of(ExhibitionPortal.id("textures/gui/stamp_handle_rect.png"));

    private static final LayoutResource<HandleConfig> STAMP_HANDLE_RECT_CONFIG = LayoutResource.of(
            ExhibitionPortal.id("textures/gui/stamp_handle_rect.json"), HandleConfig.READER
    );

    public static void setEditingExhibition(Exhibition exhibition, ExhibitionStamp stamp) {
        syncEditingExhibition();
        EDITING_EXHIBITION.set(new EditingStamp(exhibition, stamp));
    }

    public static void clearEditingExhibition() {
        syncEditingExhibition();
        EDITING_EXHIBITION.set(null);
    }

    public static void syncEditingExhibition() {
        EditingStamp editingStamp = RenderAccess.get(EDITING_EXHIBITION);
        if (editingStamp != null) {
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null) {
                conn.send(new UpdateExhibitionStampPacket(editingStamp.exhibition.uuid(), editingStamp.stamp));
                RenderAccess.get(GALLERY_LOOKUP).computeIfPresent(editingStamp.exhibition.uuid(), (_, exhibition) ->
                        new Exhibition(exhibition.declaration(), exhibition.metadata(), exhibition.footprint().withStamp(editingStamp.stamp))
                );
            }
        }
    }

    public static final class StampRender {
        private final Exhibition exhibition;
        private final ExhibitionStamp stamp;
        private final Rectangle rectangle;
        private final float rotate;
        private final @Nullable TextureMetadata handle;
        private final @Nullable HandleConfig config;

        private StampRender(Exhibition exhibition, ExhibitionStamp stamp, Rectangle map, @Nullable TextureMetadata handle, HandleConfig config) {
            this.exhibition = exhibition;
            this.stamp = stamp;
            this.rectangle = resolveIn(stamp.location(), map);
            this.rotate = stamp.rotate();
            this.handle = handle;
            this.config = config;
        }

        public Identifier item() {
            return stamp.item();
        }

        public Rectangle rectangle() {
            return rectangle;
        }

        public float rotate() {
            return rotate;
        }

        @Nullable
        public TextureMetadata handle() {
            return handle;
        }
    }

    private static Rectangle resolveIn(Rectangle portion, Rectangle container) {
        return new Rectangle(
                container.x() + container.w() * portion.x(),
                container.y() + container.h() * portion.y(),
                container.w() * portion.w(),
                container.h() * portion.h()
        );
    }

    private static Rectangle resolveBy(Rectangle inner, Rectangle container) {
        return new Rectangle(
                (inner.x() - container.x()) / container.w(),
                (inner.y() - container.y()) / container.h(),
                inner.w() / container.w(),
                inner.h() / container.h()
        );
    }

    public static final class EditingStamp {
        private final Exhibition exhibition;
        private ExhibitionStamp stamp;

        public EditingStamp(Exhibition exhibition, ExhibitionStamp stamp) {
            this.exhibition = exhibition;
            this.stamp = stamp;
        }

        public Exhibition exhibition() {
            return exhibition;
        }

        public ExhibitionStamp stamp() {
            return stamp;
        }
    }

    private static final LayoutParameter<@Nullable EditingStamp> EDITING_EXHIBITION = LayoutParameter.of(null);

    public static final LayoutBinding<List<StampRender>> RENDERED_STAMPS = LayoutBinding.of(
            () -> List.of(GALLERY_LOOKUP, EDITING_EXHIBITION, MapLayouts.MAP_AREA, STAMP_HANDLE_RECT, STAMP_HANDLE_RECT_CONFIG),
            context -> {
                Rectangle map = context.get(MapLayouts.MAP_AREA);

                List<StampRender> renders = new ArrayList<>();
                EditingStamp editingStamp = context.get(EDITING_EXHIBITION);
                for (Exhibition exhibition : context.get(GALLERY_LOOKUP).values()) {
                    for (ExhibitionStamp stamp : exhibition.footprint().stamps()) {
                        if (editingStamp != null && editingStamp.stamp().id().equals(stamp.id())) {
                            continue;
                        }

                        renders.add(new StampRender(exhibition, stamp, map, null, null));
                    }
                }
                if (editingStamp != null) {
                    renders.add(new StampRender(
                            editingStamp.exhibition(), editingStamp.stamp(), map,
                            context.get(STAMP_HANDLE_RECT), context.get(STAMP_HANDLE_RECT_CONFIG)
                    ));
                }
                return renders;
            }
    );

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYER_STAMP_HANDLE;
            }

            @Override
            public Class<? extends Screen> screen() {
                return StampScreen.class;
            }

            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                try {
                    EditingStamp editing = RenderAccess.get(EDITING_EXHIBITION);

                    for (StampRender render : RenderAccess.get(RENDERED_STAMPS).reversed()) {
                        Pos center = render.rectangle.center();
                        Vector2f vec = new Vector2f((float) event.x(), (float) event.y())
                                .sub(center.x(), center.y())
                                .mul(new Matrix2f().rotate(-render.rotate))
                                .add(center.x(), center.y());
                        boolean contains = render.rectangle().contains(vec.x, vec.y);
                        if (contains) {
                            if (editing == null || !editing.exhibition.uuid().equals(render.exhibition.uuid()) || !editing.stamp.id().equals(render.stamp.id())) {
                                setEditingExhibition(render.exhibition, render.stamp);
                            }
                        }

                        if (render.config != null && editing != null) {
                            if (resolveIn(render.config.move, render.rectangle).contains(vec.x, vec.y)) {
                                Rectangle previous = editing.stamp.location();
                                DragContext.begin(new DragContext.IContext() {
                                    @Override
                                    public boolean onMouseMove(Pos mouse) {
                                        if (RenderAccess.get(EDITING_EXHIBITION) != editing) {
                                            return true;
                                        }

                                        Rectangle map = RenderAccess.get(MapLayouts.MAP_AREA);
                                        Vector2f vec = new Vector2f(mouse.x(), mouse.y()).sub((float) event.x(), (float) event.y());
                                        Rectangle absolute = resolveIn(previous, map);
                                        absolute = new Rectangle(absolute.x() + vec.x, absolute.y() + vec.y, absolute.w(), absolute.h());
                                        Rectangle portion = resolveBy(absolute, map);

                                        editing.stamp = editing.stamp.withLocation(portion);
                                        EDITING_EXHIBITION.markDirty();
                                        return false;
                                    }

                                    @Override
                                    public boolean onMouseScroll(float deltaX) {
                                        return DragContext.IContext.super.onMouseScroll(deltaX);
                                    }

                                    @Override
                                    public void close() {
                                        syncEditingExhibition();
                                    }
                                });
                                return new Layers.IEventResult.Consumed();
                            }

                            if (resolveIn(render.config.rotate, render.rectangle).contains(vec.x, vec.y)) {
                                Rectangle previous = editing.stamp.location();
                                float previousRotation = editing.stamp.rotate();
                                Pos previousCenter = resolveIn(previous, RenderAccess.get(MapLayouts.MAP_AREA)).center();
                                float previousMouseRotation = new Vector2f((float) event.x(), (float) event.y())
                                        .sub(previousCenter.x(), previousCenter.y())
                                        .angle(new Vector2f(0, 1));

                                DragContext.begin(new DragContext.IContext() {
                                    @Override
                                    public boolean onMouseMove(Pos mouse) {
                                        if (RenderAccess.get(EDITING_EXHIBITION) != editing) {
                                            return true;
                                        }

                                        Pos center = resolveIn(previous, RenderAccess.get(MapLayouts.MAP_AREA)).center();
                                        float currentMouseRotation = new Vector2f(mouse.x(), mouse.y())
                                                .sub(center.x(), center.y())
                                                .angle(new Vector2f(0, 1));

                                        editing.stamp = editing.stamp.withRotate(previousRotation + previousMouseRotation - currentMouseRotation);
                                        EDITING_EXHIBITION.markDirty();
                                        return false;
                                    }

                                    @Override
                                    public boolean onMouseScroll(float deltaX) {
                                        return DragContext.IContext.super.onMouseScroll(deltaX);
                                    }

                                    @Override
                                    public void close() {
                                        syncEditingExhibition();
                                    }
                                });
                                return new Layers.IEventResult.Consumed();
                            }

                            if (resolveIn(render.config.scale, render.rectangle).contains(vec.x, vec.y)) {
                                Rectangle previous = editing.stamp.location();
                                float rotation = editing.stamp.rotate();

                                DragContext.begin(new DragContext.IContext() {
                                    @Override
                                    public boolean onMouseMove(Pos mouse) {
                                        if (RenderAccess.get(EDITING_EXHIBITION) != editing) {
                                            return true;
                                        }

                                        Rectangle map = RenderAccess.get(MapLayouts.MAP_AREA);
                                        Rectangle prevAbs = resolveIn(previous, map);
                                        Pos center = prevAbs.center();

                                        Vector2f initialLocal = new Vector2f((float) event.x(), (float) event.y())
                                                .sub(center.x(), center.y())
                                                .mul(new Matrix2f().rotate(-rotation));

                                        Vector2f currentLocal = new Vector2f(mouse.x(), mouse.y())
                                                .sub(center.x(), center.y())
                                                .mul(new Matrix2f().rotate(-rotation));

                                        float initDistX = Math.abs(initialLocal.x);
                                        float initDistY = Math.abs(initialLocal.y);

                                        float scaleFactor = 1.0f;
                                        if (initDistX > 1e-4f || initDistY > 1e-4f) {
                                            if (initDistX >= initDistY) {
                                                scaleFactor = currentLocal.x / initialLocal.x;
                                            } else {
                                                scaleFactor = currentLocal.y / initialLocal.y;
                                            }
                                        }

                                        float minSize = 2.0f;
                                        float newW = Math.max(minSize, prevAbs.w() * scaleFactor);
                                        float newH = Math.max(minSize * (prevAbs.h() / prevAbs.w()), prevAbs.h() * scaleFactor);

                                        if (newW / prevAbs.w() != newH / prevAbs.h()) {
                                            float actualScale = Math.max(newW / prevAbs.w(), newH / prevAbs.h());
                                            newW = prevAbs.w() * actualScale;
                                            newH = prevAbs.h() * actualScale;
                                        }

                                        newW = Math.clamp(newW, 20, 200);
                                        newH = Math.clamp(newH, 20, 200);
                                        float newX = center.x() - newW / 2.0f;
                                        float newY = center.y() - newH / 2.0f;

                                        Rectangle absolute = new Rectangle(newX, newY, newW, newH);
                                        Rectangle portion = resolveBy(absolute, map);

                                        editing.stamp = editing.stamp.withLocation(portion);
                                        EDITING_EXHIBITION.markDirty();
                                        return false;
                                    }

                                    @Override
                                    public boolean onMouseScroll(float deltaX) {
                                        return DragContext.IContext.super.onMouseScroll(deltaX);
                                    }

                                    @Override
                                    public void close() {
                                        syncEditingExhibition();
                                    }
                                });
                                return new Layers.IEventResult.Consumed();
                            }

                            clearEditingExhibition();
                            return new Layers.IEventResult.Miss();
                        }

                        if (contains) {
                            return new Layers.IEventResult.Consumed();
                        }
                    }

                    return new Layers.IEventResult.Miss();
                } catch (LayoutFailureException _) {
                    return new Layers.IEventResult.Miss();
                }
            }
        });
    }
}
