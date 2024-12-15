package org.teacon.signmeup.config;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.network.SetWaypointPacket;

import java.util.Objects;

@EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = SignMeUp.MODID, bus = EventBusSubscriber.Bus.GAME)
public class WaypointSyncHandle {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                LocalPlayer lp = Minecraft.getInstance().player;
                if (lp != null && Objects.equals(lp.getUUID(), player.getUUID())) {
                    return;
                }
            }

            player.server.execute(() -> {
                for (Waypoints.WayPoint waypoint : ConfigHelper.getConfigRead(Waypoints.class).waypoints) {
                    NetworkHelper.sendToPlayer(player, new SetWaypointPacket(waypoint.name, waypoint.description, new BlockPos(waypoint.x, waypoint.y, waypoint.z), new Rotations(waypoint.rx, 0, waypoint.ry)));
                }
            });
        }
    }
}
