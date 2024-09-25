package org.firedragon91245.cctresourceapi.cct;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.FormatConversionProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CustomizedAudioSystem {

    private final List<ClassLoader> loaders;
    private final List<AudioFileReader> readers;
    private final List<FormatConversionProvider> conversions;
    private final CustomizedAudioSystem base;

    protected CustomizedAudioSystem(final List<ClassLoader> loaders, final List<AudioFileReader> readers, final List<FormatConversionProvider> conversions, @Nullable CustomizedAudioSystem base)
    {
        this.loaders = loaders;
        this.readers = readers;
        this.conversions = conversions;

        if(base == null)
        {
            base = this;
        }
        this.base = base;
    }

    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding,
                                                       AudioInputStream sourceStream) {
        Objects.requireNonNull(targetEncoding);
        Objects.requireNonNull(sourceStream);
        if (sourceStream.getFormat().getEncoding().equals(targetEncoding)) {
            return sourceStream;
        }

        List<FormatConversionProvider> codecs = getFormatConversionProviders();

        for (FormatConversionProvider codec : codecs) {
            if (codec.isConversionSupported(targetEncoding, sourceStream.getFormat())) {
                return codec.getAudioInputStream(targetEncoding, sourceStream);
            }
        }
        // we ran out of options, throw an exception
        throw new IllegalArgumentException("Unsupported conversion: " + targetEncoding + " from " + sourceStream.getFormat());
    }

    public AudioInputStream getAudioInputStream(AudioFormat targetFormat,
                                                       AudioInputStream sourceStream) {
        if (sourceStream.getFormat().matches(targetFormat)) {
            return sourceStream;
        }

        List<FormatConversionProvider> codecs = getFormatConversionProviders();

        for (FormatConversionProvider codec : codecs) {
            if (codec.isConversionSupported(targetFormat, sourceStream.getFormat())) {
                return codec.getAudioInputStream(targetFormat, sourceStream);
            }
        }

        // we ran out of options...
        throw new IllegalArgumentException("Unsupported conversion: " + targetFormat + " from " + sourceStream.getFormat());
    }

    private List<FormatConversionProvider> getFormatConversionProviders() {
        List<FormatConversionProvider> providers = new ArrayList<>();
        for(final ClassLoader classLoader : loaders) {
            ServiceLoader<FormatConversionProvider> loader = ServiceLoader.load(FormatConversionProvider.class, classLoader);
            for(FormatConversionProvider provider : loader) {
                if(providers.stream().noneMatch(p -> p.getClass().equals(provider.getClass()))) {
                    providers.add(provider);
                }
            }
        }

        for(final FormatConversionProvider provider : conversions) {
            if(providers.stream().noneMatch(p -> p.getClass().equals(provider.getClass()))) {
                providers.add(provider);
            }
        }

        for(final FormatConversionProvider provider : base.conversions) {
            if(provider instanceof CustomizedFormatConversionProvider)
            {
                ((CustomizedFormatConversionProvider) provider).setCustomizedAudiosSystem(this.base);
            }
        }

        return providers;
    }

    public AudioInputStream getAudioInputStream(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        Objects.requireNonNull(stream);

        List<AudioFileReader> readers = getAudioFileReaders();

        for (final AudioFileReader reader : readers) {
            try {
                return reader.getAudioInputStream(stream);
            } catch (final UnsupportedAudioFileException ignored) {
            }
        }
        throw new UnsupportedAudioFileException("Stream of unsupported format");
    }

    private List<AudioFileReader> getAudioFileReaders() {
        List<AudioFileReader> readers = new ArrayList<>();

        for(final ClassLoader loader : loaders)
        {
            ServiceLoader<AudioFileReader> serviceLoader = ServiceLoader.load(AudioFileReader.class, loader);
            for(AudioFileReader reader : serviceLoader) {
                if(readers.stream().noneMatch(r -> r.getClass().equals(reader.getClass())))
                {
                    readers.add(reader);
                }
            }
        }

        for(final AudioFileReader reader : this.readers) {
            if(readers.stream().noneMatch(r -> r.getClass().equals(reader.getClass())))
            {
                readers.add(reader);
            }
        }

        return readers;
    }

    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        Objects.requireNonNull(targetFormat);
        Objects.requireNonNull(sourceFormat);
        if (sourceFormat.matches(targetFormat)) {
            return true;
        }

        List<FormatConversionProvider> codecs = getFormatConversionProviders();

        for (FormatConversionProvider codec : codecs) {
            if (codec.isConversionSupported(targetFormat, sourceFormat)) {
                return true;
            }
        }
        return false;
    }

    public boolean isConversionSupported(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        Objects.requireNonNull(targetEncoding);
        Objects.requireNonNull(sourceFormat);
        if (sourceFormat.getEncoding().equals(targetEncoding)) {
            return true;
        }

        List<FormatConversionProvider> codecs = getFormatConversionProviders();

        for (FormatConversionProvider codec : codecs) {
            if (codec.isConversionSupported(targetEncoding, sourceFormat)) {
                return true;
            }
        }
        return false;
    }

}
