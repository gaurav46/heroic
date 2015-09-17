package com.spotify.heroic;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

import com.google.inject.Inject;
import com.spotify.heroic.shell.protocol.Message;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.ResolvableFuture;
import eu.toolchain.async.Transform;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ShellProtocolFactory {
    private final AsyncFramework async;
    private final EventLoopGroup group;

    @Inject
    public ShellProtocolFactory(final AsyncFramework async, final EventLoopGroup group) {
        this.async = async;
        this.group = group;
    }

    public AsyncFuture<ShellProtocolClient> connect(final InetSocketAddress address, final Consumer<Message> consumer) {
        final Bootstrap b = new Bootstrap();

        b.channel(NioSocketChannel.class);
        b.group(group);

        b.handler(new ShellProtocolInitializer(consumer));

        return bind(b.connect(address)).transform(new Transform<Channel, ShellProtocolClient>() {
            @Override
            public ShellProtocolClient transform(Channel result) throws Exception {
                return new ShellProtocolClient(ShellProtocolFactory.this, result);
            }
        });
    }

    public AsyncFuture<ShellProtocolServer> bind(final InetSocketAddress address, final Consumer<Message> consumer) {
        final ServerBootstrap b = new ServerBootstrap();

        b.channel(NioServerSocketChannel.class);
        b.group(group);

        b.childHandler(new ShellProtocolInitializer(consumer));

        return bind(b.bind(address)).transform(new Transform<Channel, ShellProtocolServer>() {
            @Override
            public ShellProtocolServer transform(Channel result) throws Exception {
                return new ShellProtocolServer(ShellProtocolFactory.this, result);
            }
        });
    }

    public AsyncFuture<Channel> bind(final ChannelFuture channelFuture) {
        final ResolvableFuture<Channel> future = async.future();

        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (channelFuture.isSuccess()) {
                    future.resolve(channelFuture.channel());
                } else {
                    future.fail(channelFuture.cause());
                }
            }
        });

        return future;
    }
}