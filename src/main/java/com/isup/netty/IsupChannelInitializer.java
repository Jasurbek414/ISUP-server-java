package com.isup.netty;

import com.isup.security.IpBanManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class IsupChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger log = LoggerFactory.getLogger(IsupChannelInitializer.class);
    private static final int READER_IDLE_SECONDS = 120; // disconnect if no data for 2 min

    private final IsupMessageHandler messageHandler;
    private final IsupPacketEncoder  encoder;
    private final IpBanManager       ipBanManager;

    public IsupChannelInitializer(IsupMessageHandler messageHandler,
                                  IsupPacketEncoder encoder,
                                  IpBanManager ipBanManager) {
        this.messageHandler = messageHandler;
        this.encoder        = encoder;
        this.ipBanManager   = ipBanManager;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        // IP ban check – first handler, closes immediately if banned
        p.addLast("banCheck", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
                if (ipBanManager.isBanned(ip)) {
                    log.info("Rejecting banned IP: {}", ip);
                    ctx.close();
                    return;
                }
                super.channelActive(ctx);
            }
        });

        // Login timeout – close if no LOGIN_REQUEST within 10 seconds
        p.addLast("loginTimeout", new LoginTimeoutHandler());

        // Raw debug handler – logs first packet bytes so we can diagnose device protocol issues
        p.addLast("rawDebug", new RawDebugHandler());

        p.addLast("idle",    new IdleStateHandler(READER_IDLE_SECONDS, 0, 0, TimeUnit.SECONDS));
        p.addLast("framer",  new IsupFrameDecoder());
        p.addLast("encoder", encoder);

        // Outbound debug: logs every write BEFORE encoding so we see raw bytes sent to device
        p.addLast("outDebug", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof ByteBuf buf) {
                    int len = buf.readableBytes();
                    int dumpLen = Math.min(len, 128);
                    byte[] bytes = new byte[dumpLen];
                    buf.getBytes(buf.readerIndex(), bytes);
                    StringBuilder hex = new StringBuilder();
                    for (byte b : bytes) hex.append(String.format("%02X ", b));
                    log.info("SEND[{}] {} bytes: {}", ctx.channel().remoteAddress(), len, hex.toString().trim());
                }
                super.write(ctx, msg, promise);
            }
        });

        p.addLast("handler", messageHandler);
    }
}
