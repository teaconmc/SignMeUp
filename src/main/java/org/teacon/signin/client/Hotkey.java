package org.teacon.signin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.teacon.signin.data.GuideMap;

@Mod.EventBusSubscriber(modid = "sign_up", value = Dist.CLIENT)
public final class Hotkey {

    @SubscribeEvent
    public static void keyTyped(InputEvent.KeyInputEvent event) {
        if (SignMeUpClient.keyOpenMap != null && SignMeUpClient.keyOpenMap.isPressed()) {
            Minecraft mc = Minecraft.getInstance();
            final GuideMap map = SignMeUpClient.MANAGER.nearestTo(mc.player);
            if (map != null) {
                mc.displayGuiScreen(new GuideMapScreen(map));
            } else if (mc.player != null) {
                mc.player.sendStatusMessage(new TranslationTextComponent("sign_up.status.no_map_available"), true);
            }
        }
    }
}
