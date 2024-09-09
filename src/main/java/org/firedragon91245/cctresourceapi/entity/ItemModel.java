package org.firedragon91245.cctresourceapi.entity;

import java.util.HashMap;
import java.util.Map;

public class ItemModel implements IModel {
    public String parent;
    public Map<String, String> textures;

    @Override
    public String getParent() {
        return parent;
    }

    @Override
    public Map<String, String> getTextures() {
        return textures;
    }

    public Map<String, Object> asHashMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("parent", parent);
        map.put("textures", textures);
        return map;
    }
}
