package org.teacon.exhibition_portal.client.screens.map;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.Nullable;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.framework.Layers;
import org.teacon.exhibition_portal.client.framework.binding.LayoutBinding;
import org.teacon.exhibition_portal.client.framework.binding.LayoutParameter;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Pos;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.components.TextureMetadata;
import org.teacon.exhibition_portal.client.screens.LayerPriority;
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.network.TeleportToExhibitionPacket;
import org.teacon.exhibition_portal.network.UpdateExhibitionMarkPacket;
import org.teacon.exhibition_portal.utils.Components;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.teacon.exhibition_portal.client.EPClient.GALLERY_LOOKUP;
import static org.teacon.exhibition_portal.client.screens.map.MapScreenLayouts.MAP_BOX;
import static org.teacon.exhibition_portal.client.screens.map.MapScreenLayouts.SELECTION_BOX;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.GALLERY_HEIGHT;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.GALLERY_HOVER;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.GALLERY_RENDERS;
import static org.teacon.exhibition_portal.client.screens.map.WaypointLayouts.WAYPOINT_TYPES;

@EventBusSubscriber(Dist.CLIENT)
/* package-private */ final class DetailLayouts {
    private DetailLayouts() {
    }

    public static final LayoutBinding<Rectangle> DETAIL_SELECTION_OUTLINE = LayoutBinding.of(
            () -> List.of(GALLERY_HOVER, GALLERY_RENDERS, GALLERY_HEIGHT, SELECTION_BOX),
            context -> {
                UUID uuid = context.get(GALLERY_HOVER);
                SelectionLayouts.ExhibitionRender render = context.get(GALLERY_RENDERS).stream()
                        .filter(r -> Objects.equals(uuid, r.exhibition().uuid()))
                        .findFirst()
                        .orElse(null);
                if (render == null) {
                    return new Rectangle(0, 0, 0, 0);
                }

                float exhibitionHeight = context.get(GALLERY_HEIGHT);
                Rectangle selectionBox = context.get(SELECTION_BOX), renderBox = render.box();
                float x = (selectionBox.x() * 2 + renderBox.x()) / 3f;
                return new Rectangle(
                        x,
                        renderBox.y() - (exhibitionHeight - renderBox.h()) / 2,
                        selectionBox.x() + selectionBox.w() - x,
                        exhibitionHeight
                );
            }
    );

    public static final LayoutBinding<Float> DETAIL_MARGIN = LayoutBinding.of(
            () -> List.of(MAP_BOX),
            context -> context.get(MAP_BOX).w() * 0.02157f
    );

    public static final LayoutBinding<Float> DETAIL_X = LayoutBinding.of(
            () -> List.of(MAP_BOX, DETAIL_MARGIN),
            context -> context.get(MAP_BOX).x() + context.get(DETAIL_MARGIN)
    );

    public static final LayoutBinding<Float> DETAIL_W = LayoutBinding.of(
            () -> List.of(MAP_BOX, DETAIL_MARGIN),
            context -> context.get(MAP_BOX).w() - context.get(DETAIL_MARGIN) * 2
    );

    public static final LayoutBinding<Float> DETAIL_PADDING = LayoutBinding.of(
            () -> List.of(DETAIL_W),
            context -> context.get(DETAIL_W) * 0.03568f
    );

    public static final LayoutBinding<Float> DETAIL_TITLE_H = LayoutBinding.of(
            () -> List.of(DETAIL_W),
            context -> context.get(DETAIL_W) * 0.03568f
    );

    public static final LayoutBinding<Float> DETAIL_SPACE = LayoutBinding.of(
            () -> List.of(DETAIL_W),
            context -> context.get(DETAIL_W) * 0.02061f
    );

    public static final LayoutBinding<Float> DETAIL_DESC_LINE_HEIGHT = LayoutBinding.of(
            () -> List.of(DETAIL_W),
            context -> context.get(DETAIL_W) * 0.01648f
    );

    public static final LayoutBinding<Float> DETAIL_INNER_W = LayoutBinding.of(
            () -> List.of(DETAIL_W, DETAIL_PADDING),
            context -> context.get(DETAIL_W) - context.get(DETAIL_PADDING)
    );

    public static final LayoutBinding<List<FormattedCharSequence>> DETAIL_DESCRIPTIONS = LayoutBinding.of(
            () -> List.of(GALLERY_LOOKUP, DETAIL_INNER_W, GALLERY_HOVER, GALLERY_RENDERS, DETAIL_DESC_LINE_HEIGHT),
            context -> {
                Exhibition exhibition = context.get(GALLERY_LOOKUP).get(context.get(GALLERY_HOVER));
                if (exhibition == null) {
                    return List.of();
                }

                UnaryOperator<Style> withStyle = s -> Components.REGULAR.withItalic(true).withColor(ChatFormatting.GRAY);
                Component component = Components.join(
                        Component.literal(exhibition.metadata().introduction()).withStyle(Components.REGULAR),
                        Component.literal("\n\n").withStyle(Components.REGULAR),
                        Component.translatable("exhibition_portal.detail.key.evt").withStyle(withStyle),
                        Component.literal(": ").withStyle(withStyle),
                        MapScreenLayouts.ofEVT(exhibition.metadata()).withStyle(withStyle),
                        Component.literal("\n").withStyle(withStyle),
                        Component.translatable("exhibition_portal.detail_hint").withStyle(withStyle)
                );

                float w = context.get(DETAIL_INNER_W) / context.get(DETAIL_DESC_LINE_HEIGHT) * 11;
                return Minecraft.getInstance().font.split(component, Math.round(w));
            }
    );

    public static final LayoutBinding<Rectangle> DETAIL_BOX = LayoutBinding.of(
            () -> List.of(
                    MAP_BOX, DETAIL_SELECTION_OUTLINE,
                    DETAIL_X, DETAIL_W, DETAIL_PADDING, DETAIL_TITLE_H, DETAIL_SPACE, DETAIL_DESC_LINE_HEIGHT, DETAIL_DESCRIPTIONS
            ), context -> {
                Rectangle selectionOutline = context.get(DETAIL_SELECTION_OUTLINE);
                if (selectionOutline.h() == 0) {
                    return new Rectangle(0, 0, 0, 0);
                }

                Rectangle mapBox = context.get(MAP_BOX);
                float padding = context.get(DETAIL_PADDING);
                float h = mapBox.y()
                        + padding * 2
                        + context.get(DETAIL_TITLE_H)
                        + context.get(DETAIL_SPACE)
                        + context.get(DETAIL_DESC_LINE_HEIGHT) * context.get(DETAIL_DESCRIPTIONS).size();

                float y;
                if (selectionOutline.y() + selectionOutline.h() / 2 <= mapBox.y() + mapBox.h() / 2) {
                    y = Math.max(selectionOutline.y1() - h, mapBox.y() + padding);
                } else {
                    y = Math.min(selectionOutline.y(), mapBox.y() + mapBox.h() - padding - h);
                }
                return new Rectangle(context.get(DETAIL_X), y, context.get(DETAIL_W), h);
            }
    );

    public static final LayoutBinding<Rectangle> DETAIL_TITLE_BOX = LayoutBinding.of(
            () -> List.of(DETAIL_BOX, DETAIL_PADDING, DETAIL_INNER_W, DETAIL_TITLE_H),
            context -> {
                Rectangle outline = context.get(DETAIL_BOX);
                return new Rectangle(
                        outline.x() + context.get(DETAIL_PADDING) / 2,
                        outline.y() + context.get(DETAIL_PADDING),
                        context.get(DETAIL_INNER_W),
                        context.get(DETAIL_TITLE_H)
                );
            }
    );

    public static final LayoutBinding<Map<Rectangle, FormattedCharSequence>> DETAIL_DESC_BOX = LayoutBinding.of(
            () -> List.of(DETAIL_BOX, DETAIL_PADDING, DETAIL_INNER_W, DETAIL_SPACE, DETAIL_DESC_LINE_HEIGHT, DETAIL_DESCRIPTIONS),
            context -> {
                List<FormattedCharSequence> list = context.get(DETAIL_DESCRIPTIONS);
                Map<Rectangle, FormattedCharSequence> lines = new Object2ObjectArrayMap<>(list.size());
                Rectangle outline = context.get(DETAIL_BOX);
                float x = outline.x() + context.get(DETAIL_PADDING) / 2;
                float y = outline.y() + context.get(DETAIL_PADDING) + context.get(DETAIL_PADDING) + context.get(DETAIL_SPACE);
                for (int i = 0; i < list.size(); i++) {
                    lines.put(new Rectangle(
                            x, y + i * context.get(DETAIL_DESC_LINE_HEIGHT),
                            context.get(DETAIL_INNER_W),
                            context.get(DETAIL_DESC_LINE_HEIGHT) * 0.73f
                    ), list.get(i));
                }
                return lines;
            }
    );

    public static final LayoutBinding<Rectangle> BUTTON_MARK_AS = LayoutBinding.of(
            () -> List.of(DETAIL_BOX, WAYPOINT_TYPES),
            context -> {
                Rectangle box = context.get(DETAIL_BOX);
                float h = box.w() * 0.02537f;
                float w2 = h * context.get(WAYPOINT_TYPES).length;
                return new Rectangle(box.x() + box.w() - w2 - box.w() * 0.03965f, box.y() + box.w() * 0.04f, w2, h);
            }
    );

    public static final LayoutBinding<Rectangle> BUTTON_TELEPORT = LayoutBinding.of(
            () -> List.of(DETAIL_BOX, WAYPOINT_TYPES),
            context -> {
                Rectangle box = context.get(DETAIL_BOX);
                float h = box.w() * 0.02537f;
                float w2 = h * context.get(WAYPOINT_TYPES).length;
                return new Rectangle(box.x() + box.w() - w2 - box.w() * 0.20856f, box.y() + box.w() * 0.04f, w2, h);
            }
    );

    public static final LayoutBinding<TextureMetadata> BUTTON_NORMAL = TextureMetadata.of(ExhibitionPortal.id("textures/gui/button_normal.png"));
    public static final LayoutBinding<TextureMetadata> BUTTON_HOVER = TextureMetadata.of(ExhibitionPortal.id("textures/gui/button_hover.png"));
    public static final LayoutBinding<TextureMetadata> BUTTON_PRESSED = TextureMetadata.of(ExhibitionPortal.id("textures/gui/button_pressed.png"));

    public static final LayoutParameter<LayoutBinding<TextureMetadata>> BUTTON_TELEPORT_STYLE = LayoutParameter.of(BUTTON_NORMAL);

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYOUT_DETAIL_BTN;
            }

            @Override
            public Class<? extends Screen> screen() {
                return Static.super.screen();
            }

            @Override
            public Layers.IEventResult onMouseMove(Pos mouse) {
                boolean in = in(mouse);

                LayoutBinding<TextureMetadata> currentStyle = RenderAccess.get(BUTTON_TELEPORT_STYLE);
                if (currentStyle == BUTTON_NORMAL && in) {
                    BUTTON_TELEPORT_STYLE.set(BUTTON_HOVER);
                } else if (currentStyle != BUTTON_NORMAL && !in) {
                    BUTTON_TELEPORT_STYLE.set(BUTTON_NORMAL);
                }

                return in ? new Layers.IEventResult.Consumed() : new Layers.IEventResult.Miss();
            }

            private static boolean in(Pos mouse) {
                return RenderAccess.get(BUTTON_TELEPORT, Rectangle.EMPTY).contains(mouse);
            }

            private boolean in(MouseButtonEvent event) {
                return in(Pos.ofLossy(event.x(), event.y()));
            }

            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                if (in(event)) {
                    BUTTON_TELEPORT_STYLE.set(BUTTON_PRESSED);
                    return new Layers.IEventResult.Consumed();
                } else {
                    return new Layers.IEventResult.Miss();
                }
            }

            @Override
            public Layers.IEventResult onMouseClicked(MouseButtonEvent down, MouseButtonEvent up) {
                if (in(down) && in(up)) {
                    LocalPlayer player = Minecraft.getInstance().player;
                    UUID uuid = RenderAccess.get(GALLERY_HOVER);
                    if (player != null && uuid != null) {
                        player.connection.send(new TeleportToExhibitionPacket(uuid));
                        Minecraft.getInstance().setScreen(null);
                        return new Layers.IEventResult.Consumed();
                    }
                }
                return new Layers.IEventResult.Miss();
            }

            @Override
            public Layers.IEventResult onMouseButtonReleased(MouseButtonEvent event) {
                if (in(event)) {
                    BUTTON_TELEPORT_STYLE.set(BUTTON_HOVER);
                    return new Layers.IEventResult.Consumed();
                } else {
                    BUTTON_TELEPORT_STYLE.set(BUTTON_NORMAL);
                    return new Layers.IEventResult.Miss();
                }
            }
        });
    }

    public static final LayoutParameter<LayoutBinding<TextureMetadata>> BUTTON_MARK_STYLE = LayoutParameter.of(BUTTON_NORMAL);

    public static final LayoutParameter<@Nullable Range<Float>> BUTTON_MARK_HOVER_RANGE_STYLE = LayoutParameter.of(null);

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYOUT_DETAIL_BTN;
            }

            @Override
            public Class<? extends Screen> screen() {
                return MapScreen.class;
            }

            private static int index(float x, float y) {
                Rectangle box = RenderAccess.get(BUTTON_MARK_AS, Rectangle.EMPTY);
                if (!box.contains(x, y)) {
                    return -1;
                }
                int count = (int) ((x - box.x()) * RenderAccess.get(WAYPOINT_TYPES, new String[0]).length);
                int v = (int) (count / box.w());
                return Math.clamp(v, 0, count - 1);
            }

            @Nullable
            private static Range<Float> range(Pos mouse) {
                Rectangle box = RenderAccess.get(BUTTON_MARK_AS, Rectangle.EMPTY);
                if (!box.contains(mouse)) {
                    return null;
                }

                int count = RenderAccess.get(WAYPOINT_TYPES, new String[0]).length;
                int i = (int) ((mouse.x() - box.x()) * count / box.w());

                float min = i / (float) count, max = (i + 1) / (float) count;
                return Range.of(min < 0.1 ? 0 : min, max > 0.9 ? 1 : max);
            }

            @Override
            public Layers.IEventResult onMouseMove(Pos mouse) {
                Range<Float> range = range(mouse);
                BUTTON_MARK_STYLE.set(BUTTON_NORMAL);
                BUTTON_MARK_HOVER_RANGE_STYLE.set(range);
                return range != null ? new Layers.IEventResult.Consumed() : new Layers.IEventResult.Miss();
            }

            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                if (range(Pos.ofLossy(event.x(), event.y())) != null) {
                    BUTTON_MARK_STYLE.set(BUTTON_PRESSED);
                    BUTTON_MARK_HOVER_RANGE_STYLE.set(null);
                    return new Layers.IEventResult.Consumed();
                } else {
                    return new Layers.IEventResult.Miss();
                }
            }

            @Override
            public Layers.IEventResult onMouseClicked(MouseButtonEvent down, MouseButtonEvent up) {
                int index = index((float) down.x(), (float) down.y());
                if (index != -1 && index == index((float) up.x(), (float) up.y())) {
                    Minecraft minecraft = Minecraft.getInstance();
                    LocalPlayer player = minecraft.player;
                    UUID uuid = RenderAccess.get(GALLERY_HOVER);

                    if (player != null && uuid != null) {
                        String mark = RenderAccess.get(WAYPOINT_TYPES, new String[index])[index];
                        player.connection.send(new UpdateExhibitionMarkPacket(uuid, mark));
                        return new Layers.IEventResult.Consumed();
                    }
                }
                return new Layers.IEventResult.Miss();
            }

            @Override
            public Layers.IEventResult onMouseButtonReleased(MouseButtonEvent event) {
                Range<Float> range = range(Pos.ofLossy(event.x(), event.y()));
                Range<Float> currentRange = RenderAccess.get(BUTTON_MARK_HOVER_RANGE_STYLE);
                if ((currentRange == null) != (range == null)) {
                    BUTTON_MARK_STYLE.set(BUTTON_NORMAL);
                    BUTTON_MARK_HOVER_RANGE_STYLE.set(range);
                }

                return range != null ? new Layers.IEventResult.Consumed() : new Layers.IEventResult.Miss();
            }
        });
    }
}
