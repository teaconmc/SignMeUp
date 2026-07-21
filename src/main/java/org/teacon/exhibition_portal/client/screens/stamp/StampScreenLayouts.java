package org.teacon.exhibition_portal.client.screens.stamp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.framework.Layers;
import org.teacon.exhibition_portal.client.framework.binding.LayoutBinding;
import org.teacon.exhibition_portal.client.framework.binding.LayoutFailureException;
import org.teacon.exhibition_portal.client.framework.binding.LayoutParameter;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.components.TextureMetadata;
import org.teacon.exhibition_portal.client.screens.LayerPriority;
import org.teacon.exhibition_portal.client.screens.MapLayouts;
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.components.ExhibitionStamp;
import org.teacon.exhibition_portal.network.UpdateExhibitionStampPacket;

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

    public static final LayoutBinding<TextureMetadata> STAMP_BORDER_RECT = TextureMetadata.of(ExhibitionPortal.id("textures/gui/stamp_handle_rect.png"));

    public static final LayoutBinding<TextureMetadata> STAMP_BORDER_ROUND = TextureMetadata.of(ExhibitionPortal.id("textures/gui/stamp_handle_round.png"));

    public record EditingStamp(Exhibition exhibition, ExhibitionStamp stamp) {
    }

    public static final LayoutParameter<@Nullable EditingStamp> EDITING_EXHIBITION = LayoutParameter.of(null);

    public static void setEditingExhibition(Exhibition exhibition, ExhibitionStamp stamp) {
        EditingStamp editingStamp = RenderAccess.get(EDITING_EXHIBITION);
        if (editingStamp != null) {
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null) {
                conn.send(new UpdateExhibitionStampPacket(exhibition.uuid(), stamp));
            }
        }
        EDITING_EXHIBITION.set(new EditingStamp(exhibition, stamp));
    }

    public static final LayoutBinding<@Nullable Rectangle> EDITING_EXHIBITION_LOCATION = LayoutBinding.of(
            () -> List.of(MapLayouts.MAP_AREA, EDITING_EXHIBITION),
            context -> {
                EditingStamp stamp = context.get(EDITING_EXHIBITION);
                return stamp == null ? null : resolveIn(stamp.stamp().location(), context.get(MapLayouts.MAP_AREA));
            }
    );

    public record StampRender(Identifier texture, Rectangle rectangle, float rotate, @Nullable TextureMetadata handle) {
        private StampRender(Exhibition exhibition, ExhibitionStamp stamp, Rectangle map, @Nullable TextureMetadata handle) {
            this(
                    ExhibitionPortal.id(String.format("textures/gui/stamps/%s/%s.png", exhibition.declaration().uuid(), stamp.id())),
                    resolveIn(stamp.location(), map), stamp.rotate(),
                    handle
            );
        }
    }

    @NonNull
    private static Rectangle resolveIn(Rectangle portion, Rectangle container) {
        return new Rectangle(
                container.x() + container.w() * portion.x(),
                container.y() + container.h() * portion.y(),
                container.w() * portion.w(),
                container.h() * portion.h()
        );
    }

    public static final LayoutBinding<List<StampRender>> RENDERED_STAMPS = LayoutBinding.of(
            () -> List.of(GALLERY_LOOKUP, EDITING_EXHIBITION, MapLayouts.MAP_AREA, STAMP_BORDER_RECT, STAMP_BORDER_ROUND),
            context -> {
                Rectangle map = context.get(MapLayouts.MAP_AREA);

                List<StampRender> renders = new ArrayList<>();
                EditingStamp editingStamp = context.get(EDITING_EXHIBITION);
                for (Exhibition exhibition : context.get(GALLERY_LOOKUP).values()) {
                    for (ExhibitionStamp stamp : exhibition.footprint().stamps()) {
                        if (editingStamp != null && editingStamp.stamp().id().equals(stamp.id())) {
                            continue;
                        }

                        renders.add(new StampRender(exhibition, stamp, map, null));
                    }
                }
                if (editingStamp != null) {
                    renders.add(new StampRender(
                            editingStamp.exhibition(), editingStamp.stamp(), map,
                            context.get(editingStamp.stamp().isRectangle() ? STAMP_BORDER_RECT : STAMP_BORDER_ROUND)
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
                    EditingStamp stamp = RenderAccess.get(EDITING_EXHIBITION);
                    if (stamp == null) {
                        return new Layers.IEventResult.Miss();
                    }

                    Rectangle rectangle = Objects.requireNonNull(RenderAccess.get(EDITING_EXHIBITION_LOCATION));

                    return new Layers.IEventResult.Miss();
                } catch (LayoutFailureException _) {
                    return new Layers.IEventResult.Miss();
                }
            }
        });
    }
}
