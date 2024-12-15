package org.teacon.signmeup.config.waypoints;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.teacon.signmeup.SignMeUp;

@EventBusSubscriber(value = Dist.CLIENT, modid = SignMeUp.MODID, bus = EventBusSubscriber.Bus.GAME)
public class WaypointClientEvents {
    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Waypoint.INSTANCES.clear();
    }
}
