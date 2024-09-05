package org.firedragon91245.cctresourceapi.entity;

import java.util.HashMap;

public class BlockModelDisplay {

    public BlockModelDisplayEntry gui;
    public BlockModelDisplayEntry ground;
    public BlockModelDisplayEntry fixed;
    public BlockModelDisplayEntry head;
    public BlockModelDisplayEntry firstperson_righthand;
    public BlockModelDisplayEntry firstperson_lefthand;
    public BlockModelDisplayEntry thirdperson_righthand;
    public BlockModelDisplayEntry thirdperson_lefthand;

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        if (gui != null)
            map.put("gui", gui.asHashMap());
        if (ground != null)
            map.put("ground", ground.asHashMap());
        if (fixed != null)
            map.put("fixed", fixed.asHashMap());
        if (head != null)
            map.put("head", head.asHashMap());
        if (firstperson_righthand != null)
            map.put("firstperson_righthand", firstperson_righthand.asHashMap());
        if (firstperson_lefthand != null)
            map.put("firstperson_lefthand", firstperson_lefthand.asHashMap());
        if (thirdperson_righthand != null)
            map.put("thirdperson_righthand", thirdperson_righthand.asHashMap());
        if (thirdperson_lefthand != null)
            map.put("thirdperson_lefthand", thirdperson_lefthand.asHashMap());
        return map;
    }
}
