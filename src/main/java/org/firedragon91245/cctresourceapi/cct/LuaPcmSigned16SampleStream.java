package org.firedragon91245.cctresourceapi.cct;

import dan200.computercraft.api.lua.LuaException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;

public class LuaPcmSigned16SampleStream extends LuaByteStream {

    private final AudioInputStream baseAudioStream;
    private final AudioFormat TARGET_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, // Encoding
            48000,                           // Sample rate
            16,                              // Sample size in bits
            1,                               // Channels (mono)
            2,                               // Frame size (bytes per frame)
            48000,                           // Frame rate
            false                            // Little endian
    );
    private boolean useConversionStream;
    private AudioInputStream convertedAudioStream;

    public LuaPcmSigned16SampleStream(AudioInputStream baseStream, boolean keepSuperByteStreamUninitialized) throws LuaException {
        super();
        this.baseAudioStream = baseStream;

        if (needsConversion()) {
            useConversionStream = true;

            if (ResourceLoading.AUDIO_SYSTEM.isConversionSupported(TARGET_FORMAT, baseStream.getFormat())) {
                convertedAudioStream = ResourceLoading.AUDIO_SYSTEM.getAudioInputStream(TARGET_FORMAT, baseStream);
            } else {
                throw new LuaException("Unsupported audio format");
            }
        }

        if (!keepSuperByteStreamUninitialized)
            super.setBaseInStream(useConversionStream ? convertedAudioStream : baseStream);
    }

    private boolean needsConversion() {
        AudioFormat format = baseAudioStream.getFormat();
        return !TARGET_FORMAT.matches(format);
    }

    protected AudioInputStream getBaseAudioStream() {
        return useConversionStream ? convertedAudioStream : baseAudioStream;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            if (useConversionStream && convertedAudioStream != null) {
                convertedAudioStream.close();
            }
            baseAudioStream.close();
            closed = true;
        }
    }

    @Override
    void closeImpl() throws LuaException {
        try {
            if (useConversionStream && convertedAudioStream != null) {
                convertedAudioStream.close();
            }
            baseAudioStream.close();
        } catch (Exception e) {
            throw new LuaException(e.getMessage());
        }
    }
}
