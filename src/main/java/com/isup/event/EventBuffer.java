package com.isup.event;

import com.isup.api.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class EventBuffer {

    private static final Logger log = LoggerFactory.getLogger(EventBuffer.class);

    @Value("${isup.event.buffer.max-size:10000}")
    private int maxSize;

    private final ConcurrentLinkedQueue<AttendanceEvent> buffer = new ConcurrentLinkedQueue<>();

    // Lazy to avoid circular dependency with EventService
    private final EventService eventService;

    public EventBuffer(@Lazy EventService eventService) {
        this.eventService = eventService;
    }

    public void add(AttendanceEvent event) {
        if (buffer.size() >= maxSize) {
            log.warn("EventBuffer full ({} events), dropping oldest event", maxSize);
            buffer.poll(); // drop oldest
        }
        buffer.offer(event);
    }

    public List<AttendanceEvent> drain() {
        List<AttendanceEvent> events = new ArrayList<>();
        AttendanceEvent e;
        while ((e = buffer.poll()) != null) {
            events.add(e);
        }
        return events;
    }

    public int size() {
        return buffer.size();
    }

    @Scheduled(fixedDelay = 30_000L)
    public void flushBuffer() {
        if (buffer.isEmpty()) return;

        int count = buffer.size();
        log.info("Flushing {} buffered events to DB...", count);
        List<AttendanceEvent> events = drain();
        int flushed = 0;
        for (AttendanceEvent event : events) {
            try {
                eventService.saveAndDispatch(event);
                flushed++;
            } catch (Exception ex) {
                log.warn("Could not flush buffered event, re-buffering: {}", ex.getMessage());
                buffer.offer(event); // put it back
            }
        }
        if (flushed > 0) {
            log.info("Flushed {} buffered events to DB", flushed);
        }
    }
}
