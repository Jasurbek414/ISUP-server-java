package com.isup.netty;

import com.isup.protocol.IsupPacket;
import com.isup.protocol.MessageType;
import com.isup.protocol.IsupProtocol;
import com.isup.api.service.DeviceService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ChannelHandler.Sharable
public class IsupMessageHandler extends SimpleChannelInboundHandler<IsupPacket> {

    @Autowired private DeviceService deviceService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IsupPacket packet) throws Exception {
        MessageType type = packet.getMessageType();
        if (type == MessageType.LOGIN_REQUEST || type == MessageType.LOGIN_REQUEST_V5) {
            handleLogin(ctx, packet);
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, IsupPacket packet) {
        String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        IsupProtocol.LoginRequest login = IsupProtocol.parseLoginRequest(packet.getPayload());
        if (login != null) {
            String deviceId = login.deviceId();
            log.info("CHALLENGE_MODE: {} using 2-step challenge for security", deviceId);
            
            // 1. Generate a random challenge (32 bytes)
            byte[] serverChallenge = new byte[32];
            new java.util.Random().nextBytes(serverChallenge);
            
            // 2. Wrap it into a Challenge Response (Status 0x01)
            int tempSid = 1234;
            byte[] challengePayload = new byte[1 + 4 + 32];
            challengePayload[0] = 0x01; // Status: challenge required
            challengePayload[1] = (byte)(tempSid);
            challengePayload[2] = (byte)(tempSid >> 8);
            challengePayload[3] = (byte)(tempSid >> 16);
            challengePayload[4] = (byte)(tempSid >> 24);
            System.arraycopy(serverChallenge, 0, challengePayload, 5, 32);
            
            ctx.writeAndFlush(IsupProtocol.encode(MessageType.LOGIN_RESPONSE, tempSid, packet.getSequenceNo(), challengePayload));
            
            deviceService.onDeviceConnected(deviceId, ip);
        }
    }
}
