package org.teacon.signin.data;

import com.google.gson.*;
import net.minecraft.util.math.vector.Vector3i;

import java.lang.reflect.Type;

public final class Vector3iAdapter
        implements JsonSerializer<Vector3i>, JsonDeserializer<Vector3i> {

    @Override
    public Vector3i deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonArray()) {
            JsonArray arr = json.getAsJsonArray();
            if (arr.size() == 3) {
                return new Vector3i(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
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
        arr.add(src.getY());
        arr.add(src.getZ());
        return arr;
    }
}
