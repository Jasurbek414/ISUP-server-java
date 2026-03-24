package com.isup.api.controller;

import com.isup.discovery.SadpDiscovery;
import com.isup.entity.DiscoveredDevice;
import com.isup.repository.DiscoveredDeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final SadpDiscovery               sadp;
    private final DiscoveredDeviceRepository  repo;

    public DiscoveryController(SadpDiscovery sadp, DiscoveredDeviceRepository repo) {
        this.sadp = sadp;
        this.repo = repo;
    }

    @GetMapping
    public List<DiscoveredDevice> list() {
        return repo.findAllByClaimedFalseOrderByLastSeenDesc();
    }

    /** Alias: GET /api/discovery/devices — same as GET /api/discovery */
    @GetMapping("/devices")
    public List<DiscoveredDevice> listDevices() {
        return repo.findAllByClaimedFalseOrderByLastSeenDesc();
    }

    @GetMapping("/all")
    public List<DiscoveredDevice> listAll() {
        return repo.findAll();
    }

    @PostMapping("/scan")
    public ResponseEntity<String> scan() {
        sadp.broadcast();
        return ResponseEntity.ok("Scan started");
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<Void> claim(@PathVariable Long id) {
        repo.findById(id).ifPresent(d -> {
            d.setClaimed(true);
            repo.save(d);
        });
        return ResponseEntity.ok().build();
    }
}
