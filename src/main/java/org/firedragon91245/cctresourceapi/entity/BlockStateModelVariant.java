package org.firedragon91245.cctresourceapi.entity;

import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class BlockStateModelVariant {
    public String model;

    final public Map<String, JsonElement> properties;

    public BlockStateModelVariant() {
        this.properties = new HashMap<>();
    }

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("model", model);
        properties.forEach((key, value) -> map.put(key, value.toString()));
        return map;
    }
}
