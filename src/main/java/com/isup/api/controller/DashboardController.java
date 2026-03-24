package com.isup.api.controller;

import com.isup.repository.DeviceRepository;
import com.isup.repository.EventLogRepository;
import com.isup.repository.ProjectRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DeviceRepository    deviceRepo;
    private final EventLogRepository  eventRepo;
    private final ProjectRepository   projectRepo;

    public DashboardController(DeviceRepository deviceRepo,
                               EventLogRepository eventRepo,
                               ProjectRepository projectRepo) {
        this.deviceRepo  = deviceRepo;
        this.eventRepo   = eventRepo;
        this.projectRepo = projectRepo;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        long total      = deviceRepo.count();
        long online     = deviceRepo.findOnlineDevices().size();
        long totalEvt   = eventRepo.count();
        long failed     = eventRepo.countByWebhookStatus("failed");
        long projects   = projectRepo.count();

        return Map.of(
            "totalDevices",   total,
            "onlineDevices",  online,
            "totalEvents",    totalEvt,
            "failedWebhooks", failed,
            "totalProjects",  projects
        );
    }

    @GetMapping("/chart")
    public List<Map<String, Object>> chart() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date  = LocalDate.now(ZoneOffset.UTC).minusDays(i);
            Instant   from  = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant   to    = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            long      count = eventRepo.countByEventTimeBetween(from, to);
            result.add(Map.of("date", date.format(fmt), "count", count));
        }
        return result;
    }
}
