package org.teacon.exhibition_portal.client.screens.map;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.util.UndashedUuid;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.Range;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.framework.binding.LayoutFailureException;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Pos;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.components.UVSource;
import org.teacon.exhibition_portal.client.framework.render.AbstractEPScreen;
import org.teacon.exhibition_portal.client.framework.render.ColoredQuadrangleRenderState;
import org.teacon.exhibition_portal.client.screens.MapLayouts;
import org.teacon.exhibition_portal.client.screens.stamp.StampScreenLayouts;
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.components.ExhibitionMetadata;
import org.teacon.exhibition_portal.utils.Components;

import java.util.List;
import java.util.Map;

import static org.teacon.exhibition_portal.client.EPClient.GALLERY_LOOKUP;
import static org.teacon.exhibition_portal.client.framework.render.text.TextFitMode.FIT_HEIGHT;
import static org.teacon.exhibition_portal.client.framework.render.text.TextHorizontalAlignment.LEFT;
import static org.teacon.exhibition_portal.client.framework.render.text.TextHorizontalAlignment.MIDDLE;
import static org.teacon.exhibition_portal.client.framework.render.text.TextHorizontalAlignment.SCROLL;
import static org.teacon.exhibition_portal.client.framework.render.text.TextVerticalAlignment.CENTER;
import static org.teacon.exhibition_portal.client.framework.render.text.TextVerticalAlignment.TOP;
import static org.teacon.exhibition_portal.client.screens.MapLayouts.MAP;
import static org.teacon.exhibition_portal.client.screens.MapLayouts.MAP_AREA;
import static org.teacon.exhibition_portal.client.screens.MapLayouts.ME_UI;
import static org.teacon.exhibition_portal.client.screens.MapLayouts.ME_UI_IMAGE;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.BUTTON_HOVER;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.BUTTON_MARK_AS;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.BUTTON_MARK_HOVER_RANGE_STYLE;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.BUTTON_MARK_STYLE;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.BUTTON_TELEPORT;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.BUTTON_TELEPORT_STYLE;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.DETAIL_BOX;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.DETAIL_DESC_BOX;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.DETAIL_SELECTION_OUTLINE;
import static org.teacon.exhibition_portal.client.screens.map.DetailLayouts.DETAIL_TITLE_BOX;
import static org.teacon.exhibition_portal.client.screens.map.MapScreenLayouts.MAP_BOX;
import static org.teacon.exhibition_portal.client.screens.map.MapScreenLayouts.SELECTION_BOX;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.GALLERY_BEGIN;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.GALLERY_HOVER;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.GALLERY_RENDERS;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.GALLERY_SCISSOR;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.HEADER;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.HEADER_BOX;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.LEFT_BACKGROUND;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.MASK_AREA;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.MASK_CHANGING_AREA;
import static org.teacon.exhibition_portal.client.screens.map.SelectionLayouts.SCROLL_BAR;
import static org.teacon.exhibition_portal.client.screens.map.WaypointLayouts.WAYPOINTS;
import static org.teacon.exhibition_portal.client.screens.map.WaypointLayouts.WAYPOINT_TYPES;
import static org.teacon.exhibition_portal.utils.Components.HEAVY;
import static org.teacon.exhibition_portal.utils.Components.REGULAR;

