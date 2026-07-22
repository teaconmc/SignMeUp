package org.teacon.exhibition_portal.client.screens;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.framework.DragContext;
import org.teacon.exhibition_portal.client.framework.GeneralLayouts;
import org.teacon.exhibition_portal.client.framework.Layers;
import org.teacon.exhibition_portal.client.framework.binding.LayoutBinding;
import org.teacon.exhibition_portal.client.framework.binding.LayoutParameter;
import org.teacon.exhibition_portal.client.framework.binding.LayoutResource;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Pos;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.components.TextureMetadata;
import org.teacon.exhibition_portal.client.framework.render.AbstractEPScreen;
import org.teacon.exhibition_portal.client.screens.map.MapScreen;
import org.teacon.exhibition_portal.client.screens.map.MapScreenLayouts;
import org.teacon.exhibition_portal.client.screens.stamp.StampScreen;
import org.teacon.exhibition_portal.client.screens.stamp.StampScreenLayouts;
import org.teacon.exhibition_portal.network.TeleportToPositionPacket;

import java.io.BufferedReader;
import java.util.List;

import static org.teacon.exhibition_portal.client.framework.GeneralLayouts.CURRENT_SCREEN;
import static org.teacon.exhibition_portal.client.framework.GeneralLayouts.PLAYER_POS;
import static org.teacon.exhibition_portal.client.framework.GeneralLayouts.PLAYER_ROT;

@EventBusSubscriber(Dist.CLIENT)
public final class MapLayouts {
    private MapLayouts() {
    }

    public static final LayoutBinding<TextureMetadata> MAP = TextureMetadata.of(ExhibitionPortal.id("textures/gui/map.png"));

    private record MapConfig(Rectangle coordinateFull, Rectangle coordinateEssential) {
    }

