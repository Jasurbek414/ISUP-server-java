package com.isup.netty;

import com.isup.session.DeviceSession;
import com.isup.session.SessionRegistry;
import com.isup.protocol.IsupPacket;
import com.isup.protocol.MessageType;
import com.isup.protocol.IsupProtocol;
import com.isup.protocol.HmacAuthenticator;
import com.isup.api.service.DeviceService;
import com.isup.security.IpBanManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Universal ISUP Handler: Multi-Success Burst for stubborn V5 terminals.
 */
@Slf4j
@Component
@Scope("prototype")
@ChannelHandler.Sharable
public class IsupMessageHandler extends SimpleChannelInboundHandler<IsupPacket> {

    @Autowired private SessionRegistry sessions;
    @Autowired private DeviceService deviceService;
    @Autowired private com.isup.api.service.EventService eventService;
    @Autowired private com.isup.event.EventParserFactory eventParserFactory;
    @Autowired private IpBanManager ipBanManager;

    private static final ConcurrentHashMap<String, byte[]> PENDING_CHALLENGES = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IsupPacket packet) throws Exception {
        MessageType type = packet.getMessageType();
        if (type == MessageType.LOGIN_REQUEST) {
            handleLogin(ctx, packet);
        } else if (type == MessageType.KEEPALIVE_REQUEST) {
            handleKeepalive(ctx, packet);
        } else if (type == MessageType.ALARM_EVENT) {
            handleAlarm(ctx, packet);
        } else if (type == MessageType.LOGOUT) {
            sessions.removeSession(ctx.channel());
            ctx.close();
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, IsupPacket packet) {
        String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        if (ipBanManager.isBanned(ip)) { ctx.close(); return; }

        int ver = packet.getProtocolVersion();
        if (ver == IsupPacket.VERSION_V1 || ver == IsupPacket.VERSION_V5) {
            IsupProtocol.LoginRequest login = IsupProtocol.parseLoginRequest(packet.getPayload());
            
            if (login == null || "unknown".equals(login.deviceId())) { 
                log.warn("Invalid/Unknown login attempt from {}", ip);
                ctx.close(); 
                return; 
            }

            String deviceId = login.deviceId();
            byte[] nonce = login.nonce();
            int payloadLen = packet.getPayload().length;

            log.info("DEBUG_HANDSHAKE: Login effort from {} (Ver={}, PathLen={})", deviceId, ver, payloadLen);

            // ONE-STEP check (Standard EHome 5.0 or self-authenticated V1)
            if (payloadLen >= 80 || (ver == IsupPacket.VERSION_V5 && payloadLen > 5)) {
                log.info("DEBUG_HANDSHAKE: One-Step Path for {}", deviceId);
                finalizeHandshake(ctx, deviceId, ip, nonce, ver);
                return;
            }

            // TWO-STEP Fallback (Challenge/Response)
            byte[] challenge = PENDING_CHALLENGES.remove(deviceId);
            if (challenge == null) {
                log.info("DEBUG_HANDSHAKE: Step 1 (Challenge) for {}", deviceId);
                byte[] serverChallenge = new byte[32];
                new java.util.Random().nextBytes(serverChallenge);
                PENDING_CHALLENGES.put(deviceId, serverChallenge);
                
                if (ver == IsupPacket.VERSION_V1) {
                    ctx.writeAndFlush(IsupProtocol.buildV1Challenge(0, serverChallenge));
                } else {
                    // Standard V5 challenge uses LOGIN_RESPONSE with status 0x02
                    // But One-Step is preferred for this integration.
                }
                return;
            }

            log.info("DEBUG_HANDSHAKE: Step 2 (Response) for {}", deviceId);
            finalizeHandshake(ctx, deviceId, ip, nonce, ver);
        }
    }

    private void finalizeHandshake(ChannelHandlerContext ctx, String deviceId, String ip, byte[] nonce, int ver) {
        final DeviceSession session = sessions.createSession(deviceId, ctx.channel(), ip);
        int sid = session.getSessionId();

        log.info("DEBUG_HANDSHAKE: Sending Response for {} (sid={}, ver={})", deviceId, sid, ver);
        
        if (ver == IsupPacket.VERSION_V5) {
            // Standard EHome 5.0 XML Success (V5 Frame)
            ctx.write(IsupProtocol.buildV5XmlSuccessV5(sid, deviceId));
            ctx.write(IsupProtocol.buildV5XmlTimeSync(sid, deviceId));
        } else {
            // Modern V5 terminals often use V1 wrapper (STX=0x10) for registration.
            // Sending ONLY the XML success (Type 0x54) as it's the standard for 5.0.
            // Note: Two responses (MiniSuccess + XML) can cause session reset.
            ctx.write(IsupProtocol.buildV5XmlSuccessFull(sid, deviceId));
            // ctx.write(IsupProtocol.buildV5XmlTimeSync(sid, deviceId));
        }
        
        ctx.flush();

        // Online Marker
        ctx.executor().schedule(() -> {
            if (ctx.channel().isActive()) {
                if (deviceService.onDeviceConnected(deviceId, ip)) {
                    log.info("V5-STABLE: Device {} is now ONLINE.", deviceId);
                } else {
                    log.warn("ID_MISMATCH: Device {} connected, but no matching device found in DB! " +
                             "Check if your dashboard 'Device ID (ISUP ID)' exactly matches '{}'", deviceId, deviceId);
                }
            }
        }, 1, TimeUnit.SECONDS);

        // Keepalive (Periodic sanity check)
        ctx.executor().scheduleAtFixedRate(() -> {
            if (ctx.channel().isActive()) {
                ctx.writeAndFlush(IsupProtocol.buildV1KeepaliveRequest(sid));
            }
        }, 15, 30, TimeUnit.SECONDS);
    }

    private void handleKeepalive(ChannelHandlerContext ctx, IsupPacket packet) {
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        if (session != null) {
            if (packet.getProtocolVersion() == IsupPacket.VERSION_V1) {
                ctx.writeAndFlush(IsupProtocol.buildV1KeepaliveResponse(session.getSessionId()));
            } else {
                ctx.writeAndFlush(IsupProtocol.buildKeepaliveResponse(session.getSessionId(), packet.getSequenceNo()));
            }
        }
    }

    private void handleAlarm(ChannelHandlerContext ctx, IsupPacket packet) {
        byte[] payload = packet.getPayload();
        log.info("ALARM Received: session={} len={}", packet.getSessionId(), payload.length);
        
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        String deviceId = session != null ? session.getDeviceId() : "unknown";
        int sid = session != null ? session.getSessionId() : packet.getSessionId();
        
        // 1. Process Event
        if (payload != null && payload.length > 0) {
            try {
                String rawXml = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
                eventParserFactory.parse(deviceId, rawXml).ifPresent(event -> {
                    log.info("ALARM Parsed: type={} employee={}", event.getEventType(), event.getEmployeeNo());
                    eventService.saveAndDispatch(event);
                });
            } catch (Exception e) {
                log.warn("Failed to parse alarm payload: {}", e.getMessage());
            }
        }

        // 2. Acknowledge
        if (packet.getProtocolVersion() == IsupPacket.VERSION_V1) {
            ctx.writeAndFlush(IsupProtocol.buildV1AlarmResponse(sid));
        } else {
            ctx.writeAndFlush(IsupProtocol.buildAlarmResponse(sid, packet.getSequenceNo()));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        if (session != null) {
            log.info("OFFLINE: {} disconnected.", session.getDeviceId());
            sessions.removeSession(ctx.channel());
            deviceService.onDeviceDisconnected(session.getDeviceId());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
