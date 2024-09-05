package org.firedragon91245.cctresourceapi.cct;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

public class ModelTexture {
    public final String formatName;
    public final BufferedImage image;
    public final byte[] imageBytes;

    public ModelTexture(String formatName, BufferedImage image, byte[] imageBytes) {
        this.formatName = formatName;
        this.image = image;
        this.imageBytes = imageBytes;
    }

    public Byte[] boxByteArray(byte[] array)
    {
        Byte[] boxedArray = new Byte[array.length];
        for(int i = 0; i < array.length; i++)
        {
            boxedArray[i] = array[i];
        }
        return boxedArray;
    }

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        if(formatName != null)
            map.put("formatName", formatName);
        if(image != null) {
            map.put("imageWidth", image.getWidth());
            map.put("imageHeight", image.getHeight());
            Byte[] imageBytesArray = boxByteArray(imageBytes);
            HashMap<Integer, Byte> imageBytesMap = IntStream.range(0, imageBytesArray.length).boxed().collect(HashMap::new, (m, i) -> m.put(i + 1, imageBytesArray[i]), HashMap::putAll);
            map.put("imageBytes", imageBytesMap);
        }
        return map;
    }
}
