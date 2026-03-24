package com.isup.api.controller;

import com.isup.repository.DeviceRepository;
import com.isup.repository.EventLogRepository;
import com.isup.session.SessionRegistry;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource           dataSource;
    private final SessionRegistry      sessionRegistry;
    private final DeviceRepository     deviceRepository;
    private final EventLogRepository   eventLogRepository;

    private static final long START_TIME = System.currentTimeMillis();

    public HealthController(DataSource dataSource,
                             SessionRegistry sessionRegistry,
                             DeviceRepository deviceRepository,
                             EventLogRepository eventLogRepository) {
        this.dataSource         = dataSource;
        this.sessionRegistry    = sessionRegistry;
        this.deviceRepository   = deviceRepository;
        this.eventLogRepository = eventLogRepository;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ISUP server info
        Map<String, Object> isupServer = new LinkedHashMap<>();
        String isupStatus = "UP";
        try {
            int onlineDevices = sessionRegistry.onlineCount();
            long totalDevices = deviceRepository.count();
            isupServer.put("status", "UP");
            isupServer.put("port", 7660);
            isupServer.put("online_devices", onlineDevices);
            isupServer.put("total_devices", totalDevices);
        } catch (Exception e) {
            isupServer.put("status", "DOWN");
            isupStatus = "DEGRADED";
        }

        // Database info
        Map<String, Object> database = new LinkedHashMap<>();
        String dbStatus = "UP";
        try {
            if (dataSource instanceof HikariDataSource hds) {
                HikariPoolMXBean pool = hds.getHikariPoolMXBean();
                database.put("status", "UP");
                database.put("pool_active", pool != null ? pool.getActiveConnections() : 0);
                database.put("pool_idle",   pool != null ? pool.getIdleConnections() : 0);
            } else {
                // Test connectivity
                dataSource.getConnection().close();
                database.put("status", "UP");
                database.put("pool_active", 0);
                database.put("pool_idle", 0);
            }
        } catch (Exception e) {
            database.put("status", "DOWN");
            database.put("error", e.getMessage());
            dbStatus = "DOWN";
        }

        // Webhook stats
        Map<String, Object> webhook = new LinkedHashMap<>();
        try {
            long pending = eventLogRepository.findPendingWebhooks(3).size();
            Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
            webhook.put("pending", pending);
            webhook.put("failed_24h", 0); // simplified - would need a dedicated query
        } catch (Exception e) {
            webhook.put("pending", -1);
            webhook.put("failed_24h", -1);
        }

        // Memory
        Runtime rt = Runtime.getRuntime();
        long usedMemoryMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        // Uptime
        long uptimeSeconds = (System.currentTimeMillis() - START_TIME) / 1000;

        // Overall status
        String overallStatus = "DOWN".equals(dbStatus) ? "DOWN" : "UP";

        result.put("status", overallStatus);
        result.put("isup_server", isupServer);
        result.put("database", database);
        result.put("webhook", webhook);
        result.put("uptime_seconds", uptimeSeconds);
        result.put("memory_mb", usedMemoryMb);

        return result;
    }
}
