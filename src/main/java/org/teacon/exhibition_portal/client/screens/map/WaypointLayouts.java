package org.teacon.exhibition_portal.client.screens.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.lang3.ArrayUtils;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.framework.Layers;
import org.teacon.exhibition_portal.client.framework.binding.LayoutBinding;
import org.teacon.exhibition_portal.client.framework.binding.LayoutResource;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Pos;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.components.TextureMetadata;
import org.teacon.exhibition_portal.client.framework.components.UVSource;
import org.teacon.exhibition_portal.client.screens.LayerPriority;
import org.teacon.exhibition_portal.client.screens.MapLayouts;
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.components.ExhibitionWaypoint;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.teacon.exhibition_portal.client.EPClient.GALLERIES;
import static org.teacon.exhibition_portal.client.EPClient.GALLERY_LOOKUP;
import static org.teacon.exhibition_portal.client.framework.GeneralLayouts.WINDOW_HEIGHT;

/* package-private */ public final class WaypointLayouts {
    private WaypointLayouts() {
    }

    public static final LayoutResource<TextureMetadata> WAYPOINT_TEXTURE = TextureMetadata.of(ExhibitionPortal.id("textures/gui/waypoints.png"));

    public static final LayoutResource<String[]> WAYPOINT_TYPES = LayoutResource.of(
            ExhibitionPortal.id("textures/gui/waypoints.json"),
            identifier -> {
                try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(identifier)) {
                    return ExhibitionPortal.GSON.fromJson(reader, String[].class);
                }
            }
    );

    public record WaypointRender(Exhibition exhibition, Rectangle rectangle, TextureMetadata texture, UVSource uv) {
    }

    public static final LayoutBinding<List<WaypointRender>> WAYPOINTS = LayoutBinding.of(
            () -> List.of(GALLERIES, GALLERY_LOOKUP, MapLayouts.MAP_AREA, MapLayouts.COORDINATES_FULL, WAYPOINT_TYPES, WINDOW_HEIGHT, WAYPOINT_TEXTURE),
            context -> {
                List<UUID> galleries = context.get(GALLERIES);
                Map<UUID, Exhibition> lookup = context.get(GALLERY_LOOKUP);

                Rectangle map = context.get(MapLayouts.MAP_AREA), coordinates = context.get(MapLayouts.COORDINATES_FULL);
                String[] waypoints = context.get(WAYPOINT_TYPES);
                TextureMetadata texture = context.get(WAYPOINT_TEXTURE);

                List<WaypointRender> renders = new ArrayList<>(galleries.size());
                float r = context.get(WINDOW_HEIGHT) * 0.02144f;
                for (UUID uuid : galleries) {
                    Exhibition exhibition = lookup.get(uuid);

                    ExhibitionWaypoint waypoint = exhibition.metadata().waypoint();
                    float v = Math.clamp(Arrays.asList(waypoints).indexOf(exhibition.footprint().mark()), 0, waypoints.length - 1);
                    renders.add(new WaypointRender(
                            exhibition,
                            new Rectangle(
                                    map.x() + (waypoint.x() - coordinates.x()) * map.w() / coordinates.w() - r / 2,
                                    map.y() + (waypoint.z() - coordinates.y()) * map.h() / coordinates.h() - r / 2,
                                    r, r
                            ),
                            texture,
                            new UVSource(v / waypoints.length, (v + 1) / waypoints.length, 0, 1)
                    ));
                }
                return renders;
            }
    );

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYER_WAYPOINTS;
            }

            private UUID previousUUID;
            private long previousTimestamp;

            @Override
            public Class<? extends Screen> screen() {
                return MapScreen.class;
            }

            @Override
            public Layers.IEventResult onMouseMove(Pos mouse) {
                for (WaypointLayouts.WaypointRender render : RenderAccess.get(WaypointLayouts.WAYPOINTS, List.of())) {
                    if (!render.rectangle().contains(mouse)) {
                        continue;
                    }

                    if (Objects.equals(previousUUID, render.exhibition().uuid())) {
                        if (System.currentTimeMillis() - previousTimestamp >= 200) {
                            SelectionLayouts.scrollTo(render.exhibition().uuid());
                        }
                    } else {
                        previousUUID = render.exhibition().uuid();
                        previousTimestamp = System.currentTimeMillis();
                    }
                    return new Layers.IEventResult.Consumed();
                }

                previousUUID = null;
                previousTimestamp = -1;
                return new Layers.IEventResult.Miss();
            }
        });
    }
}
