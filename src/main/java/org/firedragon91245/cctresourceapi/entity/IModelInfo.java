package org.firedragon91245.cctresourceapi.entity;

import java.util.Map;

public interface IModelInfo {

    public Map<String, ? extends IModel> getModels();

    Map<String, ?> getTextures();

    void putTexture(String key, ModelTexture modelTexture);
}
