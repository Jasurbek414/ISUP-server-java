package com.isup;

import com.isup.protocol.HmacAuthenticator;
import com.isup.protocol.IsupPacket;
import com.isup.protocol.IsupProtocol;
import com.isup.protocol.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IsupProtocolTest {

    @Test
    void encodeDecodeRoundTrip() {
        byte[] payload = "hello".getBytes();
        ByteBuf encoded = IsupProtocol.encode(MessageType.ALARM_EVENT, 42, 7, payload);

        IsupPacket decoded = IsupProtocol.decode(encoded);
        assertNotNull(decoded);
        assertEquals(MessageType.ALARM_EVENT, decoded.getMessageType());
        assertEquals(42, decoded.getSessionId());
        assertEquals(7, decoded.getSequenceNo());
        assertArrayEquals(payload, decoded.getPayload());
        encoded.release();
    }

    @Test
    void emptyPayloadEncodesDecode() {
        ByteBuf encoded = IsupProtocol.encode(MessageType.KEEPALIVE_REQUEST, 1, 1, null);
        IsupPacket decoded = IsupProtocol.decode(encoded);
        assertNotNull(decoded);
        assertEquals(0, decoded.getPayloadSafe().length);
        encoded.release();
    }

    @Test
    void hmacEmptyPassword() {
        byte[] nonce = new byte[33];
        for (int i = 0; i < 33; i++) nonce[i] = (byte) i;
        byte[] hmac1 = HmacAuthenticator.compute(nonce, "");
        byte[] hmac2 = HmacAuthenticator.compute(nonce, null);
        // Both empty/null passwords should produce same result
        assertArrayEquals(hmac1, hmac2);
        assertEquals(32, hmac1.length);
    }

    @Test
    void hmacVerify() {
        byte[] nonce    = new byte[33];
        String password = "test123";
        byte[] hmac     = HmacAuthenticator.compute(nonce, password);
        assertTrue(HmacAuthenticator.verify(nonce, password, hmac));
        assertFalse(HmacAuthenticator.verify(nonce, "wrong", hmac));
    }

    @Test
    void parseLoginRequest() {
        // Build: deviceId + null + nonce(33 bytes)
        String devId = "DS-K1T343EWX";
        byte[] nonce = new byte[33];
        byte[] payload = new byte[devId.length() + 1 + 33];
        System.arraycopy(devId.getBytes(), 0, payload, 0, devId.length());
        payload[devId.length()] = 0; // null terminator
        System.arraycopy(nonce, 0, payload, devId.length() + 1, 33);

        IsupProtocol.LoginRequest req = IsupProtocol.parseLoginRequest(payload);
        assertEquals(devId, req.deviceId());
        assertArrayEquals(nonce, req.nonce());
    }
}
