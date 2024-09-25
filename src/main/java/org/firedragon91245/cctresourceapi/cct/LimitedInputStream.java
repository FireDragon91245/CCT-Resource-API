package org.firedragon91245.cctresourceapi.cct;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {
    private final long maxBytes;
    private long bytesRead;

    public LimitedInputStream(InputStream in, long maxBytes) {
        super(in);
        this.maxBytes = maxBytes;
        this.bytesRead = 0;
    }

    @Override
    public int read() throws IOException {
        if (bytesRead >= maxBytes) {
            return -1; // EOF
        }
        int result = super.read();
        if (result != -1) {
            bytesRead++;
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bytesRead >= maxBytes) {
            return -1; // EOF
        }
        // Ensure we don't read more than maxBytes
        long bytesRemaining = maxBytes - bytesRead;
        int bytesToRead = (int) Math.min(len, bytesRemaining);
        int result = super.read(b, off, bytesToRead);
        if (result != -1) {
            bytesRead += result;
        }
        return result;
    }
}
