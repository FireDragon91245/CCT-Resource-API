package org.firedragon91245.cctresourceapi.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.firedragon91245.cctresourceapi.OneOrMore;
import org.firedragon91245.cctresourceapi.cct.BlockStateModel;
import org.firedragon91245.cctresourceapi.cct.BlockStateModelVariant;

import java.lang.reflect.Type;

public class BlockStateModelSerializer implements JsonSerializer<BlockStateModel>, JsonDeserializer<BlockStateModel> {
    @Override
    public BlockStateModel deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement.isJsonObject()) {
            JsonObject object = jsonElement.getAsJsonObject();
            if (object.has("variants")) {
                JsonElement variants = object.get("variants");
                if (variants.isJsonObject()) {
                    BlockStateModel model = new BlockStateModel();
                    JsonObject variantsObject = variants.getAsJsonObject();
                    variantsObject.entrySet().forEach(entry -> {
                        model.variants.put(entry.getKey(), jsonDeserializationContext.deserialize(entry.getValue(), new TypeToken<OneOrMore<BlockStateModelVariant>>() {
                        }.getType()));
                    });
                    return model;
                }
            }
        }
        return null;
    }

    @Override
    public JsonElement serialize(BlockStateModel blockStateModel, Type type, JsonSerializationContext jsonSerializationContext) {
        return null;
        // TODO: Implement this method
    }
}
