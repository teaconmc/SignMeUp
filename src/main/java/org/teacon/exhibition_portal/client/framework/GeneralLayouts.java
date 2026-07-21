package org.teacon.exhibition_portal.client.framework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.teacon.exhibition_portal.client.framework.binding.LayoutParameter;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;

@EventBusSubscriber(Dist.CLIENT)
public final class GeneralLayouts {
    private GeneralLayouts() {
    }

    public static final LayoutParameter<Integer> WINDOW_WIDTH = LayoutParameter.of(0);

    public static final LayoutParameter<Integer> WINDOW_HEIGHT = LayoutParameter.of(0);

    public static final LayoutParameter<@Nullable Vector3f> PLAYER_POS = LayoutParameter.of(null);

    public static final LayoutParameter<@Nullable Vector2f> PLAYER_ROT = LayoutParameter.of(null);

    public static final LayoutParameter<@Nullable Screen> CURRENT_SCREEN = LayoutParameter.of(null);

    @SubscribeEvent
    private static void on(ScreenEvent.Init.Pre event) {
        WINDOW_WIDTH.set(event.getScreen().width);
        WINDOW_HEIGHT.set(event.getScreen().height);
    }

    @SubscribeEvent
    private static void on(ScreenEvent.Opening event) {
        CURRENT_SCREEN.set(event.getNewScreen());
    }

    @SubscribeEvent
    private static void on(ScreenEvent.Closing event) {
        if (RenderAccess.get(CURRENT_SCREEN) == event.getScreen()) {
            CURRENT_SCREEN.set(event.getScreen());
        }
    }

    @SubscribeEvent
    private static void on(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            PLAYER_POS.set(null);
            PLAYER_ROT.set(null);
        } else {
            Vector3f pos = RenderAccess.get(PLAYER_POS);
            Vec3 posNow = player.position();
            if (pos == null || pos.x != posNow.x || pos.y != posNow.y || pos.z != posNow.z) {
                PLAYER_POS.set(posNow.toVector3f());
            }

            Vector2f rot = RenderAccess.get(PLAYER_ROT);
            Vec2 rotNow = player.getRotationVector();
            if (rot == null || rot.x != rotNow.x || rot.y != rotNow.y) {
                PLAYER_ROT.set(new Vector2f(rotNow.x, rotNow.y));
            }
        }
    }
}
