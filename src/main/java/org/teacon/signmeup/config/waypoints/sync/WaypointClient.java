package org.teacon.signmeup.config.waypoints.sync;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.waypoints.Waypoint;

@EventBusSubscriber(value = Dist.CLIENT, modid = SignMeUp.MODID, bus = EventBusSubscriber.Bus.GAME)
public class WaypointClient {
    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Waypoint.INSTANCES.clear();
    }
}
