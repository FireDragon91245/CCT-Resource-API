package org.firedragon91245.cctresourceapi.json;

import com.google.gson.*;
import org.firedragon91245.cctresourceapi.entity.BlockStateModelVariant;

import java.lang.reflect.Type;

public class BlockStateModelVariantSerializer implements JsonSerializer<BlockStateModelVariant>, JsonDeserializer<BlockStateModelVariant> {
    @Override
    public BlockStateModelVariant deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("BlockStateModelVariant must be a JsonObject");
        }

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        BlockStateModelVariant blockStateModelVariant = new BlockStateModelVariant();

        blockStateModelVariant.model = jsonObject.get("model").getAsString();

        jsonObject.entrySet().stream().filter(entry -> !entry.getKey().equals("model")).forEach(entry -> blockStateModelVariant.properties.put(entry.getKey(), entry.getValue()));
        return blockStateModelVariant;
    }

    @Override
    public JsonElement serialize(BlockStateModelVariant blockStateModelVariant, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("model", blockStateModelVariant.model);
        blockStateModelVariant.properties.forEach(jsonObject::add);
        return jsonObject;
    }
}
