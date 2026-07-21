package org.teacon.exhibition_portal.client.screens.stamp;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.teacon.exhibition_portal.client.framework.binding.LayoutFailureException;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.render.AbstractEPScreen;
import org.teacon.exhibition_portal.client.screens.MapLayouts;
import org.teacon.exhibition_portal.client.screens.map.WaypointLayouts;

import java.util.Objects;

import static org.teacon.exhibition_portal.client.screens.MapLayouts.MAP;
import static org.teacon.exhibition_portal.client.screens.MapLayouts.MAP_AREA;
import static org.teacon.exhibition_portal.client.screens.map.WaypointLayouts.WAYPOINTS;

public class StampScreen extends AbstractEPScreen {
    public StampScreen() {
        super(Component.translatable("exhibition_portal.title"));
    }

    @Override
    protected void render(@NonNull GuiGraphicsExtractor graphics) throws LayoutFailureException {
        GpuSampler LINEAR_CLAMP = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        graphics.enableScissor(RenderAccess.get(MapLayouts.MAP_BOX));
        try {
            graphics.blit(RenderAccess.get(MAP), LINEAR_CLAMP, RenderAccess.get(MAP_AREA));

            for (WaypointLayouts.WaypointRender waypoint : RenderAccess.get(WAYPOINTS)) {
                graphics.blit(waypoint.texture(), LINEAR_CLAMP, waypoint.rectangle(), waypoint.uv());
            }

            for (StampScreenLayouts.StampRender render : RenderAccess.get(StampScreenLayouts.RENDERED_STAMPS)) {
                graphics.pose().pushMatrix();
                try {
                    graphics.pose().rotateAbout(render.rotate(), render.rectangle().center().x(), render.rectangle().center().y());
                    graphics.blit(render.texture(), LINEAR_CLAMP, render.rectangle());
                } finally {
                    graphics.pose().popMatrix();
                }
            }

            StampScreenLayouts.EditingStamp stamp = RenderAccess.get(StampScreenLayouts.EDITING_EXHIBITION);
            if (stamp != null) {
                graphics.pose().pushMatrix();
                try {
                    Rectangle rec = Objects.requireNonNull(RenderAccess.get(StampScreenLayouts.EDITING_EXHIBITION_LOCATION));
                    graphics.pose().rotateAbout(stamp.stamp().rotate(), rec.center().x(), rec.center().y());
                } finally {
                    graphics.pose().popMatrix();
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }
}
