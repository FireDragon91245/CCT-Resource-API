package org.firedragon91245.cctresourceapi.cct;

import javazoom.spi.vorbis.sampled.file.VorbisEncoding;
import org.firedragon91245.cctresourceapi.CCT_Resource_API;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public class StereoVorbisToMonoPCMProvider extends CustomizedFormatConversionProvider {

    private static final AudioFormat.Encoding SOURCE_ENCODING = VorbisEncoding.VORBISENC;
    private static final AudioFormat.Encoding TARGET_ENCODING = VorbisEncoding.PCM_SIGNED;

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[]{
                SOURCE_ENCODING
        };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[]{
                TARGET_ENCODING
        };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (isConversionSupported(sourceFormat)) {
            return new AudioFormat.Encoding[]{TARGET_ENCODING};
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (TARGET_ENCODING.equals(targetEncoding) && isConversionSupported(sourceFormat)) {
            return new AudioFormat[]{
                    new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            sourceFormat.getSampleRate(),
                            16, // sample size in bits
                            1,  // channels (mono)
                            2,  // frame size (bytes)
                            sourceFormat.getSampleRate(),
                            false // little endian
                    )
            };
        } else {
            return new AudioFormat[0];
        }
    }

    /**
     * Checks if the conversion from the given source format is supported.
     *
     * @param sourceFormat The source audio format.
     * @return True if conversion is supported, false otherwise.
     */
    private boolean isConversionSupported(AudioFormat sourceFormat) {
        return sourceFormat.getChannels() == 2 &&
                SOURCE_ENCODING.equals(sourceFormat.getEncoding());
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        if (!TARGET_ENCODING.equals(targetEncoding)) {
            throw new IllegalArgumentException("Unsupported target encoding: " + targetEncoding);
        }

        AudioFormat sourceFormat = sourceStream.getFormat();
        if (!isConversionSupported(sourceFormat)) {
            throw new IllegalArgumentException("Unsupported source format: " + sourceFormat);
        }

        try {
            // Step 1: Convert VORBIS Stereo -> PCM_SIGNED Stereo
            AudioInputStream pcmStereoStream = convertToPcmSignedStereo(sourceStream);

            // Step 2: Convert PCM_SIGNED Stereo -> PCM_SIGNED Mono
            AudioFormat monoFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    pcmStereoStream.getFormat().getSampleRate(),
                    16, // sample size in bits
                    1,  // channels (mono)
                    2,  // frame size (bytes)
                    pcmStereoStream.getFormat().getSampleRate(),
                    false // little endian
            );

            AudioInputStream monoStream = system.getAudioInputStream(monoFormat, pcmStereoStream);

            return monoStream;
        } catch (UnsupportedAudioFileException | IOException e) {
            CCT_Resource_API.LOGGER.error("Failed to convert audio stream", e);
            return null;
        }
    }

    /**
     * Converts the given AudioInputStream from VORBIS Stereo to PCM_SIGNED Stereo.
     *
     * @param sourceStream The source AudioInputStream in VORBIS Stereo.
     * @return An AudioInputStream in PCM_SIGNED Stereo.
     * @throws UnsupportedAudioFileException If the conversion is not supported.
     * @throws IOException                   If an I/O error occurs during conversion.
     */
    private AudioInputStream convertToPcmSignedStereo(AudioInputStream sourceStream)
            throws UnsupportedAudioFileException, IOException {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16, // sample size in bits
                2,  // channels (stereo)
                4,  // frame size (bytes)
                sourceFormat.getSampleRate(),
                false // little endian
        );

        AudioInputStream pcmStereoStream = system.getAudioInputStream(targetFormat, sourceStream);
        return pcmStereoStream;
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        if (!isConversionSupported(sourceStream.getFormat()) ||
                !TARGET_ENCODING.equals(targetFormat.getEncoding()) ||
                targetFormat.getChannels() != 1 ||
                targetFormat.getSampleSizeInBits() != 16 ||
                targetFormat.isBigEndian()) {

            throw new IllegalArgumentException("Unsupported target format: " + targetFormat);
        }

        try {
            // Step 1: Convert VORBIS Stereo -> PCM_SIGNED Stereo
            AudioInputStream pcmStereoStream = convertToPcmSignedStereo(sourceStream);

            // Step 2: Convert PCM_SIGNED Stereo -> PCM_SIGNED Mono
            AudioInputStream monoStream = system.getAudioInputStream(targetFormat, pcmStereoStream);

            return monoStream;
        } catch (UnsupportedAudioFileException | IOException e) {
            CCT_Resource_API.LOGGER.error("Failed to convert audio stream", e);
            return null;
        }
    }
}
