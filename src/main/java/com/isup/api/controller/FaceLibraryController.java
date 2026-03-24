package com.isup.api.controller;

import com.isup.api.service.FaceLibraryService;
import com.isup.api.service.FaceLibraryService.CreateLibraryReq;
import com.isup.api.service.FaceLibraryService.PersonReq;
import com.isup.entity.FaceLibrary;
import com.isup.entity.FaceRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/libraries")
public class FaceLibraryController {

    private final FaceLibraryService service;

    public FaceLibraryController(FaceLibraryService service) {
        this.service = service;
    }

    @GetMapping
    public List<FaceLibrary> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public FaceLibrary get(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public FaceLibrary create(@RequestBody CreateLibraryReq req) { return service.create(req); }

    @PutMapping("/{id}")
    public FaceLibrary update(@PathVariable Long id, @RequestBody CreateLibraryReq req) { return service.update(id, req); }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @GetMapping("/{id}/persons")
    public List<FaceRecord> persons(@PathVariable Long id) { return service.getPersons(id); }

    @PostMapping("/{id}/persons")
    public FaceRecord addPerson(@PathVariable Long id, @RequestBody PersonReq req) { return service.addPerson(id, req); }

    @PutMapping("/{id}/persons/{personId}")
    public FaceRecord updatePerson(@PathVariable Long id, @PathVariable Long personId, @RequestBody PersonReq req) {
        return service.updatePerson(id, personId, req);
    }

    @DeleteMapping("/{id}/persons/{personId}")
    public ResponseEntity<?> deletePerson(@PathVariable Long id, @PathVariable Long personId) {
        service.deletePerson(id, personId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PostMapping("/{id}/upload-to-device")
    public Map<String, Object> uploadToDevice(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.uploadToDevice(id, body.get("deviceId"));
    }

    @PostMapping("/{id}/upload-to-all")
    public Map<String, Object> uploadToAll(@PathVariable Long id) {
        return service.uploadToAll(id);
    }

    @PostMapping("/{id}/persons/{personId}/upload")
    public Map<String, Object> uploadPerson(@PathVariable Long id, @PathVariable Long personId,
                                            @RequestBody Map<String, List<String>> body) {
        return service.uploadPerson(id, personId, body.get("deviceIds"));
    }
}
