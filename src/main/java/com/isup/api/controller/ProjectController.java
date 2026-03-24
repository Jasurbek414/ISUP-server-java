package com.isup.api.controller;

import com.isup.api.service.ProjectService;
import com.isup.entity.Project;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<Project> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Project get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public Project create(@RequestBody Map<String, Object> body) {
        return service.create(
            (String) body.get("name"),
            (String) body.get("webhookUrl"),
            body.containsKey("retryCount") ? Integer.valueOf(body.get("retryCount").toString()) : null
        );
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return service.update(
            id,
            (String) body.get("name"),
            (String) body.get("webhookUrl"),
            body.containsKey("retryCount") ? Integer.valueOf(body.get("retryCount").toString()) : null,
            body.containsKey("active") ? Boolean.valueOf(body.get("active").toString()) : null
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate-secret")
    public Map<String, String> regenerateSecret(@PathVariable Long id) {
        return Map.of("secretKey", service.regenerateSecret(id));
    }
}
