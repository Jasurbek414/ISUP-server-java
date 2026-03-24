package com.isup.monitoring;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsRegistry {

    private final AtomicLong devicesOnline     = new AtomicLong(0);
    private final AtomicLong webhookSentTotal  = new AtomicLong(0);
    private final AtomicLong webhookFailedTotal = new AtomicLong(0);
    private final AtomicLong loginAttemptsTotal = new AtomicLong(0);

    // event type -> count
    private final ConcurrentHashMap<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();

    public void setDevicesOnline(long count) {
        devicesOnline.set(count);
    }

    public long getDevicesOnline() {
        return devicesOnline.get();
    }

    public void incrementEvent(String type) {
        eventCounters.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
    }

    public ConcurrentHashMap<String, AtomicLong> getEventCounters() {
        return eventCounters;
    }

    public void incrementWebhookSent() {
        webhookSentTotal.incrementAndGet();
    }

    public long getWebhookSentTotal() {
        return webhookSentTotal.get();
    }

    public void incrementWebhookFailed() {
        webhookFailedTotal.incrementAndGet();
    }

    public long getWebhookFailedTotal() {
        return webhookFailedTotal.get();
    }

    public void incrementLoginAttempts() {
        loginAttemptsTotal.incrementAndGet();
    }

    public long getLoginAttemptsTotal() {
        return loginAttemptsTotal.get();
    }
}
