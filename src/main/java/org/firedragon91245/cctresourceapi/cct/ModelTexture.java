package org.firedragon91245.cctresourceapi.cct;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public class ModelTexture {
    public final String formatName;
    public final BufferedImage image;
    public final byte[] imageBytes;

    public ModelTexture(String formatName, BufferedImage image, byte[] imageBytes) {
        this.formatName = formatName;
        this.image = image;
        this.imageBytes = imageBytes;
    }

    public HashMap<String, Object> asHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        if(formatName != null)
            map.put("formatName", formatName);
        if(image != null)
            map.put("imageBytes", imageBytes);
        return map;
    }
}
