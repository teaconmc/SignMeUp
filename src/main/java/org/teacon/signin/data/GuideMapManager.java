package org.teacon.signin.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.network.PartialUpdate;
import org.teacon.signin.network.SyncGuideMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

public class GuideMapManager extends JsonReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("SignMeIn");
    private static final Marker MARKER = MarkerManager.getMarker("GuideMapManager");

    private static final Gson GSON = new GsonBuilder().setLenient()
            .registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer())
            .registerTypeAdapter(GuideMap.class, new GuideMap.Serializer())
            .registerTypeAdapter(Waypoint.Location.class, new Waypoint.Location.Serializer())
            .registerTypeAdapter(Trigger.class, new Trigger.Serializer())
            .registerTypeAdapter(ResourceLocation.class, new Serializers.ResourceLocationSerializer())
            .registerTypeAdapter(Vector3i.class, new Serializers.Vec3iSerializer())
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
    public void sync(EntityJoinWorldEvent event) {
        // Being a ServerPlayerEntity implies a logical server, thus no isRemote check.
        if (event.getEntity() instanceof ServerPlayerEntity) {
            final ServerPlayerEntity p = (ServerPlayerEntity) event.getEntity();
            final SortedMap<ResourceLocation, GuideMap> mapsToSend = new TreeMap<>();
            this.maps.forEach((id, map) -> {
                if (p.world.getDimensionKey().getLocation().equals(map.dim)) {
                    mapsToSend.put(id, map);
                }
            });
            SignMeUp.channel.sendTo(new SyncGuideMap(mapsToSend), p.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.START) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            this.points.forEach((id, wp) -> {
                try {
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
                    final Set<ServerPlayerEntity> matched = Collections.newSetFromMap(new WeakHashMap<>());
                    matched.addAll(wp.getSelector().selectPlayers(server.getCommandSource()));
                    final Set<ServerPlayerEntity> update = Collections.newSetFromMap(new IdentityHashMap<>());
                    final Set<ServerPlayerEntity> removal = Collections.newSetFromMap(new IdentityHashMap<>());
                    setDiff(wp.visiblePlayers, matched, removal, update);
                    for (ServerPlayerEntity p : update) {
                        SignMeUp.channel.sendTo(new PartialUpdate(PartialUpdate.Mode.ADD_WAYPOINT, id, wp), p.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
                    }
                    for (ServerPlayerEntity p : removal) {
                        SignMeUp.channel.sendTo(new PartialUpdate(PartialUpdate.Mode.REMOVE_WAYPOINT, id, wp), p.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
                    }
                    wp.visiblePlayers = matched;
                } catch (CommandSyntaxException ignored) {
                    // tfw entity selector requires some permission level...
                }
            });
            this.triggers.forEach((id, trigger) -> {
                try {
                    final Set<ServerPlayerEntity> matched = Collections.newSetFromMap(new WeakHashMap<>());
                    matched.addAll(trigger.getSelector().selectPlayers(server.getCommandSource()));
                    final Set<ServerPlayerEntity> update = Collections.newSetFromMap(new IdentityHashMap<>());
                    final Set<ServerPlayerEntity> removal = Collections.newSetFromMap(new IdentityHashMap<>());
                    setDiff(trigger.visiblePlayers, matched, removal, update);
                    for (ServerPlayerEntity p : update) {
                        SignMeUp.channel.sendTo(new PartialUpdate(PartialUpdate.Mode.ADD_TRIGGER, id, trigger), p.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
                    }
                    for (ServerPlayerEntity p : removal) {
                        SignMeUp.channel.sendTo(new PartialUpdate(PartialUpdate.Mode.REMOVE_TRIGGER, id, trigger), p.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
                    }
                    trigger.visiblePlayers = matched;
                } catch (CommandSyntaxException ignored) {
                    // tfw entity selector requires some permission level...
                }
            });
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, IResourceManager manager, IProfiler profiler) {
        this.maps.clear();
        this.points.clear();
        this.triggers.clear();
        profiler.startSection("SignInGuides");
        objects.forEach(this::process);
        profiler.endSection();
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


}
