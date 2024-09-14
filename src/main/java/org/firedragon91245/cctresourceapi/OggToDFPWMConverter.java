package org.firedragon91245.cctresourceapi;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

public class OggToDFPWMConverter {
    public static byte[] convertOggToDFPWM(InputStream oggInputStream) throws Exception {
        // Get AudioInputStream from OGG InputStream
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(oggInputStream);

        // Convert to PCM_SIGNED if necessary
        AudioFormat baseFormat = audioInputStream.getFormat();
        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false);

        AudioInputStream pcmInputStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);

        // Convert sample rate and channels
        AudioInputStream convertedInputStream = convertSampleRateAndChannels(pcmInputStream);

        // Read PCM data
        byte[] pcmData = readAllBytesFromAudioInputStream(convertedInputStream);

        // Encode PCM data to DFPWM
        byte[] dfpwmData = new DFPWMEncoder().encode(pcmData);

        return dfpwmData;
    }

    private static AudioInputStream convertSampleRateAndChannels(AudioInputStream audioInputStream) {
        // Convert to 8000 Hz mono
        AudioFormat originalFormat = audioInputStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
                8000.0f,
                16,
                1,
                true,
                false);

        return AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
    }

    private static byte[] readAllBytesFromAudioInputStream(AudioInputStream audioInputStream) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = audioInputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        return out.toByteArray();
    }
}
