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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Optional<String> stateModelJson = loadBundledFileText("bundled_resources/minecraft/blockstates/" + blockid + ".json");
            if (stateModelJson.isPresent()) {
                return loadStatefulBlockModelInfo(modelInfo, stateModelJson.get());
            } else {
                Optional<String> modelJson = loadBundledFileText("bundled_resources/minecraft/models/block/" + blockid + ".json");
                if (modelJson.isPresent()) {
                    BlockModel model = CCT_Resource_API.GSON.fromJson(modelJson.get(), BlockModel.class);
                    modelInfo.statefullModel = false;
                    modelInfo.rootModel = model;
                    modelInfo.models.put("minecraft:block/" + blockid, model);

                    modelInfo.models.putAll(getParentModelsRecursive(modelInfo));
                    return modelInfo;
                }
                return null;
            }
        } else {
            File f = getModJarFromModId(location.getNamespace()).orElse(null);
            if (f == null)
                return null;

            URL jarUrl = null;
            try {
                jarUrl = new URL("jar:file:" + f.getAbsolutePath() + "!/");
            } catch (MalformedURLException ignored) {
            }
            if(jarUrl == null)
                return null;

            try(URLClassLoader loader = new URLClassLoader(new URL[]{ jarUrl }))
            {
                Optional<String> stateModelJson = loadFileText(loader, "assets/" + location.getNamespace() + "/blockstates/" + location.getPath() + ".json");
                if (stateModelJson.isPresent()) {
                    return loadStatefulBlockModelInfo(modelInfo, stateModelJson.get());
                } else {
                    Optional<String> modelJson = loadFileText(loader, "assets/" + location.getNamespace() + "/models/block/" + location.getPath() + ".json");
                    if (modelJson.isPresent()) {
                        BlockModel model = CCT_Resource_API.GSON.fromJson(modelJson.get(), BlockModel.class);
                        modelInfo.statefullModel = false;
                        modelInfo.rootModel = model;
                        modelInfo.models.put(location.getNamespace() + ":block/" + location.getPath(), model);

                        modelInfo.models.putAll(getParentModelsRecursive(modelInfo));
                        return modelInfo;
                    }
                }
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
            return null;
        }
    }

    private static HashMap<String, BlockModel> getParentModelsRecursive(BlockModelInfo modelInfo) {
        HashMap<String, BlockModel> newModelsCollector = new HashMap<>();
        HashMap<String, BlockModel> newModels = new HashMap<>();
        do {
            newModels.clear();
            modelInfo.models.forEach((key, value) -> {
                if (value != null && value.parent != null) {
                    if (modelInfo.models.containsKey(value.parent) || newModels.containsKey(value.parent) || newModelsCollector.containsKey(value.parent))
                        return;
                    BlockModel parentModel = loadBlockModelByLocation(value.parent);
                    newModels.put(value.parent, parentModel);
                }
            });
            newModelsCollector.putAll(newModels);
        } while (!newModels.isEmpty());

        return newModelsCollector;
    }

    private static BlockModelInfo loadStatefulBlockModelInfo(BlockModelInfo modelInfo, String stateModelJson) {
        BlockStateModel stateModel = CCT_Resource_API.GSON.fromJson(stateModelJson, BlockStateModel.class);
        modelInfo.statefullModel = true;
        modelInfo.modelState = stateModel;
        modelInfo.modelState.variants.forEach((key, value) -> {
            if(value == null)
                return;
            modelInfo.models = new HashMap<>();
            value.ifOneOrElse(
                    one -> modelInfo.models.put(one.model, loadBlockModelByLocation(one.model)),
                    more -> {
                        for (BlockStateModelVariant variant : more) {
                            modelInfo.models.put(variant.model, loadBlockModelByLocation(variant.model));
                        }
                    });
        });

        modelInfo.models.putAll(getParentModelsRecursive(modelInfo));
        return modelInfo;
    }

    private static Optional<String> loadFileText(ClassLoader loader, String location) {
        try (InputStream modelStream = loader.getResourceAsStream(location)) {
            return readInStreamAll(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    @Nonnull
    private static Optional<String> readInStreamAll(InputStream modelStream) throws IOException {
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
        return Optional.empty();
    }

    private static BlockModel loadBlockModelByLocation(String model) {
        if(!model.contains(":"))
        {
            model = "minecraft:" + model;
        }
        if (model.startsWith("minecraft:")) {
            String modelid = model.substring(10);
            Optional<String> modelJson = loadBundledFileText("bundled_resources/minecraft/models/" + modelid + ".json");
            return modelJson.map(s -> CCT_Resource_API.GSON.fromJson(s, BlockModel.class)).orElse(null);
        } else {
            File f = getModJarFromModId(model.split(":")[0]).orElse(null);
            if (f == null)
                return null;

            URL jarUrl = null;
            try {
                jarUrl = new URL("jar:file:" + f.getAbsolutePath() + "!/");
            } catch (MalformedURLException ignored) {
            }

            if(jarUrl == null)
                return null;

            try(URLClassLoader loader = new URLClassLoader(new URL[]{ jarUrl }))
            {
                Optional<String> modelJson = loadFileText(loader, "assets/" + model.split(":")[0] + "/models/" + model.split(":")[1] + ".json");
                return modelJson.map(s -> CCT_Resource_API.GSON.fromJson(s, BlockModel.class)).orElse(null);
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
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

    private static Optional<String> loadBundledFileText(String location) {
        try (InputStream modelStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(location)) {
            return readInStreamAll(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "unused"})
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

    @SuppressWarnings({"unchecked", "unused"})
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

    @SuppressWarnings("unused")
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
        }
        if (flags.contains("m")) { // m = moodels + textures
            loadStatefulBlockModelInfo(Objects.requireNonNull(b), blockInfo);
        }

        return blockInfo;
    }

    private void loadStatefulBlockModelInfo(Block b, HashMap<String, Object> blockInfo) {
        ResourceLocation blockId = b.getRegistryName();
        if (blockId == null)
            return;

        BlockModelInfo model = loadBlockModelByBlockId(blockId);
        if (model == null)
            return;

        blockInfo.put("model", model.asHashMap());
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
