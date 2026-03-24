package com.isup.api.service;

import com.isup.entity.Device;
import com.isup.entity.Project;
import com.isup.isapi.DeviceCapabilityDetector;
import com.isup.repository.DeviceRepository;
import com.isup.repository.ProjectRepository;
import com.isup.session.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    @Value("${isup.allow-unknown-devices:true}")
    private boolean allowUnknownDevices;

    private final DeviceRepository        deviceRepo;
    private final ProjectRepository       projectRepo;
    private final SessionRegistry         sessions;
    private final DeviceCapabilityDetector capabilityDetector;

    public DeviceService(DeviceRepository deviceRepo, ProjectRepository projectRepo,
                         SessionRegistry sessions, DeviceCapabilityDetector capabilityDetector) {
        this.deviceRepo         = deviceRepo;
        this.projectRepo        = projectRepo;
        this.sessions           = sessions;
        this.capabilityDetector = capabilityDetector;
    }

    /**
     * Returns the device password for HMAC computation.
     * Empty string for unknown devices (if allowUnknownDevices=true).
     */
    public String getDevicePassword(String deviceId) {
        Optional<Device> device = deviceRepo.findByDeviceId(deviceId);
        if (device.isPresent()) {
            String pwd = device.get().getPasswordHash();
            return pwd == null ? "" : pwd;
        }
        if (allowUnknownDevices) {
            log.info("Unknown device {} accepted with empty password", deviceId);
            return "";
        }
        throw new SecurityException("Device not registered: " + deviceId);
    }

    public String getDeviceName(String deviceId) {
        return deviceRepo.findByDeviceId(deviceId).map(Device::getName).orElse(deviceId);
    }

    public String getDeviceModel(String deviceId) {
        return deviceRepo.findByDeviceId(deviceId).map(Device::getModel).orElse(null);
    }

    @Transactional
    public void onDeviceConnected(String deviceId, String ip) {
        Optional<Device> existing = deviceRepo.findByDeviceId(deviceId);
        if (existing.isPresent()) {
            deviceRepo.updateStatus(deviceId, "online", Instant.now(), ip);
        } else if (allowUnknownDevices) {
            // Auto-register unknown device
            Device d = Device.builder()
                    .deviceId(deviceId)
                    .name(deviceId)
                    .ipAddress(ip)
                    .status("online")
                    .lastSeen(Instant.now())
                    .build();
            // Attach to default project if exists
            projectRepo.findAll().stream().findFirst().ifPresent(d::setProject);
            deviceRepo.save(d);
        }
        // Async capability detection via ISAPI
        capabilityDetector.detectAsync(deviceId, ip);
    }

    @Transactional
    public void onDeviceDisconnected(String deviceId) {
        deviceRepo.findByDeviceId(deviceId).ifPresent(d -> {
            d.setStatus("offline");
            d.setLastSeen(Instant.now());
            deviceRepo.save(d);
        });
    }

    public List<Device> findAll() {
        return deviceRepo.findAll();
    }

    public Device findById(Long id) {
        return deviceRepo.findById(id).orElseThrow(() -> new com.isup.isapi.IsapiException("Device not found: " + id, 404));
    }

    @Transactional
    public Device create(String deviceId, String name, String location, String model,
                         String password, String deviceType, Long projectId) {
        Project project = projectId != null ? projectRepo.findById(projectId).orElse(null) : null;
        Device d = Device.builder()
                .deviceId(deviceId)
                .name(name)
                .location(location)
                .model(model)
                .passwordHash(password)
                .deviceType(deviceType != null ? deviceType : "face_terminal")
                .project(project)
                .status("offline")
                .build();
        return deviceRepo.save(d);
    }

    @Transactional
    public Device update(Long id, String name, String location, String model, String password, Long projectId) {
        Device d = findById(id);
        if (name       != null) d.setName(name);
        if (location   != null) d.setLocation(location);
        if (model      != null) d.setModel(model);
        if (password   != null) d.setPasswordHash(password);
        if (projectId  != null) {
            projectRepo.findById(projectId).ifPresent(d::setProject);
        }
        return deviceRepo.save(d);
    }

    @Transactional
    public void delete(Long id) {
        deviceRepo.deleteById(id);
    }

    public int onlineCount() {
        return sessions.onlineCount();
    }

    public List<Device> findOnlineDevices() {
        return deviceRepo.findOnlineDevices();
    }
}
