package com.spotify.heroic;

import java.util.List;
import java.util.function.Consumer;

import com.spotify.heroic.shell.protocol.Message;
import com.spotify.heroic.shell.protocol.Message_Serializer;

import eu.toolchain.serializer.SerialReader;
import eu.toolchain.serializer.SerialWriter;
import eu.toolchain.serializer.Serializer;
import eu.toolchain.serializer.SerializerFramework;
import eu.toolchain.serializer.TinySerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShellProtocolInitializer extends ChannelInitializer<Channel> {
    private static final SerializerFramework framework = TinySerializer.builder().build();
    private static final Serializer<Message> serializer = new Message_Serializer(framework);

    private final Consumer<Message> consumer;

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(0xffffff, 0, 4, 0, 4));
        ch.pipeline().addLast(new ByteToMessageDecoder() {
            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
                    throws Exception {
                try (final SerialReader reader = new ByteBufSerialReader(in)) {
                    out.add(serializer.deserialize(reader));
                }
            }
        });
        ch.pipeline().addLast(new SimpleChannelInboundHandler<Message>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Message in) throws Exception {
                consumer.accept(in);
            }
        });

        ch.pipeline().addLast(new LengthFieldPrepender(4));
        ch.pipeline().addLast(new MessageToByteEncoder<Message>() {
            @Override
            protected void encode(ChannelHandlerContext ctx, Message in, ByteBuf out) throws Exception {
                try (final SerialWriter writer = new ByteBufSerialWriter(out)) {
                    serializer.serialize(writer, in);
                }
            }
        });
    }
}
