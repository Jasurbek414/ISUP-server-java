package com.isup.api.controller;

import com.isup.api.service.EventService;
import com.isup.entity.EventLog;
import org.springframework.data.domain.Page;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @GetMapping("/photos/{filename:.+}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        try {
            Path file = Paths.get("storage", "photos").resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/events
     * Supports optional filters: deviceId, eventType, employeeNo, startTime (ISO-8601), endTime (ISO-8601)
     */
    @GetMapping
    public Page<EventLog> list(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "50")  int size,
            @RequestParam(required = false)     String deviceId,
            @RequestParam(required = false)     String eventType,
            @RequestParam(required = false)     String employeeNo,
            @RequestParam(required = false)     String startTime,
            @RequestParam(required = false)     String endTime) {

        Instant from = startTime != null ? Instant.parse(startTime) : null;
        Instant to   = endTime   != null ? Instant.parse(endTime)   : null;

        return service.findFiltered(deviceId, eventType, employeeNo, from, to, page, size);
    }

    @GetMapping("/recent")
    public List<EventLog> recent(@RequestParam(defaultValue = "10") int limit) {
        return service.findRecent(limit);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
            "today", service.countToday()
        );
    }
}
