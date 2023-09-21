package org.teacon.signin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.teacon.signin.data.entity.GuideMap;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "sign_up", value = Dist.CLIENT)
public final class Hotkey {

    @SubscribeEvent
    public static void keyTyped(InputEvent.Key event) {
        if (SignMeUpClient.keyOpenMap != null && SignMeUpClient.keyOpenMap.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                final Vec3 position = mc.player.position();
                final Map.Entry<ResourceLocation, GuideMap> entry = SignMeUpClient.MANAGER.nearestTo(position);
                if (entry != null) {
                    mc.setScreen(new GuideMapScreen(entry.getKey(), entry.getValue(), position));
                } else {
                    mc.player.displayClientMessage(Component.translatable("sign_up.status.no_map_available"), true);
                }
            }
        }
    }
}
