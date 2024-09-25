package org.firedragon91245.cctresourceapi.entity;

import java.util.HashMap;
import java.util.Map;

public class ItemModelInfo implements IModelInfo {
    final public Map<String, ItemModel> models;
    final public Map<String, ModelTexture> textures;
    private final String itemId;
    public ItemModel rootModel;

    public ItemModelInfo(String itemId) {
        this.itemId = itemId;
        this.models = new HashMap<>();
        this.textures = new HashMap<>();
    }

    public Map<String, Object> asHashMap() {
        Map<String, Object> result = new HashMap<>();
        if (rootModel != null)
            result.put("rootModel", rootModel.asHashMap());
        Map<String, Object> modelsMap = new HashMap<>();
        for (Map.Entry<String, ItemModel> entry : models.entrySet()) {
            if (entry.getValue() != null)
                modelsMap.put(entry.getKey(), entry.getValue().asHashMap());
        }
        result.put("models", modelsMap);
        Map<String, Object> texturesMap = new HashMap<>();
        for (Map.Entry<String, ModelTexture> entry : textures.entrySet()) {
            if (entry.getValue() != null)
                texturesMap.put(entry.getKey(), entry.getValue().asHashMap());
        }
        result.put("textures", texturesMap);
        result.put("itemId", itemId);
        return result;
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
