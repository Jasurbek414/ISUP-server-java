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
            log.info("CLASSIC_HANDSHAKE: Device {} connecting via Binary ACK (sid=1234)", deviceId);
            
            // 1. Send Classic Binary Register ACK (Type 0x01) — The most compatible answer
            ctx.write(IsupProtocol.buildV1RegisterResponseMini(1234, 60));
            
            // 2. Send XML TimeSync (Type 0x51) — Required for modern firmware
            ctx.write(IsupProtocol.buildV5XmlTimeSync(1234, deviceId));
            
            ctx.flush();
            
            deviceService.onDeviceConnected(deviceId, ip);
        }
    }
}
