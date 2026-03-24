package com.isup.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.isup.entity.EventLog;
import com.isup.event.AttendanceEvent;
import com.isup.repository.EventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WebhookRetryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookRetryService.class);

    @Value("${isup.webhook.max-retries:3}")
    private int maxRetries;

    private final EventLogRepository eventRepo;
    private final WebhookDispatcher  dispatcher;
    private final ObjectMapper       mapper;

    public WebhookRetryService(EventLogRepository eventRepo, WebhookDispatcher dispatcher) {
        this.eventRepo  = eventRepo;
        this.dispatcher = dispatcher;
        this.mapper     = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Scheduled(fixedDelayString = "${isup.webhook.retry-delay-ms:5000}")
    @Transactional
    public void retryFailed() {
        List<EventLog> pending = eventRepo.findPendingWebhooks(maxRetries);
        if (pending.isEmpty()) return;

        log.debug("Retrying {} failed webhooks", pending.size());
        for (EventLog eventLog : pending) {
            try {
                AttendanceEvent event = rebuildEvent(eventLog);
                dispatcher.dispatch(event, eventLog);
            } catch (Exception e) {
                log.warn("Retry failed for event={}: {}", eventLog.getId(), e.getMessage());
                eventLog.setWebhookAttempts(eventLog.getWebhookAttempts() + 1);
                if (eventLog.getWebhookAttempts() >= maxRetries) {
                    eventLog.setWebhookStatus("exhausted");
                }
                eventRepo.save(eventLog);
            }
        }
    }

    private AttendanceEvent rebuildEvent(EventLog log) {
        return AttendanceEvent.builder()
                .eventId(String.valueOf(log.getId()))
                .eventType(log.getEventType())
                .deviceId(log.getDeviceId())
                .employeeNo(log.getEmployeeNo())
                .employeeName(log.getEmployeeName())
                .cardNo(log.getCardNo())
                .direction(log.getDirection())
                .doorNo(log.getDoorNo())
                .verifyMode(log.getVerifyMode())
                .eventTime(log.getEventTime())
                .photoBase64(log.getPhotoBase64())
                .timestamp(log.getCreatedAt())
                .build();
    }
}
