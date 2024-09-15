package org.firedragon91245.cctresourceapi.cct;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PCM16to8ByteArrayOutputStream extends ByteArrayOutputStream {

    private final boolean isBigEndian;
    private final int numChannels;
    AudioInputStream stream;


    public PCM16to8ByteArrayOutputStream(AudioInputStream stream) throws UnsupportedAudioFileException, IOException {
        this.stream = stream;

        AudioFormat format = stream.getFormat();
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            throw new UnsupportedAudioFileException("Only PCM_SIGNED encoding is supported.");
        }

        if (format.getSampleSizeInBits() != 16) {
            throw new UnsupportedAudioFileException("Only 16-bit samples are supported.");
        }

        this.isBigEndian = format.isBigEndian();
        this.numChannels = format.getChannels();

        downscaleSamples();
    }

    public void downscaleSamples() throws IOException {
        byte[] inputBuffer = new byte[4096]; // Buffer size (must be multiple of frame size)
        int bytesRead;

        while ((bytesRead = stream.read(inputBuffer)) != -1) {
            // Ensure that we read a complete frame
            int frameSize = stream.getFormat().getFrameSize();
            if (bytesRead % frameSize != 0) {
                // Handle incomplete frame if necessary
                // For simplicity, we'll ignore incomplete frames here
                bytesRead -= bytesRead % frameSize;
            }

            // Number of samples read
            int totalSamples = bytesRead / 2; // 2 bytes per 16-bit sample
            byte[] outputBuffer = new byte[totalSamples / numChannels];

            for (int i = 0, j = 0; i < bytesRead; i += 2 * numChannels, j++) {
                for (int channel = 0; channel < numChannels; channel++) {
                    int sampleIndex = i + (2 * channel);
                    if (sampleIndex + 1 >= bytesRead) {
                        // Prevent out-of-bounds access
                        break;
                    }

                    int low;
                    int high;

                    if (isBigEndian) {
                        high = inputBuffer[sampleIndex] << 8;
                        low = inputBuffer[sampleIndex + 1] & 0xFF;
                    } else {
                        low = inputBuffer[sampleIndex] & 0xFF;
                        high = inputBuffer[sampleIndex + 1] << 8;
                    }

                    short sample16 = (short) (high | low);

                    // Convert to 8-bit signed with rounding
                    int eightBit = sample16 / 256;

                    // Apply rounding based on the lower byte
                    // Equivalent to:
                    // eightBit += (sample16 & 0xFF) > 0x80 ? 1 : 0;
                    if ((sample16 & 0xFF) > 0x80) {
                        eightBit += 1;
                    }

                    // Clamp the value to fit into signed byte range (-128 to 127)
                    if (eightBit > 127) {
                        eightBit = 127;
                    } else if (eightBit < -128) {
                        eightBit = -128;
                    }

                    byte sample8 = (byte) eightBit;

                    // Store the converted sample
                    outputBuffer[j] = sample8;
                }
            }

            // Write the converted 8-bit samples to the ByteArrayOutputStream
            this.write(outputBuffer);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        stream.close();
    }
}
