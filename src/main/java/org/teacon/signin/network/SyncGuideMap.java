package org.teacon.signin.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.client.SignMeUpClient;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Trigger;
import org.teacon.signin.data.Waypoint;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

public class SyncGuideMap {

    private static final Gson GSON = new GsonBuilder().setLenient()
            .registerTypeAdapter(GuideMap.class, new GuideMap.Serializer())
            .registerTypeAdapter(Waypoint.class, new Waypoint.Serializer())
            .registerTypeAdapter(Trigger.class, new Trigger.Serializer())
            .registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer())
            .create();

    private final SortedMap<ResourceLocation, GuideMap> maps;
    private final Map<ResourceLocation, Waypoint> waypoints = new HashMap<>();
    private final Map<ResourceLocation, Trigger> triggers = new HashMap<>();

    public SyncGuideMap(SortedMap<ResourceLocation, GuideMap> mapsToSend) {
        for (GuideMap map : (this.maps = mapsToSend).values()) {
            for (ResourceLocation waypointId : map.getWaypointIds()) {
                this.waypoints.put(waypointId, SignMeUp.MANAGER.findWaypoint(waypointId));
            }
            for (ResourceLocation triggerId : map.getTriggerIds()) {
                this.triggers.put(triggerId, SignMeUp.MANAGER.findTrigger(triggerId));
            }
        }
    }

    public SyncGuideMap(PacketBuffer buf) {
        this.maps = new TreeMap<>(); // We want all guide maps to be sorted by key.
        try {
            final String src = new String(buf.readByteArray(), StandardCharsets.UTF_8);
            final JsonObject json = GSON.fromJson(src, JsonObject.class);
            json.getAsJsonObject("maps").entrySet().forEach(e -> SyncGuideMap.accept(e, GuideMap.class, this.maps));
            json.getAsJsonObject("waypoints").entrySet().forEach(e -> SyncGuideMap.accept(e, Waypoint.class, this.waypoints));
            json.getAsJsonObject("triggers").entrySet().forEach(e -> SyncGuideMap.accept(e, Trigger.class, this.triggers));
        } catch (Exception ignored) {
            ignored.printStackTrace(System.err);
        }
    }

    private static <T> void accept(Map.Entry<String, JsonElement> entry, Class<T> type, Map<ResourceLocation, T> dst) {
        dst.put(new ResourceLocation(entry.getKey()), GSON.fromJson(entry.getValue(), type));
    }

    public void write(PacketBuffer buf) {
        final JsonObject json = new JsonObject();
        json.add("maps", GSON.toJsonTree(this.maps));
        json.add("waypoints", GSON.toJsonTree(this.waypoints));
        json.add("triggers", GSON.toJsonTree(this.triggers));
        final String payload = GSON.toJson(json); //.getBytes(StandardCharsets.UTF_8);
        buf.writeByteArray(payload.getBytes(StandardCharsets.UTF_8));
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        // acceptUpdateFromServer has been marked with synchronized keyword,
        // thus we can do this directly on the netty worker thread.
        SignMeUpClient.MANAGER.acceptUpdateFromServer(this.maps, this.waypoints, this.triggers);
        contextGetter.get().setPacketHandled(true);
    }

}
