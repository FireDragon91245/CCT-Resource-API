package org.firedragon91245.cctresourceapi.cct;

import com.google.gson.JsonElement;

import java.util.HashMap;

public class BlockStateModelVariant {
    public String model;

    public HashMap<String, JsonElement> properties;

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
