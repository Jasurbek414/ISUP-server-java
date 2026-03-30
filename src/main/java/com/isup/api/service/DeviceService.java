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
    private final com.isup.repository.EventLogRepository eventRepo;
    private final SessionRegistry         sessions;
    private final DeviceCapabilityDetector capabilityDetector;

    public DeviceService(DeviceRepository deviceRepo, ProjectRepository projectRepo,
                         com.isup.repository.EventLogRepository eventRepo,
                         SessionRegistry sessions, DeviceCapabilityDetector capabilityDetector) {
        this.deviceRepo         = deviceRepo;
        this.projectRepo        = projectRepo;
        this.eventRepo          = eventRepo;
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
            String currentIp = existing.get().getDeviceIp();
            // Don't overwrite the real IP with the Docker bridge's source NAT IP
            if (ip.startsWith("172.") || ip.startsWith("10.") && currentIp != null && !currentIp.isEmpty() && !currentIp.startsWith("172.")) {
                ip = currentIp;
            }
            deviceRepo.updateStatus(deviceId, "online", Instant.now(), ip);
            // Ensure deviceIp is also updated for IsapiService calls
            existing.get().setDeviceIp(ip);
            deviceRepo.save(existing.get());
        } else if (allowUnknownDevices) {
            // Auto-register unknown device
            Device d = Device.builder()
                    .deviceId(deviceId)
                    .name(deviceId)
                    .ipAddress(ip)
                    .deviceIp(ip) // Set this too!
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

    /**
     * Sends a remote command to open the door (v5.0 CONTROL_DOOR)
     */
    public void openDoor(String deviceId, int doorNo) {
        sessions.findByDeviceIdAndPort(deviceId, 7660).ifPresent(session -> {
            log.info("Remote Door Control: Sending OPEN command to device {} (Door {})", deviceId, doorNo);
            session.getChannel().writeAndFlush(com.isup.protocol.IsupProtocol.buildV5DoorControl(
                session.getSessionId(), doorNo, "open"
            ));
        });
    }

    public List<com.isup.entity.EventLog> getRecentEvents(String deviceId, int limit) {
        return eventRepo.findByDeviceIdOrderByCreatedAtDesc(deviceId, org.springframework.data.domain.PageRequest.of(0, limit))
                .getContent();
    }

    public List<Device> findAll() {
        return deviceRepo.findAll();
    }

    public Device findById(Long id) {
        return deviceRepo.findById(id).orElseThrow(() -> new com.isup.isapi.IsapiException("Device not found: " + id, 404));
    }

    @Transactional
    public Device create(String deviceId, String name, String location, String model,
                         String password, String devicePassword, String deviceUsername, 
                         String ipAddress, String deviceType, Long projectId) {
        Project project = projectId != null ? projectRepo.findById(projectId).orElse(null) : null;
        Device d = Device.builder()
                .deviceId(deviceId)
                .name(name)
                .location(location)
                .model(model)
                .passwordHash(password)
                .devicePassword(devicePassword)
                .deviceUsername(deviceUsername != null ? deviceUsername : "admin")
                .deviceIp(ipAddress)
                .ipAddress(ipAddress)
                .deviceType(deviceType != null ? deviceType : "face_terminal")
                .project(project)
                .status("offline")
                .build();
        return deviceRepo.save(d);
    }

    @Transactional
    public Device update(Long id, String name, String location, String model, 
                         String password, String devicePassword, String deviceUsername, 
                         String deviceIp, Long projectId) {
        Device d = findById(id);
        if (name           != null) d.setName(name);
        if (location       != null) d.setLocation(location);
        if (model          != null) d.setModel(model);
        if (password       != null) d.setPasswordHash(password);
        if (devicePassword != null) d.setDevicePassword(devicePassword);
        if (deviceUsername != null) d.setDeviceUsername(deviceUsername);
        if (deviceIp       != null) {
            d.setDeviceIp(deviceIp);
            d.setIpAddress(deviceIp);
        }
        if (projectId      != null) {
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
