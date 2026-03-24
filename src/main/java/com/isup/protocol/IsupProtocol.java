package com.isup.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * ISUP packet parser and builder.
 *
 * v5 frame layout (STX=0x20):
 *   STX(1) | Length(4 LE) | MsgType(2 LE) | SessionID(4 LE) | SeqNo(2 LE) | Payload(N) | ETX(1)
 *   Total header = 14 bytes (including STX + ETX)
 *   Length field = total frame length (all 14 bytes + payload)
 *
 * v1 frame layout (DS-K1T343EWX and similar, STX=0x10):
 *   FrameType(1=0x10) | BodyLen(1) | Body(BodyLen)
 *   Body[0] = message type (0x01=LOGIN, 0x13=KEEPALIVE, etc.)
 *   Body structure for LOGIN: [01 01 00][serialTLV][modelTLV][29 25][idTLV][33-nonce][serialTLV]
 */
public class IsupProtocol {

    private static final Logger log = LoggerFactory.getLogger(IsupProtocol.class);

    public static final int HEADER_SIZE = 14; // STX + len(4) + type(2) + sess(4) + seq(2) + ETX
    public static final int NONCE_SIZE   = 33; // device sends 33-byte random challenge
    public static final int HMAC_SIZE    = 32;

    // ─── Parser ──────────────────────────────────────────────────────────────

    public static IsupPacket decode(ByteBuf buf) {
        if (buf.readableBytes() < 2) return null;

        buf.markReaderIndex();
        int first = buf.getByte(buf.readerIndex()) & 0xFF;

        if (first == 0x10) {
            return decodeV1(buf);
        } else if (first == 0x20) {
            return decodeV5(buf);
        } else if (first == 0x21) {
            return decodeV4(buf);
        } else {
            log.info("DECODE: unknown frame type 0x{}, readable={}",
                    String.format("%02X", first), buf.readableBytes());
            return null;
        }
    }

    /**
     * Parse ISUP v4 frame (STX=0x21). Same structure as v5 but different STX byte.
     * Frame: [0x21][Length 4 LE][MsgType 2 LE][SessionID 4 LE][SeqNo 2 LE][Payload N][ETX]
     */
    private static IsupPacket decodeV4(ByteBuf buf) {
        if (buf.readableBytes() < HEADER_SIZE) return null;

        buf.markReaderIndex();
        buf.readByte(); // consume STX=0x21

        int length    = buf.readIntLE();
        int typeCode  = buf.readShortLE() & 0xFFFF;
        int sessionId = buf.readIntLE();
        int seqNo     = buf.readShortLE() & 0xFFFF;

        int payloadLen = length - HEADER_SIZE;
        if (payloadLen < 0 || buf.readableBytes() < payloadLen + 1) {
            buf.resetReaderIndex();
            return null;
        }

        byte[] payload = new byte[Math.max(0, payloadLen)];
        if (payloadLen > 0) buf.readBytes(payload);

        buf.readByte(); // ETX

        return IsupPacket.builder()
                .messageType(MessageType.fromCode(typeCode))
                .sessionId(sessionId)
                .sequenceNo(seqNo)
                .payload(payload)
                .protocolVersion(IsupPacket.VERSION_V4)
                .build();
    }

    /** Parse ISUP v5 frame (STX=0x20). */
    private static IsupPacket decodeV5(ByteBuf buf) {
        if (buf.readableBytes() < HEADER_SIZE) return null;

        buf.markReaderIndex();
        buf.readByte(); // consume STX=0x20

        int length    = buf.readIntLE();
        int typeCode  = buf.readShortLE() & 0xFFFF;
        int sessionId = buf.readIntLE();
        int seqNo     = buf.readShortLE() & 0xFFFF;

        int payloadLen = length - HEADER_SIZE;
        if (payloadLen < 0 || buf.readableBytes() < payloadLen + 1) {
            buf.resetReaderIndex();
            return null;
        }

        byte[] payload = new byte[Math.max(0, payloadLen)];
        if (payloadLen > 0) buf.readBytes(payload);

        buf.readByte(); // ETX (tolerate if missing)

        return IsupPacket.builder()
                .messageType(MessageType.fromCode(typeCode))
                .sessionId(sessionId)
                .sequenceNo(seqNo)
                .payload(payload)
                .protocolVersion(IsupPacket.VERSION_V5)
                .build();
    }

