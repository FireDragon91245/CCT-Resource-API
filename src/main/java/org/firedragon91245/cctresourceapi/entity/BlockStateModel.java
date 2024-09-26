package org.firedragon91245.cctresourceapi.entity;

import org.firedragon91245.cctresourceapi.OneOrMore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlockStateModel {

    final public Map<String, OneOrMore<BlockStateModelVariant>> variants;

    public BlockStateModel() {
        this.variants = new HashMap<>();
    }

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> result = new HashMap<>();
        variants.forEach((key, value) -> value.ifOneOrElse(one -> result.put(key, one.asHashMap()),
                more -> {
                    List<HashMap<String, Object>> list = more.stream().map(BlockStateModelVariant::asHashMap).collect(Collectors.toList());
                    result.put(key, list);
                }));
        return result;
    }
}
