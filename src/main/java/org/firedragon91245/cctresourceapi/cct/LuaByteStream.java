package org.firedragon91245.cctresourceapi.cct;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class LuaByteStream {
    private InputStream baseInStream;

    public LuaByteStream(InputStream baseStream) {
        this.baseInStream = baseStream;
    }

    protected LuaByteStream() {
        this.baseInStream = null;
    }

    protected void setBaseInStream(InputStream baseInStream) {
        this.baseInStream = baseInStream;
    }

    @LuaFunction
    final public Double read() throws LuaException {
        try {
            int result = baseInStream.read();
            if(result == -1) {
                return null;
            }
            return (double) result;
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }

    @LuaFunction
    final public MethodResult read(int count) throws LuaException {
        if (count < 0) {
            return MethodResult.of(0, null);
        }

        byte[] buffer = new byte[count];
        try {
            int bytesRead = baseInStream.read(buffer);
            if (bytesRead == -1) {
                return MethodResult.of(0, null);
            }

            Map<Integer, Byte> result = new HashMap<>();
            for (int i = 0; i < bytesRead; i++) {
                result.put(i + 1, buffer[i]);
            }

            return MethodResult.of(bytesRead, result);
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }

    @LuaFunction
    final public void close() throws LuaException {
        closeImpl();
    }

    void closeImpl() throws LuaException {
        try {
            baseInStream.close();
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }
}
