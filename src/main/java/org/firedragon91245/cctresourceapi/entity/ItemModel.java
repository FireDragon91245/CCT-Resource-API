package org.firedragon91245.cctresourceapi.entity;

import java.util.Collections;
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
}
