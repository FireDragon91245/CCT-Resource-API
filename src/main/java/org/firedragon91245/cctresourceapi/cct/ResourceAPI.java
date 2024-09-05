package org.firedragon91245.cctresourceapi.cct;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.firedragon91245.cctresourceapi.CCT_Resource_API;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static BlockModelInfo loadBlockModelInfoByBlockId(@Nonnull ResourceLocation location) {
        BlockModelInfo modelInfo = new BlockModelInfo(location);
        if (location.getNamespace().equals("minecraft")) {
            String blockId = location.getPath();
            Optional<String> stateModelJson = loadBundledFileText("bundled_resources/minecraft/blockstates/" + blockId + ".json");
            if (stateModelJson.isPresent()) {
                return loadStatefulBlockModelInfo(modelInfo, stateModelJson.get());
            } else {
                Optional<String> modelJson = loadBundledFileText("bundled_resources/minecraft/models/block/" + blockId + ".json");
                if (modelJson.isPresent()) {
                    BlockModel model = CCT_Resource_API.GSON.fromJson(modelJson.get(), BlockModel.class);
                    modelInfo.statefullModel = false;
                    modelInfo.rootModel = model;
                    modelInfo.models.put("minecraft:block/" + blockId, model);

                    modelInfo.models.putAll(getParentModelsRecursive(modelInfo));
                    loadBlockModelTextures(modelInfo);
                    return modelInfo;
                }
                else {
                    return null;
                }
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
                        loadBlockModelTextures(modelInfo);
                        return modelInfo;
                    }
                }
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }

        }
        return null;
    }

    private static void loadBlockModelTextures(BlockModelInfo modelInfo) {
        modelInfo.models.forEach((key, value) -> {
            if (value != null) {
                if (value.textures != null) {
                    value.textures.forEach((key1, value1) -> {
                        if (value1 != null && !value1.startsWith("#")) {
                            if(modelInfo.textures.containsKey(value1))
                                return;
                            Optional<ModelTexture> texture = loadBlockTextureByLocation(value1);
                            texture.ifPresent(modelTexture -> modelInfo.textures.put(value1, modelTexture));
                        }
                    });
                }
            }
        });
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
            value.ifOneOrElse(
                    one ->
                    {
                        if(modelInfo.models.containsKey(one.model))
                            return;
                        BlockModel model = loadBlockModelByLocation(one.model);
                        modelInfo.models.put(one.model, model);
                    },
                    more -> {
                        for (BlockStateModelVariant variant : more) {
                            if(modelInfo.models.containsKey(variant.model))
                                continue;
                            BlockModel model = loadBlockModelByLocation(variant.model);
                            modelInfo.models.put(variant.model, model);
                        }
                    });
        });

        modelInfo.models.putAll(getParentModelsRecursive(modelInfo));
        loadBlockModelTextures(modelInfo);
        return modelInfo;
    }

    private static Optional<String> loadFileText(ClassLoader loader, String location) {
        try (InputStream modelStream = loader.getResourceAsStream(location)) {
            return readInStreamAll(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    private static Optional<ModelTexture> loadFileImage(ClassLoader loader, String location) {
        try (InputStream modelStream = loader.getResourceAsStream(location)) {
            return readInStreamImage(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    private static Optional<ModelTexture> loadFileBundledImage(String location) {
        try (InputStream modelStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(location)) {
            return readInStreamImage(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    private static Optional<ModelTexture> readInStreamImage(InputStream modelStream) {
        if (modelStream == null) {
            return Optional.empty();
        }

        try (ImageInputStream imageStream = ImageIO.createImageInputStream(modelStream)) {
            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageStream);
            if (!imageReaders.hasNext()) {
                return Optional.empty();
            }

            ImageReader reader = imageReaders.next();
            reader.setInput(imageStream);
            String formatName = reader.getFormatName();

            BufferedImage image = reader.read(0, reader.getDefaultReadParam());

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, formatName, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            ModelTexture modelTexture = new ModelTexture(formatName, image, imageBytes);
            return Optional.of(modelTexture);
        } catch (IOException e) {
            CCT_Resource_API.LOGGER.error("Failed to read image", e);
            return Optional.empty();
        }
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

    private static Optional<ModelTexture> loadBlockTextureByLocation(String texture)
    {
        if(!texture.contains(":"))
        {
            texture = "minecraft:" + texture;
        }
        if (texture.startsWith("minecraft:")) {
            String textureId = texture.substring(10);
            return loadFileBundledImage("bundled_resources/minecraft/textures/" + textureId + ".png");
        } else {
            File f = getModJarFromModId(texture.split(":")[0]).orElse(null);
            if (f == null)
                return Optional.empty();

            URL jarUrl = null;
            try {
                jarUrl = new URL("jar:file:" + f.getAbsolutePath() + "!/");
            } catch (MalformedURLException ignored) {
            }

            if(jarUrl == null)
                return Optional.empty();

            try(URLClassLoader loader = new URLClassLoader(new URL[]{ jarUrl }))
            {
                return loadFileImage(loader, "assets/" + texture.split(":")[0] + "/textures/" + texture.split(":")[1] + ".png");
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
            return Optional.empty();
        }
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
    public String[] getRecipeIds(@Nullable Object filter) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Stream<ResourceLocation> recipies = server.getRecipeManager().getRecipeIds();
        if (filter instanceof String) {
            return recipies.map(ResourceLocation::toString)
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof HashMap) {
            HashMap<String, Object> filterMap = (HashMap<String, Object>) filter;
            String modid = (String) filterMap.getOrDefault("modid", ".*");
            String recipeid = (String) filterMap.getOrDefault("recipeid", ".*");

            Pattern modidRegex = Pattern.compile(modid);
            Pattern recipeidRegex = Pattern.compile(recipeid);

            return recipies.map(ResourceLocation::toString)
                    .filter(str -> filterIds(str, modidRegex, recipeidRegex))
                    .toArray(String[]::new);
        }
        return recipies.map(ResourceLocation::toString)
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    private static HashMap<String, Object> ingredientAsHashMap(Ingredient ingredient)
    {
        HashMap<String, Object> ingredientInfo = new HashMap<>();
        ingredientInfo.put("empty", ingredient.isEmpty());
        ingredientInfo.put("simple", ingredient.isSimple());
        ingredientInfo.put("vanilla", ingredient.isVanilla());
        List<HashMap<String, Object>> items = Arrays.stream(ingredient.getItems()).map(ResourceAPI::itemStackAsHashMap).collect(Collectors.toList());
        ListIterator<HashMap<String, Object>> itemIter = items.listIterator();
        HashMap<Integer, Object> itemsMap = new HashMap<>();
        while(itemIter.hasNext())
        {
            itemsMap.put(itemIter.nextIndex() + 1, itemIter.next());
        }
        ingredientInfo.put("items", itemsMap);
        return ingredientInfo;
    }

    private static HashMap<Integer, Object> ingredientsAsHashMap(List<Ingredient> ingredients)
    {
        HashMap<Integer, Object> ingredientsInfo = new HashMap<>();
        for (int i = 0; i < ingredients.size(); i++) {
            ingredientsInfo.put(i + 1, ingredientAsHashMap(ingredients.get(i)));
        }
        return ingredientsInfo;
    }

    private static Map<String, Object> convertNBTtoMap(@Nullable CompoundNBT compoundNBT) {
        if(compoundNBT == null)
            return null;
        Map<String, Object> map = new HashMap<>();

        for (String key : compoundNBT.getAllKeys()) {
            INBT value = compoundNBT.get(key);
            if(value == null)
                continue;

            if (value instanceof CompoundNBT) {
                map.put(key, convertNBTtoMap((CompoundNBT) value));
            } else if (value instanceof ListNBT) {
                map.put(key, convertListNBTtoList((ListNBT) value));
            } else {
                map.put(key, value.getAsString());
            }
        }

        return map;
    }

    private static List<Object> convertListNBTtoList(ListNBT listNBT) {
        List<Object> list = new ArrayList<>();

        for (INBT element : listNBT) {
            if (element instanceof CompoundNBT) {
                list.add(convertNBTtoMap((CompoundNBT) element));
            } else if (element instanceof ListNBT) {
                list.add(convertListNBTtoList((ListNBT) element));
            } else {
                list.add(element.getAsString());
            }
        }

        return list;
    }

    public static HashMap<String, Object> itemStackAsHashMap(ItemStack item)
    {
        HashMap<String, Object> itemStackInfo = new HashMap<>();
        itemStackInfo.put("item", Objects.requireNonNull(item.getItem().getRegistryName()).toString());
        itemStackInfo.put("count", item.getCount());
        itemStackInfo.put("nbt", convertNBTtoMap(item.getTag()));
        return itemStackInfo;
    }

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    public HashMap<String, Object> getRecipeInfo(Object filter)
    {
        if(filter == null)
            return null;

        HashMap<String, Object> recipeInfo = new HashMap<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(filter instanceof String)
        {
            String recipeId = (String) filter;
            ResourceLocation recipeLocation = new ResourceLocation(recipeId);
            Collection<IRecipe<?>> recipes = server.getRecipeManager().getRecipes();
            recipes.stream().filter(recipe -> recipe.getId().equals(recipeLocation)).findFirst().ifPresent(recipe -> {
                recipeInfo.put("recipeid", recipe.getId().toString());
                recipeInfo.put("type", recipe.getType().toString());
                recipeInfo.put("group", recipe.getGroup());
                recipeInfo.put("ingredients", ingredientsAsHashMap(recipe.getIngredients()));
                recipeInfo.put("result", itemStackAsHashMap(recipe.getResultItem()));
            });
            return recipeInfo;
        }
        else if(filter instanceof Map)
        {
            Map<String, Object> filterMap = (Map<String, Object>) filter;

            Predicate<IRecipe<?>> simpleFilter = assembleRecipeSimpleFilter(filterMap);
            Predicate<IRecipe<?>> resultFilter = assembleRecipeResultFilter(filterMap);
            Predicate<IRecipe<?>> ingredientFilter = assembleRecipeIngredientFilter(filterMap);

            Collection<IRecipe<?>> recipes = server.getRecipeManager().getRecipes();
            recipes.stream().filter(simpleFilter.and(resultFilter).and(ingredientFilter)).findFirst().ifPresent(recipe -> {
                recipeInfo.put("recipeid", recipe.getId().toString());
                recipeInfo.put("type", recipe.getType().toString());
                recipeInfo.put("group", recipe.getGroup());
                recipeInfo.put("ingredients", ingredientsAsHashMap(recipe.getIngredients()));
                recipeInfo.put("result", itemStackAsHashMap(recipe.getResultItem()));
            });

            return recipeInfo;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Predicate<IRecipe<?>> assembleRecipeIngredientFilter(Map<String, Object> filterMap) {
        if(!filterMap.containsKey("ingredients"))
            return recipe -> true;

        Object ingredientsFilter = filterMap.get("ingredients");
        if(ingredientsFilter instanceof Map)
        {
            Map<Object, Object> ingredientsFilterMap = (Map<Object, Object>) ingredientsFilter;
            return recipe -> matchIngredients(recipe.getIngredients(), ingredientsFilterMap);
        } else if (ingredientsFilter instanceof String) {
            String ingredientsId = (String) ingredientsFilter;
            return recipe -> {
                NonNullList<Ingredient> ingredients = recipe.getIngredients();
                return ingredients.stream()
                        .map(Ingredient::getItems)
                        .flatMap(Arrays::stream)
                        .map(ItemStack::getItem)
                        .map(item -> defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                        .allMatch(str -> str.equals(ingredientsId));
            };
        }

        return recipe -> true;

        // TODO: implement ingredient filter
        // TODO: also modify assembleRecipeResultFilter to use the same pattern (extract common parts for example matchItemStack)
        // TODO: use hirarchy of method matchIngedients -> matchIngredient -> matchItemStacks -> matchItemStack -> matchNBT
        // TODO: at any stage allow String as contains filter itemid + modid as regex filter and count if posible
        // TODO: note at matchIngedients, matchIngedient and matchItemStacks if modid and itemid or z.b. "minecraft:stone" = 1 are present acumulate items of childs so {ingidients={"minecraft:stone"=3}} acumulate all ingidents item stacks and filter recipees that only take in a total of 3 stone
    }

    @SuppressWarnings("unchecked")
    private static boolean matchIngredients(NonNullList<Ingredient> ingredients, Map<Object, Object> ingredientsFilterMap) {
        if(ingredientsFilterMap.containsKey("count"))
        {
            Integer count = objectToInt(ingredientsFilterMap.get("count"));
            if(count == null)
                return false;

            int ingredientCount = countIngredients(ingredients);

            if(ingredientCount != count)
                return false;
        }
        if(ingredientsFilterMap.containsKey("itemid") || ingredientsFilterMap.containsKey("modid"))
        {
            String modid = (String) ingredientsFilterMap.getOrDefault("modid", ".*");
            String itemid = (String) ingredientsFilterMap.getOrDefault("itemid", ".*");

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(itemid);

            boolean allMatch = matchIngedients(ingredients, modidRegex, itemidRegex);
            if(!allMatch)
                return false;
        }
        Map<String, Integer> specificItemCounts = filterSpecificItemFilters(ingredientsFilterMap);
        if(!specificItemCounts.isEmpty()){
            for (Map.Entry<String, Integer> entry : specificItemCounts.entrySet()) {
                String[] parts = entry.getKey().split(":");
                String modid = parts[0];
                String itemid = parts[1];

                Pattern modidRegex = Pattern.compile(modid);
                Pattern itemidRegex = Pattern.compile(itemid);

                int ingredientCount = countIngredientsFiltered(ingredients, item -> {
                    ResourceLocation resourceLocation = defaultIfNull(item.getRegistryName(), new ResourceLocation(""));
                    return modidRegex.matcher(resourceLocation.getNamespace()).matches() && itemidRegex.matcher(resourceLocation.getPath()).matches();
                });

                if(ingredientCount != entry.getValue())
                    return false;
            }
        }
        Map<Integer, Object> ingredientFilters = ingredientsFilterMap.entrySet().stream()
                .filter(entry -> objectToInt(entry.getKey()) != null)
                .collect(Collectors.toMap(entry -> objectToInt(entry.getKey()), Map.Entry::getValue));
        if(!ingredientFilters.isEmpty())
        {
            for (Map.Entry<Integer, Object> entry : ingredientFilters.entrySet()) {
                int index = entry.getKey();
                Object filter = entry.getValue();

                if(filter instanceof Map)
                {
                    Ingredient ingredient = safeGetIndex(ingredients, index - 1);
                    if(ingredient == null)
                        return false;
                    boolean match = matchIngredient(ingredient, (Map<Object, Object>) filter);
                    if(!match)
                        return false;
                } else if (filter instanceof String) {
                    Ingredient ingredient = safeGetIndex(ingredients, index - 1);
                    if(ingredient == null)
                        return false;
                    boolean allMatch = Arrays.stream(ingredient.getItems())
                            .map(ItemStack::getItem)
                            .map(item -> defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                            .allMatch(str -> str.equals(filter));
                    if(!allMatch)
                        return false;
                }
            }
        }
        return true;
    }

    private static Map<String, Integer> filterSpecificItemFilters(Map<Object, Object> ingredientsFilterMap) {
        return ingredientsFilterMap.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String && objectToInt(entry.getValue()) != null)
                .map(entry -> new AbstractMap.SimpleEntry<>((String) entry.getKey(), objectToInt(entry.getValue())))
                .filter(entry -> countChar(entry.getKey(), ':') == 1)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @SuppressWarnings("unchecked")
    private static boolean matchIngredient(Ingredient ingredient, Map<Object, Object> filter) {
        if(filter.containsKey("count"))
        {
            Integer count = objectToInt(filter.get("count"));
            if(count == null)
                return false;

            int ingredientCount = Arrays.stream(ingredient.getItems())
                    .map(ItemStack::getCount)
                    .reduce(0, Integer::sum);

            if(ingredientCount != count)
                return false;
        }
        if(filter.containsKey("itemid") || filter.containsKey("modid"))
        {
            String modid = (String) filter.getOrDefault("modid", ".*");
            String itemid = (String) filter.getOrDefault("itemid", ".*");

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(itemid);

            boolean allMatch = Arrays.stream(ingredient.getItems())
                    .map(ItemStack::getItem)
                    .map(item -> defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                    .allMatch(str -> modidRegex.matcher(str).matches() && itemidRegex.matcher(str).matches());
            if(!allMatch)
                return false;
        }
        Map<String, Integer> specificItemCounts = filterSpecificItemFilters(filter);
        if(!specificItemCounts.isEmpty()){
            for (Map.Entry<String, Integer> entry : specificItemCounts.entrySet()) {
                String[] parts = entry.getKey().split(":");
                String modid = parts[0];
                String itemid = parts[1];

                Pattern modidRegex = Pattern.compile(modid);
                Pattern itemidRegex = Pattern.compile(itemid);

                int ingredientCount = Arrays.stream(ingredient.getItems())
                        .filter(item -> {
                            ResourceLocation resourceLocation = defaultIfNull(item.getItem().getRegistryName(), new ResourceLocation(""));
                            return modidRegex.matcher(resourceLocation.getNamespace()).matches() && itemidRegex.matcher(resourceLocation.getPath()).matches();
                        })
                        .map(ItemStack::getCount)
                        .reduce(0, Integer::sum);

                if(ingredientCount != entry.getValue())
                    return false;
            }
        }
        Map<Integer, Object> itemStackFilters = filter.entrySet().stream()
                .filter(entry -> objectToInt(entry.getKey()) != null)
                .collect(Collectors.toMap(entry -> objectToInt(entry.getKey()), Map.Entry::getValue));
        if(!itemStackFilters.isEmpty())
        {
            for (Map.Entry<Integer, Object> entry : itemStackFilters.entrySet()) {
                int index = entry.getKey();
                Object filterObj = entry.getValue();

                ItemStack itemStack = safeGetIndex(ingredient.getItems(), index - 1);
                if(itemStack == null)
                    return false;

                if(filterObj instanceof Map)
                {
                    boolean match = matchItemStack(itemStack, (Map<Object, Object>) filterObj);
                    if(!match)
                        return false;
                } else if (filterObj instanceof String) {
                    boolean allMatch = defaultIfNull(itemStack.getItem().getRegistryName(), new ResourceLocation("")).toString().equals(filterObj);
                    if(!allMatch)
                        return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean matchItemStack(ItemStack itemStack, Map<Object, Object> filterObj) {
        if(filterObj.containsKey("count"))
        {
            Integer count = objectToInt(filterObj.get("count"));
            if(count == null)
                return false;

            if(itemStack.getCount() != count)
                return false;
        }
        if(filterObj.containsKey("itemid") || filterObj.containsKey("modid"))
        {
            String modid = (String) filterObj.getOrDefault("modid", ".*");
            String itemid = (String) filterObj.getOrDefault("itemid", ".*");

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(itemid);

            String itemStackId = defaultIfNull(itemStack.getItem().getRegistryName(), new ResourceLocation("")).toString();
            if(!modidRegex.matcher(itemStackId).matches() || !itemidRegex.matcher(itemStackId).matches())
                return false;
        }
        if(filterObj.containsKey("nbt"))
        {
            Object nbtObj = filterObj.get("nbt");
            if(nbtObj instanceof Map)
            {
                boolean nbtMatch = matchNBT(itemStack.getTag(), (Map<Object, Object>) nbtObj);
                if(!nbtMatch)
                    return false;
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean matchNBT(CompoundNBT tag, Map<Object, Object> nbtObj) {
        if(tag == null)
            return false;

        for (Map.Entry<Object, Object> entry : nbtObj.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            INBT tagValue = tag.get(key);

            if(value instanceof String)
            {
                if(!(tagValue instanceof StringNBT))
                    return false;
                if(!tagValue.getAsString().equals(value))
                    return false;
            }
            else if(value instanceof Integer)
            {
                if(!(tagValue instanceof IntNBT))
                    return false;
                if(((IntNBT) tagValue).getAsInt() != (int) value)
                    return false;
            }
            else if(value instanceof Double)
            {
                if(!(tagValue instanceof DoubleNBT))
                    return false;
                if(((DoubleNBT) tagValue).getAsDouble() != (double) value)
                    return false;
            }
            else if(value instanceof Float)
            {
                if(!(tagValue instanceof FloatNBT))
                    return false;
                if(((FloatNBT) tagValue).getAsFloat() != (float) value)
                    return false;
            }
            else if(value instanceof Long)
            {
                if(!(tagValue instanceof LongNBT))
                    return false;
                if(((LongNBT) tagValue).getAsLong() != (long) value)
                    return false;
            }
            else if(value instanceof Byte)
            {
                if(!(tagValue instanceof ByteNBT))
                    return false;
                if(((ByteNBT) tagValue).getAsByte() != (byte) value)
                    return false;
            }
            else if(value instanceof Short)
            {
                if(!(tagValue instanceof ShortNBT))
                    return false;
                if(((ShortNBT) tagValue).getAsShort() != (short) value)
                    return false;
            }
            else if(value instanceof Boolean)
            {
                if(!(tagValue instanceof ByteNBT))
                    return false;
                if(((ByteNBT) tagValue).getAsByte() == 1 != (boolean) value)
                    return false;
            }
            else if(value instanceof Map)
            {
                CompoundNBT tagCompound = tag.getCompound(key);
                boolean nbtMatch = matchNBT(tagCompound, (Map<Object, Object>) value);
                if(!nbtMatch)
                    return false;
            }
            else if(value instanceof List)
            {
                ListNBT tagList = tag.getList(key, 10);
                boolean nbtMatch = matchListNBT(tagList, (List<Object>) value);
                if(!nbtMatch)
                    return false;
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean matchListNBT(ListNBT tagList, List<Object> value) {
        if(tagList.size() != value.size())
            return false;

        for (int i = 0; i < tagList.size(); i++) {
            INBT tagElement = tagList.get(i);
            Object valueElement = value.get(i);

            if(tagElement instanceof StringNBT && valueElement instanceof String)
            {
                if(!tagElement.getAsString().equals(valueElement))
                    return false;
            }
            else if(tagElement instanceof IntNBT && valueElement instanceof Integer)
            {
                if(((IntNBT) tagElement).getAsInt() != (int) valueElement)
                    return false;
            }
            else if(tagElement instanceof DoubleNBT && valueElement instanceof Double)
            {
                if(((DoubleNBT) tagElement).getAsDouble() != (double) valueElement)
                    return false;
            }
            else if(tagElement instanceof FloatNBT && valueElement instanceof Float)
            {
                if(((FloatNBT) tagElement).getAsFloat() != (float) valueElement)
                    return false;
            }
            else if(tagElement instanceof LongNBT && valueElement instanceof Long)
            {
                if(((LongNBT) tagElement).getAsLong() != (long) valueElement)
                    return false;
            }
            else if(tagElement instanceof ByteNBT && valueElement instanceof Byte)
            {
                if(((ByteNBT) tagElement).getAsByte() != (byte) valueElement)
                    return false;
            }
            else if(tagElement instanceof ShortNBT && valueElement instanceof Short)
            {
                if(((ShortNBT) tagElement).getAsShort() != (short) valueElement)
                    return false;
            }
            else if(tagElement instanceof CompoundNBT && valueElement instanceof Map)
            {
                boolean nbtMatch = matchNBT((CompoundNBT) tagElement, (Map<Object, Object>) valueElement);
                if(!nbtMatch)
                    return false;
            }
            else if(tagElement instanceof ListNBT && valueElement instanceof List)
            {
                boolean nbtMatch = matchListNBT((ListNBT) tagElement, (List<Object>) valueElement);
                if(!nbtMatch)
                    return false;
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    private static <T> T safeGetIndex(T[] array, int index)
    {
        if(index < 0 || index >= array.length)
            return null;
        return array[index];
    }

    private static <T> T safeGetIndex(List<T> list, int index) {
        if(index < 0 || index >= list.size())
            return null;
        return list.get(index);
    }

    private static int countIngredientsFiltered(NonNullList<Ingredient> ingredients, Predicate<Item> filter) {
        return ingredients.stream()
                .map(Ingredient::getItems)
                .flatMap(Arrays::stream)
                .filter(stack -> filter.test(stack.getItem()))
                .map(ItemStack::getCount)
                .reduce(0, Integer::sum);
    }

    private static int countChar(String str, char c) {
        if(str == null)
            return 0;
        return (int) str.chars().filter(ch -> ch == c).count();
    }

    private static boolean matchIngedients(NonNullList<Ingredient> ingredients, Pattern modidRegex, Pattern itemidRegex) {
        return ingredients.stream()
                .map(Ingredient::getItems)
                .flatMap(Arrays::stream)
                .map(ItemStack::getItem)
                .map(item -> defaultIfNull(item.getRegistryName(), new ResourceLocation("")))
                .allMatch(resourceLocation ->
                        modidRegex.matcher(resourceLocation.getNamespace()).matches() && itemidRegex.matcher(resourceLocation.getPath()).matches());
    }

    private static int countIngredients(NonNullList<Ingredient> ingredients) {
        return ingredients.stream()
                .map(Ingredient::getItems)
                .flatMap(Arrays::stream)
                .map(ItemStack::getCount)
                .reduce(0, Integer::sum);
    }

    private static Integer objectToInt(Object count) {
        if(count instanceof Float)
            return ((Float) count).intValue();

        if(count instanceof Double)
            return ((Double) count).intValue();

        if(count instanceof Integer)
            return (Integer) count;

        if(count instanceof Long)
            return ((Long) count).intValue();

        if(count instanceof String) {
            try {
                return Integer.parseInt((String) count);
            }
            catch (NumberFormatException ignored)
            {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Predicate<IRecipe<?>> assembleRecipeResultFilter(Map<String, Object> filterMap) {
        if(!filterMap.containsKey("result"))
            return recipe -> true;

        Object resultFilter = filterMap.get("result");
        if(resultFilter instanceof String)
        {
            String resultId = (String) resultFilter;
            ResourceLocation resultLocation = new ResourceLocation(resultId);
            return recipe -> Objects.equals(recipe.getResultItem().getItem().getRegistryName(), resultLocation);
        } else if (resultFilter instanceof Map) {
            Map<Object, Object> resultFilterMap = (Map<Object, Object>) resultFilter;

            return recipe -> matchItemStack(recipe.getResultItem(), resultFilterMap);
        }
        return recipe -> true;
    }

    private static Predicate<IRecipe<?>> assembleRecipeSimpleFilter(Map<String, Object> filterMap) {
        String recipeType = (String) filterMap.getOrDefault("type", ".*");
        String recipeGroup = (String) filterMap.getOrDefault("group", ".*");
        String modid = (String) filterMap.getOrDefault("modid", ".*");
        String recipeId = (String) filterMap.getOrDefault("recipeid", ".*");

        Pattern recipeTypeRegex = Pattern.compile(recipeType);
        Pattern recipeGroupRegex = Pattern.compile(recipeGroup);
        Pattern modidRegex = Pattern.compile(modid);
        Pattern recipeIdRegex = Pattern.compile(recipeId);

        return recipe -> {
            ResourceLocation recipeLocation = recipe.getId();
            if (recipeLocation == null)
                return false;
            String recipeIdStr = recipeLocation.toString();
            return recipeTypeRegex.matcher(recipe.getType().toString()).matches() &&
                    recipeGroupRegex.matcher(recipe.getGroup()).matches() &&
                    modidRegex.matcher(recipeLocation.getNamespace()).matches() &&
                    recipeIdRegex.matcher(recipeLocation.getPath()).matches();
        };
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
            loadBlockModelInfo(Objects.requireNonNull(b), blockInfo);
        }

        return blockInfo;
    }

    private void loadBlockModelInfo(Block b, HashMap<String, Object> blockInfo) {
        ResourceLocation blockId = b.getRegistryName();
        if (blockId == null)
            return;

        BlockModelInfo model = loadBlockModelInfoByBlockId(blockId);
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
