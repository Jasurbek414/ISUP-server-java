package com.isup.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SessionRegistry {

    public static final AttributeKey<DeviceSession> SESSION_KEY = AttributeKey.valueOf("DEVICE_SESSION");

    private final ConcurrentHashMap<String, DeviceSession>  byDeviceId  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, DeviceSession> bySessionId = new ConcurrentHashMap<>();

    public DeviceSession createSession(String deviceId, Channel channel, String ip) {
        int sessionId = generateUniqueSessionId();
        DeviceSession session = new DeviceSession(deviceId, sessionId, channel, ip);
        byDeviceId.put(deviceId, session);
        bySessionId.put(sessionId, session);
        channel.attr(SESSION_KEY).set(session);
        return session;
    }

    public void removeSession(Channel channel) {
        DeviceSession session = channel.attr(SESSION_KEY).get();
        if (session != null) {
            byDeviceId.remove(session.getDeviceId());
            bySessionId.remove(session.getSessionId());
        }
    }

    public Optional<DeviceSession> findByDeviceId(String deviceId) {
        return Optional.ofNullable(byDeviceId.get(deviceId));
    }

    public Optional<DeviceSession> findBySessionId(int sessionId) {
        return Optional.ofNullable(bySessionId.get(sessionId));
    }

    public DeviceSession getFromChannel(Channel channel) {
        return channel.attr(SESSION_KEY).get();
    }

    public List<DeviceSession> getAllSessions() {
        return new ArrayList<>(byDeviceId.values());
    }

    public int onlineCount() {
        return (int) byDeviceId.values().stream().filter(DeviceSession::isActive).count();
    }

    private int generateUniqueSessionId() {
        int id;
        do {
            id = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        } while (bySessionId.containsKey(id));
        return id;
    }
}
