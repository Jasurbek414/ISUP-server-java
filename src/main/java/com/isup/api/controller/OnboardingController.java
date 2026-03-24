package com.isup.api.controller;

import com.isup.repository.DeviceRepository;
import com.isup.repository.EventLogRepository;
import com.isup.repository.FaceLibraryRepository;
import com.isup.repository.FaceRecordRepository;
import com.isup.repository.ProjectRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final ProjectRepository     projectRepo;
    private final DeviceRepository      deviceRepo;
    private final FaceLibraryRepository libraryRepo;
    private final FaceRecordRepository  recordRepo;
    private final EventLogRepository    eventRepo;

    public OnboardingController(ProjectRepository projectRepo, DeviceRepository deviceRepo,
                                FaceLibraryRepository libraryRepo, FaceRecordRepository recordRepo,
                                EventLogRepository eventRepo) {
        this.projectRepo = projectRepo;
        this.deviceRepo  = deviceRepo;
        this.libraryRepo = libraryRepo;
        this.recordRepo  = recordRepo;
        this.eventRepo   = eventRepo;
    }

    @GetMapping("/steps")
    public Map<String, Object> steps() {
        boolean hasProject = projectRepo.count() > 0;
        boolean hasDevice  = deviceRepo.count() > 0;
        boolean hasLibrary = libraryRepo.count() > 0;
        boolean hasPerson  = recordRepo.count() > 0;
        boolean hasUpload  = recordRepo.countByUploadStatus("uploaded") > 0;
        boolean hasEvent   = eventRepo.count() > 0;

        List<Map<String, Object>> stepList = List.of(
            step(1, "Loyiha yaratish",   hasProject, "/projects"),
            step(2, "Qurilma qo'shish",  hasDevice,  "/devices"),
            step(3, "Fayl yaratish",     hasLibrary, "/libraries"),
            step(4, "Odam qo'shish",     hasPerson,  "/libraries"),
            step(5, "Qurilmaga yuklash", hasUpload,  "/libraries"),
            step(6, "Test qilish",       hasEvent,   "/events")
        );

        long done = stepList.stream().filter(s -> Boolean.TRUE.equals(s.get("done"))).count();
        return Map.of("steps", stepList, "completedCount", done, "totalCount", stepList.size());
    }

    private Map<String, Object> step(int n, String title, boolean done, String action) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("step", n);
        m.put("title", title);
        m.put("done", done);
        m.put("action", action);
        return m;
    }
}
