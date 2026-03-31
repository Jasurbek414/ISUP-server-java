package com.isup.netty;

import com.isup.session.DeviceSession;
import com.isup.session.SessionRegistry;
import com.isup.protocol.IsupPacket;
import com.isup.protocol.MessageType;
import com.isup.protocol.IsupProtocol;
import com.isup.protocol.HmacAuthenticator;
import com.isup.api.service.DeviceService;
import com.isup.event.AttendanceEvent;
import com.isup.entity.EventLog;
import com.isup.security.IpBanManager;
import com.isup.netty.LoginTimeoutHandler;
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
 * Universal ISUP Handler: Stabilized for DS-K1T343EWX and V5.0 devices.
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
        if (type == MessageType.LOGIN_REQUEST || type == MessageType.LOGIN_REQUEST_V5) {
            ctx.channel().attr(LoginTimeoutHandler.LOGIN_RECEIVED).set(true);
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
        if (ver == IsupPacket.VERSION_V1 || ver == IsupPacket.VERSION_V4 || ver == IsupPacket.VERSION_V5) {
            IsupProtocol.LoginRequest login = IsupProtocol.parseLoginRequest(packet.getPayload());
            
            if (login == null || "unknown".equals(login.deviceId())) { 
                log.warn("Invalid/Unknown login attempt from {}", ip);
                ctx.close(); 
                return; 
            }

            String deviceId = login.deviceId();
            byte[] nonce = login.nonce();
            int payloadLen = packet.getPayload().length;

            // ONE-STEP check: EHome 4.0/5.0 XML or Modern V5
            if (payloadLen >= 80 || ver == IsupPacket.VERSION_V4 || ver == IsupPacket.VERSION_V5) {
                finalizeHandshake(ctx, deviceId, ip, nonce, ver);
                return;
            }

            // TWO-STEP Challenge/Response
            byte[] challenge = PENDING_CHALLENGES.remove(deviceId);
            if (challenge == null) {
                byte[] serverChallenge = new byte[32];
                new java.util.Random().nextBytes(serverChallenge);
                PENDING_CHALLENGES.put(deviceId, serverChallenge);
                
                if (ver == IsupPacket.VERSION_V1) {
                    ctx.writeAndFlush(IsupProtocol.buildV1Challenge(0, serverChallenge));
                } else {
                    int tempSid = 1000 + new java.util.Random().nextInt(9000);
                    byte[] challengePayload = new byte[1 + 4 + 32];
                    challengePayload[0] = 0x01; 
                    challengePayload[1] = (byte)(tempSid);
                    challengePayload[2] = (byte)(tempSid >> 8);
                    challengePayload[3] = (byte)(tempSid >> 16);
                    challengePayload[4] = (byte)(tempSid >> 24);
                    System.arraycopy(serverChallenge, 0, challengePayload, 5, 32);
                    ctx.writeAndFlush(IsupProtocol.encode(MessageType.LOGIN_RESPONSE, tempSid, packet.getSequenceNo(), challengePayload));
                }
                return;
            }
            finalizeHandshake(ctx, deviceId, ip, nonce, ver);
        }
    }

    private void finalizeHandshake(ChannelHandlerContext ctx, String deviceId, String ip, byte[] nonce, int ver) {
        final DeviceSession session = sessions.createSession(deviceId, ctx.channel(), ip);
        final int sid = session.getSessionId();
        final int activeVer = ver;
        final String password = deviceService.getDevicePassword(deviceId);
        final int handshakeSid = 10001; 
        
        if (activeVer == IsupPacket.VERSION_V5) {
            ctx.write(IsupProtocol.buildV5XmlSuccessV5(handshakeSid, deviceId, password));
        } else {
            ctx.write(IsupProtocol.buildV5XmlSuccessFull(handshakeSid, deviceId, password));
            ctx.write(IsupProtocol.buildV5XmlTimeSync(handshakeSid, deviceId));
        }
        ctx.flush();

        deviceService.updateDeviceIp(deviceId, ip);
        if (deviceService.onDeviceConnected(deviceId, ip)) {
            log.info("ONLINE: {} registered (sid={}, ip={})", deviceId, handshakeSid, ip);
        }

        ctx.executor().scheduleAtFixedRate(() -> {
            if (ctx.channel().isActive()) {
                if (activeVer == IsupPacket.VERSION_V5) {
                    ctx.writeAndFlush(IsupProtocol.encode(MessageType.KEEPALIVE_REQUEST, sid, 0, null));
                } else {
                    ctx.writeAndFlush(IsupProtocol.buildV1KeepaliveRequest(sid));
                }
            }
        }, 15, 30, TimeUnit.SECONDS);
    }

    private void handleKeepalive(ChannelHandlerContext ctx, IsupPacket packet) {
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        if (session != null) {
            int ver = packet.getProtocolVersion();
            if (ver == IsupPacket.VERSION_V5) {
                ctx.writeAndFlush(IsupProtocol.buildKeepaliveResponse(session.getSessionId(), packet.getSequenceNo()));
            } else {
                ctx.writeAndFlush(IsupProtocol.buildV1KeepaliveResponse(session.getSessionId()));
            }
        }
    }

    private void handleAlarm(ChannelHandlerContext ctx, IsupPacket packet) {
        byte[] payload = packet.getPayload();
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        String deviceId = session != null ? session.getDeviceId() : "unknown";
        int sid = session != null ? session.getSessionId() : packet.getSessionId();
        
        if (payload != null && payload.length > 0) {
            try {
                String rawXml = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
                eventParserFactory.parse(deviceId, rawXml).ifPresent(event -> {
                    eventService.saveAndDispatch(event);
                });
            } catch (Exception e) {
                log.warn("Alarm parse failed: {}", e.getMessage());
            }
        }

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
            sessions.removeSession(ctx.channel());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
