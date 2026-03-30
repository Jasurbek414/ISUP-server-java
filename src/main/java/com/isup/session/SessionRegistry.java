package com.isup.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SessionRegistry {

    public static final AttributeKey<DeviceSession> SESSION_KEY = AttributeKey.valueOf("DEVICE_SESSION");

    private final ConcurrentHashMap<String, DeviceSession>  byDeviceIdPort  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, DeviceSession> bySessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<DeviceSession>> byDeviceId = new ConcurrentHashMap<>();

    public DeviceSession createSession(String deviceId, Channel channel, String ip) {
        int localPort = ((InetSocketAddress) channel.localAddress()).getPort();
        int sessionId = generateUniqueSessionId();
        DeviceSession session = new DeviceSession(deviceId, sessionId, channel, ip);
        
        byDeviceIdPort.put(deviceId + ":" + localPort, session);
        bySessionId.put(sessionId, session);
        byDeviceId.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(session);
        
        channel.attr(SESSION_KEY).set(session);
        return session;
    }

    public void removeSession(Channel channel) {
        DeviceSession session = channel.attr(SESSION_KEY).get();
        if (session != null) {
            int localPort = ((InetSocketAddress) channel.localAddress()).getPort();
            byDeviceIdPort.remove(session.getDeviceId() + ":" + localPort);
            bySessionId.remove(session.getSessionId());
            List<DeviceSession> list = byDeviceId.get(session.getDeviceId());
            if (list != null) {
                list.remove(session);
                if (list.isEmpty()) byDeviceId.remove(session.getDeviceId());
            }
        }
    }

    public Optional<DeviceSession> findByDeviceId(String deviceId) {
        // Return any active session (usually 7660 preferred)
        List<DeviceSession> list = byDeviceId.get(deviceId);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    public Optional<DeviceSession> findByDeviceIdAndPort(String deviceId, int port) {
        return Optional.ofNullable(byDeviceIdPort.get(deviceId + ":" + port));
    }

    public Optional<DeviceSession> findBySessionId(int sessionId) {
        return Optional.ofNullable(bySessionId.get(sessionId));
    }

    public DeviceSession getFromChannel(Channel channel) {
        return channel.attr(SESSION_KEY).get();
    }

    public List<DeviceSession> getAllSessions() {
        return new ArrayList<>(bySessionId.values());
    }

    public int onlineCount() {
        return (int) bySessionId.values().stream().filter(DeviceSession::isActive).count();
    }

    private int generateUniqueSessionId() {
        int id;
        do {
            // Use a smaller positive range (6 digits) to avoid sign/overflow issues in strict devices
            id = ThreadLocalRandom.current().nextInt(100000, 999999);
        } while (bySessionId.containsKey(id));
        return id;
    }
}
