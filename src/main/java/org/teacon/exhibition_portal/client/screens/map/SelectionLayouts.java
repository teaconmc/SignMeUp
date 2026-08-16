package org.teacon.exhibition_portal.client.screens.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;
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
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.network.TeleportToExhibitionPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.teacon.exhibition_portal.client.EPClient.GALLERIES;
import static org.teacon.exhibition_portal.client.EPClient.GALLERY_LOOKUP;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.DETAIL_BOX;
import static org.teacon.exhibition_portal.client.screens.map.MapScreenLayouts.SELECTION_BOX;

@EventBusSubscriber(Dist.CLIENT)
/* package-private */ final class SelectionLayouts {
    private SelectionLayouts() {
    }

    public static final LayoutParameter<Float> LEFT_SCROLL = LayoutParameter.of(0f);

    public static final LayoutResource<TextureMetadata> LEFT_BACKGROUND = TextureMetadata.of(
            ExhibitionPortal.id("textures/gui/units_selection_background.png")
    );

    public static final LayoutResource<TextureMetadata> HEADER = TextureMetadata.of(
            ExhibitionPortal.id("textures/gui/units_selection_header.png")
    );

    public static final LayoutBinding<Float> LEFT_L1 = LayoutBinding.of(
            () -> List.of(SELECTION_BOX, HEADER, LEFT_SCROLL),
            context -> context.get(SELECTION_BOX).w() / context.get(HEADER).aspectRatio() - context.get(LEFT_SCROLL)
    );

    public static final LayoutBinding<Float> LEFT_L2 = LayoutBinding.of(
            () -> List.of(SELECTION_BOX),
            context -> context.get(SELECTION_BOX).w() * 0.16360f
    );

    public static final LayoutBinding<Float> LEFT_L3 = LayoutBinding.of(
            () -> List.of(LEFT_L1, LEFT_L2),
            context -> context.get(LEFT_L1) + context.get(LEFT_L2)
    );

    public static final LayoutBinding<Float> LEFT_SCROLL_START = LayoutBinding.of(
            () -> List.of(
                    SELECTION_BOX, HEADER, LEFT_L2
            ), context -> {
                return context.get(SELECTION_BOX).w() / context.get(HEADER).aspectRatio() + context.get(LEFT_L2);
            }
    );

    public static final LayoutBinding<Rectangle> HEADER_BOX = LayoutBinding.of(
            () -> List.of(HEADER, SELECTION_BOX, LEFT_L1),
            context -> {
                float leftL1 = context.get(LEFT_L1);
                if (leftL1 <= 0) {
                    return new Rectangle(0, 0, 0, 0);
                }

                Rectangle box = context.get(SELECTION_BOX);
                float w2 = leftL1 * context.get(HEADER).aspectRatio();
                return new Rectangle(box.x() + (box.w() - w2) / 2f, box.y(), w2, leftL1);
            }
    );

    public static final LayoutBinding<Rectangle> GALLERY_BEGIN = LayoutBinding.of(
            () -> List.of(LEFT_L1, LEFT_L2, LEFT_L3, SELECTION_BOX),
            context -> {
                Rectangle box = context.get(SELECTION_BOX);
                float bottom = Math.max(context.get(LEFT_L2), (context.get(LEFT_L1) + context.get(LEFT_L3)) / 2);
                return new Rectangle(box.x(), bottom - 1, box.w(), 1);
            }
    );

    public static final LayoutBinding<Rectangle> MASK_CHANGING_AREA = LayoutBinding.of(
            () -> List.of(SELECTION_BOX, LEFT_L1, LEFT_L3),
            context -> {
                float leftY1 = context.get(LEFT_L1), leftY3 = context.get(LEFT_L3);
                if (leftY3 <= 0) {
                    return new Rectangle(0, 0, 0, 0);
                }
                Rectangle box = context.get(SELECTION_BOX);
                return new Rectangle(box.x(), leftY1, box.w(), leftY3 - leftY1);
            }
    );

    public static final LayoutBinding<Rectangle> MASK_AREA = LayoutBinding.of(
            () -> List.of(SELECTION_BOX, LEFT_L3, SELECTION_BOX),
            context -> {
                float leftY3 = Math.max(0, context.get(LEFT_L3));
                Rectangle box = context.get(SELECTION_BOX);
                return new Rectangle(box.x(), leftY3, box.w(), box.h() - leftY3);
            }
    );

    public static final LayoutBinding<Float> GALLERY_HEIGHT = LayoutBinding.of(
            () -> List.of(SELECTION_BOX),
            context -> context.get(SELECTION_BOX).w() * 0.17195f
    );

    public static final LayoutBinding<Float> LEFT_SCROLL_MAX = LayoutBinding.of(
            () -> List.of(
                    LEFT_SCROLL_START, SELECTION_BOX, GALLERIES, GALLERY_HEIGHT
            ),
            context -> {
                float v = context.get(LEFT_SCROLL_START)
                        + context.get(GALLERIES).size() * context.get(GALLERY_HEIGHT)
                        - context.get(SELECTION_BOX).h();
                return Math.max(0, v);
            }
    );

    public static final LayoutBinding<Rectangle> SCROLL_BAR = LayoutBinding.of(
            () -> List.of(
                    LEFT_SCROLL, LEFT_SCROLL_MAX, LEFT_SCROLL_START, SELECTION_BOX
            ),
            context -> {
                float scrollMax = context.get(LEFT_SCROLL_MAX);
                if (scrollMax == 0) {
                    return new Rectangle(0, 0, 0, 0);
                }

                Rectangle box = context.get(SELECTION_BOX);
                float scrollBarW = 5, scrollBarH = box.h() / 6f;
                float scrollBarY = context.get(LEFT_SCROLL)
                        * (box.h() - context.get(LEFT_SCROLL_START) - scrollBarH)
                        / scrollMax
                        + context.get(LEFT_SCROLL_START);
                return new Rectangle(box.x() + box.w() - scrollBarW, scrollBarY, scrollBarW, scrollBarH);
            }
    );

    public static void scrollTo(UUID uuid) {
        try {
            List<UUID> galleries = RenderAccess.get(GALLERIES);
            for (int i = 0; i < galleries.size(); i++) {
                if (!galleries.get(i).equals(uuid)) {
                    continue;
                }

                float v = i * RenderAccess.get(GALLERY_HEIGHT);
                LEFT_SCROLL.set(Math.clamp(v, 0, RenderAccess.get(LEFT_SCROLL_MAX)));
                GALLERY_HOVER.set(uuid);
                return;
            }
        } catch (LayoutFailureException _) {
        }
    }

    private record ScrollBarMouseDrag(float relativeY) implements DragContext.IContext {
        @Override
        public boolean onMouseMove(Pos mouse) {
            try {
                Rectangle scrollBar = RenderAccess.get(SCROLL_BAR);
                float x = (mouse.y() - relativeY - RenderAccess.get(LEFT_SCROLL_START))
                        * RenderAccess.get(LEFT_SCROLL_MAX)
                        / (float) (RenderAccess.get(SELECTION_BOX).h() - RenderAccess.get(LEFT_SCROLL_START) - scrollBar.h());
                LEFT_SCROLL.set(Math.clamp(x, 0, RenderAccess.get(LEFT_SCROLL_MAX)));
                return false;
            } catch (LayoutFailureException e) {
                return true;
            }
        }
    }

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYER_SELECTION_SCROLL;
            }

            @Override
            public Class<? extends Screen> screen() {
                return MapScreen.class;
            }

            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                Rectangle scrollBar = RenderAccess.get(SCROLL_BAR, null);
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && scrollBar != null && scrollBar.contains(event.x(), event.y())) {
                    DragContext.begin(new ScrollBarMouseDrag((float) (event.y() - scrollBar.y())));
                    return new Layers.IEventResult.Consumed();
                }
                return new Layers.IEventResult.Miss();
            }

            @Override
            public Layers.IEventResult onMouseScrolled(Pos mouse, float deltaY) {
                Rectangle selection = RenderAccess.get(SELECTION_BOX, null);
                if (selection != null && selection.contains(mouse)) {
                    float v = RenderAccess.get(LEFT_SCROLL) - deltaY * 16;
                    LEFT_SCROLL.set(Math.clamp(v, 0, RenderAccess.get(LEFT_SCROLL_MAX, 0f)));
                    return new Layers.IEventResult.Consumed();
                }
                return new Layers.IEventResult.Miss();
            }
        });
    }

    @SubscribeEvent
    private static void on(ScreenEvent.Render.Pre event) {
        float v = RenderAccess.get(LEFT_SCROLL);
        LEFT_SCROLL.set(Math.clamp(v, 0, RenderAccess.get(LEFT_SCROLL_MAX, 0f)));
    }

    public static final LayoutBinding<Rectangle> GALLERY_SCISSOR = LayoutBinding.of(
            () -> List.of(SELECTION_BOX, SELECTION_BOX, LEFT_L2),
            context -> {
                float y = context.get(LEFT_L2);
                Rectangle box = context.get(SELECTION_BOX);
                return new Rectangle(box.x(), y, box.w(), box.h() - y);
            }
    );

    public static final LayoutBinding<Float> GALLERY_INNER_WIDTH = LayoutBinding.of(
            () -> List.of(SELECTION_BOX),
            context -> context.get(SELECTION_BOX).w() * 0.84307f
    );

    public static final LayoutBinding<Float> GALLERY_INNER_HEIGHT = LayoutBinding.of(
            () -> List.of(SELECTION_BOX),
            context -> context.get(SELECTION_BOX).w() * 0.14691f
    );

    public record ExhibitionRender(
            Exhibition exhibition,
            Rectangle box,
            Rectangle icon,
            Rectangle title,
            Rectangle description
    ) {
    }

    public static final LayoutBinding<List<ExhibitionRender>> GALLERY_RENDERS = LayoutBinding.of(
            () -> List.of(
                    SELECTION_BOX, LEFT_L3,
                    GALLERIES, GALLERY_LOOKUP, GALLERY_SCISSOR, GALLERY_HEIGHT, GALLERY_INNER_WIDTH, GALLERY_INNER_HEIGHT
            ),
            context -> {
                Rectangle selectionBox = context.get(SELECTION_BOX);
                float leftL3 = context.get(LEFT_L3), exhibitionHeight = context.get(GALLERY_HEIGHT);
                Rectangle scissor = context.get(GALLERY_SCISSOR);
                List<UUID> galleries = context.get(GALLERIES);
                Map<UUID, Exhibition> lookup = context.get(GALLERY_LOOKUP);

                int begin = (int) Math.max(0, Math.floor((scissor.y() - leftL3) / exhibitionHeight));
                int end = galleries.size() - (int) Math.max(0, Math.floor((leftL3 + galleries.size() * exhibitionHeight - scissor.y1()) / exhibitionHeight));

                float innerW = context.get(GALLERY_INNER_WIDTH), innerH = context.get(GALLERY_INNER_HEIGHT);

                ArrayList<ExhibitionRender> renders = new ArrayList<>(end - begin);
                float leftY3 = leftL3 + exhibitionHeight * begin;
                for (int i = begin; i < end; i++) {
                    Exhibition exhibition = lookup.get(galleries.get(i));

                    Rectangle box = new Rectangle(selectionBox.x() + (selectionBox.w() - innerW) / 2f, leftY3, innerW, innerH);

                    float titleH = box.h() * 0.46579f, descriptionH = box.h() * 0.33056f;
                    float spacing3 = (box.h() - titleH - descriptionH) / 3;
                    float x = box.x() + box.h() + box.w() * 0.02967f;
                    Rectangle title = new Rectangle(x, box.y() + spacing3, box.w() - x, titleH);
                    Rectangle description = new Rectangle(x, box.y() + spacing3 * 2 + titleH, box.w() - x, descriptionH);

                    renders.add(new ExhibitionRender(exhibition, box, new Rectangle(box.x(), box.y(), box.h(), box.h()), title, description));
                    leftY3 += exhibitionHeight;
                }

                return renders;
            }
    );

    public static final LayoutParameter<UUID> GALLERY_HOVER = LayoutParameter.of(null);

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYOUT_DETAIL;
            }

            @Override
            public Class<? extends Screen> screen() {
                return MapScreen.class;
            }

            private Exhibition clickedExhibition;
            private long clickedExhibitionTimestamp = -1;

            @Override
            public Layers.IEventResult onMouseClicked(MouseButtonEvent down, MouseButtonEvent up) {
                for (SelectionLayouts.ExhibitionRender render : RenderAccess.get(GALLERY_RENDERS, List.of())) {
                    if (render.box().contains(down.x(), down.y()) && render.box().contains(up.x(), up.y())) {
                        Exhibition exhibition = render.exhibition();
                        if (clickedExhibition == exhibition && System.currentTimeMillis() - clickedExhibitionTimestamp <= 200) {
                            LocalPlayer player = Minecraft.getInstance().player;
                            if (player != null) {
                                player.connection.send(new TeleportToExhibitionPacket(exhibition.uuid()));
                                Minecraft.getInstance().setScreen(null);
                                return new Layers.IEventResult.Consumed();
                            }
                            clickedExhibition = null;
                            clickedExhibitionTimestamp = -1;
                        } else {
                            clickedExhibition = exhibition;
                            clickedExhibitionTimestamp = System.currentTimeMillis();
                        }
                    }
                }
                return new Layers.IEventResult.Miss();
            }

            private SelectionLayouts.ExhibitionRender previous;
            private long previousTimestamp = -1;

            @Override
            public Layers.IEventResult onMouseMove(Pos mouse) {
                Rectangle outline = RenderAccess.get(DETAIL_BOX, null);
                if (outline != null && outline.contains(mouse)) {
                    previousTimestamp = System.currentTimeMillis();
                    return new Layers.IEventResult.Consumed();
                }

                for (SelectionLayouts.ExhibitionRender render : RenderAccess.get(GALLERY_RENDERS, List.of())) {
                    if (render.box().contains(mouse)) {
                        moveTo(render);
                        return new Layers.IEventResult.Consumed();
                    }
                }

                moveTo(null);
                return new Layers.IEventResult.Miss();
            }

            private void moveTo(SelectionLayouts.ExhibitionRender exhibition) {
                if (previous == null) {
                    if (exhibition != null) {
                        previous = exhibition;
                        previousTimestamp = System.currentTimeMillis();
                    }
                } else {
                    if (exhibition == null) {
                        if (System.currentTimeMillis() - previousTimestamp <= 200) {
                            exhibition = previous;
                        }
                    } else {
                        if (System.currentTimeMillis() - previousTimestamp > 200) {
                            previous = exhibition;
                            previousTimestamp = System.currentTimeMillis();
                        }
                    }
                }
                GALLERY_HOVER.set(exhibition != null ? exhibition.exhibition().uuid() : null);
            }
        });
    }

    public static final LayoutParameter<StringBuilder> SEARCH_STRING = LayoutParameter.of(new StringBuilder());

    public static final LayoutBinding<Rectangle> SEARCH_BOX = LayoutBinding.of(
            () -> List.of(GALLERY_BEGIN, SELECTION_BOX, GALLERY_INNER_WIDTH),
            context -> {
                float bottom = context.get(GALLERY_BEGIN).y1();
                Rectangle selectionBox = context.get(SELECTION_BOX);
                float w = context.get(GALLERY_INNER_WIDTH);
                float h = selectionBox.h() * 0.040740f;

                return new Rectangle(selectionBox.x() + selectionBox.w() / 2f - w / 2f, bottom - h, w, h);
            }
    );
}
