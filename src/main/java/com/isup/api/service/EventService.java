package com.isup.api.service;

import com.isup.entity.Device;
import com.isup.entity.EventLog;
import com.isup.event.AttendanceEvent;
import com.isup.event.EventBuffer;
import com.isup.monitoring.MetricsRegistry;
import com.isup.repository.DeviceRepository;
import com.isup.repository.EventLogRepository;
import com.isup.webhook.WebhookDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventLogRepository     eventRepo;
    private final DeviceRepository       deviceRepo;
    private final WebhookDispatcher      webhook;
    private final SimpMessagingTemplate  ws;
    private final EventBuffer            eventBuffer;
    private final MetricsRegistry        metrics;

    public EventService(EventLogRepository eventRepo,
                        DeviceRepository deviceRepo,
                        WebhookDispatcher webhook,
                        SimpMessagingTemplate ws,
                        @Lazy EventBuffer eventBuffer,
                        MetricsRegistry metrics) {
        this.eventRepo   = eventRepo;
        this.deviceRepo  = deviceRepo;
        this.webhook     = webhook;
        this.ws          = ws;
        this.eventBuffer = eventBuffer;
        this.metrics     = metrics;
    }

    @Transactional
    public EventLog saveAndDispatch(AttendanceEvent event) {
        Optional<Device> deviceOpt = deviceRepo.findByDeviceId(event.getDeviceId());

        String photoPath = saveImage(event.getDeviceId(), event.getPhotoBase64());

        EventLog record = EventLog.builder()
                .deviceId(event.getDeviceId())
                .project(deviceOpt.map(Device::getProject).orElse(null))
                .eventType(event.getEventType())
                .employeeNo(event.getEmployeeNo())
                .employeeName(event.getEmployeeName())
                .cardNo(event.getCardNo())
                .direction(event.getDirection())
                .doorNo(event.getDoorNo())
                .verifyMode(event.getVerifyMode())
                .eventTime(event.getEventTime() != null ? event.getEventTime() : Instant.now())
                .photoBase64(event.getPhotoBase64() != null && event.getPhotoBase64().length() < 1000 ? event.getPhotoBase64() : null) // Only keep small ones in DB
                .photoPath(photoPath)
                .rawPayload(event.getRawPayload())
                .webhookStatus("pending")
                .build();

        EventLog savedRecord;
        try {
            savedRecord = eventRepo.save(record);
            log.info("Event saved id={} device={} type={} employee={}",
                    savedRecord.getId(), savedRecord.getDeviceId(),
                    savedRecord.getEventType(), savedRecord.getEmployeeNo());
        } catch (Exception e) {
            log.warn("DB save failed for event from device={}, buffering: {}", event.getDeviceId(), e.getMessage());
            eventBuffer.add(event);
            return record; // return unsaved record
        }

        // Increment metrics counter for this event type
        String eventType = event.getEventType() != null ? event.getEventType() : "unknown";
        metrics.incrementEvent(eventType);

        // Push to WebSocket subscribers (live feed)
        try {
            ws.convertAndSend("/topic/events", event);
        } catch (Exception e) {
            log.warn("WS push failed: {}", e.getMessage());
        }

        // Dispatch webhook asynchronously
        final EventLog finalRecord = savedRecord;
        Thread dispatchThread = new Thread(() -> webhook.dispatch(event, finalRecord), "webhook-dispatch");
        dispatchThread.setDaemon(true);
        dispatchThread.start();

        return savedRecord;
    }

    public Page<EventLog> findAll(int page, int size) {
        return eventRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public Page<EventLog> findByDevice(String deviceId, int page, int size) {
        return eventRepo.findByDeviceIdOrderByCreatedAtDesc(deviceId, PageRequest.of(page, size));
    }

    /**
     * Flexible filtered query. Any null parameter is ignored (treated as "no filter").
     */
    public Page<EventLog> findFiltered(String deviceId, String eventType, String employeeNo,
                                       Instant from, Instant to, int page, int size) {
        // Hibernate 6: null Instant params in JPQL cause type-binding issues.
        // Fall back to simple query when no filters are provided.
        boolean noFilters = deviceId == null && eventType == null && employeeNo == null
                            && from == null && to == null;
        if (noFilters) {
            return eventRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        }
        // When only deviceId filter — use the dedicated indexed query
        if (eventType == null && employeeNo == null && from == null && to == null && deviceId != null) {
            return eventRepo.findByDeviceIdOrderByCreatedAtDesc(deviceId, PageRequest.of(page, size));
        }
        return eventRepo.findFiltered(deviceId, eventType, employeeNo, from, to, PageRequest.of(page, size));
    }

    public List<EventLog> findRecent(int limit) {
        return eventRepo.findTop10ByOrderByCreatedAtDesc();
    }

    public long countToday() {
        Instant startOfDay = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        return eventRepo.countSince(startOfDay);
    }

    private String saveImage(String deviceId, String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            // Remove MIME prefix if present (e.g. data:image/jpeg;base64,)
            String cleaned = base64.contains(",") ? base64.split(",")[1] : base64;
            byte[] data = java.util.Base64.getDecoder().decode(cleaned);
            
            String fileName = deviceId + "_" + System.currentTimeMillis() + ".jpg";
            java.nio.file.Path root = java.nio.file.Paths.get("storage", "photos");
            java.nio.file.Files.createDirectories(root);
            
            java.nio.file.Path file = root.resolve(fileName);
            java.nio.file.Files.write(file, data);
            
            log.debug("Photo saved to disk: {}", file.toString());
            return "/api/events/photos/" + fileName; // Return relative URL
        } catch (Exception e) {
            log.warn("Failed to save image for {}: {}", deviceId, e.getMessage());
            return null;
        }
    }
}
