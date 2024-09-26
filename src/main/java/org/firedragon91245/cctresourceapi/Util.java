package org.firedragon91245.cctresourceapi;

import java.util.List;

public class Util {
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static <T> T safeGetIndex(T[] array, int index) {
        if (index < 0 || index >= array.length)
            return null;
        return array[index];
    }

    public static <T> T safeGetIndex(List<T> list, int index) {
        if (index < 0 || index >= list.size())
            return null;
        return list.get(index);
    }

    public static int countChar(String str, char c) {
        if (str == null)
            return 0;
        return (int) str.chars().filter(ch -> ch == c).count();
    }

    public static Integer objectToInt(Object count) {
        if (count instanceof Float)
            return ((Float) count).intValue();

        if (count instanceof Double)
            return ((Double) count).intValue();

        if (count instanceof Integer)
            return (Integer) count;

        if (count instanceof Long)
            return ((Long) count).intValue();

        if (count instanceof String) {
            try {
                return Integer.parseInt((String) count);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
