package org.teacon.signmeup.config.waypoints;

import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerLifecycleEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.network.SyncWaypointPacket;

import java.util.List;
import java.util.Objects;

@EventBusSubscriber(modid = SignMeUp.MODID, bus = EventBusSubscriber.Bus.GAME)
public class WaypointServerEvents {
    @SubscribeEvent
    public static void onServerLaunch(ServerStartedEvent event) {
        Waypoint.load();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                LocalPlayer lp = Minecraft.getInstance().player;
                if (lp != null && Objects.equals(lp.getUUID(), player.getUUID())) {
                    // If this is a client, we only use in-memory communication with the client.
                    return;
                }
            }

            player.server.execute(() -> NetworkHelper.sendToPlayer(player, new SyncWaypointPacket(
                    List.copyOf(Waypoint.INSTANCES.values())
            )));
        }
    }
}
