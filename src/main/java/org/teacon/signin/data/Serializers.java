package org.teacon.signin.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3i;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;

public class Serializers {

    private static final Logger LOGGER = LogManager.getLogger("SignIn");
    public static final class ResourceLocationSerializer
            implements JsonSerializer<ResourceLocation>, JsonDeserializer<ResourceLocation> {

        @Override
        public ResourceLocation deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return ResourceLocation.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, LOGGER::error);
        }

        @Override
        public JsonElement serialize(ResourceLocation src, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    public static final class Vec3iSerializer
            implements JsonSerializer<Vector3i>, JsonDeserializer<Vector3i> {

        @Override
        public Vector3i deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonArray()) {
                JsonArray arr = json.getAsJsonArray();
                if (arr.size() == 3) {
                    return new Vector3i(arr.get(0).getAsInt(), 0, arr.get(2).getAsInt());
                } else if (arr.size() == 2) {
                    return new Vector3i(arr.get(0).getAsInt(), 0, arr.get(1).getAsInt());
                }
            }
            throw new JsonSyntaxException("Coordinates must be array of 2 or 3 integers");
        }

        @Override
        public JsonElement serialize(Vector3i src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray arr = new JsonArray();
            arr.add(src.getX());
            arr.add(src.getZ());
            return arr;
        }
    }
}
