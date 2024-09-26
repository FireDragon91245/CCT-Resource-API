package org.firedragon91245.cctresourceapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.firedragon91245.cctresourceapi.cct.ResourceAPI;
import org.firedragon91245.cctresourceapi.entity.BlockStateModel;
import org.firedragon91245.cctresourceapi.entity.BlockStateModelVariant;
import org.firedragon91245.cctresourceapi.json.*;

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

    }
}
