package org.firedragon91245.cctresourceapi;

import java.awt.*;

public class ColorUtil {
    // Constants for the XYZ color space that are used for D65 white point
    private static final double REF_X = 95.047;
    private static final double REF_Y = 100.0;
    private static final double REF_Z = 108.883;

    public static double rgbDistance(Color a, Color b) {
        double[] labA = rgbToLab(a);
        double[] labB = rgbToLab(b);
        return calculateLabDistance(labA, labB);
    }

    private static double[] rgbToLab(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        // Convert RGB to XYZ
        double[] xyz = rgbToXYZ(r, g, b);

        // Convert XYZ to Lab
        return xyzToLab(xyz[0], xyz[1], xyz[2]);
    }

    private static double[] rgbToXYZ(int r, int g, int b) {
        // Normalize RGB values to the range 0-1
        double R = r / 255.0;
        double G = g / 255.0;
        double B = b / 255.0;

        // Apply gamma correction
        R = (R > 0.04045) ? Math.pow((R + 0.055) / 1.055, 2.4) : R / 12.92;
        G = (G > 0.04045) ? Math.pow((G + 0.055) / 1.055, 2.4) : G / 12.92;
        B = (B > 0.04045) ? Math.pow((B + 0.055) / 1.055, 2.4) : B / 12.92;

        // Convert to XYZ
        double X = (R * 0.4124 + G * 0.3576 + B * 0.1805) * 100;
        double Y = (R * 0.2126 + G * 0.7152 + B * 0.0722) * 100;
        double Z = (R * 0.0193 + G * 0.1192 + B * 0.9505) * 100;

        return new double[]{X, Y, Z};
    }

    private static double[] xyzToLab(double X, double Y, double Z) {
        X /= REF_X;
        Y /= REF_Y;
        Z /= REF_Z;

        X = (X > 0.008856) ? Math.pow(X, 1.0 / 3.0) : (7.787 * X) + (16.0 / 116.0);
        Y = (Y > 0.008856) ? Math.pow(Y, 1.0 / 3.0) : (7.787 * Y) + (16.0 / 116.0);
        Z = (Z > 0.008856) ? Math.pow(Z, 1.0 / 3.0) : (7.787 * Z) + (16.0 / 116.0);

        double L = (116 * Y) - 16;
        double a = 500 * (X - Y);
        double b = 200 * (Y - Z);

        return new double[]{L, a, b};
    }

    private static double calculateLabDistance(double[] lab1, double[] lab2) {
        return Math.sqrt(Math.pow(lab1[0] - lab2[0], 2) +
                Math.pow(lab1[1] - lab2[1], 2) +
                Math.pow(lab1[2] - lab2[2], 2));
    }
}
