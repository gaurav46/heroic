package com.spotify.heroic;

import java.io.IOException;

import eu.toolchain.serializer.io.AbstractSerialWriter;
import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ByteBufSerialWriter extends AbstractSerialWriter {
    private final ByteBuf out;

    @Override
    public void write(byte b) throws IOException {
        out.writeByte(b);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        out.writeBytes(bytes, offset, length);
    }
}