package org.firedragon91245.cctresourceapi.cct;

import net.minecraft.util.ResourceLocation;

import java.util.HashMap;

public class BlockModelInfo {
    private final ResourceLocation blockId;
    public boolean statefullModel;
    public BlockStateModel modelState;
    public BlockModel rootModel;
    public HashMap<String, BlockModel> models;

    public BlockModelInfo(ResourceLocation blockId) {
        this.blockId = blockId;
        this.models = new HashMap<>();
    }
}
