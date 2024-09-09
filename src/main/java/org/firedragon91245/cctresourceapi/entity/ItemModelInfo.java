package org.firedragon91245.cctresourceapi.entity;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ItemModelInfo implements IModelInfo {
    private final String itemId;
    public ItemModel rootModel;
    public Map<String, ItemModel> models;
    public Map<String, ModelTexture> textures;

    public ItemModelInfo(String itemId) {
        this.itemId = itemId;
    }

    public HashMap<Object, Object> asHashMap() {
        return new HashMap<>();
        // TODO: Implement this method
    }

    @Override
    public Map<String, ? extends IModel> getModels() {
        return models;
    }

    @Override
    public Map<String, ?> getTextures() {
        return textures;
    }

    @Override
    public void putTexture(String key, ModelTexture modelTexture) {
        textures.put(key, modelTexture);
    }
}
