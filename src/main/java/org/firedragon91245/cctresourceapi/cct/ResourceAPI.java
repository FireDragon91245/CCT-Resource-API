package org.firedragon91245.cctresourceapi.cct;

import com.google.gson.JsonObject;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.block.Block;
import net.minecraft.data.BlockModelDefinition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.firedragon91245.cctresourceapi.CCT_Resource_API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceAPI implements ILuaAPI {
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static boolean filterIds(String toFilter, Pattern modidRegex, Pattern idRegex)
    {
        if (toFilter.isEmpty())
            return false;

        String[] parts = toFilter.split(":");
        if (parts.length != 2)
            return false;

        Matcher modidMatcher = modidRegex.matcher(parts[0]);
        Matcher blockidMatcher = idRegex.matcher(parts[1]);
        return modidMatcher.matches() && blockidMatcher.matches();
    }

    @SuppressWarnings("unchecked")
    @LuaFunction
    public String[] getBlockIds(@Nullable Object filter) {
        if (filter instanceof String) {
            return ForgeRegistries.BLOCKS.getValues().stream()
                    .map(block -> defaultIfNull(block.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof HashMap) {
            HashMap<String, Object> filterMap = (HashMap<String, Object>) filter;
            String modid = (String) filterMap.getOrDefault("modid", ".*");
            String blockid = (String) filterMap.getOrDefault("blockid", ".*");

            Pattern  modidRegex = Pattern.compile(modid);
            Pattern  itemidRegex = Pattern.compile(blockid);

            return ForgeRegistries.BLOCKS.getValues().stream()
                    .map(block -> defaultIfNull(block.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> filterIds(str, modidRegex, itemidRegex))
                    .toArray(String[]::new);

        }
        return ForgeRegistries.BLOCKS.getValues().stream()
                .map(block -> defaultIfNull(block.getRegistryName(), new ResourceLocation("")).toString())
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    @SuppressWarnings("unchecked")
    @LuaFunction
    public String[] getItemIds(@Nullable Object filter)
    {
        if (filter instanceof String) {
            return ForgeRegistries.ITEMS.getValues().stream()
                    .map(item -> defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof HashMap) {
            HashMap<String, Object> filterMap = (HashMap<String, Object>) filter;
            String modid = (String) filterMap.getOrDefault("modid", ".*");
            String itemid = (String) filterMap.getOrDefault("itemid", ".*");

            Pattern  modidRegex = Pattern.compile(modid);
            Pattern  itemidRegex = Pattern.compile(itemid);

            return ForgeRegistries.ITEMS.getValues().stream()
                    .map(item -> defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> filterIds(str, modidRegex, itemidRegex))
                    .toArray(String[]::new);

        }
        return ForgeRegistries.ITEMS.getValues().stream()
                .map(item -> defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    @LuaFunction
    public HashMap<String, Object> getBlockInfo(String blockId, String flags){
        if(blockId.isEmpty())
            return null;

        ResourceLocation blockLocation = new ResourceLocation(blockId);
        if(!ForgeRegistries.BLOCKS.containsKey(blockLocation))
            return null;

        Block b  = ForgeRegistries.BLOCKS.getValue(blockLocation);
        HashMap<String, Object> blockInfo = new HashMap<>();
        blockInfo.put("blockid", blockLocation.toString());
        if(flags.contains("t")) { // t = tags
            blockInfo.put("tags", Objects.requireNonNull(b).getTags().stream().map(ResourceLocation::toString).toArray(String[]::new));
        }
        else if(flags.contains("m")) { // m = moodels + textures
            getBlockModelInfo(b, blockInfo);
        }

        return blockInfo;
    }

    private void getBlockModelInfo(Block b, HashMap<String, Object> blockInfo) {
        ResourceLocation blockId = b.getRegistryName();
        if(blockId == null)
            return;

        if(blockId.getNamespace().equals("minecraft"))
        {
            String id = blockId.toString();
            String[] parts = id.split(":");
            if(parts.length != 2)
                return;

            String model  = loadBlockModelBundled(parts[1]);
            if(model == null)
                return;
            blockInfo.put("model_json", model);
            BlockModelJson modelJson = CCT_Resource_API.GSON.fromJson(model, BlockModelJson.class);

        }
        else
        {

        }
    }

    @Nullable
    private static String loadBlockModelBundled(String blockid) {
        String modelLocation = "bundled_resources/minecraft/models/block/" + blockid + ".json";
        try(InputStream modelStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(modelLocation))
        {
            if(modelStream != null)
            {
                StringBuilder sb = new StringBuilder();
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(modelStream)))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        sb.append(line);
                    }
                }
                return sb.toString();
            }
        }
        catch (IOException e)
        {
            CCT_Resource_API.LOGGER.error("Failed to load model for block {}", blockid, e);
        }
        return null;
    }

    @Override
    public String[] getNames() {
        return new String[]{
                "resourceapi"
        };
    }

    @Override
    public void startup() {
        ILuaAPI.super.startup();
    }

    @Override
    public void update() {
        ILuaAPI.super.update();
    }

    @Override
    public void shutdown() {
        ILuaAPI.super.shutdown();
    }

    public static class Factory implements ILuaAPIFactory {
        @Nullable
        @Override
        public ILuaAPI create(@Nonnull IComputerSystem iComputerSystem) {
            return new ResourceAPI();
        }
    }
}
