package org.firedragon91245.cctresourceapi.json;

import com.google.gson.*;
import com.mojang.math.Vector3f;

import java.lang.reflect.Type;

public class Vector3fSerializer implements JsonSerializer<Vector3f>, JsonDeserializer<Vector3f> {
    @Override
    public Vector3f deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            float x = jsonObject.get("x").getAsFloat();
            float y = jsonObject.get("y").getAsFloat();
            float z = jsonObject.get("z").getAsFloat();
            return new Vector3f(x, y, z);
        } else if (jsonElement.isJsonArray()) {
            JsonArray array = jsonElement.getAsJsonArray();
            float x = array.get(0).getAsFloat();
            float y = array.get(1).getAsFloat();
            float z = array.get(2).getAsFloat();
            return new Vector3f(x, y, z);
        } else {
            throw new JsonParseException("Invalid Vector3f format");
        }
    }

    @Override
    public JsonElement serialize(Vector3f vector3f, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonArray array = new JsonArray();
        array.add(vector3f.x());
        array.add(vector3f.y());
        array.add(vector3f.z());
        return array;
    }
}
