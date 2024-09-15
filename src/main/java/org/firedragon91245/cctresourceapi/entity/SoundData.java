package org.firedragon91245.cctresourceapi.entity;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SoundData {

    private float sampleRate;
    private int channels;
    private int sampleSizeInBits;
    private boolean isBigEndian;
    private String encoding;
    private long frameLength;
    private float frameRate;
    private double durationInSeconds;
    private String mimeType;
    private byte[] byteData;

    public Map<String, Object> asHashMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("sampleRate", sampleRate);
        result.put("channels", channels);
        result.put("sampleSizeInBits", sampleSizeInBits);
        result.put("isBigEndian", isBigEndian);
        result.put("encoding", encoding);
        result.put("frameLength", frameLength);
        result.put("frameRate", frameRate);
        result.put("durationInSeconds", durationInSeconds);
        result.put("mimeType", mimeType);
        AtomicInteger counter = new AtomicInteger(1);
        result.put("byteData", Arrays.stream(boxBytes(byteData)).collect(Collectors.toMap(b -> counter.getAndIncrement(), b -> b)));
        return result;
    }

    private Byte[] boxBytes(byte[] byteData) {
        Byte[] boxedBytes = new Byte[byteData.length];
        for (int i = 0; i < byteData.length; i++) {
            boxedBytes[i] = byteData[i];
        }
        return boxedBytes;
    }

    public void loadProperties(AudioFormat format, AudioInputStream audioInputStream) {
        this.sampleRate = format.getSampleRate();
        this.channels = format.getChannels();
        this.sampleSizeInBits = format.getSampleSizeInBits();
        this.isBigEndian = format.isBigEndian();
        this.encoding = format.getEncoding().toString();

        this.frameLength = audioInputStream.getFrameLength();
        this.frameRate = format.getFrameRate();

        // Calculate duration in seconds
        this.durationInSeconds = frameLength / frameRate;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setData(byte[] byteArray) {
        this.byteData = byteArray;
    }
}
