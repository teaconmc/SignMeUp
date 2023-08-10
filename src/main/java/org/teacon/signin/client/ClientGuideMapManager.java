package org.teacon.signin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import org.teacon.signin.data.entity.GuideMap;
import org.teacon.signin.data.entity.Trigger;
import org.teacon.signin.data.entity.Waypoint;
import org.teacon.signin.network.MapScreenPacket;

import java.util.*;

public final class ClientGuideMapManager {

    private SortedMap<ResourceLocation, GuideMap> availableMaps = Collections.emptySortedMap();
    private final Map<ResourceLocation, Waypoint> availableWaypoints = new HashMap<>();
    private final Map<ResourceLocation, Trigger> availableTriggers = new HashMap<>();

    public void openMapByPacket(MapScreenPacket.Action action, ResourceLocation mapId, Vec3 position) {
        Minecraft mc = Minecraft.getInstance();
        mc.submitAsync(() -> {
            if (action == MapScreenPacket.Action.OPEN_SPECIFIC) {
                Objects.requireNonNull(mapId);
                GuideMap map = SignMeUpClient.MANAGER.findMap(mapId);
                mc.setScreen(new GuideMapScreen(mapId, map, position));
            } else if (mc.screen instanceof GuideMapScreen) {
                final GuideMapScreen screen = (GuideMapScreen) mc.screen;
                if (action != MapScreenPacket.Action.CLOSE_SPECIFIC || screen.mapId.equals(mapId)) {
                    mc.setScreen(null);
                }
            }
        });
    }

    public synchronized void acceptUpdateFromServer(SortedMap<ResourceLocation, GuideMap> maps) {
        this.availableMaps = maps;
    }

    public Map.Entry<ResourceLocation, GuideMap> nearestTo(Vec3 pos) {
        double minDistanceSq = Double.MAX_VALUE;
        Map.Entry<ResourceLocation, GuideMap> result = null;
        for (Map.Entry<ResourceLocation, GuideMap> entry : this.availableMaps.entrySet()) {
            // Skip the dimension check because the client manager only knows
            // guide maps that are for the current dimension.
            final GuideMap guideMap = entry.getValue();
            final double dx = pos.x() - guideMap.center.getX();
            final double dz = pos.z() - guideMap.center.getZ();
            if (Math.min(Math.abs(dx), Math.abs(dz)) <= guideMap.radius) {
                final double distanceSq = dx * dx + dz * dz;
                if (distanceSq < minDistanceSq) {
                    minDistanceSq = distanceSq;
                    result = entry;
                }
            }
        }
        return result;
    }

    public Collection<? extends ResourceLocation> getAllTriggers() {
        return Collections.unmodifiableSet(this.availableTriggers.keySet());
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
        this.availableWaypoints.remove(waypointId);
    }

    public synchronized void addTrigger(ResourceLocation triggerId, Trigger trigger) {
        this.availableTriggers.put(triggerId, trigger);
    }

    public synchronized void removeTrigger(ResourceLocation triggerId) {
        this.availableTriggers.remove(triggerId);
    }
}
