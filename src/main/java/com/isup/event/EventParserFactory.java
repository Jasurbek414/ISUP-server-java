package com.isup.event;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Selects the right EventParser based on raw payload content.
 * Priority: XmlV2 → XmlV1 → JSON
 */
@Component
public class EventParserFactory {

    private final List<EventParser> parsers;

    public EventParserFactory(XmlV2EventParser xmlV2, XmlV1EventParser xmlV1, JsonEventParser json) {
        this.parsers = List.of(xmlV2, xmlV1, json);
    }

    public Optional<AttendanceEvent> parse(String deviceId, String rawPayload) {
        for (EventParser parser : parsers) {
            if (parser.supports(rawPayload)) {
                return parser.parse(deviceId, rawPayload);
            }
        }
        return Optional.empty();
    }
}
