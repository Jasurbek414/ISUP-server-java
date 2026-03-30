package com.isup.api.controller;

import com.isup.api.service.DeviceService;
import com.isup.entity.Device;
import com.isup.session.DeviceSession;
import com.isup.session.SessionRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService    service;
    private final SessionRegistry  sessions;

    public DeviceController(DeviceService service, SessionRegistry sessions) {
        this.service  = service;
        this.sessions = sessions;
    }

    @GetMapping
    public List<Device> list() {
        return service.findAll();
    }

    @GetMapping("/online")
    public Map<String, Object> online() {
        return Map.of(
            "count",   sessions.onlineCount(),
            "devices", sessions.getAllSessions().stream()
                .filter(DeviceSession::isActive)
                .map(s -> Map.of(
                    "deviceId",  s.getDeviceId(),
                    "ip",        s.getIpAddress() != null ? s.getIpAddress() : "",
                    "connectedAt", s.getConnectedAt().toString(),
                    "lastSeen",  s.getLastSeen().toString()
                )).toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        try {
            Long numericId = Long.parseLong(id);
            return ResponseEntity.ok(service.findById(numericId));
        } catch (NumberFormatException e) {
            return service.findAll().stream()
                    .filter(d -> id.equals(d.getDeviceId()))
                    .findFirst()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String deviceId = (String) body.get("deviceId");
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "deviceId is required and cannot be empty"));
        }
        Device created = service.create(
            deviceId,
            (String) body.get("name"),
            (String) body.get("location"),
            (String) body.get("model"),
            (String) body.get("password"),
            (String) body.get("devicePassword"),
            (String) body.get("deviceUsername"),
            (String) body.get("deviceIp"),
            (String) body.get("deviceType"),
            parseLong(body.get("projectId")),
            parseInt(body.get("devicePort")),
            body.containsKey("useHttps") ? (Boolean) body.get("useHttps") : null
        );
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public Device update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return service.update(
            id,
            (String) body.get("name"),
            (String) body.get("location"),
            (String) body.get("model"),
            (String) body.get("password"),
            (String) body.get("devicePassword"),
            (String) body.get("deviceUsername"),
            (String) body.get("deviceIp"),
            parseLong(body.get("projectId")),
            parseInt(body.get("devicePort")),
            body.containsKey("useHttps") ? (Boolean) body.get("useHttps") : null
        );
    }

    private Long parseLong(Object obj) {
        if (obj == null || obj.toString().isBlank()) return null;
        try { return Long.valueOf(obj.toString()); } catch (Exception e) { return null; }
    }

    private Integer parseInt(Object obj) {
        if (obj == null || obj.toString().isBlank()) return null;
        try { return Integer.valueOf(obj.toString()); } catch (Exception e) { return null; }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deviceId}/disconnect")
    public ResponseEntity<Void> disconnect(@PathVariable String deviceId) {
        sessions.findByDeviceId(deviceId).ifPresent(DeviceSession::close);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/open")
    public ResponseEntity<?> open(@PathVariable String deviceId, @RequestParam(defaultValue = "1") int door) {
        service.openDoor(deviceId, door);
        return ResponseEntity.ok(Map.of("message", "Open command sent to " + deviceId, "door", door));
    }

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of("version", "v1.1.4-STABLE", "status", "active", "timestamp", String.valueOf(System.currentTimeMillis()));
    }
}
