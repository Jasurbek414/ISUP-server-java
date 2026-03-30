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
        // LOGIN_REQUEST = V1/V5 binary login (0x01)
        // LOGIN_REQUEST_V5 = EHome 4.0/5.0 XML REG command (0x53)
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

            log.info("DEBUG_HANDSHAKE: Login effort from {} (Ver={}, PathLen={})", deviceId, ver, payloadLen);

            // ONE-STEP check: EHome 4.0/5.0 XML (usually >80 bytes) or V5 binary
            if (payloadLen >= 80 || ver == IsupPacket.VERSION_V4 || ver == IsupPacket.VERSION_V5) {
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
                    // Standard V5 challenge: send LOGIN_RESPONSE with status 0x01 + nonce
                    int tempSid = 1000 + new java.util.Random().nextInt(9000);
                    byte[] challengePayload = new byte[1 + 4 + 32];
                    challengePayload[0] = 0x01; // status: challenge required
                    challengePayload[1] = (byte)(tempSid);
                    challengePayload[2] = (byte)(tempSid >> 8);
                    challengePayload[3] = (byte)(tempSid >> 16);
                    challengePayload[4] = (byte)(tempSid >> 24);
                    System.arraycopy(serverChallenge, 0, challengePayload, 5, 32);
                    ctx.writeAndFlush(IsupProtocol.encode(MessageType.LOGIN_RESPONSE, tempSid, packet.getSequenceNo(), challengePayload));
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

        String password = deviceService.getDevicePassword(deviceId);
        log.info("DEBUG_HANDSHAKE: Sending Response for {} (sid={}, ver={}, auth={})", 
                 deviceId, sid, ver, (password!=null && !password.isEmpty()));
        
        if (ver == IsupPacket.VERSION_V5) {
            // EHome 5.0 — STX=0x20 XML success frame
            ctx.write(IsupProtocol.buildV5XmlSuccessV5(sid, deviceId, password));
        } else if (ver == IsupPacket.VERSION_V4) {
            // EHome 4.0 — STX=0x10, XML REG_RESULT with MD5 signature
            ctx.write(IsupProtocol.buildV5XmlSuccessFull(sid, deviceId, password));
            // Time sync helps V4 terminals stabilize
            ctx.write(IsupProtocol.buildV5XmlTimeSync(sid, deviceId));
        } else {
            // V1 binary — STX=0x10
            // Try type=0x01 (Register ACK) with computed HMAC first (most common for V1 devices)
            // Frame: [10][28][01][00][interval:2LE][SID:4LE][HMAC:32]
            // Try all HMAC variants — device accepts whichever matches its formula
            // Primary: V1 = HMAC-SHA256(MD5(password), nonce)
            // Fallback: zeros (no-auth mode)
            byte[] hmac = new byte[32];
            try {
                if (password != null && !password.isEmpty() && nonce != null && nonce.length > 0) {
                    hmac = com.isup.protocol.HmacAuthenticator.computeV1(nonce, password);
                }
            } catch (Exception ignored) {}
            // DEBUG: Also try zeros to check if device accepts no-auth
            // hmac = new byte[32]; // uncomment to test zero HMAC
            io.netty.buffer.ByteBuf ack = io.netty.buffer.Unpooled.buffer(42);
            ack.writeByte(0x10);
            ack.writeByte(40);
            ack.writeByte(0x01);           // Type 0x01: Register ACK (mirrors login type)
            ack.writeByte(0x00);           // Status 0x00: Success
            ack.writeShortLE(60);          // Interval 60s
            ack.writeIntLE(sid);
            ack.writeBytes(hmac);
            ctx.write(ack);
        }

        ctx.flush();

        // Online Marker — call immediately (device may disconnect quickly after ACK)
        if (deviceService.onDeviceConnected(deviceId, ip)) {
            log.info("ONLINE: Device {} registered (sid={}, ip={})", deviceId, sid, ip);
        } else {
            log.warn("ID_MISMATCH: Device {} connected but not found in DB (auto-register failed?)", deviceId);
        }

        // Keepalive (Periodic sanity check) — format depends on protocol version
        ctx.executor().scheduleAtFixedRate(() -> {
            if (ctx.channel().isActive()) {
                if (ver == IsupPacket.VERSION_V5) {
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
                // V1 and V4 both use STX=0x10 keepalive response
                ctx.writeAndFlush(IsupProtocol.buildV1KeepaliveResponse(session.getSessionId()));
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
            log.debug("TCP disconnected: {} (TTL monitor will handle offline status)", session.getDeviceId());
            sessions.removeSession(ctx.channel());
            // Do NOT call onDeviceDisconnected here — EHome 4.0 devices use one-shot
            // heartbeat TCP (connect every ~3.5s, immediately disconnect after ACK).
            // DeviceStatusService TTL (90s) handles offline detection instead.
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
