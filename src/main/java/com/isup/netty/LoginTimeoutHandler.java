package com.isup.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LoginTimeoutHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoginTimeoutHandler.class);

    public static final AttributeKey<Boolean> LOGIN_RECEIVED = AttributeKey.valueOf("LOGIN_RECEIVED");

    private static final long LOGIN_TIMEOUT_SECONDS = 10;

    private volatile ScheduledFuture<?> timeoutTask;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        timeoutTask = ctx.executor().schedule(() -> {
            Boolean loginReceived = ctx.channel().attr(LOGIN_RECEIVED).get();
            if (loginReceived == null || !loginReceived) {
                log.info("Login timeout: no LOGIN_REQUEST within {}s from {}, closing channel",
                        LOGIN_TIMEOUT_SECONDS, ctx.channel().remoteAddress());
                ctx.close();
            }
        }, LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cancelTimeout();
        super.channelInactive(ctx);
    }

    private void cancelTimeout() {
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(false);
        }
    }
}
