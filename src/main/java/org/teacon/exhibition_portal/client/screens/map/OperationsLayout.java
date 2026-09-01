package org.teacon.exhibition_portal.client.screens.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.EPClient;
import org.teacon.exhibition_portal.client.framework.GeneralLayouts;
import org.teacon.exhibition_portal.client.framework.Layers;
import org.teacon.exhibition_portal.client.framework.binding.LayoutBinding;
import org.teacon.exhibition_portal.client.framework.binding.LayoutParameter;
import org.teacon.exhibition_portal.client.framework.binding.LayoutResource;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Pos;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.framework.components.TextureMetadata;
import org.teacon.exhibition_portal.client.screens.LayerPriority;
import org.teacon.exhibition_portal.client.screens.MapLayouts;
import org.teacon.exhibition_portal.components.EPOperation;
import org.teacon.exhibition_portal.network.ExecuteOperationPacket;

import java.util.List;

public final class OperationsLayout {
    private OperationsLayout() {
    }

    public static final LayoutResource<TextureMetadata> OPERATION_TEXTURE = TextureMetadata.of(ExhibitionPortal.id("textures/gui/operations.png"));

    public static final LayoutResource<TextureMetadata> OPERATION_HOVER_TEXTURE = TextureMetadata.of(ExhibitionPortal.id("textures/gui/operation_hover.png"));

    public static final LayoutParameter<@Nullable Rectangle> OPERATION_HOVER = LayoutParameter.of(null);

    public static final LayoutParameter<@Nullable List<ClientTooltipComponent>> OPERATION_TOOLTIP = LayoutParameter.of(null);

    public static final LayoutBinding<@Nullable Rectangle> OPERATION_BOX = LayoutBinding.of(
            () -> List.of(EPClient.OPERATION, MapLayouts.MAP_BOX, GeneralLayouts.WINDOW_WIDTH, OPERATION_HOVER),
            context -> {
                Rectangle box = context.get(MapLayouts.MAP_BOX);
                if (box == null) {
                    return null;
                }

                EPOperation operation = context.get(EPClient.OPERATION);
                int windowWidth = context.get(GeneralLayouts.WINDOW_WIDTH);
                float width = windowWidth * (context.get(OPERATION_HOVER) != null ? 0.8f : 0.3f);
                float height = width / operation.size();
                float marginBottom = windowWidth * 0.6f / operation.size();

                return new Rectangle(box.x() + box.w() / 2 - width / 2, box.y1() - marginBottom - height / 2f, width, height);
            }
    );

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYER_OPERATION;
            }

            @Override
            public Class<? extends Screen> screen() {
                return MapScreen.class;
            }

            @Override
            public Layers.IEventResult onMouseMove(Pos mouse) {
                Rectangle box = RenderAccess.get(OPERATION_BOX, null);
                EPOperation operation = RenderAccess.get(EPClient.OPERATION);
                TextureMetadata texture = RenderAccess.get(OPERATION_TEXTURE, null);
                TextureMetadata hover = RenderAccess.get(OPERATION_HOVER_TEXTURE, null);
                if (box == null || !box.contains(mouse) || operation == null || hover == null) {
                    OPERATION_HOVER.set(null);
                    OPERATION_TOOLTIP.set(null);
                    return new Layers.IEventResult.Miss();
                }

                int i = (int) ((mouse.x() - box.x()) * operation.size() / box.w());

                float gridW = hover.width() * box.h() / texture.height();
                float spacing = box.w() - gridW * operation.size();
                OPERATION_HOVER.set(new Rectangle(
                        box.x() + i * (gridW + spacing / (operation.size() - 1)),
                        box.y(),
                        gridW,
                        box.h()
                ));
                OPERATION_TOOLTIP.set(List.of(
                        new ClientTextTooltip(Component.literal(operation.operations().get(i).title()).getVisualOrderText()),
                        new ClientTextTooltip(Component.literal(operation.operations().get(i).tooltip()).getVisualOrderText())
                ));
                return new Layers.IEventResult.Consumed();
            }


            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                Rectangle box = RenderAccess.get(OPERATION_BOX, null);
                EPOperation operation = RenderAccess.get(EPClient.OPERATION);
                if (box == null || !box.contains(event.x(), event.y()) || operation == null) {
                    return new Layers.IEventResult.Miss();
                }

                int i = (int) ((event.x() - box.x()) * operation.size() / box.w());
                ClientPacketListener conn = Minecraft.getInstance().getConnection();
                if (conn != null && i >= 0 && i < operation.size()) {
                    conn.send(new ExecuteOperationPacket(operation.operations().get(i).id()));
                    Minecraft.getInstance().setScreen(null);
                }

                return new Layers.IEventResult.Consumed();
            }
        });
    }
}
