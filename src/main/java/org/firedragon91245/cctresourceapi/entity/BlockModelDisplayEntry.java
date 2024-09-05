package org.firedragon91245.cctresourceapi.entity;

import net.minecraft.util.math.vector.Vector3f;

import java.util.Arrays;
import java.util.HashMap;

public class BlockModelDisplayEntry {

    public Vector3f rotation;
    public Vector3f translation;
    public Vector3f scale;

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("rotation", Arrays.asList(rotation.x(), rotation.y(), rotation.z()));
        map.put("translation", Arrays.asList(translation.x(), translation.y(), translation.z()));
        map.put("scale", Arrays.asList(scale.x(), scale.y(), scale.z()));
        return map;
    }
}
