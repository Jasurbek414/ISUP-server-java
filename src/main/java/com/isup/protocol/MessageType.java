package com.isup.protocol;

public enum MessageType {
    LOGIN_REQUEST(0x0001),
    LOGIN_RESPONSE(0x0002),
    LOGOUT(0x0003),
    ALARM_EVENT(0x0004),
    ALARM_RESPONSE(0x0005),
    KEEPALIVE_REQUEST(0x0013),
    KEEPALIVE_RESPONSE(0x0014),
    UNKNOWN(-1);

    public final int code;

    MessageType(int code) {
        this.code = code;
    }

    public static MessageType fromCode(int code) {
        for (MessageType t : values()) {
            if (t.code == code) return t;
        }
        return UNKNOWN;
    }
}
