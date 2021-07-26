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
    public static final ResourceLocation DEFAULT_IMAGE = new ResourceLocation("sign_up:textures/map_default.png");

    ITextComponent title;
    ITextComponent subtitle;
    ITextComponent desc;

    public Vector3i center;
    public float radius = 128F;
    public ResourceLocation dim = null;

    // TODO Actually ensure the missing texture exists
    public ResourceLocation texture = new ResourceLocation("minecraft", "missing");

    private List<ResourceLocation> imageIds = Collections.emptyList();

    private int displayingImageIndex;

    List<ResourceLocation> waypointIds = Collections.emptyList();
    List<ResourceLocation> triggerIds = Collections.emptyList();

    public ITextComponent getTitle() {
        return this.title != null ? this.title : new TranslationTextComponent("sign_up.map.unnamed");
    }

    public ITextComponent getSubtitle() {
        return this.subtitle != null ? this.subtitle : this.getTitle();
    }

    public ITextComponent getDesc() {
        return this.desc != null ? this.desc : StringTextComponent.EMPTY;
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
        return this.imageIds.isEmpty() ? DEFAULT_IMAGE : this.imageIds.get(this.displayingImageIndex);
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
                map.title = context.deserialize(json.get("title"), ITextComponent.class);
            }
            if (json.has("subtitle")) {
                map.subtitle = context.deserialize(json.get("subtitle"), ITextComponent.class);
            }
            if (json.has("description")) {
                map.desc = context.deserialize(json.get("description"), ITextComponent.class);
            }
            if (json.has("center")) {
                map.center = context.deserialize(json.get("center"), Vector3i.class);
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
            if (json.has("images")) {
                map.imageIds = StreamSupport.stream(json.getAsJsonArray("images").spliterator(), false)
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
            if (!src.imageIds.isEmpty()) {
                json.add("images", src.imageIds.stream()
                        .map(ResourceLocation::toString)
                        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            }
            return json;
        }
    }
}
