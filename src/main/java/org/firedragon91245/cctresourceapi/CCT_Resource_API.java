package org.firedragon91245.cctresourceapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.math.Vector3f;
import dan200.computercraft.api.ComputerCraftAPI;
import javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.firedragon91245.cctresourceapi.entity.BlockStateModel;
import org.firedragon91245.cctresourceapi.entity.BlockStateModelVariant;
import org.firedragon91245.cctresourceapi.cct.ResourceAPI;
import org.firedragon91245.cctresourceapi.json.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.AudioFileReader;
import java.util.ServiceLoader;

@Mod("cct_resource_api")
public class CCT_Resource_API {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockStateModelVariant.class, new BlockStateModelVariantSerializer())
            .registerTypeAdapter(OneOrMore.class, new OneOrMoreSerializer())
            .registerTypeAdapter(BlockStateModel.class, new BlockStateModelSerializer())
            .registerTypeAdapter(Vector3f.class, new Vector3fSerializer())
            .registerTypeAdapter(VariantArray.class, new VariantArraySerializer())
            .create();

    public CCT_Resource_API() {

        ComputerCraftAPI.registerAPIFactory(new ResourceAPI.Factory());

        AudioFileFormat.Type[] t = AudioSystem.getAudioFileTypes();
        for (AudioFileFormat.Type type : t) {
            LOGGER.info(type.getExtension());
        }

        ServiceLoader<AudioFileReader> reader = ServiceLoader.load(AudioFileReader.class, VorbisAudioFileReader.class.getClassLoader());
        for (AudioFileReader r : reader) {
            LOGGER.info(r.getClass().getName());
        }
    }
}
