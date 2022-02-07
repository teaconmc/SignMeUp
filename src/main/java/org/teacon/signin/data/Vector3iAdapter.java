package org.teacon.signin.data;

import com.google.gson.*;
import net.minecraft.core.Vec3i;

import java.lang.reflect.Type;

public final class Vector3iAdapter
        implements JsonSerializer<Vec3i>, JsonDeserializer<Vec3i> {

    @Override
    public Vec3i deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonArray()) {
            JsonArray arr = json.getAsJsonArray();
            if (arr.size() == 3) {
                return new Vec3i(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
            } else if (arr.size() == 2) {
                return new Vec3i(arr.get(0).getAsInt(), 0, arr.get(1).getAsInt());
            }
        }
        throw new JsonSyntaxException("Coordinates must be array of 2 or 3 integers");
    }

    @Override
    public JsonElement serialize(Vec3i src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray arr = new JsonArray();
        arr.add(src.getX());
        arr.add(src.getY());
        arr.add(src.getZ());
        return arr;
    }
}
