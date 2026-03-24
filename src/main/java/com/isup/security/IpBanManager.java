package com.isup.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpBanManager {

    private static final Logger log = LoggerFactory.getLogger(IpBanManager.class);

    private static final int    MAX_FAILURES      = 5;
    private static final long   FAILURE_WINDOW_MS = 60_000L;   // 60 seconds
    private static final long   AUTO_BAN_DURATION = 3_600_000L; // 1 hour

    // ip -> list of failure timestamps
    private final ConcurrentHashMap<String, List<Long>> failureMap = new ConcurrentHashMap<>();

    // ip -> ban expiry timestamp
    private final ConcurrentHashMap<String, Long> banMap = new ConcurrentHashMap<>();

    public void recordFailedLogin(String ip) {
        long now = System.currentTimeMillis();
        failureMap.compute(ip, (k, timestamps) -> {
            if (timestamps == null) timestamps = new ArrayList<>();
            // Remove old entries outside the window
            long cutoff = now - FAILURE_WINDOW_MS;
            timestamps.removeIf(t -> t < cutoff);
            timestamps.add(now);
            return timestamps;
        });

        List<Long> failures = failureMap.get(ip);
        if (failures != null && failures.size() >= MAX_FAILURES) {
            ban(ip, Duration.ofMillis(AUTO_BAN_DURATION));
            failureMap.remove(ip);
            log.warn("IP {} auto-banned for 1 hour after {} failed login attempts", ip, MAX_FAILURES);
        }
    }

    public boolean isBanned(String ip) {
        Long expiry = banMap.get(ip);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            banMap.remove(ip);
            return false;
        }
        return true;
    }

    public void ban(String ip, Duration duration) {
        long expiry = System.currentTimeMillis() + duration.toMillis();
        banMap.put(ip, expiry);
        log.info("IP {} banned until {}", ip, Instant.ofEpochMilli(expiry));
    }

    public void unban(String ip) {
        banMap.remove(ip);
        failureMap.remove(ip);
        log.info("IP {} unbanned", ip);
    }

    @Scheduled(fixedDelay = 600_000L) // every 10 minutes
    public void cleanExpiredBans() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (java.util.Iterator<java.util.Map.Entry<String, Long>> it = banMap.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<String, Long> entry = it.next();
            if (now > entry.getValue()) {
                it.remove();
                removed++;
            }
        }
        // Also clean stale failure records
        long cutoff = now - FAILURE_WINDOW_MS;
        for (java.util.Iterator<java.util.Map.Entry<String, List<Long>>> it = failureMap.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<String, List<Long>> entry = it.next();
            entry.getValue().removeIf(t -> t < cutoff);
            if (entry.getValue().isEmpty()) it.remove();
        }
        if (removed > 0) {
            log.debug("Cleaned {} expired IP bans", removed);
        }
    }
}
