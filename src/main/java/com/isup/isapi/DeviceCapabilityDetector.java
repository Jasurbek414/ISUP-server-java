package com.isup.isapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isup.entity.Device;
import com.isup.isapi.dto.DeviceInfo;
import com.isup.isapi.modules.DeviceModule;
import com.isup.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Detects Hikvision device capabilities via ISAPI after ISUP login.
 * Stores capabilities as JSON array in device.capabilities field.
 */
@Component
public class DeviceCapabilityDetector {

    private static final Logger log = LoggerFactory.getLogger(DeviceCapabilityDetector.class);

    private final DeviceRepository deviceRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeviceCapabilityDetector(DeviceRepository deviceRepo) {
        this.deviceRepo = deviceRepo;
    }

    /**
     * Called after device connects via ISUP. Detects and saves capabilities.
     * Non-blocking: uses virtual thread.
     */
    public void detectAsync(String deviceId, String ip) {
        Thread t = new Thread(() -> detect(deviceId, ip));
        t.setName("cap-detect-" + deviceId);
        t.setDaemon(true);
        t.start();
    }

    @org.springframework.transaction.annotation.Transactional
    private void detect(String deviceId, String ip) {
        try {
            Device device = deviceRepo.findByDeviceId(deviceId).orElse(null);
            if (device == null) {
                log.debug("Device not found for capability detection: {}", deviceId);
                return;
            }

            // Use stored credentials or defaults
            String username = device.getDeviceUsername() != null ? device.getDeviceUsername() : "admin";
            String password = device.getDevicePassword() != null ? device.getDevicePassword() : "";
            int    port     = device.getDevicePort() != null ? device.getDevicePort() : 80;
            boolean ssl     = device.getUseHttps() != null && device.getUseHttps();

            String effectiveIp = (ip != null && !ip.isBlank()) ? ip : device.getDeviceIp();
            if (effectiveIp == null || effectiveIp.isBlank()) {
                log.debug("No IP for capability detection on {}", deviceId);
                return;
            }

            IsapiClient client = new IsapiClient(effectiveIp, port, ssl, username, password);
            DeviceModule dm = new DeviceModule(client);
            DeviceInfo info = dm.getDeviceInfo();

            String detectedModel    = device.getModel();
            String detectedFirmware = device.getFirmware();

            if (info != null) {
                if (info.getModel() != null && !info.getModel().isBlank()) {
                    detectedModel = info.getModel();
                }
                if (info.getFirmwareVersion() != null) {
                    detectedFirmware = info.getFirmwareVersion();
                }
            }

            // Determine capabilities from model name
            String model = detectedModel != null ? detectedModel.toUpperCase() : "";
            List<String> caps = new ArrayList<>();

            if (model.contains("K1TA")) {
                caps.addAll(List.of("face", "door", "attendance", "temperature"));
            } else if (model.contains("K1T") || model.contains("K2T") || model.contains("K3T")) {
                caps.addAll(List.of("face", "door", "attendance", "rtsp"));
            } else if (model.contains("K1")) {
                caps.addAll(List.of("door", "card", "attendance"));
            } else if (model.contains("K2")) {
                caps.addAll(List.of("door", "card", "alarm"));
            } else if (model.contains("IDS-2SK")) {
                caps.addAll(List.of("rtsp", "face", "motion", "panoramic"));
            } else if (model.contains("IDS-2CD")) {
                caps.addAll(List.of("rtsp", "face", "motion", "line_crossing"));
            } else if (model.contains("2DE") || model.contains("2DF")) {
                caps.addAll(List.of("rtsp", "motion", "ptz", "line_crossing"));
            } else if (model.contains("2TD")) {
                caps.addAll(List.of("rtsp", "thermal", "motion"));
            } else if (model.contains("2CD")) {
                caps.addAll(List.of("rtsp", "motion", "line_crossing"));
            } else if (model.matches(".*DS-[79].*") || model.contains("DS-7") || model.contains("DS-9")) {
                caps.addAll(List.of("nvr", "recording", "playback", "rtsp"));
            } else if (model.contains("DS-8")) {
                caps.addAll(List.of("dvr", "recording", "playback", "rtsp"));
            } else if (model.contains("DS-6")) {
                caps.addAll(List.of("rtsp"));
            } else {
                caps.add("rtsp");
                if (probeEndpoint(client, "/ISAPI/Intelligent/FDLib/capabilities")) caps.add("face");
                if (probeEndpoint(client, "/ISAPI/AccessControl/capabilities")) caps.add("door");
                if (probeEndpoint(client, "/ISAPI/PTZCtrl/channels/1/capabilities")) caps.add("ptz");
                caps.add("attendance");
            }

            String capsJson = mapper.writeValueAsString(caps);
            // Use targeted update — does NOT touch status/lastSeen to avoid race condition
            deviceRepo.updateCapabilities(deviceId, detectedModel, detectedFirmware, capsJson);
            log.info("Device {} capabilities: {}", deviceId, caps);
        } catch (Exception e) {
            log.debug("Capability detection skipped for {}: {}", deviceId, e.getMessage());
        }
    }

    private boolean probeEndpoint(IsapiClient client, String path) {
        try {
            client.get(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
