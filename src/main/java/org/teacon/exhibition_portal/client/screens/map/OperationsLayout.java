package org.teacon.exhibition_portal.client.screens.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
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

    public static final LayoutBinding<@Nullable Rectangle> OPERATION_BOX = LayoutBinding.of(
            () -> List.of(EPClient.OPERATION, MapLayouts.MAP_BOX, GeneralLayouts.WINDOW_WIDTH),
            context -> {
                Rectangle box = context.get(MapLayouts.MAP_BOX);
                if (box == null) {
                    return null;
                }

                EPOperation operation = context.get(EPClient.OPERATION);
                float width = context.get(GeneralLayouts.WINDOW_WIDTH) * 0.3f;
                float height = width / operation.size();

                return new Rectangle(box.x() + box.w() / 2 - width / 2, box.y1() - height * 2, width, height);
            }
    );

    public static final LayoutParameter<@Nullable Rectangle> OPERATION_HOVER = LayoutParameter.of(null);

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYER_OPERATION;
            }

            @Override
            public Layers.IEventResult onMouseMove(Pos mouse) {
                Rectangle box = RenderAccess.get(OPERATION_BOX, null);
                EPOperation operation = RenderAccess.get(EPClient.OPERATION);
                TextureMetadata texture = RenderAccess.get(OPERATION_TEXTURE, null);
                TextureMetadata hover = RenderAccess.get(OPERATION_HOVER_TEXTURE, null);
                if (box == null || !box.contains(mouse) || operation == null || hover == null) {
                    OPERATION_HOVER.set(null);
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
                return new Layers.IEventResult.Consumed();
            }

            private int lastIndex = -1;
            private long lastTimestamp = -1;

            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                Rectangle box = RenderAccess.get(OPERATION_BOX, null);
                EPOperation operation = RenderAccess.get(EPClient.OPERATION);
                if (box == null || !box.contains(event.x(), event.y()) || operation == null) {
                    return new Layers.IEventResult.Miss();
                }

                int i = (int) ((event.x() - box.x()) * operation.size() / box.w());
                if (lastIndex != i) {
                    lastIndex = i;
                    lastTimestamp = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastTimestamp > 500) {
                    lastTimestamp = System.currentTimeMillis();
                } else {
                    ClientPacketListener conn = Minecraft.getInstance().getConnection();
                    if (conn != null && i >= 0 && i < operation.size()) {
                        conn.send(new ExecuteOperationPacket(operation.operations().get(i).id()));
                        if (Minecraft.getInstance().screen instanceof MapScreen) {
                            Minecraft.getInstance().setScreen(null);
                        }
                    }
                }

                return new Layers.IEventResult.Consumed();
            }
        });
    }
}
