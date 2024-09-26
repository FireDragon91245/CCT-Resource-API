package org.firedragon91245.cctresourceapi.json;

import com.google.gson.*;
import org.firedragon91245.cctresourceapi.VariantArray;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class VariantArraySerializer implements JsonSerializer<VariantArray<?, ?>>, JsonDeserializer<VariantArray> {
    @Override
    public VariantArray<?, ?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (!jsonElement.isJsonArray()) {
            throw new JsonParseException("VariantArray must be an array");
        }
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        if (type instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length != 2) {
                throw new JsonParseException("VariantArray must have two type arguments");
            }
            try {
                List<?> result = jsonDeserializationContext.deserialize(jsonArray, new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[]{parameterizedType.getActualTypeArguments()[0]};
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
                if (typeArguments[0] instanceof Class<?> aClass) {
                    return VariantArray.ofA(result, aClass);
                } else {
                    throw new JsonParseException("Type argument A must be a class");
                }
            } catch (JsonParseException e) {
                List<?> result = jsonDeserializationContext.deserialize(jsonArray, new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[]{parameterizedType.getActualTypeArguments()[1]};
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
                if (typeArguments[1] instanceof Class<?> bClass) {
                    return VariantArray.ofB(result, bClass);
                } else {
                    throw new JsonParseException("Type argument B must be a class");
                }
            }
        } else {
            throw new JsonParseException("VariantArray must be a parameterized type");
        }
    }

    @Override
    public JsonElement serialize(VariantArray<?, ?> variantArray, Type type, JsonSerializationContext jsonSerializationContext) {
        if (variantArray.isA()) {
            return jsonSerializationContext.serialize(variantArray.getA());
        } else {
            return jsonSerializationContext.serialize(variantArray.getB());
        }
    }
}
