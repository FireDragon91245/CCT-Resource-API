package org.firedragon91245.cctresourceapi.entity;

import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class BlockModelInfo implements IModelInfo {
    final public Map<String, BlockModel> models;
    final public Map<String, ModelTexture> textures;
    private final ResourceLocation blockId;
    public boolean statefullModel;
    public BlockStateModel modelState;
    public BlockModel rootModel;

    public BlockModelInfo(ResourceLocation blockId) {
        this.blockId = blockId;
        this.models = new HashMap<>();
        this.textures = new HashMap<>();
    }

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("statefullModel", statefullModel);
        map.put("blockId", blockId.toString());
        if (statefullModel && modelState != null)
            map.put("states", modelState.asHashMap());
        if (!statefullModel && rootModel != null)
            map.put("rootModel", rootModel.asHashMap());
        HashMap<String, Object> modelsMap = new HashMap<>();
        for (String key : models.keySet()) {
            if (models.get(key) != null)
                modelsMap.put(key, models.get(key).asHashMap());
        }
        HashMap<String, Object> texturesMap = new HashMap<>();
        for (String key : textures.keySet()) {
            if (textures.get(key) != null)
                texturesMap.put(key, textures.get(key).asHashMap());
        }
        map.put("textures", texturesMap);
        map.put("models", modelsMap);
        return map;
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
