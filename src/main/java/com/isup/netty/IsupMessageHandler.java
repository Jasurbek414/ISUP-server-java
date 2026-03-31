package com.isup.netty;

import com.isup.protocol.IsupPacket;
import com.isup.protocol.MessageType;
import static com.isup.protocol.MessageType.*;
import com.isup.protocol.IsupProtocol;
import com.isup.api.service.DeviceService;
import com.isup.api.service.EventService;
import com.isup.event.EventParser;
import com.isup.event.AttendanceEvent;
import com.isup.session.SessionRegistry;
import com.isup.session.DeviceSession;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ChannelHandler.Sharable
public class IsupMessageHandler extends SimpleChannelInboundHandler<IsupPacket> {

    @Autowired private DeviceService deviceService;
    @Autowired private EventService eventService;
    @Autowired private SessionRegistry sessionRegistry;
    @Autowired private List<EventParser> parsers;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IsupPacket packet) throws Exception {
        MessageType type = packet.getMessageType();
        log.debug("RECV [{}]: type={} sid={}", ctx.channel().remoteAddress(), type, packet.getSessionId());

        switch (type) {
            case LOGIN_REQUEST, LOGIN_REQUEST_V5 -> handleLogin(ctx, packet);
            case KEEPALIVE_REQUEST -> handleKeepalive(ctx, packet);
            case ALARM_EVENT -> handleAlarm(ctx, packet);
            case LOGOUT -> handleLogout(ctx, packet);
            default -> log.debug("Unhandled packet type {} from {}", type, ctx.channel().remoteAddress());
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, IsupPacket packet) {
        String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        IsupProtocol.LoginRequest login = IsupProtocol.parseLoginRequest(packet.getPayload());
        
        if (login == null) return;
        String deviceId = login.deviceId();
        
        DeviceSession session = sessionRegistry.createSession(deviceId, ctx.channel(), ip);
        log.info("Device login attempt: {} (IP: {}, Sid: {}, Ver: {})", 
                deviceId, ip, session.getSessionId(), packet.getProtocolVersion());

        // For v5.0 terminals like DS-K1T343EWX, especially those without a key:
        // They often use the V1 frame (0x10) but expect a REG_RESULT XML success message.
        if (packet.getMessageType() == LOGIN_REQUEST_V5 || packet.getProtocolVersion() == IsupPacket.VERSION_V5) {
            log.info("Sending EHome 5.0 XML Success (REG_RESULT) to {}", deviceId);
            ctx.writeAndFlush(IsupProtocol.buildV5XmlSuccessFull(session.getSessionId(), deviceId, ""));
        } else {
            // Check if we have a password for this device
            String password = deviceService.getDevicePassword(deviceId);
            if (password == null || password.isEmpty()) {
                log.info("No password for {}, sending immediate XML success", deviceId);
                ctx.writeAndFlush(IsupProtocol.buildV5XmlSuccessFull(session.getSessionId(), deviceId, ""));
            } else {
                log.info("Sending Login Challenge to {}", deviceId);
                byte[] challenge = new byte[32];
                new java.util.Random().nextBytes(challenge);
                ctx.writeAndFlush(IsupProtocol.buildV1Challenge(session.getSessionId(), challenge));
            }
        }
        
        deviceService.onDeviceConnected(deviceId, ip);
    }

    private void handleKeepalive(ChannelHandlerContext ctx, IsupPacket packet) {
        DeviceSession session = sessionRegistry.getFromChannel(ctx.channel());
        if (session != null) {
            session.touch();
            ctx.writeAndFlush(IsupProtocol.buildV1KeepaliveResponse(session.getSessionId()));
        }
    }

    private void handleAlarm(ChannelHandlerContext ctx, IsupPacket packet) {
        DeviceSession session = sessionRegistry.getFromChannel(ctx.channel());
        if (session == null) return;

        String raw = new String(packet.getPayload(), StandardCharsets.UTF_8);
        log.debug("Alarm from {}: {}", session.getDeviceId(), raw);

        // Acknowledge first
        ctx.writeAndFlush(IsupProtocol.buildV1AlarmResponse(session.getSessionId()));

        // Parse and save
        for (EventParser parser : parsers) {
            if (parser.supports(raw)) {
                Optional<AttendanceEvent> event = parser.parse(session.getDeviceId(), raw);
                event.ifPresent(eventService::saveAndDispatch);
                return;
            }
        }
        log.warn("No parser found for alarm from {}: {}", session.getDeviceId(), raw.substring(0, Math.min(raw.length(), 100)));
    }

    private void handleLogout(ChannelHandlerContext ctx, IsupPacket packet) {
        DeviceSession session = sessionRegistry.getFromChannel(ctx.channel());
        if (session != null) {
            log.info("Device logout: {}", session.getDeviceId());
            deviceService.onDeviceDisconnected(session.getDeviceId());
            sessionRegistry.removeSession(ctx.channel());
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        DeviceSession session = sessionRegistry.getFromChannel(ctx.channel());
        if (session != null) {
            log.debug("TCP closed: {} (DeviceStatusService TTL will handle offline detection)", session.getDeviceId());
            sessionRegistry.removeSession(ctx.channel());
            // Do NOT call onDeviceDisconnected immediately — EHome 4.0 devices reconnect every ~3.5s.
            // DeviceStatusService checks active sessions every 15s and handles offline transition.
        }
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            DeviceSession session = sessionRegistry.getFromChannel(ctx.channel());
            if (session != null) {
                log.warn("Idle timeout: disconnecting device {}", session.getDeviceId());
                sessionRegistry.removeSession(ctx.channel());
                // Status will be updated to offline by DeviceStatusService on next 15s check
            }
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel error [{}]: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}

