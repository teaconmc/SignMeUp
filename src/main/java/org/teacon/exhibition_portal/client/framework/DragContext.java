package org.teacon.exhibition_portal.client.framework;

import net.minecraft.client.input.MouseButtonEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.exhibition_portal.client.framework.components.Pos;

@EventBusSubscriber(Dist.CLIENT)
public final class DragContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragContext.class);

    public interface IContext {
        /// @return Whether to remove the current context
        boolean onMouseMove(Pos mouse);

        default void close() {
        }
    }

    @Nullable
    private static IContext context;

    @SubscribeEvent(priority = EventPriority.HIGH)
    private static void on(ScreenEvent.Init.Pre event) {
        close();
    }

    public static void begin(IContext next) {
        if (context != null) {
            context.close();
            LOGGER.warn("Unexpected context switching: {} -> {}", context, next);
        }
        context = next;
    }

    private static boolean close() {
        if (context != null) {
            context.close();
            context = null;
            return true;
        }
        return false;
    }

    static {
        Layers.push(new Layers.ILayer.Static() {
            @Override
            public byte priority() {
                return Byte.MAX_VALUE;
            }

            @Override
            public Layers.IEventResult onMouseButtonPressed(MouseButtonEvent event) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && close()) {
                    return new Layers.IEventResult.Consumed();
                }
                return new Layers.IEventResult.Miss();
            }

            @Override
            public Layers.IEventResult onMouseButtonReleased(MouseButtonEvent event) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && close()) {
                    return new Layers.IEventResult.Consumed();
                }
                return new Layers.IEventResult.Miss();
            }

            @Override
            public Layers.IEventResult onMouseMove(Pos pos) {
                if (context != null) {
                    if (context.onMouseMove(pos)) {
                        close();
                        return new Layers.IEventResult.Miss();
                    }
                    return new Layers.IEventResult.Consumed();
                }
                return new Layers.IEventResult.Miss();
            }
        });
    }

}
