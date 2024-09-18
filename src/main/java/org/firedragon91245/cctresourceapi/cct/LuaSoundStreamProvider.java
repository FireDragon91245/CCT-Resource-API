package org.firedragon91245.cctresourceapi.cct;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import org.apache.tika.Tika;
import org.firedragon91245.cctresourceapi.entity.SoundData;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.util.Map;

public class LuaSoundStreamProvider {

    private final String audioLocation;
    private final ResourceAPI apiInstance;

    public LuaSoundStreamProvider(String audioLocation, ResourceAPI apiInstance) {
        this.audioLocation = audioLocation;
        this.apiInstance = apiInstance;
    }

    @LuaFunction
    final public String getSoundLocation() {
        return audioLocation;
    }

    @LuaFunction
    final public LuaByteStream createByteStream() {
        AudioInputStream base_stream = ResourceLoading.loadSoundStream(this.audioLocation);
        if (base_stream == null)
            return null;
        LuaByteStream luaByteStream = new LuaByteStream(base_stream);
        apiInstance.addStreamToClose(luaByteStream);
        return luaByteStream;
    }

    @LuaFunction
    final public LuaPcmSigned16SampleStream createPcmSigned16SampleStream() throws LuaException {
        AudioInputStream base_stream = ResourceLoading.loadSoundStream(this.audioLocation);
        if (base_stream == null)
            return null;
        LuaPcmSigned16SampleStream luaPcmSigned16SampleStream = new LuaPcmSigned16SampleStream(base_stream, false);
        apiInstance.addStreamToClose(luaPcmSigned16SampleStream);
        return luaPcmSigned16SampleStream;
    }

    @LuaFunction
    final public LuaPcmSigned8SampleStream createPcmSigned8SampleStream() throws LuaException {
        AudioInputStream base_stream = ResourceLoading.loadSoundStream(this.audioLocation);
        if (base_stream == null)
            return null;
        LuaPcmSigned8SampleStream luaPcmSigned8SampleStream = new LuaPcmSigned8SampleStream(base_stream);
        apiInstance.addStreamToClose(luaPcmSigned8SampleStream);
        return luaPcmSigned8SampleStream;
    }

    @LuaFunction
    final public Map<String, Object> getSoundInfo() throws LuaException {
        try (AudioInputStream base_stream = ResourceLoading.loadSoundStream(this.audioLocation)) {
            if (base_stream == null)
                return null;

            AudioFormat format = base_stream.getFormat();
            SoundData data = new SoundData();
            data.loadProperties(format, base_stream);
            Tika t = new Tika();
            String mimeType = t.detect(base_stream);
            data.setMimeType(mimeType);
            return data.asHashMap();
        } catch (Exception e) {
            throw new LuaException(e.getMessage());
        }
    }
}
