package com.isup.netty;

import com.isup.api.service.DeviceService;
import com.isup.api.service.EventService;
import com.isup.event.AttendanceEvent;
import com.isup.event.EventParserFactory;
import com.isup.monitoring.MetricsRegistry;
import com.isup.protocol.IsupPacket;
import com.isup.protocol.IsupProtocol;
import com.isup.protocol.MessageType;
import com.isup.security.IpBanManager;
import com.isup.session.DeviceSession;
import com.isup.session.SessionRegistry;
import com.isup.webhook.WebhookDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.isup.protocol.HmacAuthenticator;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@ChannelHandler.Sharable
public class IsupMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(IsupMessageHandler.class);

    private final SessionRegistry    sessions;
    private final DeviceService      deviceService;
    private final EventParserFactory parserFactory;
    private final EventService       eventService;
    private final WebhookDispatcher  webhookDispatcher;
    private final IpBanManager       ipBanManager;
    private final MetricsRegistry    metrics;

    public IsupMessageHandler(SessionRegistry sessions,
                               DeviceService deviceService,
                               EventParserFactory parserFactory,
                               EventService eventService,
                               WebhookDispatcher webhookDispatcher,
                               IpBanManager ipBanManager,
                               MetricsRegistry metrics) {
        this.sessions          = sessions;
        this.deviceService     = deviceService;
        this.parserFactory     = parserFactory;
        this.eventService      = eventService;
        this.webhookDispatcher = webhookDispatcher;
        this.ipBanManager      = ipBanManager;
        this.metrics           = metrics;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
        IsupPacket packet = IsupProtocol.decode(buf);
        if (packet == null) return;

        switch (packet.getMessageType()) {
            case LOGIN_REQUEST     -> handleLogin(ctx, packet);
            case KEEPALIVE_REQUEST -> handleKeepalive(ctx, packet);
            case ALARM_EVENT       -> handleAlarm(ctx, packet);
            case LOGOUT            -> handleLogout(ctx, packet);
            default                -> log.debug("Unhandled msg type: {}", packet.getMessageType());
        }
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    private void handleLogin(ChannelHandlerContext ctx, IsupPacket packet) {
        IsupProtocol.LoginRequest req = IsupProtocol.parseLoginRequest(packet.getPayloadSafe());
        String deviceId = req.deviceId();
        byte[] nonce    = req.nonce();

        String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        log.info("LOGIN deviceId={} ip={} proto=v{}", deviceId, ip, packet.getProtocolVersion());

        metrics.incrementLoginAttempts();

        // Mark login received to cancel the timeout
        ctx.channel().attr(LoginTimeoutHandler.LOGIN_RECEIVED).set(Boolean.TRUE);

        String password;
        try {
            password = deviceService.getDevicePassword(deviceId);
        } catch (SecurityException e) {
            log.warn("Login rejected for deviceId={} ip={}: {}", deviceId, ip, e.getMessage());
            ipBanManager.recordFailedLogin(ip);
            ctx.close();
            return;
        }

        DeviceSession session = sessions.createSession(deviceId, ctx.channel(), ip);
        deviceService.onDeviceConnected(deviceId, ip);

        // ── v1: Try all HMAC variants and log for diagnosis ──────────────────
        if (packet.getProtocolVersion() == IsupPacket.VERSION_V1) {
            // Hypothesis A: 33-byte block = nonce[0] (1-byte challenge) + nonce[1..32] (device HMAC)
            byte[] nonce1  = new byte[]{nonce[0]};
            byte[] devHmac = Arrays.copyOfRange(nonce, 1, 33);

            log.info("V1-AUTH nonce1={} nonce33={} devHmac={} password='{}'",
                    hex(nonce1), hex(nonce), hex(devHmac), password);

            // All variants with 1-byte nonce
            Object[][] variants = {
                { "V1(HMAC-SHA256, key=MD5(pwd), nonce1)",      HmacAuthenticator.computeV1(nonce1, password) },
                { "V2(HMAC-SHA256, key=pwd,       nonce1)",      HmacAuthenticator.computeV2(nonce1, password) },
                { "V3(HMAC-SHA256, key=MD5(pwd),  nonce1+devId)",HmacAuthenticator.computeV3(nonce1, password, deviceId) },
                { "V4(SHA256,      nonce1+pwd)",                  HmacAuthenticator.computeV4(nonce1, password) },
                { "V5(HMAC-SHA1,   key=MD5(pwd),  nonce1)",      HmacAuthenticator.computeV5(nonce1, password) },
                { "V6(HMAC-SHA1,   key=pwd,       nonce1)",      HmacAuthenticator.computeV6(nonce1, password) },
                // All variants with full 33-byte nonce (Hypothesis B: all 33 bytes are challenge)
                { "V1(HMAC-SHA256, key=MD5(pwd),  nonce33)",     HmacAuthenticator.computeV1(nonce, password) },
                { "V2(HMAC-SHA256, key=pwd,       nonce33)",     HmacAuthenticator.computeV2(nonce, password) },
                { "V4(SHA256,      nonce33+pwd)",                 HmacAuthenticator.computeV4(nonce, password) },
                { "V5(HMAC-SHA1,   key=MD5(pwd),  nonce33)",     HmacAuthenticator.computeV5(nonce, password) },
                { "V6(HMAC-SHA1,   key=pwd,       nonce33)",     HmacAuthenticator.computeV6(nonce, password) },
            };
            for (Object[] v : variants) {
                byte[] computed = (byte[]) v[1];
                boolean match   = Arrays.equals(computed, devHmac);
                log.info("  {} = {} {}", v[0], hex(computed), match ? "<<MATCH!>>" : "");
            }
        }

        ByteBuf response;
        if (packet.getProtocolVersion() == IsupPacket.VERSION_V1) {
            response = IsupProtocol.buildV1LoginResponse(session.getSessionId(), nonce, password);
        } else if (packet.getProtocolVersion() == IsupPacket.VERSION_V4) {
            response = IsupProtocol.buildV4LoginResponse(session.getSessionId(), nonce, password, packet.getSequenceNo());
        } else {
            response = IsupProtocol.buildLoginResponse(session.getSessionId(), nonce, password, packet.getSequenceNo());
        }
        ctx.writeAndFlush(response);
    }

    /** Converts byte array to uppercase hex string. */
    private static String hex(byte[] b) {
        if (b == null || b.length == 0) return "";
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02X", x & 0xFF));
        return sb.toString();
    }

    // ─── Keepalive ───────────────────────────────────────────────────────────

    private void handleKeepalive(ChannelHandlerContext ctx, IsupPacket packet) {
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        if (session != null) session.touch();

        ByteBuf response;
        if (packet.getProtocolVersion() == IsupPacket.VERSION_V1) {
            response = IsupProtocol.buildV1KeepaliveResponse();
        } else if (packet.getProtocolVersion() == IsupPacket.VERSION_V4) {
            response = IsupProtocol.buildV4KeepaliveResponse(packet.getSessionId(), packet.getSequenceNo());
        } else {
            response = IsupProtocol.buildKeepaliveResponse(packet.getSessionId(), packet.getSequenceNo());
        }
        ctx.writeAndFlush(response);
    }

    // ─── Alarm / Event ───────────────────────────────────────────────────────

    private void handleAlarm(ChannelHandlerContext ctx, IsupPacket packet) {
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        String deviceId = session != null ? session.getDeviceId() : "unknown";
        if (session != null) session.touch();

        String raw = packet.getPayloadAsString();
        log.debug("ALARM from={} payload={}", deviceId, raw.length() > 200 ? raw.substring(0, 200) + "..." : raw);

        ByteBuf alarmResp;
        if (packet.getProtocolVersion() == IsupPacket.VERSION_V1) {
            alarmResp = IsupProtocol.buildV1AlarmResponse();
        } else if (packet.getProtocolVersion() == IsupPacket.VERSION_V4) {
            alarmResp = IsupProtocol.buildV4AlarmResponse(packet.getSessionId(), packet.getSequenceNo());
        } else {
            alarmResp = IsupProtocol.buildAlarmResponse(packet.getSessionId(), packet.getSequenceNo());
        }
        ctx.writeAndFlush(alarmResp);

        Optional<AttendanceEvent> eventOpt = parserFactory.parse(deviceId, raw);
        eventOpt.ifPresentOrElse(event -> {
            event = enrichWithDeviceInfo(event, session);
            eventService.saveAndDispatch(event);
        }, () -> log.warn("Could not parse alarm payload from device={}", deviceId));
    }

    private AttendanceEvent enrichWithDeviceInfo(AttendanceEvent event, DeviceSession session) {
        if (session == null) return event;
        String name  = deviceService.getDeviceName(session.getDeviceId());
        String model = deviceService.getDeviceModel(session.getDeviceId());
        return AttendanceEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .deviceId(event.getDeviceId())
                .deviceName(name)
                .deviceModel(model)
                .employeeNo(event.getEmployeeNo())
                .employeeName(event.getEmployeeName())
                .cardNo(event.getCardNo())
                .verifyMode(event.getVerifyMode())
                .direction(event.getDirection())
                .doorNo(event.getDoorNo())
                .channel(event.getChannel())
                .eventTime(event.getEventTime())
                .photoBase64(event.getPhotoBase64())
                .timestamp(event.getTimestamp())
                .rawPayload(event.getRawPayload())
                .build();
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    private void handleLogout(ChannelHandlerContext ctx, IsupPacket packet) {
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        if (session != null) {
            log.info("LOGOUT deviceId={}", session.getDeviceId());
            deviceService.onDeviceDisconnected(session.getDeviceId());
        }
        sessions.removeSession(ctx.channel());
        ctx.close();
    }

    // ─── Channel lifecycle ───────────────────────────────────────────────────

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        DeviceSession session = sessions.getFromChannel(ctx.channel());
        if (session != null) {
            log.info("DISCONNECTED deviceId={}", session.getDeviceId());
            deviceService.onDeviceDisconnected(session.getDeviceId());
            sessions.removeSession(ctx.channel());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            DeviceSession session = sessions.getFromChannel(ctx.channel());
            String id = session != null ? session.getDeviceId() : ctx.channel().remoteAddress().toString();
            log.info("IDLE timeout, closing connection: {}", id);
            if (session != null) {
                deviceService.onDeviceDisconnected(session.getDeviceId());
                sessions.removeSession(ctx.channel());
            }
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof TooLongFrameException) {
            log.debug("Oversized/invalid frame from {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        } else {
            DeviceSession session = sessions.getFromChannel(ctx.channel());
            String id = session != null ? session.getDeviceId() : ctx.channel().remoteAddress().toString();
            log.warn("Channel error [{}]: {}", id, cause.getMessage());
        }
        ctx.close();
    }
}
