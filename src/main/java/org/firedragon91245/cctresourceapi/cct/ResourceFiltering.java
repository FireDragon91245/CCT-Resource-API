package org.firedragon91245.cctresourceapi.cct;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.firedragon91245.cctresourceapi.Util;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResourceFiltering {
    protected static boolean filterIds(String toFilter, Pattern modidRegex, Pattern idRegex) {
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
    protected static Predicate<IRecipe<?>> assembleRecipeIngredientFilter(Map<String, Object> filterMap) {
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
                        .map(item -> Util.defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
                        .allMatch(str -> str.equals(ingredientsId));
            };
        }

        return recipe -> true;
    }

    @SuppressWarnings("unchecked")
    private static boolean matchIngredients(NonNullList<Ingredient> ingredients, Map<Object, Object> ingredientsFilterMap) {
        if(ingredientsFilterMap.containsKey("count"))
        {
            Integer count = Util.objectToInt(ingredientsFilterMap.get("count"));
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
                    ResourceLocation resourceLocation = Util.defaultIfNull(item.getRegistryName(), new ResourceLocation(""));
                    return modidRegex.matcher(resourceLocation.getNamespace()).matches() && itemidRegex.matcher(resourceLocation.getPath()).matches();
                });

                if(ingredientCount != entry.getValue())
                    return false;
            }
        }
        Map<Integer, Object> ingredientFilters = ingredientsFilterMap.entrySet().stream()
                .filter(entry -> Util.objectToInt(entry.getKey()) != null)
                .collect(Collectors.toMap(entry -> Util.objectToInt(entry.getKey()), Map.Entry::getValue));
        if(!ingredientFilters.isEmpty())
        {
            for (Map.Entry<Integer, Object> entry : ingredientFilters.entrySet()) {
                int index = entry.getKey();
                Object filter = entry.getValue();

                if(filter instanceof Map)
                {
                    Ingredient ingredient = Util.safeGetIndex(ingredients, index - 1);
                    if(ingredient == null)
                        return false;
                    boolean match = matchIngredient(ingredient, (Map<Object, Object>) filter);
                    if(!match)
                        return false;
                } else if (filter instanceof String) {
                    Ingredient ingredient = Util.safeGetIndex(ingredients, index - 1);
                    if(ingredient == null)
                        return false;
                    boolean allMatch = Arrays.stream(ingredient.getItems())
                            .map(ItemStack::getItem)
                            .map(item -> Util.defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
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
                .filter(entry -> entry.getKey() instanceof String && Util.objectToInt(entry.getValue()) != null)
                .map(entry -> new AbstractMap.SimpleEntry<>((String) entry.getKey(), Util.objectToInt(entry.getValue())))
                .filter(entry -> Util.countChar(entry.getKey(), ':') == 1)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @SuppressWarnings("unchecked")
    private static boolean matchIngredient(Ingredient ingredient, Map<Object, Object> filter) {
        if(filter.containsKey("count"))
        {
            Integer count = Util.objectToInt(filter.get("count"));
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
                    .map(item -> Util.defaultIfNull(item.getRegistryName(), new ResourceLocation("")).toString())
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
                            ResourceLocation resourceLocation = Util.defaultIfNull(item.getItem().getRegistryName(), new ResourceLocation(""));
                            return modidRegex.matcher(resourceLocation.getNamespace()).matches() && itemidRegex.matcher(resourceLocation.getPath()).matches();
                        })
                        .map(ItemStack::getCount)
                        .reduce(0, Integer::sum);

                if(ingredientCount != entry.getValue())
                    return false;
            }
        }
        Map<Integer, Object> itemStackFilters = filter.entrySet().stream()
                .filter(entry -> Util.objectToInt(entry.getKey()) != null)
                .collect(Collectors.toMap(entry -> Util.objectToInt(entry.getKey()), Map.Entry::getValue));
        if(!itemStackFilters.isEmpty())
        {
            for (Map.Entry<Integer, Object> entry : itemStackFilters.entrySet()) {
                int index = entry.getKey();
                Object filterObj = entry.getValue();

                ItemStack itemStack = Util.safeGetIndex(ingredient.getItems(), index - 1);
                if(itemStack == null)
                    return false;

                if(filterObj instanceof Map)
                {
                    boolean match = matchItemStack(itemStack, (Map<Object, Object>) filterObj);
                    if(!match)
                        return false;
                } else if (filterObj instanceof String) {
                    boolean allMatch = Util.defaultIfNull(itemStack.getItem().getRegistryName(), new ResourceLocation("")).toString().equals(filterObj);
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
            Integer count = Util.objectToInt(filterObj.get("count"));
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

            String itemStackId = Util.defaultIfNull(itemStack.getItem().getRegistryName(), new ResourceLocation("")).toString();
            if(!modidRegex.matcher(itemStackId).matches() || !itemidRegex.matcher(itemStackId).matches())
                return false;
        }
        if(filterObj.containsKey("nbt"))
        {
            Object nbtObj = filterObj.get("nbt");
            if(nbtObj instanceof Map)
            {
                return matchNBT(itemStack.getTag(), (Map<Object, Object>) nbtObj);
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

    private static int countIngredientsFiltered(NonNullList<Ingredient> ingredients, Predicate<Item> filter) {
        return ingredients.stream()
                .map(Ingredient::getItems)
                .flatMap(Arrays::stream)
                .filter(stack -> filter.test(stack.getItem()))
                .map(ItemStack::getCount)
                .reduce(0, Integer::sum);
    }

    private static boolean matchIngedients(NonNullList<Ingredient> ingredients, Pattern modidRegex, Pattern itemidRegex) {
        return ingredients.stream()
                .map(Ingredient::getItems)
                .flatMap(Arrays::stream)
                .map(ItemStack::getItem)
                .map(item -> Util.defaultIfNull(item.getRegistryName(), new ResourceLocation("")))
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

    @SuppressWarnings("unchecked")
    protected static Predicate<IRecipe<?>> assembleRecipeResultFilter(Map<String, Object> filterMap) {
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

    protected static Predicate<IRecipe<?>> assembleRecipeSimpleFilter(Map<String, Object> filterMap) {
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
            return recipeTypeRegex.matcher(recipe.getType().toString()).matches() &&
                    recipeGroupRegex.matcher(recipe.getGroup()).matches() &&
                    modidRegex.matcher(recipeLocation.getNamespace()).matches() &&
                    recipeIdRegex.matcher(recipeLocation.getPath()).matches();
        };
    }
}
