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

    public LuaSoundStreamProvider(String audioLocation) {
        this.audioLocation = audioLocation;
    }

    @LuaFunction
    final public String getSoundLocation() {
        return audioLocation;
    }

    @LuaFunction
    final public LuaByteStream createByteStream() {
        AudioInputStream base_stream = ResourceLoading.loadSoundStream(this.audioLocation);
        if(base_stream == null)
            return null;
        return new LuaByteStream(base_stream);
    }

    @LuaFunction
    final public LuaPcmSigned16SampleStream createPcmSigned16SampleStream() throws LuaException {
        AudioInputStream base_stream = ResourceLoading.loadSoundStream(this.audioLocation);
        if(base_stream == null)
            return null;
        return new LuaPcmSigned16SampleStream(base_stream, false);
    }

    @LuaFunction
    final public LuaPcmSigned8SampleStream createPcmSigned8SampleStream() throws LuaException {
        AudioInputStream base_stream = ResourceLoading.loadSoundStream(this.audioLocation);
        if(base_stream == null)
            return null;
        return new LuaPcmSigned8SampleStream(base_stream);
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
