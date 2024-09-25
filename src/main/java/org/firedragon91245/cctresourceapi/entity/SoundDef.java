package org.firedragon91245.cctresourceapi.entity;

import java.util.HashMap;
import java.util.Map;

public class SoundDef {
    String name;
    Boolean stream;
    Integer weight;
    Float volume;
    Float pitch;

    public String getName() {
        return name;
    }

    public Map<String, Object> asHashMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("stream", stream);
        result.put("weight", weight);
        result.put("volume", volume);
        result.put("pitch", pitch);
        return result;
    }
}
