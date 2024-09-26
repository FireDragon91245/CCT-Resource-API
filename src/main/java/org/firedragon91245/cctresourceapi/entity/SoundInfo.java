package org.firedragon91245.cctresourceapi.entity;

import org.firedragon91245.cctresourceapi.VariantArray;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SoundInfo {
    String subtitle;
    VariantArray<String, SoundDef> sounds;

    public String getSubtitle() {
        return subtitle;
    }

    public Map<Integer, Object> soundsAsHashMap() {
        AtomicInteger counter = new AtomicInteger(1);
        return sounds.isA() ? Stream.of(sounds.getA()).collect(Collectors.toMap(s -> counter.getAndIncrement(), s -> s)) :
                Stream.of(sounds.getB()).map(SoundDef::asHashMap).collect(Collectors.toMap(s -> counter.getAndIncrement(), s -> s));
    }

    public Stream<String> soundNamesStream() {
        if (sounds.isA()) {
            return Stream.of(sounds.getA());
        } else {
            return Stream.of(sounds.getB()).map(SoundDef::getName);
        }
    }
}
