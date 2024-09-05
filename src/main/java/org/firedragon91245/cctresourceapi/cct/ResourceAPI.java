package org.firedragon91245.cctresourceapi.cct;

import dan200.computercraft.api.lua.*;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.firedragon91245.cctresourceapi.ColorUtil;
import org.firedragon91245.cctresourceapi.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceAPI implements ILuaAPI {

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public String[] getRecipeIds(@Nullable Object filter) {
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
                    .filter(str -> ResourceFiltering.filterIds(str, modidRegex, recipeidRegex))
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

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public String imageBytesToCCFormat(Object image) throws LuaException {
        if(image instanceof Map) {
            Map<String, Object> imageMap = (Map<String, Object>) image;
            if (!imageMap.containsKey("imageBytes") || !imageMap.containsKey("formatName"))
                return null;

            Object imageBytesObj = imageMap.get("imageBytes");
            Object formatObj = imageMap.get("formatName");
            if (imageBytesObj instanceof Map && formatObj instanceof String) {
                Map<Integer, Double> imageBytes = (Map<Integer, Double>) imageBytesObj;
                Byte[] bytes = imageBytes.entrySet().stream()
                        .sorted(Comparator.comparingInt(Map.Entry::getKey))
                        .map(entry -> entry.getValue().byteValue())
                        .toArray(Byte[]::new);
                String format = (String) formatObj;

                byte[] byteArray = new byte[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    byteArray[i] = bytes[i];
                }

                try {
                    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(byteArray));
                    return convertBufferedImageToCCString(bufferedImage);
                } catch (IOException e) {
                    throw new LuaException("Failed to read image bytes");
                }
            }
        }
        return null;
    }

    private static String convertBufferedImageToCCString(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        StringBuilder ccImageBuilder = new StringBuilder();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y), true);
                int ccColor = findClosestCCColor(color);
                ccImageBuilder.append(Integer.toHexString(ccColor));
            }
            if (y < height - 1) {
                ccImageBuilder.append("\n"); // Add newline between rows
            }
        }

        return ccImageBuilder.toString();
    }

    private static final Map<Integer, Color> COMPUTECRAFT_PALETTE = new HashMap<Integer, Color>()
    {{
            put(0, new Color(240, 240, 240));   // 1: white
            put(1, new Color(242, 178, 51));    // 2: orange
            put(2, new Color(229, 127, 216));   // 4: magenta
            put(3, new Color(153, 178, 242));   // 8: light blue
            put(4, new Color(222, 222, 108));   // 16: yellow
            put(5, new Color(127, 204, 25));    // 32: lime
            put(6, new Color(242, 178, 204));   // 64: pink
            put(7, new Color(76, 76, 76));    // 128: gray
            put(8, new Color(153, 153, 153));   // 256: light gray
            put(9, new Color(76, 153, 178));    // 512: cyan
            put(10, new Color(178, 102, 229));   // 1024: purple
            put(11, new Color(51, 102, 204));    // 2048: blue
            put(12, new Color(127, 102, 76));    // 4096: brown
            put(13, new Color(87, 166, 78));     // 8192: green
            put(14, new Color(204, 76, 76));     // 16384: red
            put(15, new Color(17, 17, 17));       // 32768: black
    }};

    private static int findClosestCCColor(Color color) {
        int closestColorIndex = 0;

        double closestDistance = ColorUtil.rgbDistance(color, COMPUTECRAFT_PALETTE.get(0));
        for (int i = 1; i < 16; i++) {
            double distance = ColorUtil.rgbDistance(color, COMPUTECRAFT_PALETTE.get(i));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestColorIndex = i;
            }
        }

        return closestColorIndex;
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
    final public HashMap<String, Object> getRecipeInfo(Object filter)
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

            Predicate<IRecipe<?>> simpleFilter = ResourceFiltering.assembleRecipeSimpleFilter(filterMap);
            Predicate<IRecipe<?>> resultFilter = ResourceFiltering.assembleRecipeResultFilter(filterMap);
            Predicate<IRecipe<?>> ingredientFilter = ResourceFiltering.assembleRecipeIngredientFilter(filterMap);

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

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public String[] getBlockIds(@Nullable Object filter) {
        if (filter instanceof String) {
            return ForgeRegistries.BLOCKS.getValues().stream()
                    .map(block -> Util.defaultIfNull(block.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof HashMap) {
            HashMap<String, Object> filterMap = (HashMap<String, Object>) filter;
            String modid = (String) filterMap.getOrDefault("modid", ".*");
            String blockid = (String) filterMap.getOrDefault("blockid", ".*");

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(blockid);

            return ForgeRegistries.BLOCKS.getValues().stream()
                    .map(block -> Util.defaultIfNull(block.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> ResourceFiltering.filterIds(str, modidRegex, itemidRegex))
                    .toArray(String[]::new);

        }
        return ForgeRegistries.BLOCKS.getValues().stream()
                .map(block -> Util.defaultIfNull(block.getRegistryName(), new ResourceLocation("")).toString())
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public String[] getItemIds(@Nullable Object filter) {
        if (filter instanceof String) {
            return ForgeRegistries.ITEMS.getValues().stream()
                    .map(item -> Util.defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof HashMap) {
            HashMap<String, Object> filterMap = (HashMap<String, Object>) filter;
            String modid = (String) filterMap.getOrDefault("modid", ".*");
            String itemid = (String) filterMap.getOrDefault("itemid", ".*");

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(itemid);

            return ForgeRegistries.ITEMS.getValues().stream()
                    .map(item -> Util.defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                    .filter(str -> ResourceFiltering.filterIds(str, modidRegex, itemidRegex))
                    .toArray(String[]::new);

        }
        return ForgeRegistries.ITEMS.getValues().stream()
                .map(item -> Util.defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    @SuppressWarnings("unused")
    @LuaFunction
    final public HashMap<String, Object> getBlockInfo(String blockId, String flags) {
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
            ResourceLoading.loadBlockModelInfo(Objects.requireNonNull(b), blockInfo);
        }

        return blockInfo;
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
