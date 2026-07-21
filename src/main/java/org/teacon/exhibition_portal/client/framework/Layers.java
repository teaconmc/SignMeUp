package org.teacon.exhibition_portal.client.framework;

import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.client.framework.components.Pos;
import org.teacon.exhibition_portal.client.framework.render.AbstractEPScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@EventBusSubscriber(Dist.CLIENT)
public final class Layers {
    private Layers() {
    }

    public sealed interface IEventResult {
        record Miss() implements IEventResult {
        }

        record Consumed() implements IEventResult {
        }
    }

    public interface ILayer {
        byte priority();

        interface Static extends ILayer {
        }

        default Class<? extends Screen> screen() {
            return Screen.class;
        }

        @Nullable
        default CursorType onRender() {
            return null;
        }

        default IEventResult onMouseScrolled(Pos mouse, float deltaY) {
            return new IEventResult.Miss();
        }

        default IEventResult onMouseMove(Pos mouse) {
            return new IEventResult.Miss();
        }

        default IEventResult onMouseButtonPressed(MouseButtonEvent event) {
            return new IEventResult.Miss();
        }

        default IEventResult onMouseButtonReleased(MouseButtonEvent event) {
            return new IEventResult.Miss();
        }

        default IEventResult onMouseClicked(MouseButtonEvent down, MouseButtonEvent up) {
            return new IEventResult.Miss();
        }

        default void onResize() {
        }
    }

    private static final List<ILayer> layers = new ArrayList<>();

    public static void push(ILayer layer) {
        add0(layer);
        validateLayers();
    }

    private static void add0(ILayer layer) {
        byte priority = layer.priority();
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).priority() > priority) {
                layers.add(i, layer);
                return;
            }
        }

        layers.add(layer);
    }

    public static boolean remove(ILayer layer) {
        return remove(layer.getClass());
    }

    public static boolean remove(Class<? extends ILayer> clazz) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            ILayer layer = layers.get(i);
            if (layer.getClass() == clazz) {
                if (layer instanceof ILayer.Static) {
                    throw new UnsupportedOperationException();
                }

                layers.remove(i);
                return true;
            }
        }
        return false;
    }

    private static void validateLayers() {
        if (!FMLEnvironment.isProduction()) {
            Set<Class<? extends ILayer>> types = Collections.newSetFromMap(new IdentityHashMap<>());
            for (ILayer l : layers) {
                Class<? extends ILayer> clazz = l.getClass();
                if (!types.add(clazz)) {
                    throw new IllegalArgumentException("Duplicate layers with the same class detected: " + clazz);
                }
            }
        }
    }

    private static MouseButtonEvent down = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void on(ScreenEvent.Init.Pre event) {
        if (shouldHandle()) {
            down = null;
            dispatch(event, layer -> {
                layer.onResize();
                return new IEventResult.Miss();
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void on(ScreenEvent.MouseButtonPressed.Pre event) {
        if (shouldHandle()) {
            down = event.getMouseButtonEvent();
            dispatch(event, layer -> layer.onMouseButtonPressed(down));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void on(ScreenEvent.MouseButtonReleased.Pre event) {
        if (shouldHandle() && down != null) {
            MouseButtonEvent up = event.getMouseButtonEvent();
            dispatch(event, layer -> layer.onMouseButtonReleased(up));
            dispatch(event, layer -> layer.onMouseClicked(down, up));
            down = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void on(ClientTickEvent.Pre event) {
        if (shouldHandle()) {
            Pos mouse = Pos.getMouse();
            dispatch(event, layer -> layer.onMouseMove(mouse));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void on(ScreenEvent.Render.Pre event) {
        if (shouldHandle()) {
            for (ILayer layer : layers) {
                CursorType cursorType = layer.onRender();
                if (cursorType != null) {
                    event.getGuiGraphics().requestCursor(cursorType);
                    break;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void on(ScreenEvent.MouseScrolled.Pre event) {
        if (shouldHandle()) {
            dispatch(event, layer -> layer.onMouseScrolled(
                    Pos.ofLossy(event.getMouseX(), event.getMouseY()), (float) event.getScrollDeltaY()
            ));
        }
    }

    private static boolean shouldHandle() {
        return Minecraft.getInstance().screen instanceof AbstractEPScreen;
    }

    private static void dispatch(Event event, Function<ILayer, IEventResult> callback) {
        Screen screen = Objects.requireNonNull(Minecraft.getInstance().screen);

        for (int i = layers.size() - 1; i >= 0; i--) {
            ILayer layer = layers.get(i);
            if (layer.screen().isAssignableFrom(screen.getClass()) && callback.apply(layer) instanceof IEventResult.Consumed) {
                if (event instanceof ICancellableEvent ev) {
                    ev.setCanceled(true);
                }
                break;
            }
        }
    }
}
