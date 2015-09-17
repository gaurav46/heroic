package com.spotify.heroic;

import java.io.IOException;

import eu.toolchain.serializer.io.AbstractSerialReader;
import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ByteBufSerialReader extends AbstractSerialReader {
    private final ByteBuf out;

    @Override
    public byte read() throws IOException {
        return out.readByte();
    }

    @Override
    public void read(byte[] bytes, int offset, int length) throws IOException {
        out.readBytes(bytes, offset, length);
    }

    @Override
    public void skip(int length) throws IOException {
        out.skipBytes(length);
    }
}