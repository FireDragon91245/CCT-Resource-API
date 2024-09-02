package org.firedragon91245.cctresourceapi.cct;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.registries.ForgeRegistries;
import org.firedragon91245.cctresourceapi.CCT_Resource_API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResourceAPI implements ILuaAPI {
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static boolean filterIds(String toFilter, Pattern modidRegex, Pattern idRegex) {
        if (toFilter.isEmpty())
            return false;

        String[] parts = toFilter.split(":");
        if (parts.length != 2)
            return false;

        Matcher modidMatcher = modidRegex.matcher(parts[0]);
        Matcher blockidMatcher = idRegex.matcher(parts[1]);
        return modidMatcher.matches() && blockidMatcher.matches();
    }

    @Nullable
    private static BlockModelInfo loadBlockModelByBlockId(@Nonnull ResourceLocation location) {
        BlockModelInfo modelInfo = new BlockModelInfo(location);
        if (location.getNamespace().equals("minecraft")) {
            String blockid = location.getPath();
            Optional<String> stateModelJson = loadBunbledFileText("bundled_resources/minecraft/blockstates/" + blockid + ".json");
            if (stateModelJson.isPresent()) {
                BlockStateModel stateModel = CCT_Resource_API.GSON.fromJson(stateModelJson.get(), BlockStateModel.class);
                modelInfo.statefullModel = true;
                modelInfo.modelState = stateModel;
                modelInfo.modelState.variants.forEach((key, value) -> {
                    modelInfo.models = new HashMap<>();
                    value.ifOneOrElse(
                            one -> modelInfo.models.put(one.model, loadBlockModelByLocation(one.model)),
                            more -> {
                                for (BlockStateModelVariant variant : more) {
                                    modelInfo.models.put(variant.model, loadBlockModelByLocation(variant.model));
                                }
                            });
                });

                HashMap<String, BlockModel> newModels = new HashMap<>();
                do {
                    newModels.clear();
                    modelInfo.models.forEach((key, value) -> {
                        if (value != null && value.parent != null) {
                            // FIXME: Inventigate why there are model enties like "block/cube" without namespace my error? did i somewere trhow minecraft: away?
                            // FIXME: Seems these are "integrated" models but these still exist as files inside the MC bundled resources, mabe just add extra handling if no namespce asumee minecraft:
                            if (modelInfo.models.containsKey(value.parent) || newModels.containsKey(value.parent))
                                return;
                            BlockModel parentModel = loadBlockModelByLocation(value.parent);
                            newModels.put(value.parent, parentModel);
                        }
                    });
                    modelInfo.models.putAll(newModels);
                } while (!newModels.isEmpty());

                return modelInfo;
            } else {
                Optional<String> modelJson = loadBunbledFileText("bundled_resources/minecraft/models/block/" + blockid + ".json");
                if (modelJson.isPresent()) {
                    BlockModel model = CCT_Resource_API.GSON.fromJson(modelJson.get(), BlockModel.class);
                    modelInfo.statefullModel = false;
                    modelInfo.rootModel = model;

                    // TODO: implement parent models recusivly

                    return modelInfo;
                }
                return null;
            }

        } else {
            // TODO: Implement non Bundled Resources
            return null;
        }
    }

    private static BlockModel loadBlockModelByLocation(String model) {
        if (model.startsWith("minecraft:")) {
            String modelid = model.substring(10);
            Optional<String> modelJson = loadBunbledFileText("bundled_resources/minecraft/models/" + modelid + ".json");
            return modelJson.map(s -> CCT_Resource_API.GSON.fromJson(s, BlockModel.class)).orElse(null);
        } else {
            // TODO: Implement non Bundled Resources
            return null;
        }
    }

    private static Optional<File> getModJarFromModId(String modid) {
        Optional<ModFile> file = ModList.get().getMods().stream()
                .filter(modContainer -> modContainer.getModId().equals(modid))
                .map(modContainer -> {
                    ModFileInfo modFileInfo = modContainer.getOwningFile();
                    if (modFileInfo != null) {
                        return modFileInfo.getFile();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst();

        return file.map(ModFile::getFilePath).map(Path::toFile);
    }

    private static Optional<String> loadBunbledFileText(String location) {
        try (InputStream modelStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(location)) {
            if (modelStream != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(modelStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                return Optional.of(sb.toString());
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
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

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(blockid);

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
    public String[] getItemIds(@Nullable Object filter) {
        if (filter instanceof String) {
            return ForgeRegistries.ITEMS.getValues().stream()
                    .map(item -> defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof HashMap) {
            HashMap<String, Object> filterMap = (HashMap<String, Object>) filter;
            String modid = (String) filterMap.getOrDefault("modid", ".*");
            String itemid = (String) filterMap.getOrDefault("itemid", ".*");

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(itemid);

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
    public HashMap<String, Object> getBlockInfo(String blockId, String flags) {
        if (blockId.isEmpty())
            return null;

        ResourceLocation blockLocation = new ResourceLocation(blockId);
        if (!ForgeRegistries.BLOCKS.containsKey(blockLocation))
            return null;

        Block b = ForgeRegistries.BLOCKS.getValue(blockLocation);
        HashMap<String, Object> blockInfo = new HashMap<>();
        blockInfo.put("blockid", blockLocation.toString());
        if (flags.contains("t")) { // t = tags
            blockInfo.put("tags", Objects.requireNonNull(b).getTags().stream().map(ResourceLocation::toString).toArray(String[]::new));
        } else if (flags.contains("m")) { // m = moodels + textures
            getBlockModelInfo(b, blockInfo);
        }

        return blockInfo;
    }

    private void getBlockModelInfo(Block b, HashMap<String, Object> blockInfo) {
        ResourceLocation blockId = b.getRegistryName();
        if (blockId == null)
            return;

        BlockModelInfo model = loadBlockModelByBlockId(blockId);
        if (model == null)
            return;

        HashMap<String, Object> modelInfo = new HashMap<>();
        modelInfo.put("statefull", model.statefullModel);
        if (model.statefullModel) {
            HashMap<String, Object> states = new HashMap<>();
            model.modelState.variants.forEach((key, value) -> {
                value.ifOneOrElse(one -> {
                    HashMap<String, Object> state = new HashMap<>();
                    state.put("model", one.model);
                    one.properties.forEach((k, v) -> state.put(k, v.toString()));
                    states.put(key, state);
                }, more -> {
                    List<HashMap<String, Object>> stateList = more.stream().map(variant -> {
                        HashMap<String, Object> state = new HashMap<>();
                        state.put("model", variant.model);
                        variant.properties.forEach((k, v) -> state.put(k, v.toString()));
                        return state;
                    }).collect(Collectors.toList());
                    states.put(key, stateList);
                });
            });
            modelInfo.put("states", states);
        } else {
            HashMap<String, Object> rootModel = new HashMap<>();
            modelInfo.put("rootModel", rootModel);
            rootModel.put("parent", model.rootModel.parent);
            rootModel.put("textures", model.rootModel.textures);
        }
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
