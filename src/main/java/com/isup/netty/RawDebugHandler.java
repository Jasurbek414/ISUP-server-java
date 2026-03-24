package com.isup.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Temporary debug handler: logs first 256 raw bytes from each new connection.
 * Placed before the FrameDecoder to see exactly what the device sends.
 * Enabled only when log level for this class is DEBUG.
 */
public class RawDebugHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(RawDebugHandler.class);
    private static final int MAX_DUMP_BYTES = 256;

    private int readCount = 0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (log.isInfoEnabled() && msg instanceof ByteBuf buf) {
            readCount++;
            String ip = ctx.channel().remoteAddress() instanceof InetSocketAddress a
                    ? a.getAddress().getHostAddress() : "?";

            int readable = buf.readableBytes();
            int dumpLen  = Math.min(readable, MAX_DUMP_BYTES);
            byte[] bytes = new byte[dumpLen];
            buf.getBytes(buf.readerIndex(), bytes);  // getBytes = non-destructive

            // Compact hex on one line for easy reading
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02X ", b));
            log.info("RAW#{}[{}] len={} bytes: {}", readCount, ip, readable, hex.toString().trim());
        }
        super.channelRead(ctx, msg);
    }

    private String typeName(int t) {
        return switch (t) {
            case 0x0001 -> "LOGIN_REQ";
            case 0x0002 -> "LOGIN_RESP";
            case 0x0003 -> "LOGOUT";
            case 0x0004 -> "ALARM";
            case 0x0005 -> "ALARM_RESP";
            case 0x0013 -> "KEEPALIVE_REQ";
            case 0x0014 -> "KEEPALIVE_RESP";
            default     -> "UNKNOWN";
        };
    }
}
