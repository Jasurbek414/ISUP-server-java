package com.isup.protocol;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IsupPacket {
    public static final byte STX    = 0x20;
    public static final byte ETX = 0x20;
    public static final int VERSION_V1 = 1;
    public static final int VERSION_V4 = 4;  // EHome 4.0: STX=0x10 + XML payload
    public static final int VERSION_V5 = 5;  // EHome 5.0: STX=0x20 binary frame

    private final MessageType messageType;
    private final int sessionId;
    private final int sequenceNo;
    private final byte[] payload;
    @Builder.Default
    private final int protocolVersion = VERSION_V5;

    public byte[] getPayloadSafe() {
        return payload == null ? new byte[0] : payload;
    }

    public String getPayloadAsString() {
        byte[] p = getPayloadSafe();
        return new String(p, java.nio.charset.StandardCharsets.UTF_8).trim();
    }
}