    /**
     * Parse ISUP v1 frame (DS-K1T343EWX style, first byte=0x10).
     * Frame: [0x10][bodyLen][body...]
     * Body[0] = msg type: 0x01=LOGIN_REQUEST, 0x13=KEEPALIVE_REQUEST, 0x03=LOGOUT
     * Login body: [01 01 00][serialTLV][modelTLV][29 25][idLenByte][deviceId][33-nonce][serialTLV]
     */
    private static IsupPacket decodeV1(ByteBuf buf) {
        buf.markReaderIndex();
        buf.readByte(); // consume 0x10
        int bodyLen = buf.readByte() & 0xFF;

        if (buf.readableBytes() < bodyLen) {
            buf.resetReaderIndex();
            return null;
        }

        byte[] body = new byte[bodyLen];
        buf.readBytes(body);

        if (bodyLen < 1) return null;
        int msgTypeByte = body[0] & 0xFF;

        MessageType msgType = switch (msgTypeByte) {
            case 0x01 -> MessageType.LOGIN_REQUEST;
            case 0x02 -> MessageType.LOGIN_RESPONSE;
            case 0x03 -> MessageType.LOGOUT;
            case 0x13 -> MessageType.KEEPALIVE_REQUEST;
            case 0x14 -> MessageType.KEEPALIVE_RESPONSE;
            default   -> MessageType.UNKNOWN;
        };

        byte[] payload = body;
        if (msgType == MessageType.LOGIN_REQUEST) {
            byte[] extracted = extractV1LoginPayload(body);
            if (extracted != null) payload = extracted;
        }

        log.debug("V1 decoded: type={} bodyLen={}", msgType, bodyLen);

        return IsupPacket.builder()
                .messageType(msgType)
                .sessionId(0)
                .sequenceNo(0)
                .payload(payload)
                .protocolVersion(IsupPacket.VERSION_V1)
                .build();
    }

    /**
     * Extract deviceId + nonce from v1 LOGIN_REQUEST body and build
     * a v5-compatible payload: deviceId (null-padded to 64 bytes) + nonce (33 bytes).
     *
     * Observed v1 body structure for DS-K1T343EWX:
     *   [01 01 00]           version (3 bytes)
     *   [09][K02434743]      serial TLV (1+9 = 10 bytes)
     *   [0C][DS-K1T343EWX]  model TLV (1+12 = 13 bytes)
     *   [29][25]             compound header (2 bytes)
     *   [05][admin]          deviceID TLV (1+5 = 6 bytes)
     *   [33 bytes]           nonce
     *   [09][K02434743]      trailing serial (10 bytes)
     */
    private static byte[] extractV1LoginPayload(byte[] body) {
        try {
            int pos = 3; // skip version header [01 01 00]

            // Skip serial TLV: [len][serial]
            if (pos >= body.length) return null;
            int serialLen = body[pos++] & 0xFF;
            pos += serialLen;

            // Skip model TLV: [len][model]
            if (pos >= body.length) return null;
            int modelLen = body[pos++] & 0xFF;
            pos += modelLen;

            // Skip 2 compound-header bytes (observed as 0x29 0x25 on DS-K1T343EWX)
            if (pos + 2 > body.length) return null;
            pos += 2;

            // Read device ID TLV: [len][deviceId]
            if (pos >= body.length) return null;
            int idLen = body[pos++] & 0xFF;
            if (idLen <= 0 || pos + idLen > body.length) return null;
            byte[] deviceIdBytes = Arrays.copyOfRange(body, pos, pos + idLen);
            pos += idLen;

            // Read 33-byte nonce
            if (pos + NONCE_SIZE > body.length) return null;
            byte[] nonce = Arrays.copyOfRange(body, pos, pos + NONCE_SIZE);

            // Build v5-compatible payload: deviceId (null-padded to 64) + nonce (33)
            byte[] deviceIdPadded = new byte[64];
            System.arraycopy(deviceIdBytes, 0, deviceIdPadded, 0, Math.min(idLen, 63));

            byte[] payload = new byte[64 + NONCE_SIZE];
            System.arraycopy(deviceIdPadded, 0, payload, 0, 64);
            System.arraycopy(nonce, 0, payload, 64, NONCE_SIZE);

            log.info("V1 login parsed: deviceId={} nonce[0]=0x{}",
                    new String(deviceIdBytes, StandardCharsets.UTF_8).trim(),
                    String.format("%02X", nonce[0] & 0xFF));
            return payload;

        } catch (Exception e) {
            log.warn("V1 login payload parse failed: {}", e.getMessage());
            return null;
        }
    }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static ByteBuf encode(MessageType type, int sessionId, int seqNo, byte[] payload) {
        byte[] p = payload == null ? new byte[0] : payload;
        int frameLen = HEADER_SIZE + p.length;

        ByteBuf buf = Unpooled.buffer(frameLen);
        buf.writeByte(IsupPacket.STX);
        buf.writeIntLE(frameLen);
        buf.writeShortLE(type.code);
        buf.writeIntLE(sessionId);
        buf.writeShortLE(seqNo);
        if (p.length > 0) buf.writeBytes(p);
        buf.writeByte(IsupPacket.ETX);
        return buf;
    }

