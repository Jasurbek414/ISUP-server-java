package com.isup.session;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
public class DeviceSession {

    private final String deviceId;
    private final int sessionId;
    private final Channel channel;
    private final Instant connectedAt;

    @Setter private String ipAddress;
    @Setter private String model;
    @Setter private volatile Instant lastSeen;

    public DeviceSession(String deviceId, int sessionId, Channel channel, String ipAddress) {
        this.deviceId    = deviceId;
        this.sessionId   = sessionId;
        this.channel     = channel;
        this.ipAddress   = ipAddress;
        this.connectedAt = Instant.now();
        this.lastSeen    = Instant.now();
    }

    public void touch() {
        this.lastSeen = Instant.now();
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }
}
