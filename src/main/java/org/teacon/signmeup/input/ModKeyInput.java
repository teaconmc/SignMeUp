package org.teacon.signmeup.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import org.teacon.signmeup.gui.map.MapScreen;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ModKeyInput {
    public static final KeyMapping OPEN_MAP = new KeyMapping(
            "key.sign_up.open_map", KeyConflictContext.IN_GAME, KeyModifier.NONE,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.sign_up"
    );
    public static final KeyMapping OPEN_NEW_MAP = new KeyMapping(
            "key.sign_up.open_new_map", KeyConflictContext.IN_GAME, KeyModifier.NONE,
            InputConstants.Type.KEYSYM, -1, "key.categories.sign_up"
    );


    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension() == Level.OVERWORLD) {
            MapScreen screen;
            if (OPEN_NEW_MAP.consumeClick()) {
                screen = MapScreen.getNewInstance();
            } else if (OPEN_MAP.consumeClick()) {
                screen = MapScreen.getInstance();
            } else {
                return;
            }
            mc.setScreen(screen);
            screen.layout();
        }
    }
}
