package org.teacon.exhibition_portal.client.screens.map;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.client.framework.DragContext;
import org.teacon.exhibition_portal.client.framework.GeneralLayouts;
import org.teacon.exhibition_portal.client.framework.Layers;
import org.teacon.exhibition_portal.client.framework.binding.LayoutBinding;
import org.teacon.exhibition_portal.client.framework.binding.LayoutParameter;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Pos;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.screens.LayerPriority;
import org.teacon.exhibition_portal.components.ExhibitionMetadata;
import org.teacon.exhibition_portal.utils.Components;

import java.util.List;

import static org.teacon.exhibition_portal.client.framework.GeneralLayouts.WINDOW_HEIGHT;
import static org.teacon.exhibition_portal.client.framework.GeneralLayouts.WINDOW_WIDTH;

@EventBusSubscriber(Dist.CLIENT)
public final class MapScreenLayouts {
    private MapScreenLayouts() {
    }

    private static final LayoutParameter<Float> LEFT_BAR_WIDTH = LayoutParameter.of(0.2f);

    private static final LayoutBinding<Float> SPLIT_X = LayoutBinding.of(
            () -> List.of(WINDOW_WIDTH, LEFT_BAR_WIDTH),
            context -> context.get(GeneralLayouts.WINDOW_WIDTH) * context.get(LEFT_BAR_WIDTH)
    );

    public static final LayoutBinding<Rectangle> SELECTION_BOX = LayoutBinding.of(
            () -> List.of(WINDOW_HEIGHT, SPLIT_X),
            context -> new Rectangle(0, 0, context.get(SPLIT_X), context.get(WINDOW_HEIGHT))
    );

    public static final LayoutBinding<Rectangle> MAP_BOX = LayoutBinding.of(
            () -> List.of(SPLIT_X, WINDOW_WIDTH, WINDOW_HEIGHT),
            context -> new Rectangle(
                    context.get(SPLIT_X), 0,
                    context.get(WINDOW_WIDTH) - context.get(SPLIT_X), context.get(WINDOW_HEIGHT)
            )
    );

    public static MutableComponent ofEVT(ExhibitionMetadata metadata) {
        if (metadata.evtMin() == -1) {
            return Component.literal("@unset").withStyle(Components.REGULAR);
        }

        return Component.translatable("exhibition_portal.detail.evt",
                Byte.toString(metadata.evtMin()), metadata.evtMax() == -1 ? "\u221E" : Byte.toString(metadata.evtMax())
        );
    }

    private record SplitLineDragContext() implements DragContext.IContext {
        @Override
        public boolean onMouseMove(Pos mouse) {
            float v = Math.clamp(mouse.x() / (float) RenderAccess.get(WINDOW_WIDTH), 0.05f, 0.95f);
            LEFT_BAR_WIDTH.set(v); // TODO: Write to config file
            return false;
        }
    }

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return LayerPriority.LAYER_SPLIT_X;
            }

            @Override
            public Class<? extends Screen> screen() {
                return MapScreen.class;
            }

            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                if (in(event.x())) {
                    DragContext.begin(new SplitLineDragContext());
                    return new Layers.IEventResult.Consumed();
                }
                return new Layers.IEventResult.Miss();
            }

            @Override
            public @Nullable CursorType onRender() {
                return in(Pos.getMouse().x()) ? CursorTypes.RESIZE_EW : null;
            }

            private boolean in(double mouseX) {
                float splitX = RenderAccess.get(SPLIT_X, -1f);
                return splitX != -1 && splitX - 1 <= mouseX && mouseX <= splitX + 5;
            }
        });
    }
}
