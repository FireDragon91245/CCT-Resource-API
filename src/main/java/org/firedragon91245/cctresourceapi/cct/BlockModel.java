package org.firedragon91245.cctresourceapi.cct;

import java.util.HashMap;
import java.util.Map;

public class BlockModel {

    public BlockModel() {
        this.textures = new HashMap<>();
    }

    public String parent;
    public Map<String, String> textures;
}
