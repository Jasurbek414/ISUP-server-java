package com.isup.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

/**
 * Parses JSON alarm events from DS-2CDxxxx cameras.
 * Handles motiondetection, linecrossing, fielddetection, facedetection, etc.
 */
@Component
public class JsonEventParser implements EventParser {

    private static final Logger log = LoggerFactory.getLogger(JsonEventParser.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supports(String rawPayload) {
        if (rawPayload == null) return false;
        String trimmed = rawPayload.trim();
        return trimmed.startsWith("{") && trimmed.contains("eventType");
    }

    @Override
    public Optional<AttendanceEvent> parse(String deviceId, String rawPayload) {
        try {
            JsonNode root      = mapper.readTree(rawPayload);
            String   eventType = mapEventType(root.path("eventType").asText());
            int      channel   = root.path("channelID").asInt(1);
            Instant  eventTime = parseDateTime(root.path("dateTime").asText(null));

            return Optional.of(AttendanceEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .deviceId(deviceId)
                    .channel(channel)
                    .eventTime(eventTime)
                    .timestamp(Instant.now())
                    .rawPayload(rawPayload)
                    .build());
        } catch (Exception e) {
            log.warn("JSON parse error for device {}: {}", deviceId, e.getMessage());
            return Optional.empty();
        }
    }

    private String mapEventType(String type) {
        if (type == null) return "unknown";
        return switch (type.toLowerCase()) {
            case "motiondetection"  -> "motion";
            case "linecrossing"     -> "line_crossing";
            case "fielddetection"   -> "intrusion";
            case "facedetection"    -> "face_detection";
            case "videoloss"        -> "video_loss";
            case "tamperdetection"  -> "tamper";
            default                 -> type.toLowerCase();
        };
    }

    private Instant parseDateTime(String dt) {
        if (dt == null || dt.isBlank()) return Instant.now();
        try { return OffsetDateTime.parse(dt).toInstant(); } catch (DateTimeParseException e) { return Instant.now(); }
    }
}
