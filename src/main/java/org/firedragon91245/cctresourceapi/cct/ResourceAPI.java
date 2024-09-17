package org.firedragon91245.cctresourceapi.cct;

import dan200.computercraft.api.lua.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.firedragon91245.cctresourceapi.ColorUtil;
import org.firedragon91245.cctresourceapi.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceAPI implements ILuaAPI {

    private static final Map<Object, Color> COMPUTECRAFT_PALETTE_BLIT = new HashMap<>() {{
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
        put('a', new Color(178, 102, 229));   // 1024: purple
        put('b', new Color(51, 102, 204));    // 2048: blue
        put('c', new Color(127, 102, 76));    // 4096: brown
        put('d', new Color(87, 166, 78));     // 8192: green
        put('e', new Color(204, 76, 76));     // 16384: red
        put('f', new Color(17, 17, 17));       // 32768: black
    }};

    private static final Map<Object, Color> COMPUTECRAFT_PALETTE_DEC = new HashMap<>() {{
        put(1, new Color(240, 240, 240));   // 1: white
        put(2, new Color(242, 178, 51));    // 2: orange
        put(4, new Color(229, 127, 216));   // 4: magenta
        put(8, new Color(153, 178, 242));   // 8: light blue
        put(16, new Color(222, 222, 108));   // 16: yellow
        put(32, new Color(127, 204, 25));    // 32: lime
        put(64, new Color(242, 178, 204));   // 64: pink
        put(128, new Color(76, 76, 76));    // 128: gray
        put(256, new Color(153, 153, 153));   // 256: light gray
        put(512, new Color(76, 153, 178));    // 512: cyan
        put(1024, new Color(178, 102, 229));   // 1024: purple
        put(2048, new Color(51, 102, 204));    // 2048: blue
        put(4096, new Color(127, 102, 76));    // 4096: brown
        put(8192, new Color(87, 166, 78));     // 8192: green
        put(16384, new Color(204, 76, 76));     // 16384: red
        put(32768, new Color(17, 17, 17));       // 32768: black
    }};

    private static HashMap<String, Object> ingredientAsHashMap(Ingredient ingredient) {
        HashMap<String, Object> ingredientInfo = new HashMap<>();
        ingredientInfo.put("empty", ingredient.isEmpty());
        ingredientInfo.put("simple", ingredient.isSimple());
        ingredientInfo.put("vanilla", ingredient.isVanilla());
        List<HashMap<String, Object>> items = Arrays.stream(ingredient.getItems()).map(ResourceAPI::itemStackAsHashMap).collect(Collectors.toList());
        ListIterator<HashMap<String, Object>> itemIter = items.listIterator();
        HashMap<Integer, Object> itemsMap = new HashMap<>();
        while (itemIter.hasNext()) {
            itemsMap.put(itemIter.nextIndex() + 1, itemIter.next());
        }
        ingredientInfo.put("items", itemsMap);
        return ingredientInfo;
    }

    private static HashMap<Integer, Object> ingredientsAsHashMap(List<Ingredient> ingredients) {
        HashMap<Integer, Object> ingredientsInfo = new HashMap<>();
        for (int i = 0; i < ingredients.size(); i++) {
            ingredientsInfo.put(i + 1, ingredientAsHashMap(ingredients.get(i)));
        }
        return ingredientsInfo;
    }

    private static Map<String, Object> convertNBTtoMap(@Nullable CompoundTag compoundNBT) {
        if (compoundNBT == null)
            return null;
        Map<String, Object> map = new HashMap<>();

        for (String key : compoundNBT.getAllKeys()) {
            Tag value = compoundNBT.get(key);
            if (value == null)
                continue;

            if (value instanceof CompoundTag) {
                map.put(key, convertNBTtoMap((CompoundTag) value));
            } else if (value instanceof ListTag) {
                map.put(key, convertListNBTtoList((ListTag) value));
            } else {
                map.put(key, value.getAsString());
            }
        }

        return map;
    }

    private static List<Object> convertListNBTtoList(ListTag listNBT) {
        List<Object> list = new ArrayList<>();

        for (Tag element : listNBT) {
            if (element instanceof CompoundTag) {
                list.add(convertNBTtoMap((CompoundTag) element));
            } else if (element instanceof ListTag) {
                list.add(convertListNBTtoList((ListTag) element));
            } else {
                list.add(element.getAsString());
            }
        }

        return list;
    }

    private static Map<Integer, Map<Integer, Object>> bufferedImageToPixels(BufferedImage image, Map<Object, Color> colorMap) {
        int width = image.getWidth();
        int height = image.getHeight();
        Map<Integer, Map<Integer, Object>> pixels = new HashMap<>();

        for (int y = 0; y < height; y++) {
            Map<Integer, Object> row = new HashMap<>();
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y), true);
                Object colorKey = findClosestCCColor(color, colorMap);
                row.put(x + 1, colorKey);
            }
            pixels.put(y + 1, row);
        }

        return pixels;
    }

    private static String convertBufferedImageToCCString(BufferedImage image, Map<Object, Color> colorMap) {
        int width = image.getWidth();
        int height = image.getHeight();
        StringBuilder ccImageBuilder = new StringBuilder();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y), true);
                Object ccColor = findClosestCCColor(color, colorMap);
                ccImageBuilder.append(ccColor);
            }
            if (y < height - 1) {
                ccImageBuilder.append("\n"); // Add newline between rows
            }
        }

        return ccImageBuilder.toString();
    }

    private static Object findClosestCCColor(Color color, Map<Object, Color> colorMap) {
        Enumeration<Map.Entry<Object, Color>> entries = Collections.enumeration(colorMap.entrySet());
        Map.Entry<Object, Color> firstEntry = entries.nextElement();
        Object closestColorKey = firstEntry.getKey();
        double closestDistance = ColorUtil.rgbDistance(color, firstEntry.getValue());
        while (entries.hasMoreElements()) {
            Map.Entry<Object, Color> currentEntry = entries.nextElement();
            double distance = ColorUtil.rgbDistance(color, currentEntry.getValue());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestColorKey = currentEntry.getKey();
            }
        }

        return closestColorKey;
    }

    public static HashMap<String, Object> itemStackAsHashMap(ItemStack item) {
        HashMap<String, Object> itemStackInfo = new HashMap<>();
        itemStackInfo.put("item", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item.getItem())).toString());
        itemStackInfo.put("count", item.getCount());
        itemStackInfo.put("nbt", convertNBTtoMap(item.getTag()));
        return itemStackInfo;
    }

    @SuppressWarnings("unchecked")
    private static Color colorFromRGBTable(Object o) {
        if (o instanceof Map) {
            Map<Object, Object> colorMap = (Map<Object, Object>) o;
            if (colorMap.containsKey("r") && colorMap.containsKey("g") && colorMap.containsKey("b")) {
                Object r = colorMap.get("r");
                Object g = colorMap.get("g");
                Object b = colorMap.get("b");
                Object a = colorMap.getOrDefault("a", 255D);
                if (r instanceof Double && g instanceof Double && b instanceof Double && a instanceof Double) {
                    return new Color(((Double) r).intValue(), ((Double) g).intValue(), ((Double) b).intValue(), ((Double) a).intValue());
                }
            }
        }
        return null;
    }

    private static Color colorFromRGBInt(Object o) {
        if (o instanceof Double) {
            int color = ((Double) o).intValue();
            return new Color(color);
        }
        return null;
    }

    private static Map<Integer, Map<Integer, Object>> bufferedImageToPixelsRGBTable(BufferedImage bufferedImage, @Nullable Map<Object, Color> objectColorMap) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        Map<Integer, Map<Integer, Object>> pixels = new HashMap<>();

        for (int y = 0; y < height; y++) {
            Map<Integer, Object> row = new HashMap<>();
            for (int x = 0; x < width; x++) {
                Color color = new Color(bufferedImage.getRGB(x, y), true);
                row.put(x + 1, new HashMap<String, Object>() {{
                    put("r", color.getRed());
                    put("g", color.getGreen());
                    put("b", color.getBlue());
                    put("a", color.getAlpha());
                }});
            }
            pixels.put(y + 1, row);
        }

        return pixels;
    }

    private static Map<Integer, Map<Integer, Object>> bufferedImageToPixelsRGB(BufferedImage bufferedImage, Map<Object, Color> objectColorMap) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        Map<Integer, Map<Integer, Object>> pixels = new HashMap<>();

        for (int y = 0; y < height; y++) {
            Map<Integer, Object> row = new HashMap<>();
            for (int x = 0; x < width; x++) {
                Color color = new Color(bufferedImage.getRGB(x, y), true);
                row.put(x + 1, color.getRGB());
            }
            pixels.put(y + 1, row);
        }

        return pixels;
    }

    private static Map<String, Object> recipeAsMap(Recipe<?> recipe) {
        Map<String, Object> recipeMap = new HashMap<>();
        recipeMap.put("recipeid", recipe.getId().toString());
        recipeMap.put("type", recipe.getType().toString());
        recipeMap.put("group", recipe.getGroup());
        recipeMap.put("ingredients", ingredientsAsHashMap(recipe.getIngredients()));
        RegistryAccess registryAccess = ServerLifecycleHooks.getCurrentServer().registryAccess();
        recipeMap.put("result", itemStackAsHashMap(recipe.getResultItem(registryAccess)));
        return recipeMap;
    }

    private static Map<String, Object> createBlockInfoTable(String flags, Block block) {
        if (block == null)
            return null;

        Map<String, Object> blockInfo = new HashMap<>();
        blockInfo.put("blockid", Util.defaultIfNull(ForgeRegistries.BLOCKS.getKey(block), new ResourceLocation("")).toString());
        if (flags.contains("t")) { // t = tags
            ITagManager<Block> tags = ForgeRegistries.BLOCKS.tags();
            if(tags == null)
                return null;
            blockInfo.put("tags", tags.stream().filter(iTag -> iTag.contains(block)).map(ITag::getKey).map(TagKey::location).map(ResourceLocation::toString).toArray(String[]::new));
        }
        if (flags.contains("m")) { // m = moodels + textures
            ResourceLoading.loadBlockModelInfo(Objects.requireNonNull(block), blockInfo);
        }

        return blockInfo;
    }

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public String[] getRecipeIds(@Nullable Object filter) throws LuaException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Stream<ResourceLocation> recipies = server.getRecipeManager().getRecipeIds();
        if (filter instanceof String) {
            return recipies.map(ResourceLocation::toString)
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof Map) {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;
            String modid;
            String recipeid;

            try{
                modid = (String) filterMap.getOrDefault("modid", ".*");
                recipeid = (String) filterMap.getOrDefault("recipieid", ".*");
            } catch (ClassCastException ignored){
                throw new LuaException("Filter value is not a string! Bitch!");
            }

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

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public Map<String, Object> getItemInfo(Object filter, String flags) throws LuaException {
        Item item = null;
        if (filter instanceof String itemId) {
            ResourceLocation itemLocation = new ResourceLocation(itemId);
            if (!ForgeRegistries.ITEMS.containsKey(itemLocation))
                return null;

            item = ForgeRegistries.ITEMS.getValue(itemLocation);
        } else if (filter instanceof Map) {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;
            String modid;
            String itemid;

            try{
                modid = (String) filterMap.getOrDefault("modid", ".*");
                itemid = (String) filterMap.getOrDefault("itemid", ".*");
            } catch (ClassCastException ignored){
                throw new LuaException("Filter value is not a string! Bitch!");
            }

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(itemid);

            String itemId = ForgeRegistries.ITEMS.getValues().stream()
                    .map(it -> Util.defaultIfNull(ForgeRegistries.ITEMS.getKey(it), new ResourceLocation("")).toString())
                    .filter(str -> ResourceFiltering.filterIds(str, modidRegex, itemidRegex))
                    .findFirst().orElse(null);

            if (itemId == null)
                return null;

            ResourceLocation itemLocation = new ResourceLocation(itemId);
            if (!ForgeRegistries.ITEMS.containsKey(itemLocation))
                return null;

            item = ForgeRegistries.ITEMS.getValue(itemLocation);
        }

        return createItemInfoTable(item, flags);
    }

    private Map<String, Object> createItemInfoTable(Item item, String flags) {
        if (item == null)
            return null;

        HashMap<String, Object> itemInfo = new HashMap<>();

        itemInfo.put("itemid", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString());
        if(flags.contains("t")) // t = tags
        {
            ITagManager<Item> tags = ForgeRegistries.ITEMS.tags();
            if(tags == null)
                return null;
            itemInfo.put("tags", tags.stream().filter(iTag -> iTag.contains(item)).map(ITag::getKey).map(TagKey::location).map(ResourceLocation::toString).toArray(String[]::new));
        }
        if (flags.contains("m")) // m = models + textures
        {
            ResourceLoading.loadItemModelInfo(item, itemInfo);
        }
        if(flags.contains("g"))
        {
            itemAddGeneralInfo(itemInfo, item);
        }
        if(flags.contains("e"))
        {
            AtomicInteger index = new AtomicInteger(1);
            itemInfo.put("enchantments", ForgeRegistries.ENCHANTMENTS.getValues().stream()
                    .filter(enchantment -> enchantment.canEnchant(new ItemStack(item)) || item.canApplyAtEnchantingTable(new ItemStack(item), enchantment))
                    .map(enchantment -> Util.defaultIfNull(ForgeRegistries.ENCHANTMENTS.getKey(enchantment), new ResourceLocation("")).toString())
                    .collect(Collectors.toMap(entry -> index.getAndIncrement(), entry -> entry)));
        }
        return itemInfo;
    }

    private void itemAddGeneralInfo(HashMap<String, Object> itemInfo, Item item) {
        itemInfo.put("fireResistant", item.isFireResistant());
        itemInfo.put("enchantable", item.isEnchantable(new ItemStack(item)));
        itemInfo.put("damageable", item.isDamageable(new ItemStack(item)));
        itemInfo.put("repairable", item.isRepairable(new ItemStack(item)));
        itemInfo.put("stackSize", new ItemStack(item).getMaxStackSize());
        itemInfo.put("foodItem", item.isEdible());
        itemInfo.put("rarity", item.getRarity(new ItemStack(item)).toString());

        if(item.isEdible())
        {
            itemInfo.put("food", foodAsHashMap(item.getFoodProperties(new ItemStack(item), null)));
        }
        
        if(isToolItem(item))
        {
            itemInfo.put("tool", toolAsHashMap(item));
        }

    }

    private Map<String, Object> toolAsHashMap(Item item) {
        Map<String, Object> result = new HashMap<>();
        if(item instanceof TieredItem tieredItem)
        {
            result.put("tier", tieredItemAsHashMap(tieredItem));
        }
        if(item instanceof ArmorItem armorItem)
        {
            result.put("armor", armorItemAsHashMap(armorItem));
        }
        if(item instanceof DiggerItem diggerItem)
        {
            result.put("tool", toolItemAsHashMap(diggerItem));
        }
        if(item instanceof BowItem bowItem)
        {
            result.put("bow", bowItemAsHashMap(bowItem));
        }

        return result;
    }

    private Map<String, Object> bowItemAsHashMap(BowItem bowItem) {
        Map<String, Object> result = new HashMap<>();
        result.put("maxUseDuration", bowItem.getUseDuration(new ItemStack(bowItem)));
        return result;
    }

    private Map<String, Object> toolItemAsHashMap(DiggerItem toolItem) {
        Map<String, Object> result = new HashMap<>();
        result.put("attackDamage", toolItem.getAttackDamage());
        result.put("breakSpeed", toolItem.getDestroySpeed(new ItemStack(toolItem), Blocks.STONE.defaultBlockState()));
        return result;
    }

    private Map<String, Object> armorItemAsHashMap(ArmorItem armorItem) {
        Map<String, Object> result = new HashMap<>();
        result.put("slot", armorItem.getEquipmentSlot().getName());
        result.put("defense", armorItem.getDefense());
        result.put("toughness", armorItem.getToughness());
        result.put("material", armorMaterialAsHashMap(armorItem.getMaterial()));
        return result;
    }

    private Map<String, Object> armorMaterialAsHashMap(ArmorMaterial material) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", material.getName());
        result.put("repairMaterial", ingredientAsHashMap(material.getRepairIngredient()));
        result.put("enchantability", material.getEnchantmentValue());
        result.put("knockbackResistance", material.getKnockbackResistance());
        return result;
    }

    private Map<String, Object> tieredItemAsHashMap(TieredItem tieredItem) {
        Map<String, Object> result = new HashMap<>();
        Tier tier = tieredItem.getTier();
        result.put("maxUses", tier.getUses());
        result.put("speed", tier.getSpeed());
        result.put("damageBonus", tier.getAttackDamageBonus());
        result.put("enchantability", tier.getEnchantmentValue());
        result.put("repairMaterial", ingredientAsHashMap(tier.getRepairIngredient()));
        return result;
    }

    private boolean isToolItem(Item item) {
        return item instanceof ArmorItem || item instanceof ShieldItem || item instanceof BowItem || item instanceof CrossbowItem || item instanceof TieredItem || item instanceof SwordItem || item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem || item instanceof HoeItem;
    }

    private Map<String, Object> foodAsHashMap(@Nullable FoodProperties food) {
        Map<String, Object> result = new HashMap<>();
        if(food == null)
            return result;
        result.put("nutrition", food.getNutrition());
        result.put("saturation", food.getSaturationModifier());
        result.put("meat", food.isMeat());
        result.put("fast", food.isFastFood());
        result.put("alwaysEdible", food.canAlwaysEat());
        return result;
    }

    @SuppressWarnings("unused")
    @LuaFunction
    final public Map<Integer, Map<Integer, Object>> imageBytesToPixelsCustomColorMap(Object image, Object colorMap, String inColorFormat) throws LuaException {
        Map<Object, Color> customColorMap;
        if (inColorFormat.equals("rgb-int")) {
            customColorMap = loadColorMap(colorMap, ResourceAPI::colorFromRGBInt);
        } else if (inColorFormat.equals("rgb-table")) {
            customColorMap = loadColorMap(colorMap, ResourceAPI::colorFromRGBTable);
        } else {
            throw new LuaException("Invalid color format");
        }

        if (customColorMap == null)
            throw new LuaException("Invalid custom color map");

        return ResourceLoading.loadBufferedImageFromTextureObject(image, customColorMap, ResourceAPI::bufferedImageToPixels);
    }

    @SuppressWarnings("unused")
    @LuaFunction
    final public Map<Integer, Map<Integer, Object>> imageBytesToPixels(Object image, String colorFormat) throws LuaException {
        if (colorFormat == null || colorFormat.isEmpty() || colorFormat.equals("blit")) {
            return ResourceLoading.loadBufferedImageFromTextureObject(image, COMPUTECRAFT_PALETTE_BLIT, ResourceAPI::bufferedImageToPixels);
        } else if (colorFormat.equals("decimal")) {
            return ResourceLoading.loadBufferedImageFromTextureObject(image, COMPUTECRAFT_PALETTE_DEC, ResourceAPI::bufferedImageToPixels);
        } else if (colorFormat.equals("rgb-int")) {
            return ResourceLoading.loadBufferedImageFromTextureObject(image, null, ResourceAPI::bufferedImageToPixelsRGB);
        } else if (colorFormat.equals("rgb-table")) {
            return ResourceLoading.loadBufferedImageFromTextureObject(image, null, ResourceAPI::bufferedImageToPixelsRGBTable);
        } else {
            throw new LuaException("Invalid color format");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Color> loadColorMap(@Nullable Object customColorMap, Function<Object, Color> converter) throws LuaException {
        if (customColorMap instanceof Map) {
            Map<Object, Object> rawMap = (Map<Object, Object>) customColorMap;
            Map<Object, Color> colorMap = new HashMap<>();
            for (Map.Entry<Object, Object> rawEntry : rawMap.entrySet()) {
                Object key = rawEntry.getKey();
                Object value = rawEntry.getValue();
                if (key == null || value == null)
                    continue;
                @Nullable Color color = converter.apply(value);
                if (color == null)
                    throw new LuaException("Malformed custom color map entry");
                colorMap.put(key, color);
            }
            return colorMap;
        }
        return null;
    }

    @SuppressWarnings("unused")
    @LuaFunction
    final public String imageBytesToCCFormat(Object image) throws LuaException {
        return ResourceLoading.loadBufferedImageFromTextureObject(image, COMPUTECRAFT_PALETTE_BLIT, ResourceAPI::convertBufferedImageToCCString);
    }

    private static void addRecipeToMap(Map<String, Object> recipeMap, Recipe<?> recipe)
    {
        recipeMap.put("recipeid", recipe.getId().toString());
        recipeMap.put("type", recipe.getType().toString());
        recipeMap.put("group", recipe.getGroup());
        recipeMap.put("ingredients", ingredientsAsHashMap(recipe.getIngredients()));
        recipeMap.put("result", itemStackAsHashMap(recipe.getResultItem(ServerLifecycleHooks.getCurrentServer().registryAccess())));
    }

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public Map<String, Object> getRecipeInfo(Object filter) {
        if (filter == null)
            return null;

        HashMap<String, Object> recipeInfo = new HashMap<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (filter instanceof String recipeId) {
            ResourceLocation recipeLocation = new ResourceLocation(recipeId);

            Collection<Recipe<?>> recipes = server.getRecipeManager().getRecipes();
            Optional<Recipe<?>> recipe = recipes.stream().filter(r -> r.getId().equals(recipeLocation)).findFirst();

            if (recipe.isPresent())
            {
                return recipeAsMap(recipe.get());
            }
        } else if (filter instanceof Map) {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;

            Predicate<Recipe<?>> simpleFilter = ResourceFiltering.assembleRecipeSimpleFilter(filterMap);
            Predicate<Recipe<?>> resultFilter = ResourceFiltering.assembleRecipeResultFilter(filterMap);
            Predicate<Recipe<?>> ingredientFilter = ResourceFiltering.assembleRecipeIngredientFilter(filterMap);

            Collection<Recipe<?>> recipes = server.getRecipeManager().getRecipes();
            Optional<Recipe<?>> recipe = recipes.stream().filter(simpleFilter.and(resultFilter).and(ingredientFilter)).findFirst();

            if(recipe.isPresent())
            {
                return recipeAsMap(recipe.get());
            }
        }
        return null;
    }

    @SuppressWarnings({"unused", "unchecked"})
    @LuaFunction
    final public Map<Integer, Map<String, Object>> getRecipeInfos(Object filter) throws LuaException {
        if(filter instanceof Map)
        {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;

            Predicate<Recipe<?>> simpleFilter = ResourceFiltering.assembleRecipeSimpleFilter(filterMap);
            Predicate<Recipe<?>> resultFilter = ResourceFiltering.assembleRecipeResultFilter(filterMap);
            Predicate<Recipe<?>> ingredientFilter = ResourceFiltering.assembleRecipeIngredientFilter(filterMap);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            Collection<Recipe<?>> recipes = server.getRecipeManager().getRecipes();
            AtomicInteger index = new AtomicInteger(1);
            return recipes.stream()
                    .filter(simpleFilter.and(resultFilter).and(ingredientFilter))
                    .map(ResourceAPI::recipeAsMap)
                    .collect(Collectors.toMap(entry -> index.getAndIncrement(), entry -> entry));

        }
        else
        {
            throw new LuaException("Filter is not a table!");
        }
    }

    @SuppressWarnings({"unused", "unchecked"})
    @LuaFunction
    final public Map<Integer, Map<String, Object>> getItemInfos(Object filter, String flags) throws LuaException {
        if (filter instanceof Map) {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;
            String modid;
            String itemid;
            try {
                modid = (String) filterMap.getOrDefault("modid", ".*");
                itemid = (String) filterMap.getOrDefault("itemid", ".*");
            } catch (ClassCastException ignored) {
                throw new LuaException("Filter value is not a string!");
            }

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(itemid);

            AtomicInteger index = new AtomicInteger(1);
            return ForgeRegistries.ITEMS.getValues().stream()
                    .filter(it -> ResourceFiltering.filterIds(Util.defaultIfNull(ForgeRegistries.ITEMS.getKey(it), new ResourceLocation("")).toString(), modidRegex, itemidRegex))
                    .map(it -> createItemInfoTable(it, flags))
                    .collect(Collectors.toMap(entry -> index.getAndIncrement(), entry -> entry));
        } else {
            throw new LuaException("Filter is not a table!");
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public String[] getBlockIds(@Nullable Object filter) throws LuaException {
        if (filter instanceof String) {
            return ForgeRegistries.BLOCKS.getValues().stream()
                    .map(block -> Util.defaultIfNull(ForgeRegistries.BLOCKS.getKey(block), new ResourceLocation("")).toString())
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof Map) {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;

            String modid;
            String blockid;
            try {
                modid = (String) filterMap.getOrDefault("modid", ".*");
                blockid = (String) filterMap.getOrDefault("blockid", ".*");
            } catch (ClassCastException ignored) {
                throw new LuaException("Filter value is not a string!");
            }

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(blockid);

            return ForgeRegistries.BLOCKS.getValues().stream()
                    .map(block -> Util.defaultIfNull(ForgeRegistries.BLOCKS.getKey(block), new ResourceLocation("")).toString())
                    .filter(str -> ResourceFiltering.filterIds(str, modidRegex, itemidRegex))
                    .toArray(String[]::new);

        } else if (filter == null) {
            return ForgeRegistries.BLOCKS.getValues().stream()
                    .map(block -> Util.defaultIfNull(ForgeRegistries.BLOCKS.getKey(block), new ResourceLocation("")).toString())
                    .filter(str -> !str.isEmpty())
                    .toArray(String[]::new);
        } else {
            throw new LuaException("Filter is not nil, string or table!");
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    @LuaFunction
    final public String[] getItemIds(@Nullable Object filter) throws LuaException {
        if (filter instanceof String) {
            return ForgeRegistries.ITEMS.getValues().stream()
                    .map(item -> Util.defaultIfNull(ForgeRegistries.ITEMS.getKey(item), new ResourceLocation("")).toString())
                    .filter(str -> !str.isEmpty() && str.contains((String) filter))
                    .toArray(String[]::new);
        } else if (filter instanceof Map) {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;

            String modid;
            String itemid;
            try{
                modid = (String) filterMap.getOrDefault("modid", ".*");
                itemid = (String) filterMap.getOrDefault("itemid", ".*");
            } catch (ClassCastException ignored){
                throw new LuaException("Filter value is not a string!");
            }

            Pattern modidRegex = Pattern.compile(modid);
            Pattern itemidRegex = Pattern.compile(itemid);

            return ForgeRegistries.ITEMS.getValues().stream()
                    .map(item -> Util.defaultIfNull(ForgeRegistries.ITEMS.getKey(item), new ResourceLocation("")).toString())
                    .filter(str -> ResourceFiltering.filterIds(str, modidRegex, itemidRegex))
                    .toArray(String[]::new);

        }
        return ForgeRegistries.ITEMS.getValues().stream()
                .map(item -> Util.defaultIfNull(ForgeRegistries.ITEMS.getKey(item), new ResourceLocation("")).toString())
                .filter(str -> !str.isEmpty())
                .toArray(String[]::new);
    }

    @SuppressWarnings({"unused", "unchecked"})
    @LuaFunction
    final public Map<Integer, Map<String, Object>> getBlockInfos(Object filter, String flags) throws LuaException {
        if (filter instanceof Map) {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;

            String modid;
            String blockid;
            try {
                modid = (String) filterMap.getOrDefault("modid", ".*");
                blockid = (String) filterMap.getOrDefault("blockid", ".*");
            } catch (ClassCastException ignored){
                throw new LuaException("Filter value is not a string!");
            }

            Pattern modidRegex = Pattern.compile(modid);
            Pattern blockidRegex = Pattern.compile(blockid);

            AtomicInteger index = new AtomicInteger(1);
            return ForgeRegistries.BLOCKS.getValues().stream()
                    .filter(b -> ResourceFiltering.filterIds(Util.defaultIfNull(ForgeRegistries.BLOCKS.getKey(b), new ResourceLocation("")).toString(), modidRegex, blockidRegex))
                    .map(b -> createBlockInfoTable(flags, b))
                    .collect(Collectors.toMap(entry -> index.getAndIncrement(), entry -> entry));
        } else {
            throw new LuaException("Filter is not a table!");
        }
    }

    @SuppressWarnings({"unused", "unchecked"})
    @LuaFunction
    final public Map<String, Object> getBlockInfo(Object filter, String flags) throws LuaException {
        Block block;
        if (filter instanceof String blockid) {
            ResourceLocation blockLocation = new ResourceLocation(blockid);

            block = ForgeRegistries.BLOCKS.getValue(blockLocation);
        } else if (filter instanceof Map) {
            Map<Object, Object> filterMap = (Map<Object, Object>) filter;

            String modid;
            String blockid;

            try{
                modid = (String) filterMap.getOrDefault("modid", ".*");
                blockid = (String) filterMap.getOrDefault("blockid", ".*");
            } catch (ClassCastException ignored){
                throw new LuaException("Filter value is not a string! Bitch!");
            }


            Pattern modidRegex = Pattern.compile(modid);
            Pattern blockidRegex = Pattern.compile(blockid);

            String blockId = ForgeRegistries.BLOCKS.getValues().stream()
                    .map(b -> Util.defaultIfNull(ForgeRegistries.BLOCKS.getKey(b), new ResourceLocation("")).toString())
                    .filter(str -> ResourceFiltering.filterIds(str, modidRegex, blockidRegex))
                    .findFirst().orElse(null);

            if (blockId == null)
                return null;

            ResourceLocation blockLocation = new ResourceLocation(blockId);

            block = ForgeRegistries.BLOCKS.getValue(blockLocation);
        } else {
            throw new LuaException("Filter is not a string (blockid) or table!");
        }

        return createBlockInfoTable(flags, block);
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
