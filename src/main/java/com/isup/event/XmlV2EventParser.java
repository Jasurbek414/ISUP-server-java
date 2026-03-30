package com.isup.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

/**
 * Parses XML v2 events from DS-K1T343, DS-K1T342, DS-K1T671, DS-K1T672, etc.
 * Contains <AccessControllerEvent> block with employeeNoString, name, AttendanceStatus.
 */
@Component
public class XmlV2EventParser implements EventParser {

    private static final Logger log = LoggerFactory.getLogger(XmlV2EventParser.class);

    @Override
    public boolean supports(String rawPayload) {
        return rawPayload != null
                && rawPayload.contains("<EventNotificationAlert")
                && rawPayload.contains("<AccessControllerEvent");
    }

    @Override
    public Optional<AttendanceEvent> parse(String deviceId, String rawPayload) {
        try {
            Document doc = parse(rawPayload);

            String eventType   = text(doc, "eventType", "Command");
            if (!"AccessControllerEvent".equalsIgnoreCase(eventType) && !"EVENT_REPORT".equalsIgnoreCase(eventType)) {
                return Optional.empty();
            }

            String employeeNo  = text(doc, "employeeNoString", "employeeNo", "employeeNoString");
            String name        = text(doc, "name", "employeeName", "name", "employeeName");
            String cardNo      = text(doc, "cardNo", "cardNum", "cardNo");
            String verifyMode  = normalizeVerifyMode(text(doc, "currentVerifyMode", "verifyMode", "currentVerifyMode"));
            String direction   = mapAttendanceStatus(text(doc, "AttendanceStatus", "attendanceStatus", "direction"));
            int    doorNo      = parseInt(text(doc, "doorNo", "doorId", "doorNo"), 1);
            String photo       = text(doc, "picData", "picDataRecord", "picData");
            Instant eventTime  = parseDateTime(text(doc, "dateTime", "eventTime", "dateTime"));

            return Optional.of(AttendanceEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("attendance")
                    .deviceId(deviceId)
                    .employeeNo(employeeNo)
                    .employeeName(name)
                    .cardNo(cardNo)
                    .verifyMode(verifyMode)
                    .direction(direction)
                    .doorNo(doorNo)
                    .photoBase64(photo)
                    .eventTime(eventTime)
                    .timestamp(Instant.now())
                    .rawPayload(rawPayload)
                    .build());
        } catch (Exception e) {
            log.warn("XmlV2 parse error for device {}: {}", deviceId, e.getMessage());
            return Optional.empty();
        }
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = f.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String text(Document doc, String... tagNames) {
        for (String tag : tagNames) {
            NodeList nl = doc.getElementsByTagName(tag);
            if (nl.getLength() > 0) {
                Node node = nl.item(0);
                if (node != null) return node.getTextContent().trim();
            }
        }
        return null;
    }

    private int parseInt(String value, int def) {
        if (value == null) return def;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return def; }
    }

    private Instant parseDateTime(String dt) {
        if (dt == null) return Instant.now();
        try { return OffsetDateTime.parse(dt).toInstant(); } catch (DateTimeParseException e) { return Instant.now(); }
    }

    private String mapAttendanceStatus(String status) {
        if (status == null) return "in";
        return switch (status) {
            case "checkIn"      -> "in";
            case "checkOut"     -> "out";
            case "breakIn"      -> "break_in";
            case "breakOut"     -> "break_out";
            case "overtimeIn"   -> "overtime_in";
            case "overtimeOut"  -> "overtime_out";
            default             -> status.toLowerCase();
        };
    }

    private String normalizeVerifyMode(String mode) {
        if (mode == null) return "unknown";
        if (mode.toLowerCase().contains("face") && mode.toLowerCase().contains("card")) return "faceAndCard";
        if (mode.toLowerCase().contains("face")) return "face";
        if (mode.toLowerCase().contains("fp"))   return "fingerprint";
        if (mode.toLowerCase().contains("card")) return "card";
        return mode;
    }
}
