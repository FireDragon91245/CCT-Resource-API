package org.firedragon91245.cctresourceapi.json;

import com.google.gson.*;
import org.firedragon91245.cctresourceapi.OneOrMore;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class OneOrMoreSerializer implements JsonDeserializer<OneOrMore<?>>, JsonSerializer<OneOrMore<?>> {
    @Override
    public OneOrMore<?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if(jsonElement.isJsonArray())
        {
            if(type instanceof ParameterizedType)
            {
                ParameterizedType genericType = (ParameterizedType) type;
                Object result = jsonDeserializationContext.deserialize(jsonElement, new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[]{genericType.getActualTypeArguments()[0]};
                    }

                    @Override
                    public Type getRawType() {
                        return List.class;
                    }

                    @Override
                    public Type getOwnerType() {
                        return null;
                    }
                });
                if(result instanceof List)
                {
                    List<?> list = (List<?>) result;
                    return OneOrMore.fromMore(list);
                }
            }
        } else if (jsonElement.isJsonObject()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType genericType = (ParameterizedType) type;
                Object result = jsonDeserializationContext.deserialize(jsonElement, genericType.getActualTypeArguments()[0]);
                return OneOrMore.fromOne(result);
            }
        }
        return null;
    }

    @Override
    public JsonElement serialize(OneOrMore<?> oneOrMore, Type type, JsonSerializationContext jsonSerializationContext) {
        AtomicReference<JsonElement> result = new AtomicReference<>();
        oneOrMore.ifOneOrElse(one -> {
            result.set(jsonSerializationContext.serialize(one));
        }, more -> {
            JsonArray array = new JsonArray();
            more.forEach(o -> {
                array.add(jsonSerializationContext.serialize(o));
            });
            result.set(array);
        });
        return result.get();
    }
}
