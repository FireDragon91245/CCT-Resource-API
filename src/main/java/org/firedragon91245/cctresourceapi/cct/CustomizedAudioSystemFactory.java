package org.firedragon91245.cctresourceapi.cct;

import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.FormatConversionProvider;
import java.util.ArrayList;
import java.util.List;

public class CustomizedAudioSystemFactory {

    private final List<ClassLoader> loaders;
    private final List<AudioFileReader> readers;
    private final List<FormatConversionProvider> conversions;
    private CustomizedAudioSystem baseAudioSystem;

    private CustomizedAudioSystemFactory() {
        loaders = new ArrayList<>();
        readers = new ArrayList<>();
        conversions = new ArrayList<>();
    }

    public static CustomizedAudioSystemFactory empty() {
        return new CustomizedAudioSystemFactory();
    }

    public CustomizedAudioSystemFactory withProvidersFromClassLoader(final ClassLoader loader) {
        loaders.add(loader);
        return this;
    }

    public CustomizedAudioSystemFactory withAudioFileReader(final AudioFileReader reader) {
        readers.add(reader);
        return this;
    }

    public CustomizedAudioSystemFactory withAudioConversionProvider(final FormatConversionProvider conversion) {
        conversions.add(conversion);
        return this;
    }

    public CustomizedAudioSystemFactory useCustomizedBaseAudioSystem(final CustomizedAudioSystem base) {
        if (this.baseAudioSystem != null) {
            throw new IllegalStateException("Base audio system is already set");
        }
        this.baseAudioSystem = base;
        return this;
    }

    public CustomizedAudioSystem build() {
        return new CustomizedAudioSystem(loaders, readers, conversions, baseAudioSystem);
    }
}