public class MapScreen extends AbstractEPScreen {
    public MapScreen() {
        super(Component.translatable("exhibition_portal.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void render(@NonNull GuiGraphicsExtractor graphics) throws LayoutFailureException {
        GpuSampler LINEAR_CLAMP = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GpuSampler NEAREST_CLAMP = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        {
            Rectangle leftBox = RenderAccess.get(SELECTION_BOX);
            graphics.blit(RenderAccess.get(LEFT_BACKGROUND), LINEAR_CLAMP, leftBox, UVSource.FULL);
            graphics.enableScissor(leftBox);
            try {
                graphics.blit(RenderAccess.get(HEADER), LINEAR_CLAMP, RenderAccess.get(HEADER_BOX));

                graphics.fill(RenderAccess.get(MASK_CHANGING_AREA), 0xA6111111, 0x00111111, 0x00111111, 0xA6111111);
                graphics.fill(RenderAccess.get(MASK_AREA), 0xA6111111);

                graphics.fill(RenderAccess.get(GALLERY_BEGIN), 0xFF0EA5E9);

                graphics.enableScissor(RenderAccess.get(GALLERY_SCISSOR));
                try {
                    for (SelectionLayouts.ExhibitionRender render : RenderAccess.get(GALLERY_RENDERS)) {
                        graphics.enableScissor(render.box());
                        try {
                            graphics.blit(
                                    ExhibitionPortal.id("textures/gui/units/" + UndashedUuid.toString(render.exhibition().uuid()) + "/icon.png"),
                                    LINEAR_CLAMP, render.icon()
                            );

                            ExhibitionMetadata metadata = render.exhibition().metadata();
                            graphics.renderString(render.title(), Component.literal(metadata.name()).withStyle(HEAVY), SCROLL, CENTER, FIT_HEIGHT);
                            graphics.renderString(render.description(), Component.literal(metadata.description()).withStyle(REGULAR), SCROLL, TOP, FIT_HEIGHT);
                        } finally {
                            graphics.disableScissor();
                        }
                    }

//                graphics.blit(exhibitionHoverTexture, LINEAR_CLAMP, hoverBox);
                } finally {
                    graphics.disableScissor();
                }
            } finally {
                graphics.disableScissor();
            }
            graphics.fill(RenderAccess.get(SCROLL_BAR), 0xFF0EA5E9);
        }

        graphics.enableScissor(RenderAccess.get(MAP_BOX));
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

            MapLayouts.MeUI meui = RenderAccess.get(ME_UI);
            if (meui != null) {
                try {
                    Pos center = meui.area().center();
                    graphics.pose().pushMatrix().rotateAbout(meui.rotate(), center.x(), center.y());
                    graphics.blit(RenderAccess.get(ME_UI_IMAGE), LINEAR_CLAMP, meui.area());
                } finally {
                    graphics.pose().popMatrix();
                }
            }
        } finally {
            graphics.disableScissor();
        }

        Rectangle operation = RenderAccess.get(OperationsLayout.OPERATION_BOX);
        if (operation != null) {
            graphics.blit(RenderAccess.get(OperationsLayout.OPERATION_TEXTURE), NEAREST_CLAMP, operation);
            Rectangle hover = RenderAccess.get(OperationsLayout.OPERATION_HOVER);
            if (hover != null) {
                graphics.blit(RenderAccess.get(OperationsLayout.OPERATION_HOVER_TEXTURE), NEAREST_CLAMP, hover);
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
            List<ClientTooltipComponent> tooltip = RenderAccess.get(OperationsLayout.OPERATION_TOOLTIP);
            if (tooltip != null) {
                graphics.tooltip(
                        minecraft.font,
                        tooltip,
                        Math.round(operation.x0()), Math.round(operation.y1()),
                        DefaultTooltipPositioner.INSTANCE,
                        null
                );
            }
        }

        Rectangle selectionOutline = RenderAccess.get(DETAIL_SELECTION_OUTLINE), descOutline = RenderAccess.get(DETAIL_BOX);

        graphics.fill(descOutline, 0xC0000000);
        graphics.submitGuiElementRenderState(new ColoredQuadrangleRenderState(
                RenderPipelines.GUI, new Matrix3x2f(graphics.pose()),
                selectionOutline.x1(), selectionOutline.y0(), 0xB3000000,
                selectionOutline.x1(), selectionOutline.y1(), 0xB3000000,
                descOutline.x0(), descOutline.y1(), 0xC0000000,
                descOutline.x0(), descOutline.y0(), 0xC0000000,
                graphics.peekScissorStack()
        ));

        Exhibition hover = RenderAccess.get(GALLERY_LOOKUP).get(RenderAccess.get(GALLERY_HOVER));
        if (hover != null) {
            graphics.renderString(RenderAccess.get(DETAIL_TITLE_BOX), Component.literal(hover.metadata().name()).withStyle(Components.HEAVY), LEFT, CENTER, FIT_HEIGHT);

            for (Map.Entry<Rectangle, FormattedCharSequence> entry : RenderAccess.get(DETAIL_DESC_BOX).entrySet()) {
                graphics.renderString(entry.getKey(), entry.getValue(), LEFT, TOP, FIT_HEIGHT);
            }

            Rectangle detailButtonTeleportBox = RenderAccess.get(BUTTON_TELEPORT);
            graphics.blit(RenderAccess.get(RenderAccess.get(BUTTON_TELEPORT_STYLE)), LINEAR_CLAMP, detailButtonTeleportBox);
            if (RenderAccess.get(BUTTON_TELEPORT_STYLE) != DetailLayouts.BUTTON_NORMAL) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
            graphics.renderString(shrink(detailButtonTeleportBox), Component.translatable("exhibition_portal.detail.teleport").withStyle(REGULAR), MIDDLE, CENTER, FIT_HEIGHT);

            Rectangle detailButtonMarkBox = RenderAccess.get(BUTTON_MARK_AS);
            graphics.blit(RenderAccess.get(RenderAccess.get(BUTTON_MARK_STYLE)), LINEAR_CLAMP, detailButtonMarkBox);
            Range<Float> detailButtonMarkRange = RenderAccess.get(BUTTON_MARK_HOVER_RANGE_STYLE);
            if (detailButtonMarkRange == null) {
                graphics.renderString(shrink(detailButtonMarkBox), Component.translatable("exhibition_portal.detail.mark").withStyle(REGULAR), MIDDLE, CENTER, FIT_HEIGHT);
            } else {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                graphics.blit(
                        RenderAccess.get(BUTTON_HOVER),
                        LINEAR_CLAMP,
                        new Rectangle(
                                detailButtonMarkBox.x() + detailButtonMarkBox.w() * detailButtonMarkRange.getMinimum(),
                                detailButtonMarkBox.y(),
                                detailButtonMarkBox.w() * (detailButtonMarkRange.getMaximum() - detailButtonMarkRange.getMinimum()),
                                detailButtonMarkBox.h()
                        ),
                        new UVSource(detailButtonMarkRange.getMinimum(), detailButtonMarkRange.getMaximum(), 0, 1)
                );

                String[] waypointTypes = RenderAccess.get(WAYPOINT_TYPES);
                for (int i = 0; i < waypointTypes.length; i++) {
                    float a = detailButtonMarkBox.h(), s = 0.15f, d = a * s, d2 = a * (1 - s * 2);
                    Rectangle r = new Rectangle(detailButtonMarkBox.x() + a * i, detailButtonMarkBox.y(), a, a);
                    r = new Rectangle(r.x() + d, r.y() + d, d2, d2);
                    UVSource uv = new UVSource(i / (float) waypointTypes.length, (i + 1) / (float) waypointTypes.length, 0, 1);
                    graphics.blit(RenderAccess.get(WaypointLayouts.WAYPOINT_TEXTURE), LINEAR_CLAMP, r, uv);
                }
            }

            final float LINE_WIDTH = 2f;
            graphics.fill(new Rectangle(selectionOutline.x0() - LINE_WIDTH, selectionOutline.y0() - LINE_WIDTH, selectionOutline.w() + LINE_WIDTH, LINE_WIDTH), 0xFF0EA5E9);
            graphics.fill(new Rectangle(descOutline.x0(), descOutline.y0() - LINE_WIDTH, descOutline.w() + LINE_WIDTH, LINE_WIDTH), 0xFF0EA5E9);
            graphics.fill(new Rectangle(descOutline.x1(), descOutline.y0(), LINE_WIDTH, descOutline.h()), 0xFF0EA5E9);
            graphics.fill(new Rectangle(descOutline.x0(), descOutline.y1(), descOutline.w() + LINE_WIDTH, LINE_WIDTH), 0xFF0EA5E9);
            graphics.fill(new Rectangle(selectionOutline.x0() - LINE_WIDTH, selectionOutline.y0(), LINE_WIDTH, selectionOutline.h()), 0xFF0EA5E9);
            graphics.fill(new Rectangle(selectionOutline.x0() - LINE_WIDTH, selectionOutline.y1(), selectionOutline.w() + LINE_WIDTH, LINE_WIDTH), 0xFF0EA5E9);

            Vector2f vec = new Vector2f(descOutline.x0(), descOutline.y0())
                    .sub(selectionOutline.x1(), selectionOutline.y0());
            vec.set(+vec.y, -vec.x)
                    .div(vec.distance(0, 0))
                    .mul(LINE_WIDTH);
            graphics.fill(
                    descOutline.x0(), descOutline.y0(), 0xFF0EA5E9,
                    descOutline.x0() + vec.x, descOutline.y0() + vec.y, 0xFF0EA5E9,
                    selectionOutline.x1() + vec.x, selectionOutline.y0() + vec.y, 0xFF0EA5E9,
                    selectionOutline.x1(), selectionOutline.y0(), 0xFF0EA5E9
            );
            graphics.fill(
                    descOutline.x0(), descOutline.y0(), 0xFF0EA5E9,
                    descOutline.x0(), descOutline.y0() - LINE_WIDTH, 0xFF0EA5E9,
                    descOutline.x0() + vec.x, descOutline.y0() + vec.y, 0xFF0EA5E9,
                    descOutline.x0(), descOutline.y0(), 0xFF0EA5E9
            );
            graphics.fill(
                    selectionOutline.x1(), selectionOutline.y0(), 0xFF0EA5E9,
                    selectionOutline.x1() + vec.x, selectionOutline.y0() + vec.y, 0xFF0EA5E9,
                    selectionOutline.x1(), selectionOutline.y0() - LINE_WIDTH, 0xFF0EA5E9,
                    selectionOutline.x1(), selectionOutline.y0(), 0xFF0EA5E9
            );

            vec.set(descOutline.x0(), descOutline.y1())
                    .sub(selectionOutline.x1(), selectionOutline.y1());
            vec.set(-vec.y, +vec.x)
                    .div(vec.distance(0, 0))
                    .mul(3f);
            graphics.fill(
                    descOutline.x0() + vec.x, descOutline.y1() + vec.y, 0xFF0EA5E9,
                    descOutline.x0(), descOutline.y1(), 0xFF0EA5E9,
                    selectionOutline.x1(), selectionOutline.y1(), 0xFF0EA5E9,
                    selectionOutline.x1() + vec.x, selectionOutline.y1() + vec.y, 0xFF0EA5E9
            );
            graphics.fill(
                    descOutline.x0(), descOutline.y1() + LINE_WIDTH, 0xFF0EA5E9,
                    descOutline.x0(), descOutline.y1(), 0xFF0EA5E9,
                    descOutline.x0(), descOutline.y1(), 0xFF0EA5E9,
                    descOutline.x0() + vec.x, descOutline.y1() + vec.y, 0xFF0EA5E9
            );
            graphics.fill(
                    selectionOutline.x1() + vec.x, selectionOutline.y1() + vec.y, 0xFF0EA5E9,
                    selectionOutline.x1(), selectionOutline.y1(), 0xFF0EA5E9,
                    selectionOutline.x1(), selectionOutline.y1(), 0xFF0EA5E9,
                    selectionOutline.x1(), selectionOutline.y1() + LINE_WIDTH, 0xFF0EA5E9
            );
        }
    }

    private Rectangle shrink(Rectangle box) {
        return new Rectangle(box.x(), box.y() + box.h() * 0.3f, box.w(), box.h() * 0.4f);
    }
}
