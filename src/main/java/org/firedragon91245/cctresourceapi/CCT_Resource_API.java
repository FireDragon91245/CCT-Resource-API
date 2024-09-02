package org.firedragon91245.cctresourceapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.firedragon91245.cctresourceapi.cct.BlockStateModel;
import org.firedragon91245.cctresourceapi.cct.BlockStateModelVariant;
import org.firedragon91245.cctresourceapi.cct.ResourceAPI;
import org.firedragon91245.cctresourceapi.json.BlockStateModelSerializer;
import org.firedragon91245.cctresourceapi.json.BlockStateModelVariantSerializer;
import org.firedragon91245.cctresourceapi.json.OneOrMoreSerializer;

@Mod("cct_resource_api")
public class CCT_Resource_API {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockStateModelVariant.class, new BlockStateModelVariantSerializer())
            .registerTypeAdapter(OneOrMore.class, new OneOrMoreSerializer())
            .registerTypeAdapter(BlockStateModel.class, new BlockStateModelSerializer())
            .create();

    public CCT_Resource_API() {

        ComputerCraftAPI.registerAPIFactory(new ResourceAPI.Factory());

    }
}
