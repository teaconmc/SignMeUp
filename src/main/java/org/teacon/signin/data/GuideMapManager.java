package org.teacon.signin.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.network.SyncGuideMapPacket;

import java.util.*;

public final class GuideMapManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("SignMeUp");
    private static final Marker MARKER = MarkerManager.getMarker("GuideMapManager");

    private static final Gson GSON = new GsonBuilder().setLenient()
            .registerTypeHierarchyAdapter(Component.class, new Component.Serializer())
            .registerTypeAdapter(GuideMap.class, new GuideMap.Serializer())
            .registerTypeAdapter(Waypoint.class, new Waypoint.Serializer())
            .registerTypeAdapter(Waypoint.Location.class, new Waypoint.Location.Serializer())
            .registerTypeAdapter(Trigger.class, new Trigger.Serializer())
            .registerTypeAdapter(Vec3i.class, new Vector3iAdapter())
            .create();

    private static <T> void setDiff(Set<T> a, Set<T> b, Set<T> aMinusB, Set<T> bMinusA) {
        aMinusB.addAll(a);
        aMinusB.removeAll(b);
        bMinusA.addAll(b);
        bMinusA.removeAll(a);
    }

    private final SortedMap<ResourceLocation, GuideMap> maps = new TreeMap<>();
    private final Map<ResourceLocation, Waypoint> points = new HashMap<>();
    private final Map<ResourceLocation, Trigger> triggers = new HashMap<>();

    public GuideMapManager() {
        // We are looking for any json files under `signup_guides` folder.
        super(GSON, "signup_guides");
    }

    /*
     * Each time a player switch to a different world, we sync all the guide maps belongs to
     * that world to that player.
     * "Switching to a world" includes logging into the game.
     * This way, we avoid syncing unnecessary data while keep maximum availability possible.
     */
    @SubscribeEvent
    public void sync(EntityJoinLevelEvent event) {
        // Being a ServerPlayerEntity implies a logical server, thus no isRemote check.
        if (event.getEntity() instanceof ServerPlayer p) {
            final SortedMap<ResourceLocation, GuideMap> mapsToSend = new TreeMap<>();
            this.maps.forEach((id, map) -> {
                if (p.level.dimension().location().equals(map.dim)) {
                    mapsToSend.put(id, map);
                }
            });
            SignMeUp.channel.sendTo(new SyncGuideMapPacket(mapsToSend), p.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.START) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            for (Map.Entry<ResourceLocation, Waypoint> entry : this.points.entrySet()) {
                ResourceLocation key = entry.getKey();
                Waypoint wp = entry.getValue();
                tickOne(key, wp, server);
            }
            for (Map.Entry<ResourceLocation, Trigger> entry : this.triggers.entrySet()) {
                ResourceLocation id = entry.getKey();
                Trigger trigger = entry.getValue();
                tickOne(id, trigger, server);
            }
        }
    }

    private static void tickOne(ResourceLocation id, PlayerTracker trackingComponent, MinecraftServer server) {
        /*
         * So there are two sets of players:
         *   1. Those who can see this waypoint in previous tick
         *   2. Those who should see this waypoint in the next tick
         * There are three cases to handle:
         *   a. For players in both sets, nothing will happen.
         *      These players form the intersection of set 1 and 2.
         *   b. For players in set 1 but not set 2, they will receive a
         *      packet so the client manager will remove that waypoint.
         *      These players form the set diff 1 - 2.
         *   c. For players in set 2 but not set 1, they will receive a
         *      packet so the client manager will receive that waypoint.
         *      These players form the set diff 2 - 1.
         */
        final Set<ServerPlayer> matched = Collections.newSetFromMap(new WeakHashMap<>());
        var selector = trackingComponent.getSelector();
        if (selector == null) {
            return;
        }
        try {
            matched.addAll(selector.findPlayers(server.createCommandSourceStack()));
        } catch (CommandSyntaxException e) {
            return;
        }
        final Set<ServerPlayer> update = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<ServerPlayer> removal = Collections.newSetFromMap(new IdentityHashMap<>());
        setDiff(trackingComponent.getTracking(), matched, removal, update);
        for (ServerPlayer p : update) {
            SignMeUp.channel.sendTo(trackingComponent.getNotifyPacket(false, id), p.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
        for (ServerPlayer p : removal) {
            SignMeUp.channel.sendTo(trackingComponent.getNotifyPacket(true, id), p.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
        trackingComponent.setTracking(matched);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        this.maps.clear();
        this.points.clear();
        this.triggers.clear();
        profiler.push("SignInGuides");
        objects.forEach(this::process);
        profiler.pop();
    }

    private void process(ResourceLocation id, JsonElement json) {
        if (this.maps.containsKey(id) || this.points.containsKey(id) || this.triggers.containsKey(id)) {
            return;
        }
        if (json.isJsonObject()) {
            final JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("type")) {
                final String type = jsonObject.get("type").getAsString();
                if ("map".equals(type)) {
                    this.maps.putIfAbsent(id, GSON.fromJson(json, GuideMap.class));
                } else if ("point".equals(type)) {
                    this.points.putIfAbsent(id, GSON.fromJson(json, Waypoint.class));
                } else if ("trigger".equals(type)) {
                    this.triggers.putIfAbsent(id, GSON.fromJson(json, Trigger.class));
                } else {
                    LOGGER.warn(MARKER, "{} declares an invalid type '{}' (must be one of: map, point, trigger)", id, type);
                }
            } else {
                LOGGER.warn(MARKER, "{} is missing required field 'type', skipping", id);
            }
        } else {
            LOGGER.warn(MARKER, "{} doesn't appear to be a JSON object, skipping", id);
        }
    }

    public Waypoint findWaypoint(ResourceLocation waypointId) {
        return this.points.get(waypointId);
    }

    public Trigger findTrigger(ResourceLocation triggerId) {
        return this.triggers.get(triggerId);
    }

    public GuideMap findMap(ResourceLocation mapId) {
        return this.maps.get(mapId);
    }

    public ResourceLocation findMapId(GuideMap map) {
        ResourceLocation id = null;
        for (Map.Entry<ResourceLocation, GuideMap> entry: this.maps.entrySet()) {
            if (entry.getValue().equals(map)) {
                id = entry.getKey();
            }
        }
        return id;
    }

    public Collection<Waypoint> getAllWaypoints() {
        return this.points.values();
    }

    public Collection<? extends ResourceLocation> getAllTriggers() {
        return this.triggers.keySet();
    }

    public Collection<GuideMap> getAllMaps() {
        return this.maps.values();
    }

}
