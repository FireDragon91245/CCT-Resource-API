package org.firedragon91245.cctresourceapi.cct;

import com.google.common.cache.AbstractCache;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;

public class BlockModelInfo {
    private final ResourceLocation blockId;
    public boolean statefullModel;
    public BlockStateModel modelState;
    public BlockModel rootModel;
    public HashMap<String, BlockModel> models;
    public HashMap<String, ModelTexture> textures;

    public BlockModelInfo(ResourceLocation blockId) {
        this.blockId = blockId;
        this.models = new HashMap<>();
        this.textures = new HashMap<>();
    }

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("statefullModel", statefullModel);
        map.put("blockId", blockId.toString());
        if(statefullModel && modelState != null)
            map.put("states", modelState.asHashMap());
        if(!statefullModel && rootModel != null)
            map.put("rootModel", rootModel.asHashMap());
        HashMap<String, Object> modelsMap = new HashMap<>();
        for (String key : models.keySet()) {
            if(models.get(key) != null)
                modelsMap.put(key, models.get(key).asHashMap());
        }
        HashMap<String, Object> texturesMap = new HashMap<>();
        for (String key : textures.keySet()) {
            if(textures.get(key) != null)
                texturesMap.put(key, textures.get(key).asHashMap());
        }
        map.put("textures", texturesMap);
        map.put("models", modelsMap);
        return map;
    }
}
