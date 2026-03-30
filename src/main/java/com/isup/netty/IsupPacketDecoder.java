package com.isup.netty;

import com.isup.protocol.IsupPacket;
import com.isup.protocol.IsupProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decodes raw ByteBuf frames into high-level IsupPacket objects.
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class IsupPacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        try {
            IsupPacket packet = IsupProtocol.decode(msg);
            if (packet != null) {
                out.add(packet);
            }
        } catch (Exception e) {
            log.error("Failed to decode ISUP packet: {}", e.getMessage());
        }
    }
}
