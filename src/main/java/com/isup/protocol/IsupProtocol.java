package com.isup.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

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

    public static final int HEADER_SIZE = 14; 
    public static final int NONCE_SIZE   = 32; // Standard 32-byte token
    public static final int HMAC_SIZE    = 32;

    // ─── Parser ──────────────────────────────────────────────────────────────

    public static IsupPacket decode(ByteBuf buf) {
        if (buf.readableBytes() < 2) return null;

        buf.markReaderIndex();
        byte stx = buf.readByte();
        buf.resetReaderIndex();

        return switch (stx) {
            case 0x10 -> decodeV1(buf);
            case 0x20 -> decodeV5(buf);
            case 0x01 -> decodeHeaderlessV1(buf);
            default -> {
                // Read a few bytes for logging
                int len = Math.min(buf.readableBytes(), 10);
                byte[] first = new byte[len];
                buf.markReaderIndex();
                buf.readBytes(first);
                buf.resetReaderIndex();
                log.debug("Unknown STX 0x{} (first 10: {})", Integer.toHexString(stx & 0xFF), Arrays.toString(first));
                buf.readByte(); // consume one to avoid loop
                yield null;
            }
        };
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

        return buildV1Packet(body);
    }

    /**
     * Parse ISUP v1 headerless frame (starts with command ID, e.g. 0x01).
     */
    private static IsupPacket decodeHeaderlessV1(ByteBuf buf) {
        int available = buf.readableBytes();
        byte[] body = new byte[available];
        buf.readBytes(body);
        return buildV1Packet(body);
    }

    private static IsupPacket buildV1Packet(byte[] body) {
        if (body == null || body.length < 1) return null;
        int msgTypeByte = body[0] & 0xFF;

        MessageType msgType = switch (msgTypeByte) {
            case 0x01 -> MessageType.LOGIN_REQUEST;
            case 0x02 -> MessageType.EHOME_LOGIN;
            case 0x03 -> MessageType.LOGOUT;
            case 0x13 -> MessageType.KEEPALIVE_REQUEST;
            case 0x14 -> MessageType.KEEPALIVE_RESPONSE;
            case 0x53 -> MessageType.LOGIN_REQUEST_V5;
            case 0x54 -> MessageType.LOGIN_RESPONSE_V5;
            default   -> MessageType.UNKNOWN;
        };

        int sessionId = 0;
        // Extract SessionID from v1 keepalive/alarm bodies if present
        if (body.length >= 5 && (msgTypeByte == 0x13 || msgTypeByte == 0x04 || msgTypeByte == 0x14)) {
            sessionId = ((body[1] & 0xFF) | ((body[2] & 0xFF) << 8) | ((body[3] & 0xFF) << 16) | ((body[4] & 0xFF) << 24));
        }

        log.debug("V1 decoded: type={} bodyLen={} sid={}", msgType, body.length, sessionId);

        return IsupPacket.builder()
                .messageType(msgType)
                .sessionId(sessionId)
                .sequenceNo(0)
                .payload(body)
                .protocolVersion(IsupPacket.VERSION_V1)
                .build();
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
    public static ByteBuf buildV1RegisterResponse(int sessionId, int interval) {
        // Ehome v5.0 Standard Register ACK (42-byte total)
        // Body: [Type:0x01][Status:0x00][Interval:2 LE][SessionID:4 LE][32 zeros]
        ByteBuf buf = Unpooled.buffer(42);
        buf.writeByte(0x10);          // Marker
        buf.writeByte(40);            // Body length (40 bytes)
        buf.writeByte(0x01);          // Type 0x01 (Register ACK)
        buf.writeByte(0x00);          // Status: Success
        buf.writeShortLE(interval);   // Interval (LE)
        buf.writeIntLE(sessionId);    // Session ID (LE)
        buf.writeZero(32);            // HMAC padding
        return buf;
    }
    
    public static ByteBuf buildV1RegisterResponseMini(int sessionId, int interval) {
        ByteBuf buf = Unpooled.buffer(10);
        buf.writeByte(0x10);
        buf.writeByte(0x08);
        buf.writeByte(0x01);
        buf.writeByte(0x00);
        buf.writeShortLE(interval);
        buf.writeIntLE(sessionId);
        return buf;
    }

    public static final AttributeKey<byte[]> SERVER_CHALLENGE = AttributeKey.valueOf("server_challenge");

    public static ByteBuf buildV1Challenge(int sessionId, byte[] challenge) {
        // v5 Login Response (Pure Binary): 401 Unauthorized / Challenge
        // Frame: [10][28][02][02][00 00][SID:4 LE][Challenge:32] (Total 42 bytes)
        // Type: 0x02 (Login Response), Status: 0x02 (Challenge/Auth Needed)
        ByteBuf buf = Unpooled.buffer(42);
        buf.writeByte(0x10);          // Marker
        buf.writeByte(40);            // Body len 40
        buf.writeByte(0x02);          // Type: Login Response (0x02)
        buf.writeByte(0x01);          // Status: Authorized (0x01) - Test
        buf.writeShortLE(0);          // Interval (0 for challenge)
        buf.writeIntLE(sessionId);    // Session ID
        buf.writeBytes(challenge);    // 32-byte challenge
        return buf;
    }

    public static ByteBuf buildV1MiniSuccess(int sessionId) {
        // 10-byte success packet (No HMAC)
        // [10][08][02][00][3C 00][SID:4 LE]
        ByteBuf buf = Unpooled.buffer(10);
        buf.writeByte(0x10);
        buf.writeByte(0x08);
        buf.writeByte(0x02); // Type 0x02
        buf.writeByte(0x00); // Status 0x00
        buf.writeShortLE(60); // Interval
        buf.writeIntLE(sessionId); // SID
        return buf;
    }

    public static ByteBuf buildV1Success(int sessionId, byte[] challenge, String password, String deviceId) {
        byte[] hmac = new byte[32];
        try { hmac = HmacAuthenticator.computeV1(challenge, password); } catch (Exception e) {}
        return buildV1SuccessEcho(sessionId, hmac, deviceId);
    }

    public static ByteBuf buildV1SuccessEcho(int sessionId, byte[] hmac, String deviceId) {
        // v5 Register Result (XML-ish Binary): Success with echoed HMAC
        // Frame: [10][28][54][00][3C 00][SID:4 LE][HMAC:32] (Total 42 bytes)
        ByteBuf buf = Unpooled.buffer(42);
        buf.writeByte(0x10);
        buf.writeByte(40);            // Body len 40
        buf.writeByte(0x54);          // Type 0x54
        buf.writeByte(0x00);          // Status: Success
        buf.writeShortLE(60);         // Interval
        buf.writeIntLE(sessionId);
        if (hmac != null && hmac.length == 32) {
            buf.writeBytes(hmac);
        } else {
            buf.writeZero(32);
        }
        return buf;
    }

    public static ByteBuf buildV1BinarySuccess(int sessionId, byte[] challenge, String password) {
        // v5 Login Response (Pure Binary): Success with recalculated HMAC
        byte[] hmac = new byte[32];
        try {
            hmac = HmacAuthenticator.computeV1(challenge, password);
        } catch (Exception e) {}
        return buildV1BinarySuccessRaw(sessionId, hmac);
    }

    public static ByteBuf buildV1BinarySuccessRaw(int sessionId, byte[] hmac) {
        // v5 Login Response (Pure Binary): Success with explicit HMAC echo
        // Frame: [10][28][02][01][Interval:2][SID:4][HMAC:32] (Total 42 bytes)
        ByteBuf buf = Unpooled.buffer(42);
        buf.writeByte(0x10);          // Marker
        buf.writeByte(40);            // Body len 40
        buf.writeByte(0x02);          // Type 0x02 (Login Response)
        buf.writeByte(0x01);          // Status: Authorized (0x01)
        buf.writeShortLE(60);         // Interval
        buf.writeIntLE(sessionId);    // Session ID
        if (hmac != null && hmac.length == 32) {
            buf.writeBytes(hmac);
        } else {
            buf.writeZero(32);
        }
        return buf;
    }

    public static ByteBuf buildV5XmlSuccess(int sessionId, String deviceId) {
        String xml = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><RegisterResult><DeviceID>%s</DeviceID><Result>1</Result></RegisterResult>", deviceId);
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        int bodyLen = 1 + xmlBytes.length; // 1 byte for command ID (0x54)

        ByteBuf buf = Unpooled.buffer(2 + bodyLen);
        buf.writeByte(0x10);          // Marker
        buf.writeByte(bodyLen);       // Body Length
        buf.writeByte(0x54);          // Command ID
        buf.writeBytes(xmlBytes);
        return buf;
    }



    private static final String PPVSP_TPL = "<PPVSPMessage><Version>5.0</Version><CommandType>%s</CommandType><Command>%s</Command><Params>%s</Params></PPVSPMessage>";

    /**
     * Build standard EHome 5.0 REG_RESULT XML with optional MD5 signature.
     */
    public static ByteBuf buildV5XmlSuccessFull(int sessionId, String deviceId, String password) {
        String params;
        if (password != null && !password.isEmpty()) {
            // EHome 5.0 common signature: MD5(DeviceID + ":" + SessionID + ":" + ResultCode + ":" + Password)
            String signData = String.format("%s:%d:0:%s", deviceId, sessionId, password);
            String md5hex = bytesToHex(md5(signData.getBytes(StandardCharsets.UTF_8)));
            params = String.format("<DeviceID>%s</DeviceID><ResultCode>0</ResultCode><SessionID>%d</SessionID><MD5>%s</MD5>", 
                                  deviceId, sessionId, md5hex.toUpperCase());
        } else {
            params = String.format("<DeviceID>%s</DeviceID><ResultCode>0</ResultCode><SessionID>%d</SessionID>", deviceId, sessionId);
        }
        
        String xml = String.format(PPVSP_TPL, "RESPONSE", "REG_RESULT", params);
        return encodeV1Xml(xml, (byte)0x54);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] md5(byte[] data) {
        try {
            return java.security.MessageDigest.getInstance("MD5").digest(data);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static ByteBuf buildV5XmlSuccessV5(int sessionId, String deviceId, String password) {
        String params;
        if (password != null && !password.isEmpty()) {
            String signData = String.format("%s:%d:0:%s", deviceId, sessionId, password);
            String md5hex = bytesToHex(md5(signData.getBytes(StandardCharsets.UTF_8)));
            params = String.format("<DeviceID>%s</DeviceID><ResultCode>0</ResultCode><SessionID>%d</SessionID><MD5>%s</MD5>", 
                                  deviceId, sessionId, md5hex.toUpperCase());
        } else {
            params = String.format("<DeviceID>%s</DeviceID><ResultCode>0</ResultCode><SessionID>%d</SessionID>", deviceId, sessionId);
        }
        
        String xml = String.format(PPVSP_TPL, "RESPONSE", "REG_RESULT", params);
        return encodeV5Xml(xml, (byte)0x54);
    }

    public static ByteBuf buildV5XmlTimeSync(int sessionId, String deviceId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String time = sdf.format(new Date());
        // Ultra-minimal XML to stay under 255 bytes
        String xml = String.format("<PPVSPMessage><Command>SET_TIME</Command><Params><DeviceID>%s</DeviceID><SessionID>%d</SessionID><Time><LocalTime>%s</LocalTime><TimeZone>CST-5</TimeZone></Time></Params></PPVSPMessage>", deviceId, sessionId, time);
        return encodeV1Xml(xml, (byte)0x09);
    }

    public static ByteBuf buildV5XmlCapabilities(int sessionId, String deviceId) {
        String params = String.format("<DeviceID>%s</DeviceID><SessionID>%d</SessionID>", deviceId, sessionId);
        String xml = String.format(PPVSP_TPL, "REQUEST", "GET_CAPABILITIES", params);
        return encodeV1Xml(xml, (byte)0x54);
    }

    public static ByteBuf buildV5DoorControl(int sessionId, int doorNo, String mode) {
        String params = String.format("<DoorNo>%d</DoorNo><ControlMode>%s</ControlMode><SessionID>%d</SessionID>", doorNo, mode, sessionId);
        String xml = String.format(PPVSP_TPL, "REQUEST", "CONTROL_DOOR", params);
        return encodeV1Xml(xml, (byte)0x54);
    }

    private static ByteBuf encodeV5(String xml, int type, int sessionId) {
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        int totalLen = 14 + xmlBytes.length;
        ByteBuf buf = Unpooled.buffer(totalLen);
        buf.writeByte(IsupPacket.STX);  // 0x20
        buf.writeIntLE(totalLen);       // Length
        buf.writeShortLE(type);         // MsgType
        buf.writeIntLE(sessionId);      // SessionID
        buf.writeShortLE(0);            // SeqNo
        buf.writeBytes(xmlBytes);       // Payload
        buf.writeByte(IsupPacket.ETX);  // 0x20/0x03/0x0A (Standardized to STX)
        return buf;
    }

    public static ByteBuf encodeV1(ByteBuf body, byte cmd) {
        int bodyLen = 1 + body.readableBytes();
        ByteBuf buf = Unpooled.buffer(2 + bodyLen);
        buf.writeByte(0x10);
        buf.writeByte(bodyLen);
        buf.writeByte(cmd);
        buf.writeBytes(body);
        return buf;
    }

    private static ByteBuf encodeV1Xml(String xml, byte cmd) {
        byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        return encodeV1(Unpooled.wrappedBuffer(xmlBytes), cmd);
    }

    public static ByteBuf buildV1LoginResponse(int sessionId, byte[] nonce, String password, String deviceId) {
        return buildV5XmlSuccessFull(sessionId, deviceId);
    }

    public static ByteBuf buildV1KeepaliveV5(int sessionId) {
        // V5 Heartbeat Request: [10][05][24][SID:4 LE]
        ByteBuf buf = Unpooled.buffer(7);
        buf.writeByte(0x10);
        buf.writeByte(5);
        buf.writeByte(0x24);
        buf.writeIntLE(sessionId);
        return buf;
    }

    public static ByteBuf buildV1KeepaliveResponse(int sessionId) {
        // v1 keepalive resp: [10][05][14][sessionid:4 LE]
        ByteBuf buf = Unpooled.buffer(7);
        buf.writeByte(0x10);
        buf.writeByte(5);             // Body length 5
        buf.writeByte(0x14);          // Type: KEEPALIVE_RESPONSE
        buf.writeIntLE(sessionId);
        return buf;
    }

    public static ByteBuf buildV1KeepaliveRequest(int sessionId) {
        // v1 keepalive req: [10][05][13][sessionid:4 LE]
        ByteBuf buf = Unpooled.buffer(7);
        buf.writeByte(0x10);
        buf.writeByte(5);             // Body length 5
        buf.writeByte(0x13);          // Type: KEEPALIVE_REQUEST
        buf.writeIntLE(sessionId);
        return buf;
    }

    public static ByteBuf buildV1AlarmResponse(int sessionId) {
        // v1 alarm resp: [10][05][05][sessionid:4 LE]
        ByteBuf buf = Unpooled.buffer(7);
        buf.writeByte(0x10);
        buf.writeByte(5);             // Body length 5
        buf.writeByte(0x05);          // Type: ALARM_RESPONSE
        buf.writeIntLE(sessionId);
        return buf;
    }

    // ─── Login request parsing ───────────────────────────────────────────────

    /**
     * Parses LOGIN_REQUEST payload → {deviceId, nonce, serial}
     * Supports both Binary (Legacy) and XML (EHome 5.0) formats.
     */
    public static LoginRequest parseLoginRequest(byte[] payload) {
        if (payload == null || payload.length < 5) return null;
        
        String s = new String(payload, StandardCharsets.UTF_8).trim();
        if (s.startsWith("<") || s.contains("<PPVSPMessage>")) {
            // EHome 5.0 XML Mode
            String deviceId = extractXml(s, "DeviceID");
            String serial   = extractXml(s, "SerialNumber");
            return new LoginRequest(deviceId, new byte[32], serial);
        }

        try {
            // Binary Fallback (Old V1/V5 styles)
            int pos = 0;
            if (payload.length > 3 && payload[0] == 0x01 && payload[1] == 0x01) {
                pos = 3; // Type 01 01 00
            }

            // Simple TLV-like parsing (Serial, Model, etc.)
            int serialLen = payload[pos] & 0xFF;
            pos++;
            String serial = new String(payload, pos, Math.min(serialLen, payload.length - pos), StandardCharsets.UTF_8).trim();
            pos += serialLen;

            int modelLen = payload[pos] & 0xFF;
            pos++;
            pos += modelLen + 2; // skip model + ports

            int idLen = payload[pos] & 0xFF;
            pos++;
            String deviceId = new String(payload, pos, Math.min(idLen, payload.length - pos), StandardCharsets.UTF_8).trim();
            pos += idLen;

            byte[] nonce = new byte[32];
            if (payload.length >= pos + 32) System.arraycopy(payload, pos, nonce, 0, 32);

            log.info("LOGIN_PARSED (Binary): deviceId={} serial={}", deviceId, serial);
            return new LoginRequest(deviceId, nonce, serial);
        } catch (Exception e) {
            log.warn("Failed to parse login binary: {}", e.getMessage());
            return new LoginRequest("unknown", new byte[32], "unknown");
        }
    }

    private static String extractXml(String xml, String tag) {
        int start = xml.indexOf("<" + tag + ">");
        if (start == -1) return "";
        start += tag.length() + 2;
        int end = xml.indexOf("</" + tag + ">", start);
        if (end == -1) return "";
        return xml.substring(start, end).trim();
    }

    public record LoginRequest(String deviceId, byte[] nonce, String serial) {}
}
