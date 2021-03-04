package org.teacon.signin.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.StringReader;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.command.arguments.EntitySelectorParser;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.teacon.signin.network.PartialUpdate;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Waypoint implements PlayerTracker {

    public static final class Location {
        Vector3i actualLocation;
        private Vector3i renderLocation;
        boolean isDynamic = false;

        public Vector3i getRenderLocation() {
            return this.renderLocation == null ? this.actualLocation : this.renderLocation;
        }

        public static final class Serializer implements JsonDeserializer<Location>, JsonSerializer<Location> {


            @Override
            public Location deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
                final Location loc = new Location();
                if (json.isJsonPrimitive() && "dynamic".equals(json.getAsString())) {
                    loc.isDynamic = true;
                    return loc;
                } else if (json.isJsonObject()) {
                    final JsonObject locations = json.getAsJsonObject();
                    loc.actualLocation = context.deserialize(locations.get("actual"), Vector3i.class);
                    if (locations.has("render")) {
                        loc.renderLocation = context.deserialize(locations.get("render"), Vector3i.class);
                    }
                    return loc;
                } else {
                    throw new JsonParseException("Location can only be either an object or 'dynamic'");
                }
            }

            @Override
            public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
                if (src.isDynamic) {
                    return new JsonPrimitive("dynamic");
                } else {
                    final JsonObject locations = new JsonObject();
                    locations.add("actual", context.serialize(src.actualLocation));
                    if (src.renderLocation != null) {
                        locations.add("render", context.serialize(src.renderLocation));
                    }
                    return locations;
                }
            }
        }
    }

    public ITextComponent title;
    public ITextComponent desc;

    public volatile boolean disabled = false;

    String selector = "@e";
    private transient EntitySelector parsedSelector;

    Location location;

    List<ResourceLocation> triggerIds = Collections.emptyList();

    transient Set<ServerPlayerEntity> visiblePlayers = Collections.newSetFromMap(new WeakHashMap<>());

    public ITextComponent getTitle() {
        return title == null ? new TranslationTextComponent("sign_me_in.waypoint.unnamed") : this.title;
    }

    public ITextComponent getDesc() {
        return this.desc == null ? StringTextComponent.EMPTY : this.desc;
    }

    public boolean hasDynamicLocation() {
        return location.isDynamic;
    }

    public Vector3i getRenderLocation() {
        return location.getRenderLocation();
    }

    public List<ResourceLocation> getTriggerIds() {
        return this.triggerIds;
    }

    @Override
    public EntitySelector getSelector() {
        if (parsedSelector == null) {
            try {
                return (parsedSelector = new EntitySelectorParser(new StringReader(this.selector)).parse());
            } catch (Exception ignored) {

            }
        }
        return parsedSelector;
    }

    @Override
    public Set<ServerPlayerEntity> getTracking() {
        return this.visiblePlayers;
    }

    @Override
    public void setTracking(Set<ServerPlayerEntity> players) {
        this.visiblePlayers = players;
    }

    @Override
    public PartialUpdate getNotifyPacket(boolean remove, ResourceLocation id) {
        return new PartialUpdate(remove ? PartialUpdate.Mode.REMOVE_WAYPOINT : PartialUpdate.Mode.ADD_WAYPOINT, id, this);
    }

    public static final class Serializer implements JsonDeserializer<Waypoint>, JsonSerializer<Waypoint> {

        @Override
        public Waypoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                final JsonObject obj = json.getAsJsonObject();
                final Waypoint wp = new Waypoint();
                if (obj.has("title")) {
                    wp.title = context.deserialize(obj.get("title"), ITextComponent.class);
                }
                if (obj.has("description")) {
                    wp.title = context.deserialize(obj.get("description"), ITextComponent.class);
                }
                if (obj.has("disabled")) {
                    wp.disabled = obj.get("disabled").getAsBoolean();
                }
                if (obj.has("selector")) {
                    wp.selector = obj.get("selector").getAsString();
                }
                wp.location = context.deserialize(obj.get("location"), Location.class);
                if (obj.has("triggers")) {
                    wp.triggerIds = StreamSupport.stream(obj.getAsJsonArray("triggers").spliterator(), false)
                            .map(JsonElement::getAsString)
                            .map(ResourceLocation::new)
                            .collect(Collectors.toList());
                }
                return wp;
            } else {
                throw new JsonParseException("Trigger must be a JSON Object");
            }
        }

        @Override
        public JsonElement serialize(Waypoint src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject json = new JsonObject();
            if (src.title != null) {
                json.add("title", context.serialize(src.title));
            }
            if (src.desc != null) {
                json.add("description", context.serialize(src.desc));
            }
            json.add("disabled", new JsonPrimitive(src.disabled));
            json.add("selector", new JsonPrimitive(src.selector));
            json.add("location", context.serialize(src.location));
            if (!src.triggerIds.isEmpty()) {
                json.add("triggers", src.triggerIds.stream()
                        .map(ResourceLocation::toString)
                        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            }
            return json;
        }
    }
}
