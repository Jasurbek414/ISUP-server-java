package com.isup.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
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
 * Parses XML v1 events from DS-K1T341 and older Face ID models.
 * Minimal payload: eventType, cardNo, dateTime (no AccessControllerEvent block).
 */
@Component
public class XmlV1EventParser implements EventParser {

    private static final Logger log = LoggerFactory.getLogger(XmlV1EventParser.class);

    @Override
    public boolean supports(String rawPayload) {
        return rawPayload != null
                && rawPayload.contains("<EventNotificationAlert")
                && !rawPayload.contains("<AccessControllerEvent");
    }

    @Override
    public Optional<AttendanceEvent> parse(String deviceId, String rawPayload) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = f.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(rawPayload.getBytes(StandardCharsets.UTF_8)));

            String cardNo     = text(doc, "cardNo");
            String eventType  = text(doc, "eventType");
            Instant eventTime = parseDateTime(text(doc, "dateTime"));

            return Optional.of(AttendanceEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("attendance")
                    .deviceId(deviceId)
                    .cardNo(cardNo)
                    .verifyMode("card")
                    .direction("in")
                    .doorNo(1)
                    .eventTime(eventTime)
                    .timestamp(Instant.now())
                    .rawPayload(rawPayload)
                    .build());
        } catch (Exception e) {
            log.warn("XmlV1 parse error for device {}: {}", deviceId, e.getMessage());
            return Optional.empty();
        }
    }

    private String text(Document doc, String tagName) {
        NodeList nl = doc.getElementsByTagName(tagName);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent().trim();
    }

    private Instant parseDateTime(String dt) {
        if (dt == null) return Instant.now();
        try { return OffsetDateTime.parse(dt).toInstant(); } catch (DateTimeParseException e) { return Instant.now(); }
    }
}
