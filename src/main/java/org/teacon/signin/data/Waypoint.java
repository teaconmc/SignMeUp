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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.signin.network.PartialUpdatePacket;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class Waypoint implements PlayerTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Waypoint.class);

    public static final class Location {
        Vec3i actualLocation;
        private Vec3i renderLocation;
        boolean isDynamic = false;

        public Vec3i getRenderLocation() {
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
                    loc.actualLocation = context.deserialize(locations.get("actual"), Vec3i.class);
                    if (locations.has("render")) {
                        loc.renderLocation = context.deserialize(locations.get("render"), Vec3i.class);
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

    private Component title;
    private Component desc;

    private String selector = "@e";
    private transient EntitySelector parsedSelector;
    private transient boolean invalid = false;

    private Location location;

    private List<ResourceLocation> triggerIds = Collections.emptyList();

    private List<ResourceLocation> imageIds = Collections.emptyList();

    private int displayingImageIndex;

    transient Set<ServerPlayer> visiblePlayers = Collections.newSetFromMap(new WeakHashMap<>());

    public Component getTitle() {
        return title == null ? new TranslatableComponent("sign_up.waypoint.unnamed") : this.title;
    }

    public Component getDesc() {
        return this.desc == null ? TextComponent.EMPTY : this.desc;
    }

    public boolean hasDynamicLocation() {
        return location.isDynamic;
    }

    public Vec3i getRenderLocation() {
        return location.getRenderLocation();
    }

    public Vec3i getActualLocation() {
        return location.actualLocation;
    }

    public List<ResourceLocation> getTriggerIds() {
        return this.triggerIds;
    }

    public boolean hasMoreThanOneImage() {
        return this.imageIds.size() > 1;
    }

    public void modifyDisplayingImageIndex(int diff) {
        if (!this.imageIds.isEmpty()) {
            this.displayingImageIndex = Math.floorMod(this.displayingImageIndex + diff, this.imageIds.size());
        }
    }

    public ResourceLocation getDisplayingImageId() {
        return this.imageIds.isEmpty() ? GuideMap.DEFAULT_IMAGE : this.imageIds.get(this.displayingImageIndex);
    }

    @Override
    public EntitySelector getSelector() {
        if (this.parsedSelector == null && !this.invalid) {
            try {
                this.parsedSelector = new EntitySelectorParser(new StringReader(this.selector)).parse();
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Invalid selector: {}", this.selector);
                this.invalid = true;
                return null;
            }
        }
        return this.parsedSelector;
    }

    @Override
    public Set<ServerPlayer> getTracking() {
        return this.visiblePlayers;
    }

    @Override
    public void setTracking(Set<ServerPlayer> players) {
        this.visiblePlayers = players;
    }

    @Override
    public PartialUpdatePacket getNotifyPacket(boolean remove, ResourceLocation id) {
        return new PartialUpdatePacket(remove ? PartialUpdatePacket.Mode.REMOVE_WAYPOINT : PartialUpdatePacket.Mode.ADD_WAYPOINT, id, this);
    }

    public static final class Serializer implements JsonDeserializer<Waypoint>, JsonSerializer<Waypoint> {

        @Override
        public Waypoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                final JsonObject obj = json.getAsJsonObject();
                final Waypoint wp = new Waypoint();
                if (obj.has("title")) {
                    wp.title = context.deserialize(obj.get("title"), Component.class);
                }
                if (obj.has("description")) {
                    wp.desc = context.deserialize(obj.get("description"), Component.class);
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
                if (obj.has("images")) {
                    wp.imageIds = StreamSupport.stream(obj.getAsJsonArray("images").spliterator(), false)
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
            json.add("selector", new JsonPrimitive(src.selector));
            json.add("location", context.serialize(src.location));
            if (!src.triggerIds.isEmpty()) {
                json.add("triggers", src.triggerIds.stream()
                        .map(ResourceLocation::toString)
                        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            }
            if (!src.imageIds.isEmpty()) {
                json.add("images", src.imageIds.stream()
                        .map(ResourceLocation::toString)
                        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            }
            return json;
        }
    }
}
