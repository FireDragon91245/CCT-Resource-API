package org.firedragon91245.cctresourceapi.cct;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PcmSigned16To8ResampleStream extends InputStream {

    private final AudioInputStream basePcm16Stream;
    private final int numChannels;
    private final boolean isBigEndian;
    private final int frameSize;

    private final byte[] inputBuffer;
    private final byte[] outputBuffer;
    private int bufferPos;
    private int bufferLimit;

    private static final int DEFAULT_INPUT_BUFFER_SIZE = 4096;

    public PcmSigned16To8ResampleStream(AudioInputStream basePcm16Stream) {
        if (basePcm16Stream == null) {
            throw new IllegalArgumentException("basePcm16Stream cannot be null");
        }

        AudioFormat format = basePcm16Stream.getFormat();

        if (format.getSampleSizeInBits() != 16) {
            throw new IllegalArgumentException("Audio format must be 16-bit PCM");
        }
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            throw new IllegalArgumentException("Audio encoding must be PCM_SIGNED");
        }

        this.basePcm16Stream = basePcm16Stream;
        this.numChannels = format.getChannels();
        this.isBigEndian = format.isBigEndian();
        this.frameSize = format.getFrameSize();

        if (DEFAULT_INPUT_BUFFER_SIZE % frameSize != 0) {
            throw new IllegalArgumentException("DEFAULT_INPUT_BUFFER_SIZE must be a multiple of frame size");
        }

        this.inputBuffer = new byte[DEFAULT_INPUT_BUFFER_SIZE];
        this.outputBuffer = new byte[DEFAULT_INPUT_BUFFER_SIZE / frameSize];
        this.bufferPos = 0;
        this.bufferLimit = 0;
    }

    private boolean fillBuffer() throws IOException {
        int bytesRead = basePcm16Stream.read(inputBuffer);
        if (bytesRead == -1) {
            return false;
        }

        if (bytesRead % frameSize != 0) {
            bytesRead -= bytesRead % frameSize;
            if (bytesRead <= 0) {
                return false;
            }
        }

        int totalSamples = bytesRead / 2;
        int total8BitSamples = totalSamples / numChannels;

        if (outputBuffer.length < total8BitSamples) {
            throw new IOException("Output buffer size is insufficient.");
        }

        int outputIndex = 0;

        for (int i = 0; i < bytesRead; i += frameSize) {
            for (int channel = 0; channel < numChannels; channel++) {
                int sampleIndex = i + (2 * channel);
                if (sampleIndex + 1 >= bytesRead) {
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
                int eightBit = sample16 / 256;

                if ((sample16 & 0xFF) > 0x80) {
                    eightBit += 1;
                }

                if (eightBit > 127) {
                    eightBit = 127;
                } else if (eightBit < -128) {
                    eightBit = -128;
                }

                byte sample8 = (byte) eightBit;
                outputBuffer[outputIndex++] = sample8;
            }
        }

        bufferPos = 0;
        bufferLimit = outputIndex;

        return bufferLimit > 0;
    }

    @Override
    public int read() throws IOException {
        if (bufferPos >= bufferLimit) {
            if (!fillBuffer()) {
                return -1;
            }
        }
        return outputBuffer[bufferPos++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("Target buffer is null");
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException("Invalid offset/length parameters");
        }
        if (len == 0) {
            return 0;
        }

        int bytesReadTotal = 0;

        while (len > 0) {
            if (bufferPos >= bufferLimit) {
                if (!fillBuffer()) {
                    return bytesReadTotal == 0 ? -1 : bytesReadTotal;
                }
            }

            int bytesAvailable = bufferLimit - bufferPos;
            int bytesToRead = Math.min(len, bytesAvailable);

            System.arraycopy(outputBuffer, bufferPos, b, off, bytesToRead);
            bufferPos += bytesToRead;
            off += bytesToRead;
            len -= bytesToRead;
            bytesReadTotal += bytesToRead;
        }

        return bytesReadTotal;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized void mark(int readlimit) {
        // Marking not supported
    }

    @Override
    public void close() throws IOException {
        super.close();
        basePcm16Stream.close();
    }
}
