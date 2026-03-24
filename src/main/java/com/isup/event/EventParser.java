package com.isup.event;

import java.util.Optional;

public interface EventParser {
    /**
     * Returns true if this parser can handle the given raw payload.
     */
    boolean supports(String rawPayload);

    /**
     * Parse raw payload into an AttendanceEvent.
     * Returns empty if parsing fails.
     */
    Optional<AttendanceEvent> parse(String deviceId, String rawPayload);
}
