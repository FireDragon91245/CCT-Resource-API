package org.firedragon91245.cctresourceapi.entity;

import java.util.HashMap;
import java.util.Map;

public class BlockModel implements IModel {

    final public Map<String, String> textures;
    public String parent;
    public String gui_light;
    public BlockModelDisplay display;
    public BlockModel() {
        this.textures = new HashMap<>();
    }

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("parent", parent);
        map.put("textures", textures);
        map.put("gui_light", gui_light);
        if (display != null)
            map.put("display", display.asHashMap());
        return map;
    }

    @Override
    public String getParent() {
        return parent;
    }

    @Override
    public Map<String, String> getTextures() {
        return textures;
    }
}
