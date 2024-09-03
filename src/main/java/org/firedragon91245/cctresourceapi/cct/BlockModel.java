package org.firedragon91245.cctresourceapi.cct;

import java.util.HashMap;
import java.util.Map;

public class BlockModel {

    public BlockModel() {
        this.textures = new HashMap<>();
    }

    public String parent;
    public Map<String, String> textures;
    public String gui_light;
    public BlockModelDisplay display;

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("parent", parent);
        map.put("textures", textures);
        map.put("gui_light", gui_light);
        if(display != null)
            map.put("display", display.asHashMap());
        return map;
    }
}
