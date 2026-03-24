package com.isup.api.controller;

import com.isup.monitoring.MetricsRegistry;
import com.isup.session.SessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class MetricsController {

    private final MetricsRegistry metrics;
    private final SessionRegistry sessions;

    public MetricsController(MetricsRegistry metrics, SessionRegistry sessions) {
        this.metrics  = metrics;
        this.sessions = sessions;
    }

    @GetMapping(value = "/metrics", produces = "text/plain; version=0.0.4; charset=utf-8")
    public String metrics() {
        // Sync devices online from session registry
        metrics.setDevicesOnline(sessions.onlineCount());

        StringBuilder sb = new StringBuilder();

        // devices online
        sb.append("# HELP isup_devices_online_total Currently online devices\n");
        sb.append("# TYPE isup_devices_online_total gauge\n");
        sb.append("isup_devices_online_total ").append(metrics.getDevicesOnline()).append("\n");

        // events by type
        sb.append("# HELP isup_events_total Total events processed\n");
        sb.append("# TYPE isup_events_total counter\n");
        Map<String, AtomicLong> eventCounters = metrics.getEventCounters();
        if (eventCounters.isEmpty()) {
            sb.append("isup_events_total{type=\"attendance\"} 0\n");
        } else {
            for (Map.Entry<String, AtomicLong> entry : eventCounters.entrySet()) {
                sb.append("isup_events_total{type=\"").append(entry.getKey()).append("\"} ")
                  .append(entry.getValue().get()).append("\n");
            }
        }

        // webhooks sent
        sb.append("# HELP isup_webhook_sent_total Total webhooks sent\n");
        sb.append("# TYPE isup_webhook_sent_total counter\n");
        sb.append("isup_webhook_sent_total ").append(metrics.getWebhookSentTotal()).append("\n");

        // webhooks failed
        sb.append("# HELP isup_webhook_failed_total Total failed webhooks\n");
        sb.append("# TYPE isup_webhook_failed_total counter\n");
        sb.append("isup_webhook_failed_total ").append(metrics.getWebhookFailedTotal()).append("\n");

        // login attempts
        sb.append("# HELP isup_login_attempts_total Total login attempts\n");
        sb.append("# TYPE isup_login_attempts_total counter\n");
        sb.append("isup_login_attempts_total ").append(metrics.getLoginAttemptsTotal()).append("\n");

        // JVM memory
        Runtime rt      = Runtime.getRuntime();
        long usedBytes  = rt.totalMemory() - rt.freeMemory();
        sb.append("# HELP jvm_memory_used_bytes JVM memory used\n");
        sb.append("# TYPE jvm_memory_used_bytes gauge\n");
        sb.append("jvm_memory_used_bytes ").append(usedBytes).append("\n");

        return sb.toString();
    }
}
