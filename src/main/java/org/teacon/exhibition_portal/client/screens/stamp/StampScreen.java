package org.teacon.exhibition_portal.client.screens.stamp;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.teacon.exhibition_portal.client.framework.binding.LayoutFailureException;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.render.AbstractEPScreen;
import org.teacon.exhibition_portal.client.screens.MapLayouts;
import org.teacon.exhibition_portal.client.screens.map.WaypointLayouts;

import static org.teacon.exhibition_portal.client.screens.MapLayouts.MAP;
import static org.teacon.exhibition_portal.client.screens.MapLayouts.MAP_AREA;
import static org.teacon.exhibition_portal.client.screens.map.WaypointLayouts.WAYPOINTS;

@SuppressWarnings("SimplifyOptionalCallChains")
public class StampScreen extends AbstractEPScreen {
    public StampScreen() {
        super(Component.translatable("exhibition_portal.title"));
    }

    @Override
    protected void render(@NonNull GuiGraphicsExtractor graphics) throws LayoutFailureException {
        GpuSampler LINEAR_CLAMP = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GpuSampler NEAREST_CLAMP = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        graphics.enableScissor(RenderAccess.get(MapLayouts.MAP_BOX));
        try {
            graphics.blit(RenderAccess.get(MAP), LINEAR_CLAMP, RenderAccess.get(MAP_AREA));

            for (StampScreenLayouts.StampRender render : RenderAccess.get(StampScreenLayouts.RENDERED_STAMPS)) {
                graphics.pose().pushMatrix();
                try {
                    graphics.pose().rotateAbout(render.rotate(), render.rectangle().center().x(), render.rectangle().center().y());

                    Holder.Reference<Item> item = BuiltInRegistries.ITEM.get(render.item()).orElse(null);
                    if (item != null) {
                        graphics.pose().pushMatrix();
                        try {
                            graphics.pose()
                                    .translate(render.rectangle().x(), render.rectangle().y())
                                    .scale(render.rectangle().w() / 16);
                            graphics.fakeItem(new ItemStack(item), 0, 0);
                        } finally {
                            graphics.pose().popMatrix();
                        }
                    } else {
                        graphics.blit(render.item(), NEAREST_CLAMP, render.rectangle());
                    }

                    if (render.handle() != null) {
                        graphics.blit(render.handle().textureView(), NEAREST_CLAMP, render.rectangle());
                    }
                } finally {
                    graphics.pose().popMatrix();
                }
            }

            for (WaypointLayouts.WaypointRender waypoint : RenderAccess.get(WAYPOINTS)) {
                graphics.blit(waypoint.texture(), LINEAR_CLAMP, waypoint.rectangle(), waypoint.uv());
            }
        } finally {
            graphics.disableScissor();
        }
    }
}
