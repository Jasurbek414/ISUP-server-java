package com.isup.api.controller;

import com.isup.entity.Device;
import com.isup.isapi.IsapiService;
import com.isup.isapi.dto.FaceRecord;
import com.isup.repository.DeviceRepository;
import com.isup.repository.EventLogRepository;
import com.isup.repository.ProjectRepository;
import com.isup.entity.Project;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

    private final ProjectRepository  projectRepo;
    private final DeviceRepository   deviceRepo;
    private final EventLogRepository eventRepo;
    private final IsapiService       isapiService;

    public IntegrationController(ProjectRepository projectRepo, DeviceRepository deviceRepo,
                                 EventLogRepository eventRepo, IsapiService isapiService) {
        this.projectRepo = projectRepo;
        this.deviceRepo  = deviceRepo;
        this.eventRepo   = eventRepo;
        this.isapiService = isapiService;
    }

    private Project authProject(String apiKey) {
        return projectRepo.findAll().stream()
            .filter(p -> p.getSecretKey().equals(apiKey) && p.isActive())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Invalid API key"));
    }

    @GetMapping("/docs")
    public Map<String, Object> docs() {
        return Map.of(
            "title", "ISUP Server Integration API",
            "version", "2.0",
            "auth", Map.of("type", "X-API-Key", "header", "X-API-Key", "description", "Project secret key"),
            "baseUrl", "/api/integration",
            "endpoints", List.of(
                Map.of("method", "GET",  "path", "/devices",              "description", "List project devices"),
                Map.of("method", "POST", "path", "/face/enroll",          "description", "Enroll face"),
                Map.of("method", "POST", "path", "/door/open",            "description", "Open door"),
                Map.of("method", "GET",  "path", "/events",               "description", "Get events"),
                Map.of("method", "GET",  "path", "/stream/{deviceId}",    "description", "Get RTSP URL")
            )
        );
    }

    @PostMapping("/webhook/verify")
    public ResponseEntity<?> verifyWebhook(@RequestHeader("X-API-Key") String apiKey,
                                           @RequestBody Map<String, String> body) {
        try {
            Project p = authProject(apiKey);
            String signature = body.get("signature");
            String payload   = body.get("payload");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(p.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            boolean valid = expected.equals(signature);
            return ResponseEntity.ok(Map.of("valid", valid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/devices")
    public ResponseEntity<?> devices(@RequestHeader("X-API-Key") String apiKey) {
        try {
            authProject(apiKey);
            List<Device> devs = deviceRepo.findAll();
            return ResponseEntity.ok(devs);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/face/enroll")
    public ResponseEntity<?> enrollFace(@RequestHeader("X-API-Key") String apiKey,
                                        @RequestBody Map<String, Object> body) {
        try {
            authProject(apiKey);
            String deviceId = (String) body.getOrDefault("deviceId", "");
            FaceRecord fr = FaceRecord.builder()
                .employeeNo((String) body.get("employeeNo"))
                .name((String) body.get("name"))
                .gender((String) body.getOrDefault("gender", "male"))
                .photoBase64((String) body.get("photoBase64"))
                .build();
            if (deviceId.isBlank()) {
                int count = isapiService.enrollFaceAll(fr);
                return ResponseEntity.ok(Map.of("status", "success", "devicesUpdated", count));
            }
            boolean ok = isapiService.enrollFace(deviceId, fr);
            return ResponseEntity.ok(Map.of("status", ok ? "success" : "failed"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/door/open")
    public ResponseEntity<?> openDoor(@RequestHeader("X-API-Key") String apiKey,
                                      @RequestBody Map<String, Object> body) {
        try {
            authProject(apiKey);
            String deviceId = (String) body.get("deviceId");
            int doorNo = body.containsKey("doorNo") ? ((Number) body.get("doorNo")).intValue() : 1;
            int seconds = body.containsKey("durationSeconds") ? ((Number) body.get("durationSeconds")).intValue() : 0;
            boolean ok = seconds > 0
                ? isapiService.openDoorTimed(deviceId, doorNo, seconds)
                : isapiService.openDoor(deviceId, doorNo);
            return ResponseEntity.ok(Map.of("status", ok ? "success" : "failed"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/events")
    public ResponseEntity<?> events(@RequestHeader("X-API-Key") String apiKey,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int limit,
                                    @RequestParam(required = false) String employeeNo,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to) {
        try {
            authProject(apiKey);
            Instant fromI = from != null ? Instant.parse(from) : null;
            Instant toI   = to   != null ? Instant.parse(to)   : null;
            var result = eventRepo.findFiltered(null, null, employeeNo, fromI, toI, PageRequest.of(page, limit));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stream/{deviceId}")
    public ResponseEntity<?> stream(@RequestHeader("X-API-Key") String apiKey,
                                    @PathVariable String deviceId) {
        try {
            authProject(apiKey);
            IsapiService.StreamInfo info = isapiService.getStreamInfo(deviceId);
            return ResponseEntity.ok(Map.of("rtspUrl", info.getRtspUrl(),
                                            "rtspSubUrl", info.getRtspSubUrl() != null ? info.getRtspSubUrl() : ""));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
