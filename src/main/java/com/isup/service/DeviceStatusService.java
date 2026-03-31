package com.isup.service;

import com.isup.entity.Device;
import com.isup.repository.DeviceRepository;
import com.isup.session.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceStatusService {
    private static final Logger log = LoggerFactory.getLogger(DeviceStatusService.class);
    private static final long OFFLINE_THRESHOLD_SECONDS = 90;

    private final DeviceRepository      deviceRepo;
    private final SimpMessagingTemplate ws;
    private final SessionRegistry       sessions;
    private final Map<String, String>   lastKnownStatus = new ConcurrentHashMap<>();

    public DeviceStatusService(DeviceRepository deviceRepo, SimpMessagingTemplate ws, SessionRegistry sessions) {
        this.deviceRepo = deviceRepo;
        this.ws = ws;
        this.sessions = sessions;
    }

    @Transactional
    @Scheduled(fixedDelay = 15_000)
    public void checkDeviceStatuses() {
        List<Device> devices = deviceRepo.findAll();
        Instant threshold = Instant.now().minusSeconds(OFFLINE_THRESHOLD_SECONDS);

        for (Device dev : devices) {
            String currentStatus = dev.getStatus();

            // Active TCP session = definitively online (handles persistent V1 + EHome 4.0)
            boolean hasActiveSession = sessions.findByDeviceId(dev.getDeviceId())
                    .map(s -> s.isActive()).orElse(false);

            boolean shouldBeOffline;
            if (hasActiveSession) {
                shouldBeOffline = false;
                // If DB says offline but TCP is active — sync lastSeen immediately
                if (!"online".equals(currentStatus) || dev.getLastSeen() == null) {
                    deviceRepo.updateStatus(dev.getDeviceId(), "online", Instant.now(), dev.getIpAddress());
                }
            } else {
                shouldBeOffline = dev.getLastSeen() == null || dev.getLastSeen().isBefore(threshold);
            }

            String expectedStatus = shouldBeOffline ? "offline" : "online";

            if (!expectedStatus.equals(currentStatus)) {
                deviceRepo.updateStatus(dev.getDeviceId(), expectedStatus, dev.getLastSeen(), dev.getIpAddress());
                log.info("Device {} status changed: {} -> {}", dev.getDeviceId(), currentStatus, expectedStatus);
            }

            String prevStatus = lastKnownStatus.getOrDefault(dev.getDeviceId(), "");
            if (!prevStatus.equals(expectedStatus)) {
                lastKnownStatus.put(dev.getDeviceId(), expectedStatus);
                try {
                    ws.convertAndSend("/topic/device-status", Map.of(
                        "deviceId", dev.getDeviceId(),
                        "status",   expectedStatus,
                        "lastSeen", dev.getLastSeen() != null ? dev.getLastSeen().toString() : "",
                        "name",     dev.getName() != null ? dev.getName() : dev.getDeviceId()
                    ));
                } catch (Exception e) {
                    log.warn("WS push failed for device {}: {}", dev.getDeviceId(), e.getMessage());
                }
            }
        }
    }
}
