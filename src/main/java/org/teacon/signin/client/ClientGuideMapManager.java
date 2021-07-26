package org.teacon.signin.client;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Trigger;
import org.teacon.signin.data.Waypoint;

import java.util.*;

public final class ClientGuideMapManager {

    private SortedMap<ResourceLocation, GuideMap> availableMaps = Collections.emptySortedMap();
    private final Map<ResourceLocation, Waypoint> availableWaypoints = new HashMap<>();
    private final Map<ResourceLocation, Trigger> availableTriggers = new HashMap<>();

    public synchronized void acceptUpdateFromServer(SortedMap<ResourceLocation, GuideMap> maps) {
        this.availableMaps = maps;
    }

    public Map.Entry<ResourceLocation, GuideMap> nearestTo(ClientPlayerEntity player) {
        for (Map.Entry<ResourceLocation, GuideMap> entry : this.availableMaps.entrySet()) {
            // Skip the dimension check because the client manager only knows
            // guide maps that are for the current dimension.
            final GuideMap map = entry.getValue();
            final Vector3d destination = Vector3d.copyCenteredWithVerticalOffset(map.center, player.getPosY());
            if (player.getPosition().withinDistance(destination, map.range)) {
                return entry;
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

    public GuideMap findMap(ResourceLocation mapId) {
        return this.availableMaps.get(mapId);
    }

    public Collection<GuideMap> getAllMaps() {
        return this.availableMaps.values();
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
