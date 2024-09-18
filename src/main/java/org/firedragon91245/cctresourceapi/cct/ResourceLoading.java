package org.firedragon91245.cctresourceapi.cct;

import com.google.gson.stream.JsonReader;
import dan200.computercraft.api.lua.LuaException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.apache.tika.Tika;
import org.firedragon91245.cctresourceapi.CCT_Resource_API;
import org.firedragon91245.cctresourceapi.entity.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;

public class ResourceLoading {
    @Nullable
    protected static BlockModelInfo loadBlockModelInfoByBlockId(@Nonnull ResourceLocation location) {
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
                    loadModelTextures(modelInfo);
                    return modelInfo;
                } else {
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
            if (jarUrl == null)
                return null;

            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
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
                        loadModelTextures(modelInfo);
                        return modelInfo;
                    }
                }
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }

        }
        return null;
    }

    protected static HashMap<String, BlockModel> getParentModelsRecursive(BlockModelInfo modelInfo) {
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

    protected static BlockModelInfo loadStatefulBlockModelInfo(BlockModelInfo modelInfo, String stateModelJson) {
        BlockStateModel stateModel = CCT_Resource_API.GSON.fromJson(stateModelJson, BlockStateModel.class);
        modelInfo.statefullModel = true;
        modelInfo.modelState = stateModel;
        modelInfo.modelState.variants.forEach((key, value) -> {
            if (value == null)
                return;
            value.ifOneOrElse(
                    one ->
                    {
                        if (modelInfo.models.containsKey(one.model))
                            return;
                        BlockModel model = loadBlockModelByLocation(one.model);
                        modelInfo.models.put(one.model, model);
                    },
                    more -> {
                        for (BlockStateModelVariant variant : more) {
                            if (modelInfo.models.containsKey(variant.model))
                                continue;
                            BlockModel model = loadBlockModelByLocation(variant.model);
                            modelInfo.models.put(variant.model, model);
                        }
                    });
        });

        modelInfo.models.putAll(getParentModelsRecursive(modelInfo));
        loadModelTextures(modelInfo);
        return modelInfo;
    }

    protected static Optional<ModelTexture> loadFileImage(ClassLoader loader, String location) {
        try (InputStream modelStream = loader.getResourceAsStream(location)) {
            return readInStreamImage(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    protected static Optional<ModelTexture> loadFileBundledImage(String location) {
        try (InputStream modelStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(location)) {
            return readInStreamImage(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    protected static Optional<ModelTexture> readInStreamImage(InputStream modelStream) {
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
    protected static Optional<String> readInStreamAll(InputStream modelStream) throws IOException {
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

    private static Optional<ModelTexture> getModelTexture(String texture) {
        if (!texture.contains(":")) {
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

            if (jarUrl == null)
                return Optional.empty();

            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
                return loadFileImage(loader, "assets/" + texture.split(":")[0] + "/textures/" + texture.split(":")[1] + ".png");
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
            return Optional.empty();
        }
    }

    protected static BlockModel loadBlockModelByLocation(String model) {
        if (!model.contains(":")) {
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

            if (jarUrl == null)
                return null;

            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
                Optional<String> modelJson = loadFileText(loader, "assets/" + model.split(":")[0] + "/models/" + model.split(":")[1] + ".json");
                return modelJson.map(s -> CCT_Resource_API.GSON.fromJson(s, BlockModel.class)).orElse(null);
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
            return null;
        }
    }

    protected static Optional<File> getModJarFromModId(String modid) {
        Optional<IModFile> file = ModList.get().getMods().stream()
                .filter(modContainer -> modContainer.getModId().equals(modid))
                .map(modContainer -> {
                    IModFileInfo modFileInfo = modContainer.getOwningFile();
                    if (modFileInfo != null) {
                        return modFileInfo.getFile();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst();

        return file.map(IModFile::getFilePath).map(Path::toFile);
    }

    protected static Optional<String> loadBundledFileText(String location) {
        try (InputStream modelStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(location)) {
            return readInStreamAll(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    protected static void loadBlockModelInfo(Block b, Map<String, Object> blockInfo) {
        ResourceLocation blockId = b.getRegistryName();
        if (blockId == null)
            return;

        BlockModelInfo model = loadBlockModelInfoByBlockId(blockId);
        if (model == null)
            return;

        blockInfo.put("model", model.asHashMap());
    }

    private static Optional<String> loadFileText(ClassLoader loader, String location) {
        try (InputStream modelStream = loader.getResourceAsStream(location)) {
            return readInStreamAll(modelStream);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    static <TReturn> TReturn loadBufferedImageFromTextureObject(Object image, Map<Object, Color> colorMap, BiFunction<BufferedImage, Map<Object, Color>, TReturn> consumer) throws LuaException {
        if (image instanceof Map) {
            Map<Object, Object> imageMap = (Map<Object, Object>) image;
            if (!imageMap.containsKey("imageBytes") || !imageMap.containsKey("formatName"))
                return null;

            Object imageBytesObj = imageMap.get("imageBytes");
            Object formatObj = imageMap.get("formatName");
            if (imageBytesObj instanceof Map && formatObj instanceof String format) {
                Map<Integer, Double> imageBytes = (Map<Integer, Double>) imageBytesObj;
                Byte[] bytes = imageBytes.entrySet().stream()
                        .sorted(Comparator.comparingInt(Map.Entry::getKey))
                        .map(entry -> entry.getValue().byteValue())
                        .toArray(Byte[]::new);

                byte[] byteArray = new byte[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    byteArray[i] = bytes[i];
                }

                try {
                    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(byteArray));
                    return consumer.apply(bufferedImage, colorMap);
                } catch (IOException e) {
                    throw new LuaException("Failed to read image bytes");
                }
            }
        }
        return null;
    }

    public static void loadItemModelInfo(Item item, HashMap<String, Object> itemInfo) {
        ResourceLocation itemId = item.getRegistryName();
        if (itemId == null)
            return;

        ItemModelInfo model = loadItemModelInfoByItemId(itemId);
        if (model == null)
            return;

        itemInfo.put("model", model.asHashMap());
    }

    private static URL getModURLFromModId(String modid) {
        File f = getModJarFromModId(modid).orElse(null);
        if (f == null)
            return null;

        URL jarUrl = null;
        try {
            jarUrl = new URL("jar:file:" + f.getAbsolutePath() + "!/");
        } catch (MalformedURLException ignored) {
        }
        return jarUrl;
    }

    private static ItemModelInfo loadItemModelInfoByItemId(ResourceLocation itemId) {
        ItemModelInfo modelInfo = new ItemModelInfo(itemId.toString());
        if (itemId.getNamespace().equals("minecraft")) {
            String itemIdStr = itemId.getPath();
            Optional<String> modelJson = loadBundledFileText("bundled_resources/minecraft/models/item/" + itemIdStr + ".json");
            if (modelJson.isPresent()) {
                ItemModel model = CCT_Resource_API.GSON.fromJson(modelJson.get(), ItemModel.class);
                modelInfo.rootModel = model;
                modelInfo.models.put("minecraft:item/" + itemIdStr, model);

                modelInfo.models.putAll(getParentModelsRecursiveItem(modelInfo));
                loadModelTextures(modelInfo);
                return modelInfo;
            } else {
                return null;
            }
        } else {
            URL jarUrl = getModURLFromModId(itemId.getNamespace());
            if (jarUrl == null)
                return null;

            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
                Optional<String> modelJson = loadFileText(loader, "assets/" + itemId.getNamespace() + "/models/item/" + itemId.getPath() + ".json");
                if (modelJson.isPresent()) {
                    ItemModel model = CCT_Resource_API.GSON.fromJson(modelJson.get(), ItemModel.class);
                    modelInfo.rootModel = model;
                    modelInfo.models.put(itemId.getNamespace() + ":item/" + itemId.getPath(), model);

                    modelInfo.models.putAll(getParentModelsRecursiveItem(modelInfo));
                    loadModelTextures(modelInfo);
                    return modelInfo;
                }
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }

        }
        return null;
    }

    private static void loadModelTextures(IModelInfo modelInfo) {
        modelInfo.getModels().forEach((key, value) -> {
            if (value != null) {
                if (value.getTextures() != null) {
                    value.getTextures().forEach((key1, value1) -> {
                        if (value1 != null && !value1.startsWith("#")) {
                            if (modelInfo.getTextures().containsKey(value1))
                                return;
                            Optional<ModelTexture> texture = getModelTexture(value1);
                            texture.ifPresent(modelTexture -> modelInfo.putTexture(value1, modelTexture));
                        }
                    });
                }
            }
        });
    }

    private static Map<String, ItemModel> getParentModelsRecursiveItem(ItemModelInfo modelInfo) {
        HashMap<String, ItemModel> newModelsCollector = new HashMap<>();
        HashMap<String, ItemModel> newModels = new HashMap<>();
        do {
            newModels.clear();
            modelInfo.models.forEach((key, value) -> {
                if (value != null && value.parent != null) {
                    if (modelInfo.models.containsKey(value.parent) || newModels.containsKey(value.parent) || newModelsCollector.containsKey(value.parent))
                        return;
                    ItemModel parentModel = loadItemModelByLocation(value.parent);
                    newModels.put(value.parent, parentModel);
                }
            });
            newModelsCollector.putAll(newModels);
        } while (!newModels.isEmpty());

        return newModelsCollector;
    }

    private static ItemModel loadItemModelByLocation(String parent) {
        if (!parent.contains(":")) {
            parent = "minecraft:" + parent;
        }
        if (parent.startsWith("minecraft:")) {
            String modelid = parent.substring(10);
            Optional<String> modelJson = loadBundledFileText("bundled_resources/minecraft/models/" + modelid + ".json");
            return modelJson.map(s -> CCT_Resource_API.GSON.fromJson(s, ItemModel.class)).orElse(null);
        } else {
            URL jarUrl = getModURLFromModId(parent.split(":")[0]);
            if (jarUrl == null)
                return null;

            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
                Optional<String> modelJson = loadFileText(loader, "assets/" + parent.split(":")[0] + "/models/" + parent.split(":")[1] + ".json");
                return modelJson.map(s -> CCT_Resource_API.GSON.fromJson(s, ItemModel.class)).orElse(null);
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
            return null;
        }
    }

    public static SoundInfo getSoundInfo(SoundEvent soundEvent) {
        ResourceLocation soundId = soundEvent.getRegistryName();
        if (soundId == null)
            return null;

        if (soundId.getNamespace().equals("minecraft")) {
            try (JsonReader soundsJson = loadBundledFileJson("bundled_resources/minecraft/sounds.json")) {
                if (soundEvent.getRegistryName() == null)
                    return null;
                if(soundsJson == null)
                    return null;
                return loadSpecificJsonKey(soundsJson, soundEvent.getRegistryName().getPath(), SoundInfo.class);
            } catch (IOException ignored) {
            }
        } else {
            URL jarUrl = getModURLFromModId(soundId.getNamespace());
            if (jarUrl == null)
                return null;

            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
                try (JsonReader soundsJson = loadFileJson(loader, "assets/" + soundId.getNamespace() + "/sounds.json")) {
                    if (soundEvent.getRegistryName() == null)
                        return null;
                    if(soundsJson == null)
                        return null;
                    return loadSpecificJsonKey(soundsJson, soundEvent.getRegistryName().getPath(), SoundInfo.class);
                } catch (IOException ignored) {
                }
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
        }
        return null;
    }

    private static JsonReader loadFileJson(URLClassLoader loader, String s) {
        try (InputStream modelStream = loader.getResourceAsStream(s)) {
            if (modelStream == null)
                return null;
            return new JsonReader(new InputStreamReader(modelStream));
        } catch (IOException ignored) {
        }
        return null;
    }

    private static <T> T loadSpecificJsonKey(JsonReader soundsJson, String path, Class<? extends T> targetClass) throws IOException {
        soundsJson.beginObject();
        while (soundsJson.hasNext()) {
            String key = soundsJson.nextName();
            if (key.equals(path)) {
                return CCT_Resource_API.GSON.fromJson(soundsJson, targetClass);
            } else {
                soundsJson.skipValue();
            }
        }
        return null;
    }

    private static JsonReader loadBundledFileJson(String location) {
        InputStream jsonStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(location);
        if (jsonStream == null)
            return null;
        return new JsonReader(new InputStreamReader(jsonStream));
    }

    public static AudioInputStream loadSoundStream(String soundName) {
        if (!soundName.contains(":")) {
            soundName = "minecraft:" + soundName;
        }

        if (soundName.startsWith("minecraft:")) {
            return loadFileBundledSoundStream("bundled_resources/minecraft/sounds/" + soundName.split(":")[1] + ".ogg");
        } else {
            URL jarUrl = getModURLFromModId(soundName.split(":")[0]);
            if (jarUrl == null)
                return null;

            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
                return loadFileSoundStream(loader, "assets/" + soundName.split(":")[0] + "/sounds/" + soundName.split(":")[1] + ".ogg");
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
        }
        return null;
    }

    private static AudioInputStream loadFileSoundStream(URLClassLoader loader, String s) {
        try {
            InputStream soundStream = loader.getResourceAsStream(s);
            return AudioSystem.getAudioInputStream(new BufferedInputStream(soundStream));
        } catch (IOException | UnsupportedAudioFileException e) {
            CCT_Resource_API.LOGGER.error("Failed to load sound data", e);
        }
        return null;
    }

    public static SoundData loadSoundData(String soundName) {
        if (!soundName.contains(":")) {
            soundName = "minecraft:" + soundName;
        }

        if (soundName.startsWith("minecraft:")) {
            return loadFileBundledSoundData("bundled_resources/minecraft/sounds/" + soundName.split(":")[1] + ".ogg");
        } else {
            URL jarUrl = getModURLFromModId(soundName.split(":")[0]);
            if (jarUrl == null)
                return null;

            try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
                return loadFileSoundData(loader, "assets/" + soundName.split(":")[0] + "/sounds/" + soundName.split(":")[1] + ".ogg");
            } catch (IOException e) {
                CCT_Resource_API.LOGGER.error("Failed to load mod jar", e);
            }
        }
        return null;
    }

    private static SoundData loadFileSoundData(URLClassLoader loader, String s) {
        try (InputStream soundStream = loader.getResourceAsStream(s)) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundStream);
            AudioFormat format = audioInputStream.getFormat();

            return soundDataFromAudioStream(s, soundStream, audioInputStream, format);
        } catch (IOException | UnsupportedAudioFileException ignored) {
        }
        return null;
    }

    private static SoundData soundDataFromAudioStream(String s, InputStream soundStream, AudioInputStream audioInputStream, AudioFormat format) throws IOException {
        SoundData result = new SoundData();
        result.loadProperties(format, audioInputStream);

        Tika tika = new Tika();
        String mimeType = tika.detect(soundStream, s);
        result.setMimeType(mimeType);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096]; // 4KB buffer
        int bytesRead;

        while ((bytesRead = audioInputStream.read(buffer)) != -1) {
            byteStream.write(buffer, 0, bytesRead);
        }

        result.setData(byteStream.toByteArray());
        return result;
    }

    private static SoundData loadFileBundledSoundData(String s) {
        try (InputStream soundStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(s)) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundStream);
            AudioFormat format = audioInputStream.getFormat();

            return soundDataFromAudioStream(s, soundStream, audioInputStream, format);
        } catch (IOException | UnsupportedAudioFileException e) {
            CCT_Resource_API.LOGGER.error("Failed to load sound data", e);
        }
        return null;
    }

    private static AudioInputStream loadFileBundledSoundStream(String s) {
        try {
            InputStream soundStream = CCT_Resource_API.class.getClassLoader().getResourceAsStream(s);
            return AudioSystem.getAudioInputStream(new BufferedInputStream(soundStream));
        } catch (IOException | UnsupportedAudioFileException e) {
            CCT_Resource_API.LOGGER.error("Failed to load sound data", e);
        }
        return null;
    }

    protected static ByteArrayOutputStream convertToSpeakerFormat(InputStream inputStream)
            throws UnsupportedAudioFileException, IOException {

        AudioInputStream originalAudio = AudioSystem.getAudioInputStream(inputStream);

        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, // Encoding
                48000,                           // Sample rate
                16,                               // Sample size in bits
                1,                               // Channels (mono)
                2,                               // Frame size (bytes per frame)
                48000,                           // Frame rate
                false                            // Little endian
        );

        AudioFormat sourceFormat = originalAudio.getFormat();

        AudioFormat.Encoding[] encodings = AudioSystem.getTargetEncodings(sourceFormat);
        System.out.println("Supported Encodings:");
        for (AudioFormat.Encoding encoding : encodings) {
            System.out.println(encoding);
        }

        AudioFormat[] formats = AudioSystem.getTargetFormats(AudioFormat.Encoding.PCM_SIGNED, sourceFormat);
        System.out.println("Supported Target Formats for PCM_SIGNED:");
        for (AudioFormat format : formats) {
            System.out.println(format);
        }

        if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            throw new UnsupportedAudioFileException("Conversion to target format not supported.");
        }

        // Convert the audio to the target format
        AudioInputStream convertedAudio = AudioSystem.getAudioInputStream(targetFormat, originalAudio);

        PcmSigned16To8ResampleStream resampledAudio = new PcmSigned16To8ResampleStream(convertedAudio);
        ByteArrayOutputStream out = readEverythingToByteOutputStream(resampledAudio);
        resampledAudio.close();

        return out;
    }

    private static ByteArrayOutputStream readEverythingToByteOutputStream(InputStream stream) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096]; // 4KB buffer
        int bytesRead;

        try {
            while ((bytesRead = stream.read(buffer)) != -1) {
                byteStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            CCT_Resource_API.LOGGER.error("Failed to read audio data", e);
        }

        return byteStream;
    }
}
