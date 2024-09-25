package org.firedragon91245.cctresourceapi.cct;

import javax.sound.sampled.spi.FormatConversionProvider;

public abstract class CustomizedFormatConversionProvider extends FormatConversionProvider {

    protected CustomizedAudioSystem system;

    public void setCustomizedAudiosSystem(CustomizedAudioSystem system) {
        this.system = system;
    }

}
