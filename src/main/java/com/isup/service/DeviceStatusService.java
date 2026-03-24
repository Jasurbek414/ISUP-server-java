package com.isup.service;

import com.isup.entity.Device;
import com.isup.repository.DeviceRepository;
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
    private final Map<String, String>   lastKnownStatus = new ConcurrentHashMap<>();

    public DeviceStatusService(DeviceRepository deviceRepo, SimpMessagingTemplate ws) {
        this.deviceRepo = deviceRepo;
        this.ws = ws;
    }

    @Transactional
    @Scheduled(fixedDelay = 15_000)
    public void checkDeviceStatuses() {
        List<Device> devices = deviceRepo.findAll();
        Instant threshold = Instant.now().minusSeconds(OFFLINE_THRESHOLD_SECONDS);

        for (Device dev : devices) {
            String currentStatus = dev.getStatus();
            boolean shouldBeOffline = dev.getLastSeen() == null || dev.getLastSeen().isBefore(threshold);
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
