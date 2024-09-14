package org.firedragon91245.cctresourceapi;

public class DFPWMEncoder {
    private int lastLevel = 0;
    private int step = 0;

    public byte[] encode(byte[] pcmData) {
        int len = pcmData.length / 2; // 16-bit samples
        byte[] dfpwmData = new byte[(len + 7) / 8];

        int dfpwmByteIndex = 0;
        int dfpwmBitMask = 0x80;
        byte dfpwmByte = 0;

        for (int i = 0; i < len; i++) {
            int sample = ((pcmData[i * 2 + 1] << 8) | (pcmData[i * 2] & 0xFF));

            // Normalize sample to range [-128, 127]
            int pcm8 = sample >> 8;

            // DFPWM encoding logic
            int delta = pcm8 - lastLevel;
            int direction = delta >= 0 ? 1 : -1;

            // Adjust step size
            step = Math.max(1, Math.min(255, step + direction * 8));

            // Predict next level
            lastLevel += (step * direction) >> 8;
            lastLevel = Math.max(-128, Math.min(127, lastLevel));

            // Set bit
            if (direction > 0) {
                dfpwmByte |= dfpwmBitMask;
            }

            // Shift bit mask
            dfpwmBitMask >>= 1;
            if (dfpwmBitMask == 0) {
                // Store dfpwmByte
                dfpwmData[dfpwmByteIndex++] = dfpwmByte;
                dfpwmByte = 0;
                dfpwmBitMask = 0x80;
            }
        }

        // Handle remaining bits
        if (dfpwmBitMask != 0x80) {
            dfpwmData[dfpwmByteIndex] = dfpwmByte;
        }

        return dfpwmData;
    }
}
