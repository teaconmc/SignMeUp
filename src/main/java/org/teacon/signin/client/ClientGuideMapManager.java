package org.teacon.signin.client;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Trigger;
import org.teacon.signin.data.Waypoint;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

public class ClientGuideMapManager {

    private SortedMap<ResourceLocation, GuideMap> availableMaps = Collections.emptySortedMap();
    private Map<ResourceLocation, Waypoint> availableWaypoints = Collections.emptyMap();
    private Map<ResourceLocation, Trigger> availableTriggers = Collections.emptyMap();

    public synchronized void acceptUpdateFromServer(SortedMap<ResourceLocation, GuideMap> maps,
                                                    Map<ResourceLocation, Waypoint> waypoints,
                                                    Map<ResourceLocation, Trigger> triggers) {
        this.availableMaps = maps;
        this.availableWaypoints = waypoints;
        this.availableTriggers = triggers;
    }

    public GuideMap nearestTo(ClientPlayerEntity player) {
        for (GuideMap map : this.availableMaps.values()) {
            // Skip the dimension check because the client manager only knows
            // guide maps that are for the current dimension.
            final Vector3d destination = Vector3d.copyCenteredWithVerticalOffset(map.center, player.getPosY());
            if (player.getPosition().withinDistance(destination, map.range)) {
                return map;
            }
        }
        return null;
    }

    public Trigger findTrigger(ResourceLocation triggerId) {
        return this.availableTriggers.get(triggerId);
    }

    public Waypoint findWaypoint(ResourceLocation waypointId) {
        return this.availableWaypoints.get(waypointId);
    }

    public synchronized void addWaypoint(ResourceLocation waypointId, Waypoint waypoint) {
        this.availableWaypoints.put(waypointId, waypoint);
    }

    public synchronized void removeWaypoint(ResourceLocation waypointId) {
        final Waypoint wp = this.availableWaypoints.remove(waypointId);
        if (wp != null) {
            wp.disabled = true;
        }
    }

    public synchronized void addTrigger(ResourceLocation triggerId, Trigger trigger) {
        this.availableTriggers.put(triggerId, trigger);
    }

    public synchronized void removeTrigger(ResourceLocation triggerId) {
        final Trigger t = this.availableTriggers.remove(triggerId);
        if (t != null) {
            t.disabled = true;
        }
    }
}
