package org.teacon.signin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.teacon.signin.data.GuideMap;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "sign_up", value = Dist.CLIENT)
public final class Hotkey {

    @SubscribeEvent
    public static void keyTyped(InputEvent.KeyInputEvent event) {
        if (SignMeUpClient.keyOpenMap != null && SignMeUpClient.keyOpenMap.isPressed()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                final Vector3d position = mc.player.getPositionVec();
                final Map.Entry<ResourceLocation, GuideMap> entry = SignMeUpClient.MANAGER.nearestTo(position);
                if (entry != null) {
                    mc.displayGuiScreen(new GuideMapScreen(entry.getKey(), entry.getValue(), position));
                } else {
                    mc.player.sendStatusMessage(new TranslationTextComponent("sign_up.status.no_map_available"), true);
                }
            }
        }
    }
}
