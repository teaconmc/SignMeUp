package org.teacon.signin.data.entity;

import com.google.gson.*;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class GuideMap {
    private Component title;
    private Component subtitle;
    private Component desc;

    public Vec3i center;
    public float radius = 128F;
    public ResourceLocation dim = null;

    // TODO Actually ensure the missing texture exists
    public ResourceLocation texture = new ResourceLocation("minecraft", "missing");

    private List<ResourceLocation> triggerIds = Collections.emptyList();
    private List<ResourceLocation> waypointIds = Collections.emptyList();

    public Component getTitle() {
        return this.title != null ? this.title : Component.translatable("sign_up.map.unnamed");
    }

    public Component getSubtitle() {
        return this.subtitle != null ? this.subtitle : this.getTitle();
    }

    public Component getDesc() {
        return this.desc != null ? this.desc : Component.empty();
    }

    public List<ResourceLocation> getWaypointIds() {
        return Collections.unmodifiableList(this.waypointIds);
    }

    public List<ResourceLocation> getTriggerIds() {
        return Collections.unmodifiableList(this.triggerIds);
    }

    public static final class Serializer implements JsonDeserializer<GuideMap>, JsonSerializer<GuideMap> {

        private static final Logger LOGGER = LogManager.getLogger("SignMeUp");
        private static final Marker MARKER = MarkerManager.getMarker("GuideMapS11n");

        @Override
        public GuideMap deserialize(JsonElement src, Type type, JsonDeserializationContext context) throws JsonParseException {
            final GuideMap map = new GuideMap();
            if (!src.isJsonObject()) {
                throw new JsonSyntaxException("Guide map must be JSON Object");
            }
            final JsonObject json = src.getAsJsonObject();
            if (json.has("title")) {
                map.title = context.deserialize(json.get("title"), Component.class);
            }
            if (json.has("subtitle")) {
                map.subtitle = context.deserialize(json.get("subtitle"), Component.class);
            }
            if (json.has("description")) {
                map.desc = context.deserialize(json.get("description"), Component.class);
            }
            if (json.has("center")) {
                map.center = context.deserialize(json.get("center"), Vec3i.class);
            } else {
                LOGGER.warn(MARKER, "Center coordinate missing, falling back to [0, 0].");
            }
            if (json.has("range")) {
                final float range = json.get("range").getAsFloat();
                if (range > 0) {
                    map.radius = range / 2F;
                } else {
                    LOGGER.warn(MARKER, "Positive range missing, falling back to 256 (blocks)");
                }
            } else {
                LOGGER.warn(MARKER, "Positive range missing, falling back to 256 (blocks)");
            }
            if (json.has("world")) {
                map.dim = new ResourceLocation(json.get("world").getAsString());
            }
            if (json.has("texture")) {
                map.texture = new ResourceLocation(json.get("texture").getAsString());
            }
            if (json.has("points")) {
                map.waypointIds = StreamSupport.stream(json.getAsJsonArray("points").spliterator(), false)
                        .map(JsonElement::getAsString)
                        .map(ResourceLocation::new)
                        .collect(Collectors.toList());
            }
            if (json.has("triggers")) {
                map.triggerIds = StreamSupport.stream(json.getAsJsonArray("triggers").spliterator(), false)
                        .map(JsonElement::getAsString)
                        .map(ResourceLocation::new)
                        .collect(Collectors.toList());
            }
            return map;
        }

        @Override
        public JsonElement serialize(GuideMap src, Type type, JsonSerializationContext context) {
            final JsonObject json = new JsonObject();
            if (src.title != null) {
                json.add("title", context.serialize(src.title));
            }
            if (src.subtitle != null) {
                json.add("subtitle", context.serialize(src.subtitle));
            }
            if (src.desc != null) {
                json.add("description", context.serialize(src.desc));
            }
            if (src.center != null) {
                json.add("center", context.serialize(src.center));
            }
            json.add("range", new JsonPrimitive(src.radius * 2));
            if (src.dim != null) {
                json.add("world", new JsonPrimitive(src.dim.toString()));
            }
            if (src.texture != null) {
                json.add("texture", new JsonPrimitive(src.texture.toString()));
            }
            if (src.waypointIds != null && !src.waypointIds.isEmpty()) {
                json.add("points", src.waypointIds.stream()
                        .map(ResourceLocation::toString)
                        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            }
            if (src.triggerIds != null && !src.triggerIds.isEmpty()) {
                json.add("triggers", src.triggerIds.stream()
                        .map(ResourceLocation::toString)
                        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            }
            return json;
        }
    }
}