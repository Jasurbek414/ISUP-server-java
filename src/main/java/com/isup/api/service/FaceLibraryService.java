package com.isup.api.service;

import com.isup.entity.FaceLibrary;
import com.isup.entity.FaceRecord;
import com.isup.entity.Device;
import com.isup.isapi.IsapiService;
import com.isup.repository.DeviceRepository;
import com.isup.repository.FaceLibraryRepository;
import com.isup.repository.FaceRecordRepository;
import com.isup.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class FaceLibraryService {
    private static final Logger log = LoggerFactory.getLogger(FaceLibraryService.class);

    private final FaceLibraryRepository libraryRepo;
    private final FaceRecordRepository  recordRepo;
    private final DeviceRepository      deviceRepo;
    private final IsapiService          isapiService;
    private final ProjectRepository     projectRepo;

    public FaceLibraryService(FaceLibraryRepository libraryRepo,
                              FaceRecordRepository recordRepo,
                              DeviceRepository deviceRepo,
                              IsapiService isapiService,
                              ProjectRepository projectRepo) {
        this.libraryRepo = libraryRepo;
        this.recordRepo  = recordRepo;
        this.deviceRepo  = deviceRepo;
        this.isapiService = isapiService;
        this.projectRepo = projectRepo;
    }

    public List<FaceLibrary> findAll() { return libraryRepo.findAllByOrderByCreatedAtDesc(); }

    public FaceLibrary findById(Long id) {
        return libraryRepo.findById(id).orElseThrow(() -> new RuntimeException("Library not found: " + id));
    }

    @Transactional
    public FaceLibrary create(CreateLibraryReq req) {
        FaceLibrary lib = FaceLibrary.builder()
            .name(req.name())
            .description(req.description())
            .type(req.type() != null ? req.type() : "whitelist")
            .deviceIds(req.deviceIds())
            .build();
        if (req.projectId() != null) {
            projectRepo.findById(req.projectId()).ifPresent(lib::setProject);
        }
        return libraryRepo.save(lib);
    }

    @Transactional
    public FaceLibrary update(Long id, CreateLibraryReq req) {
        FaceLibrary lib = findById(id);
        if (req.name() != null) lib.setName(req.name());
        if (req.description() != null) lib.setDescription(req.description());
        if (req.type() != null) lib.setType(req.type());
        if (req.deviceIds() != null) lib.setDeviceIds(req.deviceIds());
        lib.setUpdatedAt(Instant.now());
        return libraryRepo.save(lib);
    }

    @Transactional
    public void delete(Long id) {
        recordRepo.deleteByLibraryId(id);
        libraryRepo.deleteById(id);
    }

    public List<FaceRecord> getPersons(Long libraryId) {
        return recordRepo.findByLibraryIdOrderByCreatedAtDesc(libraryId);
    }

    @Transactional
    public FaceRecord addPerson(Long libraryId, PersonReq req) {
        FaceLibrary lib = findById(libraryId);
        FaceRecord rec = FaceRecord.builder()
            .library(lib)
            .employeeNo(req.employeeNo())
            .name(req.name())
            .gender(req.gender() != null ? req.gender() : "male")
            .phone(req.phone())
            .department(req.department())
            .position(req.position())
            .photoBase64(req.photoBase64())
            .uploadStatus("pending")
            .build();
        if (req.validFrom() != null) rec.setValidFrom(Instant.parse(req.validFrom()));
        if (req.validTo() != null)   rec.setValidTo(Instant.parse(req.validTo()));
        FaceRecord saved = recordRepo.save(rec);
        libraryRepo.refreshFaceCount(libraryId);
        return saved;
    }

    @Transactional
    public FaceRecord updatePerson(Long libraryId, Long personId, PersonReq req) {
        FaceRecord rec = recordRepo.findById(personId)
            .orElseThrow(() -> new RuntimeException("Person not found"));
        if (req.name() != null)        rec.setName(req.name());
        if (req.gender() != null)      rec.setGender(req.gender());
        if (req.phone() != null)       rec.setPhone(req.phone());
        if (req.department() != null)  rec.setDepartment(req.department());
        if (req.position() != null)    rec.setPosition(req.position());
        if (req.photoBase64() != null) rec.setPhotoBase64(req.photoBase64());
        if (req.validFrom() != null)   rec.setValidFrom(Instant.parse(req.validFrom()));
        if (req.validTo() != null)     rec.setValidTo(Instant.parse(req.validTo()));
        rec.setUpdatedAt(Instant.now());
        return recordRepo.save(rec);
    }

    @Transactional
    public void deletePerson(Long libraryId, Long personId) {
        recordRepo.deleteById(personId);
        libraryRepo.refreshFaceCount(libraryId);
    }

    /** Upload all persons in library to a specific device */
    @Transactional
    public Map<String, Object> uploadToDevice(Long libraryId, String deviceId) {
        List<FaceRecord> persons = recordRepo.findByLibraryIdOrderByCreatedAtDesc(libraryId);
        int ok = 0, fail = 0;
        for (FaceRecord p : persons) {
            try {
                com.isup.isapi.dto.FaceRecord fr = com.isup.isapi.dto.FaceRecord.builder()
                    .employeeNo(p.getEmployeeNo()).name(p.getName())
                    .gender(p.getGender()).photoBase64(p.getPhotoBase64())
                    .build();
                boolean success = isapiService.enrollFace(deviceId, fr);
                p.setUploadStatus(success ? "uploaded" : "failed");
                if (success) ok++; else fail++;
            } catch (Exception e) {
                p.setUploadStatus("failed");
                fail++;
                log.warn("Upload failed person={} device={}: {}", p.getEmployeeNo(), deviceId, e.getMessage());
            }
            recordRepo.save(p);
        }
        return Map.of("uploaded", ok, "failed", fail, "total", persons.size(), "deviceId", deviceId);
    }

    /** Upload all persons to all online devices */
    @Transactional
    public Map<String, Object> uploadToAll(Long libraryId) {
        List<Device> online = deviceRepo.findOnlineDevices();
        int totalOk = 0, totalFail = 0;
        for (Device dev : online) {
            try {
                Map<String, Object> r = uploadToDevice(libraryId, dev.getDeviceId());
                totalOk   += (int) r.getOrDefault("uploaded", 0);
                totalFail += (int) r.getOrDefault("failed", 0);
            } catch (Exception e) {
                log.warn("Upload to device {} failed: {}", dev.getDeviceId(), e.getMessage());
            }
        }
        return Map.of("uploaded", totalOk, "failed", totalFail, "devices", online.size());
    }

    /** Upload single person to selected devices */
    @Transactional
    public Map<String, Object> uploadPerson(Long libraryId, Long personId, List<String> deviceIds) {
        FaceRecord p = recordRepo.findById(personId)
            .orElseThrow(() -> new RuntimeException("Person not found"));
        int ok = 0, fail = 0;
        List<String> targets = (deviceIds != null && !deviceIds.isEmpty())
            ? deviceIds
            : deviceRepo.findOnlineDevices().stream().map(Device::getDeviceId).toList();
        for (String did : targets) {
            try {
                com.isup.isapi.dto.FaceRecord fr = com.isup.isapi.dto.FaceRecord.builder()
                    .employeeNo(p.getEmployeeNo()).name(p.getName())
                    .gender(p.getGender()).photoBase64(p.getPhotoBase64())
                    .build();
                boolean success = isapiService.enrollFace(did, fr);
                if (success) ok++; else fail++;
            } catch (Exception e) {
                fail++;
            }
        }
        p.setUploadStatus(ok > 0 ? "uploaded" : "failed");
        recordRepo.save(p);
        return Map.of("uploaded", ok, "failed", fail, "total", targets.size());
    }

    public record CreateLibraryReq(String name, String description, String type, String deviceIds, Long projectId) {}
    public record PersonReq(String employeeNo, String name, String gender, String phone,
                            String department, String position, String photoBase64,
                            String photoUrl, String validFrom, String validTo) {}
}