    private static final LayoutResource<MapConfig> MAP_CONFIG = LayoutResource.of(ExhibitionPortal.id("textures/gui/map.json"), id -> {
        record X01Y01(
                @SerializedName("x0") int x0,
                @SerializedName("x1") int x1,
                @SerializedName("y0") int y0,
                @SerializedName("y1") int y1
        ) {
            Rectangle cast() {
                return new Rectangle(x0, y0, x1 - x0, y1 - y0);
            }
        }
        record Coordinates(
                @SerializedName("full") X01Y01 full,
                @SerializedName("essential") X01Y01 essential
        ) {
        }

        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(id)) {
            Coordinates coordinates = ExhibitionPortal.GSON.fromJson(reader, Coordinates.class);
            return new MapConfig(coordinates.full.cast(), coordinates.essential.cast());
        }
    });

    public static final LayoutBinding<Rectangle> COORDINATES_FULL = LayoutBinding.of(
            () -> List.of(MAP_CONFIG),
            context -> context.get(MAP_CONFIG).coordinateFull
    );

    private static final LayoutBinding<Rectangle> COORDINATES_ESSENTIAL = LayoutBinding.of(
            () -> List.of(MAP_CONFIG),
            context -> context.get(MAP_CONFIG).coordinateEssential
    );

    public static final LayoutBinding<Rectangle> MAP_BOX = LayoutBinding.of(
            () -> List.of(CURRENT_SCREEN, MapScreenLayouts.MAP_BOX, StampScreenLayouts.MAP_BOX),
            context -> switch (context.get(GeneralLayouts.CURRENT_SCREEN)) {
                case MapScreen _ -> context.get(MapScreenLayouts.MAP_BOX);
                case StampScreen _ -> context.get(StampScreenLayouts.MAP_BOX);
                case null, default -> null;
            }
    );

    public static final LayoutParameter<Rectangle> MAP_AREA = LayoutParameter.of(
            new Rectangle(-1, -1, 0, 0)
    );

    public static final LayoutResource<TextureMetadata> ME_UI_IMAGE = TextureMetadata.of(ExhibitionPortal.id("textures/gui/me_map.png"));

    public record MeUI(Rectangle area, float rotate) {
    }

    public static final LayoutBinding<@Nullable MeUI> ME_UI = LayoutBinding.of(
            () -> List.of(PLAYER_POS, PLAYER_ROT, MAP_AREA, MAP_CONFIG),
            context -> {
                Vector3f pos = context.get(PLAYER_POS);
                Vector2f rot = context.get(PLAYER_ROT);
                if (pos == null || rot == null) {
                    return null;
                }

                Rectangle map = context.get(MAP_AREA);
                Rectangle config = context.get(MAP_CONFIG).coordinateFull;
                float x = (pos.x - config.x()) / config.w();
                float y = (pos.z - config.y()) / config.h();
                if (!(x >= 0 && x <= 1 && y >= 0 && y <= 1)) {
                    return null;
                }

                x = map.x() + map.w() * x;
                y = map.y() + map.h() * y;
                float rotate = (float) (Math.toRadians(rot.y) + Mth.PI);
                float SIZE = 15;

                return new MeUI(new Rectangle(x - SIZE, y - SIZE, SIZE * 2, SIZE * 2), rotate);
            }
    );

    public record MapDragContext(float relativeX, float relativeY) implements DragContext.IContext {
        @Override
        public boolean onMouseMove(Pos mouse) {
            Rectangle area = RenderAccess.get(MAP_AREA);
            area = new Rectangle(
                    mouse.x() - relativeX, mouse.y() - relativeY,
                    area.w(), area.h()
            );
            MAP_AREA.set(clampViewport(area));

            return false;
        }
    }

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYER_MAP;
            }

            private long last = -1;

            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                Rectangle map = RenderAccess.get(MAP_BOX, null);
                float mouseX = (float) event.x(), mouseY = (float) event.y();

                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && map != null && map.contains(mouseX, mouseY)) {
                    Rectangle area = RenderAccess.get(MAP_AREA);
                    if (System.currentTimeMillis() - last < 500) {
                        last = -1;
                        ClientPacketListener conn = Minecraft.getInstance().getConnection();
                        MapConfig box = RenderAccess.get(MAP_CONFIG, null);
                        if (conn != null && box != null) {
                            Rectangle boxArea = box.coordinateFull;
                            float x = boxArea.x() + (mouseX - area.x()) / area.w() * boxArea.w();
                            float z = boxArea.y() + (mouseY - area.y()) / area.h() * boxArea.h();
                            conn.send(new TeleportToPositionPacket(Math.round(x), Math.round(z)));
                        }
                        return new Layers.IEventResult.Consumed();
                    }
                    last = System.currentTimeMillis();

                    DragContext.begin(new MapDragContext(mouseX - area.x(), mouseY - area.y()));
                    return new Layers.IEventResult.Consumed();
                }
                return new Layers.IEventResult.Miss();
            }

            @Override
            public Layers.IEventResult onMouseScrolled(Pos mouse, float deltaY) {
                Rectangle area = RenderAccess.get(MAP_AREA), background = RenderAccess.get(MAP_BOX, null);
                if (background != null && area.contains(mouse) && background.contains(mouse)) {
                    float k = (float) Math.exp(deltaY * 0.2f);
                    MAP_AREA.set(clampViewport(new Rectangle(
                            mouse.x() - (mouse.x() - area.x()) * k,
                            mouse.y() - (mouse.y() - area.y()) * k,
                            area.w() * k,
                            area.h() * k
                    )));
                    return new Layers.IEventResult.Consumed();
                }
                return new Layers.IEventResult.Miss();
            }
        });
    }

    private static Rectangle clampViewport(Rectangle area) {
        Rectangle background = RenderAccess.get(MAP_BOX, null);
        if (background != null) {
            Pos center = background.center();
            if (!area.contains(center)) {
                if (area.x() >= center.x()) {
                    area = new Rectangle(center.x(), area.y(), area.w(), area.h());
                } else if (area.x() + area.w() <= center.x()) {
                    area = new Rectangle(center.x() - area.w(), area.y(), area.w(), area.h());
                }

                if (area.y() >= center.y()) {
                    area = new Rectangle(area.x(), center.y(), area.w(), area.h());
                } else if (area.y() + area.h() <= center.y()) {
                    area = new Rectangle(area.x(), center.y() - area.h(), area.w(), area.h());
                }
            }
        }
        return area;
    }


    @SubscribeEvent
    private static void on(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AbstractEPScreen) {
            Rectangle background = RenderAccess.get(MAP_BOX, null);
            Rectangle essential = RenderAccess.get(COORDINATES_ESSENTIAL, null), full = RenderAccess.get(COORDINATES_FULL, null);

            if (background != null && essential != null && full != null && !RenderAccess.get(MAP_AREA).contains(background.center())) {
                Rectangle target = computeEssentialViewport(background, essential);
                float k = target.w() / essential.w(), k2 = target.h() / essential.h();
                if (Math.abs(k - k2) >= 0.01D) {
                    throw new AssertionError(String.format("Illegal resize: %s -> %s", essential, target));
                }

                MAP_AREA.set(new Rectangle(
                        target.x() - (essential.x() - full.x()) * k,
                        target.y() - (essential.y() - full.y()) * k,
                        full.w() * k,
                        full.h() * k
                ));
            }
        }
    }

    private static Rectangle computeEssentialViewport(Rectangle background, Rectangle essential) {
        float r1 = background.ratio(), r2 = essential.ratio();
        if (r1 >= r2) {
            float w = background.h() * r2;
            return new Rectangle(
                    background.x() + background.w() / 2f - w / 2f, background.y(),
                    w, background.h()
            );
        } else {
            float h = background.w() / r2;
            return new Rectangle(
                    background.x(), background.y() + background.h() / 2f - h / 2f,
                    background.w(), h
            );
        }
    }
}
