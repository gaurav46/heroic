package com.spotify.heroic;

import com.spotify.heroic.shell.protocol.Message;

import eu.toolchain.async.AsyncFuture;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShellProtocolClient {
    private final ShellProtocolFactory factory;
    private final Channel channel;

    public AsyncFuture<Channel> send(final Message message) {
        return factory.bind(channel.write(message));
    }
}