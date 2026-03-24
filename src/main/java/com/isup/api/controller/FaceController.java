package com.isup.api.controller;

import com.isup.isapi.IsapiService;
import com.isup.isapi.dto.FaceRecord;
import com.isup.repository.EventLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faces")
public class FaceController {

    private final IsapiService isapiService;
    private final EventLogRepository eventRepo;

    public FaceController(IsapiService isapiService, EventLogRepository eventRepo) {
        this.isapiService = isapiService;
        this.eventRepo    = eventRepo;
    }

    /** POST /api/faces/enroll — Enroll face on a specific device */
    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody EnrollRequest req) {
        FaceRecord face = FaceRecord.builder()
                .employeeNo(req.employeeNo())
                .name(req.name())
                .gender(req.gender() != null ? req.gender() : "male")
                .photoBase64(req.photoBase64())
                .build();

        boolean ok = isapiService.enrollFace(req.deviceId(), face);
        if (ok) return ResponseEntity.ok(Map.of("status", "success", "deviceId", req.deviceId()));
        return ResponseEntity.internalServerError().body(Map.of("status", "failed"));
    }

    /** POST /api/faces/enroll-all — Enroll on all online face devices */
    @PostMapping("/enroll-all")
    public ResponseEntity<?> enrollAll(@RequestBody EnrollAllRequest req) {
        FaceRecord face = FaceRecord.builder()
                .employeeNo(req.employeeNo())
                .name(req.name())
                .gender(req.gender() != null ? req.gender() : "male")
                .photoBase64(req.photoBase64())
                .build();

        int count = isapiService.enrollFaceAll(face);
        return ResponseEntity.ok(Map.of("status", "success", "devicesUpdated", count));
    }

    /** DELETE /api/faces/{employeeNo} — Delete from all devices */
    @DeleteMapping("/{employeeNo}")
    public ResponseEntity<?> deleteFace(@PathVariable String employeeNo) {
        int count = isapiService.deleteFaceAll(employeeNo);
        return ResponseEntity.ok(Map.of("status", "success", "devicesUpdated", count));
    }

    /** GET /api/faces?deviceId=xxx — List faces on device (raw ISAPI) */
    @GetMapping
    public ResponseEntity<?> listFaces(@RequestParam(required = false) String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            String result = isapiService.listFaces(deviceId);
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * GET /api/faces/employees — List unique employees from event logs.
     * Used by the frontend faces list view.
     */
    @GetMapping("/employees")
    public ResponseEntity<List<Map<String, Object>>> employees() {
        List<Map<String, Object>> result = eventRepo.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .filter(e -> e.getEmployeeNo() != null)
                .collect(Collectors.toMap(
                        e -> e.getEmployeeNo(),
                        e -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("employeeNo",  e.getEmployeeNo());
                            m.put("name",        e.getEmployeeName() != null ? e.getEmployeeName() : e.getEmployeeNo());
                            m.put("photoBase64", e.getPhotoBase64());
                            m.put("blacklisted", false);
                            return m;
                        },
                        (a, b) -> a,          // keep first occurrence
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    record EnrollRequest(String deviceId, String employeeNo, String name, String gender, String photoBase64) {}
    record EnrollAllRequest(String employeeNo, String name, String gender, String photoBase64) {}
}
