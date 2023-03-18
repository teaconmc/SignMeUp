package org.teacon.signin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;


@Mod.EventBusSubscriber(modid = "sign_up", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SignMeUpClient {

    public static final ClientGuideMapManager MANAGER = new ClientGuideMapManager();

    static KeyMapping keyOpenMap;

    @SubscribeEvent
    public static void setup(RegisterKeyMappingsEvent event) {
        event.register(keyOpenMap = new KeyMapping("sign_up.keybinding.open_map",
                KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "sign_up.keybinding"));
    }
}
