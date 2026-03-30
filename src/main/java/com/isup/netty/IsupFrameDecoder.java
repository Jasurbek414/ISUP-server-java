package com.isup.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ISUP frame decoder — handles two wire formats:
 *
 * v5 format (newer firmware, STX=0x20):
 *   STX(1=0x20) + TotalLength(4 LE) + MsgType(2) + SessionID(4) + SeqNo(2) + Payload(N) + ETX(1)
 *
 * v1 format (DS-K1T343EWX and similar, first byte=0x10):
 *   TypeByte(1=0x10) + BodyLen(1) + Body(BodyLen bytes)
 *   Total frame = 2 + BodyLen bytes
 */
public class IsupFrameDecoder extends ByteToMessageDecoder {

    private static final Logger log     = LoggerFactory.getLogger(IsupFrameDecoder.class);
    private static final int    MAX_LEN = 10 * 1024 * 1024; // 10 MB

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.readableBytes() >= 2) {
            in.markReaderIndex();
            int first = in.getByte(in.readerIndex()) & 0xFF;

            if (first == 0x20) {
                // ── ISUP v5 ─────────────────────────────────────────────────
                if (in.readableBytes() < 5) {
                    in.resetReaderIndex();
                    return;
                }
                int total = in.getIntLE(in.readerIndex() + 1);
                if (total < 14 || total > MAX_LEN) {
                    log.debug("v5 frame invalid length={}, skipping byte", total);
                    in.skipBytes(1);
                    continue;
                }
                if (in.readableBytes() < total) {
                    in.resetReaderIndex();
                    return;
                }
                log.info("DECODER: Found V5 frame total={}", total);
                out.add(in.readRetainedSlice(total));

            } else if (first == 0x10) {
                // ── ISUP v1 (Standard) ─────────────────────────────────────
                if (in.readableBytes() < 2) {
                    in.resetReaderIndex();
                    return;
                }
                int bodyLen = in.getByte(in.readerIndex() + 1) & 0xFF;
                int total   = 2 + bodyLen;
                if (in.readableBytes() < total) {
                    in.resetReaderIndex();
                    return;
                }
                log.info("DECODER: Found V1-10 frame total={}", total);
                out.add(in.readRetainedSlice(total));

            } else if (first == 0x01) {
                // ── ISUP v1 (Modern Bypass/Headerless) ──────────────────────
                // Sometimes EHome 5.0 sends the Login starting with 01 01 00 directly.
                int available = in.readableBytes();
                if (available >= 80) {
                    log.info("DECODER: Found Headerless Login len={}", available);
                    out.add(in.readRetainedSlice(available));
                } else {
                    in.resetReaderIndex();
                    return;
                }

            } else if (first == 0x3C) {
                // ── ISUP v5 (Naked XML Frame) ───────────────────────────────
                // Frame starts with '<PPVSPMessage' or '<?xml'
                int available = in.readableBytes();
                log.info("DECODER: Found Naked XML frame len={}", available);
                out.add(in.readRetainedSlice(available));

            } else {
                // Unknown — skip one byte and try again
                log.info("DECODER: Unknown byte 0x{}, skipping", String.format("%02X", first));
                in.skipBytes(1);
            }
        }
    }
}
