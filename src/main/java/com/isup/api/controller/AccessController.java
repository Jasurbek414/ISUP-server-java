package com.isup.api.controller;

import com.isup.entity.AccessRule;
import com.isup.isapi.IsapiService;
import com.isup.repository.AccessRuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final IsapiService         isapiService;
    private final AccessRuleRepository ruleRepo;

    public AccessController(IsapiService isapiService, AccessRuleRepository ruleRepo) {
        this.isapiService = isapiService;
        this.ruleRepo     = ruleRepo;
    }

    /** POST /api/access/door/open */
    @PostMapping("/door/open")
    public ResponseEntity<?> openDoor(@RequestBody DoorRequest req) {
        boolean ok = isapiService.openDoor(req.deviceId(), req.doorNo() > 0 ? req.doorNo() : 1);
        return ok ? ResponseEntity.ok(Map.of("status", "opened"))
                  : ResponseEntity.internalServerError().body(Map.of("status", "failed"));
    }

    /** POST /api/access/door/close */
    @PostMapping("/door/close")
    public ResponseEntity<?> closeDoor(@RequestBody DoorRequest req) {
        boolean ok = isapiService.closeDoor(req.deviceId(), req.doorNo() > 0 ? req.doorNo() : 1);
        return ok ? ResponseEntity.ok(Map.of("status", "closed"))
                  : ResponseEntity.internalServerError().body(Map.of("status", "failed"));
    }

    /** POST /api/access/door/open-timed */
    @PostMapping("/door/open-timed")
    public ResponseEntity<?> openTimed(@RequestBody TimedDoorRequest req) {
        int secs = req.seconds() > 0 ? req.seconds() : 5;
        boolean ok = isapiService.openDoorTimed(req.deviceId(), req.doorNo() > 0 ? req.doorNo() : 1, secs);
        return ok ? ResponseEntity.ok(Map.of("status", "opened", "closeAfterSeconds", secs))
                  : ResponseEntity.internalServerError().body(Map.of("status", "failed"));
    }

    /** POST /api/access/blacklist */
    @PostMapping("/blacklist")
    public ResponseEntity<?> addBlacklist(@RequestBody BlacklistRequest req) {
        // Save rule to DB
        AccessRule rule = AccessRule.builder()
                .employeeNo(req.employeeNo())
                .ruleType("blacklist")
                .deviceIds(req.deviceIds() != null ? String.join(",", req.deviceIds()) : null)
                .reason(req.reason())
                .createdAt(Instant.now())
                .build();
        ruleRepo.save(rule);

        // Apply on devices
        int count = isapiService.addToBlacklist(req.employeeNo(), req.deviceIds());
        return ResponseEntity.ok(Map.of("status", "success", "devicesUpdated", count));
    }

    /** DELETE /api/access/blacklist/{employeeNo} */
    @DeleteMapping("/blacklist/{employeeNo}")
    public ResponseEntity<?> removeBlacklist(@PathVariable String employeeNo) {
        ruleRepo.deleteByEmployeeNo(employeeNo);
        int count = isapiService.removeFromBlacklist(employeeNo);
        return ResponseEntity.ok(Map.of("status", "removed", "devicesUpdated", count));
    }

    /** GET /api/access/blacklist */
    @GetMapping("/blacklist")
    public ResponseEntity<?> getBlacklist() {
        return ResponseEntity.ok(ruleRepo.findByRuleType("blacklist"));
    }

    /** POST /api/access/whitelist */
    @PostMapping("/whitelist")
    public ResponseEntity<?> addWhitelist(@RequestBody WhitelistRequest req) {
        AccessRule rule = AccessRule.builder()
                .employeeNo(req.employeeNo())
                .ruleType("whitelist")
                .deviceIds(req.deviceIds() != null ? String.join(",", req.deviceIds()) : null)
                .validFrom(req.validFrom() != null ? Instant.parse(req.validFrom()) : null)
                .validTo(req.validTo() != null ? Instant.parse(req.validTo()) : null)
                .createdAt(Instant.now())
                .build();
        ruleRepo.save(rule);
        return ResponseEntity.ok(Map.of("status", "success", "id", rule.getId()));
    }

    /** GET /api/access/whitelist */
    @GetMapping("/whitelist")
    public ResponseEntity<?> getWhitelist() {
        return ResponseEntity.ok(ruleRepo.findByRuleType("whitelist"));
    }

    record DoorRequest(String deviceId, int doorNo) {}
    record TimedDoorRequest(String deviceId, int doorNo, int seconds) {}
    record BlacklistRequest(String employeeNo, List<String> deviceIds, String reason) {}
    record WhitelistRequest(String employeeNo, List<String> deviceIds, String validFrom, String validTo) {}
}
