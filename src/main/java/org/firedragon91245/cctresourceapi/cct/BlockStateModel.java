package org.firedragon91245.cctresourceapi.cct;

import org.firedragon91245.cctresourceapi.OneOrMore;

import java.util.HashMap;

public class BlockStateModel {

    public BlockStateModel() {
        this.variants = new HashMap<>();
    }

    public HashMap<String, OneOrMore<BlockStateModelVariant>> variants;
}
