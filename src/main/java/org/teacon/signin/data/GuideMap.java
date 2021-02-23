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
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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

    ITextComponent title;
    ITextComponent subtitle;
    ITextComponent desc;

    public int range = 256;
    public Vector3i center;
    public ResourceLocation dim = null;

    // TODO Actually ensure the missing texture exists
    public ResourceLocation texture = new ResourceLocation("minecraft", "missing");

    List<ResourceLocation> waypointIds = Collections.emptyList();
    List<ResourceLocation> triggerIds = Collections.emptyList();

    public ITextComponent getTitle() {
        return this.title != null ? this.title : new TranslationTextComponent("sign_me_in.map.unnamed");
    }

    public ITextComponent getSubtitle() {
        return this.subtitle != null ? this.subtitle : this.getTitle();
    }

    public ITextComponent getDesc() {
        return this.desc != null ? this.desc : StringTextComponent.EMPTY;
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
                map.title = ITextComponent.Serializer.getComponentFromJson(json.get("title"));
            }
            if (json.has("subtitle")) {
                map.subtitle = ITextComponent.Serializer.getComponentFromJson(json.get("subtitle"));
            }
            if (json.has("description")) {
                map.desc = ITextComponent.Serializer.getComponentFromJson(json.get("description"));
            }
            if (json.has("center")) {
                map.center = context.deserialize(json.get("center"), Vector3i.class);
            } else {
                LOGGER.warn(MARKER, "Center coordinate missing, falling back to [0, 0].");
            }
            if (json.has("range")) {
                map.range = json.get("range").getAsInt();
            } else {
                LOGGER.warn(MARKER, "Range missing, falling back to 256 (blocks)");
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
            json.add("range", new JsonPrimitive(src.range));
            if (src.dim != null) {
                json.add("world", new JsonPrimitive(src.dim.toString()));
            }
            if (src.texture != null) {
                json.add("texture", new JsonPrimitive(src.texture.toString()));
            }
            if (src.waypointIds != null && !src.waypointIds.isEmpty()) {
                json.add("waypoints", src.waypointIds.stream()
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
