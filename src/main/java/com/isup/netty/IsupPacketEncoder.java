package com.isup.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Passes a pre-built ByteBuf directly to the channel.
 * IsupProtocol.encode() methods return ready ByteBuf instances.
 */
@ChannelHandler.Sharable
public class IsupPacketEncoder extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        out.writeBytes(msg);
    }
}