    // ─── Login response ──────────────────────────────────────────────────────

    /**
     * Builds LOGIN_RESPONSE payload: result(1) + sessionId(4 LE) + hmac(32)
     */
    public static ByteBuf buildLoginResponse(int sessionId, byte[] nonce, String password, int seqNo) {
        byte[] hmac = HmacAuthenticator.compute(nonce, password);

        ByteBuf payload = Unpooled.buffer(1 + 4 + HMAC_SIZE);
        payload.writeByte(0x00);        // result = success
        payload.writeIntLE(sessionId);
        payload.writeBytes(hmac);

        byte[] pBytes = new byte[payload.readableBytes()];
        payload.readBytes(pBytes);
        payload.release();

        return encode(MessageType.LOGIN_RESPONSE, sessionId, seqNo, pBytes);
    }

    /**
     * Builds KEEPALIVE_RESPONSE (empty payload, mirror sessionId/seqNo).
     */
    public static ByteBuf buildKeepaliveResponse(int sessionId, int seqNo) {
        return encode(MessageType.KEEPALIVE_RESPONSE, sessionId, seqNo, null);
    }

    /**
     * Builds ALARM_RESPONSE (acknowledge event).
     */
    public static ByteBuf buildAlarmResponse(int sessionId, int seqNo) {
        return encode(MessageType.ALARM_RESPONSE, sessionId, seqNo, null);
    }

    /**
     * Builds LOGOUT packet to send to a device (server-initiated logout).
     */
    public static ByteBuf buildLogoutPacket(int sessionId) {
        return encode(MessageType.LOGOUT, sessionId, 0, null);
    }

    // ─── v4 response builders ─────────────────────────────────────────────────

    /** Builds v4 LOGIN_RESPONSE (STX=0x21, same payload structure as v5). */
    public static ByteBuf buildV4LoginResponse(int sessionId, byte[] nonce, String password, int seqNo) {
        byte[] hmac = HmacAuthenticator.compute(nonce, password);

        ByteBuf payload = Unpooled.buffer(1 + 4 + HMAC_SIZE);
        payload.writeByte(0x00);
        payload.writeIntLE(sessionId);
        payload.writeBytes(hmac);

        byte[] pBytes = new byte[payload.readableBytes()];
        payload.readBytes(pBytes);
        payload.release();

        return encodeV4(MessageType.LOGIN_RESPONSE, sessionId, seqNo, pBytes);
    }

    /** Builds v4 KEEPALIVE_RESPONSE (STX=0x21). */
    public static ByteBuf buildV4KeepaliveResponse(int sessionId, int seqNo) {
        return encodeV4(MessageType.KEEPALIVE_RESPONSE, sessionId, seqNo, null);
    }

    /** Builds v4 ALARM_RESPONSE (STX=0x21). */
    public static ByteBuf buildV4AlarmResponse(int sessionId, int seqNo) {
        return encodeV4(MessageType.ALARM_RESPONSE, sessionId, seqNo, null);
    }

