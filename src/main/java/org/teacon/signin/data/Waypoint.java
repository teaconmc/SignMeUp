package org.teacon.signin.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class Waypoint {

    public static final class Location {
        Vector3i actualLocation;
        private Vector3i renderLocation;
        boolean isDynamic = false;

        public Vector3i getRenderLocation() {
            return this.renderLocation == null ? this.actualLocation : this.renderLocation;
        }

        public static final class Serializer implements JsonDeserializer<Location> {
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
        }
    }

    public ITextComponent title = new TranslationTextComponent("sign_me_in.waypoint.unnamed");
    public ITextComponent desc = StringTextComponent.EMPTY;

    public volatile boolean disabled = false;

    String selector = "@e";
    private transient EntitySelector parsedSelector;

    Location location;

    public List<ResourceLocation> triggerIds;

    transient Set<ServerPlayerEntity> visiblePlayers = Collections.newSetFromMap(new WeakHashMap<>());

    public boolean hasDynamicLocation() {
        return location.isDynamic;
    }

    public EntitySelector getSelector() {
        if (parsedSelector == null) {
            try {
                return (parsedSelector = new EntitySelectorParser(new StringReader(this.selector)).parse());
            } catch (Exception ignored) {

            }
        }
        return parsedSelector;
    }

    public static final class Serializer implements JsonDeserializer<Waypoint>, JsonSerializer<Waypoint> {

        @Override
        public Waypoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return null;
        }

        @Override
        public JsonElement serialize(Waypoint src, Type typeOfSrc, JsonSerializationContext context) {
            return null;
        }
    }
}
