package org.firedragon91245.cctresourceapi.cct;

import dan200.computercraft.api.lua.LuaException;

import javax.sound.sampled.AudioInputStream;
import java.io.IOException;

public class LuaPcmSigned8SampleStream extends LuaPcmSigned16SampleStream{

    PcmSigned16To8ResampleStream baseResampleStream;

    public LuaPcmSigned8SampleStream(AudioInputStream baseStream) throws LuaException {
        super(baseStream, true);
        AudioInputStream stream = super.getBaseAudioStream();
        baseResampleStream = new PcmSigned16To8ResampleStream(stream);
        super.setBaseInStream(baseResampleStream);
    }

    @Override
    public void close() throws IOException {
        if(!closed)
        {
            baseResampleStream.close();
            closed = true;
        }
    }

    @Override
    void closeImpl() throws LuaException {
        try {
            baseResampleStream.close();
        } catch (Exception e) {
            throw new LuaException(e.getMessage());
        }
    }
}