    /** Encode v4 frame using STX=0x21 instead of 0x20. */
    private static ByteBuf encodeV4(MessageType type, int sessionId, int seqNo, byte[] payload) {
        byte[] p = payload == null ? new byte[0] : payload;
        int frameLen = HEADER_SIZE + p.length;

        ByteBuf buf = Unpooled.buffer(frameLen);
        buf.writeByte(IsupPacket.STX_V4); // 0x21
        buf.writeIntLE(frameLen);
        buf.writeShortLE(type.code);
        buf.writeIntLE(sessionId);
        buf.writeShortLE(seqNo);
        if (p.length > 0) buf.writeBytes(p);
        buf.writeByte(IsupPacket.ETX);
        return buf;
    }

    // ─── v1 response builders ─────────────────────────────────────────────────

    /**
     * Builds v1 LOGIN_RESPONSE for DS-K1T343EWX.
     *
     * Tries both response body structures (logged for diagnosis):
     *   Structure A (with sessionId, 38-byte body):
     *     [0x10][0x26][0x02][0x00][sessionId:4LE][HMAC:32]
     *   Structure B (without sessionId, 34-byte body — previous attempt):
     *     [0x10][0x22][0x02][0x00][HMAC:32]
     *
     * Currently using Structure A (with sessionId) as primary attempt.
     * HMAC: HMAC-SHA256(key=MD5(password), data=nonce[0]) — Variant 1.
     */
    public static ByteBuf buildV1LoginResponse(int sessionId, byte[] nonce, String password) {
        byte[] hmac = new byte[32];
        if (nonce != null && nonce.length > 0) {
            byte[] nonce1 = new byte[]{nonce[0]};
            hmac = HmacAuthenticator.computeV1(nonce1, password);
        }

        ByteBuf buf = Unpooled.buffer(36);
        buf.writeByte(0x10);
        buf.writeByte(0x22);          // bodyLen = 34
        buf.writeByte(0x02);          // LOGIN_RESPONSE
        buf.writeByte(0x00);          // success
        buf.writeBytes(hmac);         // HMAC, 32 bytes
        return buf;
    }

    /**
     * Builds v1 KEEPALIVE_RESPONSE frame (server responding to device's keepalive request).
     * Frame: [0x10][0x01][0x14=KEEPALIVE_RESP]
     */
    public static ByteBuf buildV1KeepaliveResponse() {
        ByteBuf buf = Unpooled.buffer(3);
        buf.writeByte(0x10);
        buf.writeByte(1);
        buf.writeByte(0x14); // KEEPALIVE_RESPONSE
        return buf;
    }

    /**
     * Builds v1 KEEPALIVE_REQUEST frame (server initiating keepalive to device).
     * Device should respond with KEEPALIVE_RESPONSE (0x14).
     * Frame: [0x10][0x01][0x13=KEEPALIVE_REQ]
     */
    public static ByteBuf buildV1KeepaliveRequest() {
        ByteBuf buf = Unpooled.buffer(3);
        buf.writeByte(0x10);
        buf.writeByte(1);
        buf.writeByte(0x13); // KEEPALIVE_REQUEST
        return buf;
    }

    /**
     * Builds v1 ALARM_RESPONSE frame.
     * Frame: [0x10][0x01][0x05=ALARM_RESP]
     */
    public static ByteBuf buildV1AlarmResponse() {
        ByteBuf buf = Unpooled.buffer(3);
        buf.writeByte(0x10);
        buf.writeByte(1);
        buf.writeByte(0x05); // ALARM_RESPONSE
        return buf;
    }

    // ─── Login request parsing ───────────────────────────────────────────────

    /**
     * Parses LOGIN_REQUEST payload → {deviceId, nonce}
     * Payload format: deviceId (null-terminated string, up to 64 bytes) + nonce (33 bytes)
     */
    public static LoginRequest parseLoginRequest(byte[] payload) {
        if (payload == null || payload.length < NONCE_SIZE) {
            return new LoginRequest("", new byte[NONCE_SIZE]);
        }

        int nonceStart = payload.length - NONCE_SIZE;
        byte[] nonce = Arrays.copyOfRange(payload, nonceStart, payload.length);

        // deviceId is null-terminated string before nonce
        int idEnd = 0;
        while (idEnd < nonceStart && payload[idEnd] != 0) idEnd++;
        String deviceId = new String(payload, 0, idEnd, StandardCharsets.UTF_8).trim();

        return new LoginRequest(deviceId, nonce);
    }

    public record LoginRequest(String deviceId, byte[] nonce) {}
}
