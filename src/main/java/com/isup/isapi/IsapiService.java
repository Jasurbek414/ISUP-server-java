package com.isup.isapi;

import com.isup.entity.Device;
import com.isup.isapi.dto.FaceRecord;
import com.isup.isapi.modules.*;
// New modules (camera, ptz, system, alarm, recording, user) are all in com.isup.isapi.modules.*
import com.isup.repository.DeviceRepository;
import com.isup.session.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * High-level service that creates ISAPI module instances for devices
 * and provides device control operations.
 */
@Service
public class IsapiService {

    private static final Logger log = LoggerFactory.getLogger(IsapiService.class);

    private final DeviceRepository deviceRepo;
    private final SessionRegistry  sessions;
    
    // Command Queue using Intents instead of pre-built ByteBufs
    public record IsupCommandIntent(String path, String method, String body) {}
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.Queue<IsupCommandIntent>> commandQueue = new java.util.concurrent.ConcurrentHashMap<>();

    public IsapiService(DeviceRepository deviceRepo, SessionRegistry sessions) {
        this.deviceRepo = deviceRepo;
        this.sessions   = sessions;
    }

    // ─── Face Management ──────────────────────────────────────────────────────

    /** Enroll face on a specific device. */
    public boolean enrollFace(String deviceId, FaceRecord face) {
        IsapiClient client = clientFor(deviceId);
        return new FaceModule(client).enrollFace(face);
    }

    /** Enroll face on ALL online face-capable devices. */
    public int enrollFaceAll(FaceRecord face) {
        List<Device> online = deviceRepo.findOnlineDevices();
        int success = 0;
        for (Device d : online) {
            if (hasCap(d, "face") && d.getDeviceIp() != null) {
                try {
                    if (new FaceModule(clientFor(d)).enrollFace(face)) success++;
                } catch (Exception e) {
                    log.warn("enrollFaceAll failed for {}: {}", d.getDeviceId(), e.getMessage());
                }
            }
        }
        return success;
    }

    /** Delete face by employeeNo from a specific device. */
    public boolean deleteFace(String deviceId, String employeeNo) {
        IsapiClient client = clientFor(deviceId);
        return new FaceModule(client).deleteFace(employeeNo);
    }

    /** Delete face from ALL online devices. */
    public int deleteFaceAll(String employeeNo) {
        List<Device> online = deviceRepo.findOnlineDevices();
        int success = 0;
        for (Device d : online) {
            if (d.getDeviceIp() != null) {
                try {
                    if (new FaceModule(clientFor(d)).deleteFace(employeeNo)) success++;
                } catch (Exception e) {
                    log.warn("deleteFaceAll failed for {}: {}", d.getDeviceId(), e.getMessage());
                }
            }
        }
        return success;
    }

    /** List faces on device. */
    public String listFaces(String deviceId) {
        return new FaceModule(clientFor(deviceId)).listFaces(200);
    }

    // ─── Door Control ─────────────────────────────────────────────────────────

    public boolean openDoor(String deviceId, int doorNo) {
        return new AccessModule(clientFor(deviceId)).openDoor(doorNo);
    }

    public boolean closeDoor(String deviceId, int doorNo) {
        return new AccessModule(clientFor(deviceId)).closeDoor(doorNo);
    }

    public boolean openDoorTimed(String deviceId, int doorNo, int seconds) {
        return new AccessModule(clientFor(deviceId)).openDoorTimed(doorNo, seconds);
    }

    // ─── Blacklist ────────────────────────────────────────────────────────────

    /** Add to blacklist on specific devices (or all if deviceIds is null). */
    public int addToBlacklist(String employeeNo, List<String> deviceIds) {
        List<Device> targets = deviceIds == null
                ? deviceRepo.findOnlineDevices()
                : deviceIds.stream()
                    .map(id -> deviceRepo.findByDeviceId(id).orElse(null))
                    .filter(d -> d != null && d.getDeviceIp() != null)
                    .toList();
        int success = 0;
        for (Device d : targets) {
            if (d != null && d.getDeviceIp() != null) {
                try {
                    if (new BlacklistModule(clientFor(d)).addToBlacklist(employeeNo, employeeNo)) success++;
                } catch (Exception e) {
                    log.warn("addToBlacklist failed for device {}: {}", d.getDeviceId(), e.getMessage());
                }
            }
        }
        return success;
    }

    public int removeFromBlacklist(String employeeNo) {
        List<Device> online = deviceRepo.findOnlineDevices();
        int success = 0;
        for (Device d : online) {
            if (d.getDeviceIp() != null) {
                try {
                    if (new BlacklistModule(clientFor(d)).removeFromBlacklist(employeeNo)) success++;
                } catch (Exception e) {
                    log.warn("removeFromBlacklist failed for {}: {}", d.getDeviceId(), e.getMessage());
                }
            }
        }
        return success;
    }

    // ─── Stream ──────────────────────────────────────────────────────────────

    public StreamInfo getStreamInfo(String deviceId) {
        Device device = findDevice(deviceId);
        String ip  = device.getDeviceIp();
        if (ip == null || ip.isBlank()) {
            throw new IsapiException("Device has no IP address configured: " + deviceId, 0);
        }
        String usr = device.getDeviceUsername() != null ? device.getDeviceUsername() : "admin";
        String pwd = device.getDevicePassword() != null ? device.getDevicePassword() : "";

        int    port = device.getDevicePort() != null ? device.getDevicePort() : 80;
        boolean ssl = device.getUseHttps()   != null && device.getUseHttps();
        IsapiClient client = new IsapiClient(ip, port, ssl, usr, pwd);
        StreamModule sm    = new StreamModule(client, ip, usr, pwd);

        return StreamInfo.builder()
                .rtspUrl(sm.getMainStreamUrl())
                .rtspSubUrl(sm.getSubStreamUrl())
                .build();
    }

    // ─── Device Capabilities ─────────────────────────────────────────────────

    public Optional<String> getCapabilities(String deviceId) {
        return deviceRepo.findByDeviceId(deviceId).map(Device::getCapabilities);
    }

    // ─── Camera ──────────────────────────────────────────────────────────────

    public String getCameraImageSettings(String deviceId, int channelId) {
        return new CameraModule(clientFor(deviceId)).getImageSettings(channelId);
    }

    public void updateCameraImageSettings(String deviceId, int channelId, String json) {
        new CameraModule(clientFor(deviceId)).putImageSettings(channelId, json);
    }

    public String getStreamingChannels(String deviceId) {
        return new CameraModule(clientFor(deviceId)).getStreamingChannels();
    }

    // ─── PTZ ─────────────────────────────────────────────────────────────────

    public void ptzMove(String deviceId, int channelId, String direction, int speed) {
        new PtzModule(clientFor(deviceId)).continuousMove(channelId, direction, speed);
    }

    public void ptzStop(String deviceId, int channelId) {
        new PtzModule(clientFor(deviceId)).stopMove(channelId);
    }

    public void ptzPresetGoto(String deviceId, int channelId, int presetId) {
        new PtzModule(clientFor(deviceId)).gotoPreset(channelId, presetId);
    }

    public String ptzGetPresets(String deviceId, int channelId) {
        return new PtzModule(clientFor(deviceId)).getPresets(channelId);
    }

    public String ptzGetStatus(String deviceId, int channelId) {
        return new PtzModule(clientFor(deviceId)).getStatus(channelId);
    }

    // ─── System ──────────────────────────────────────────────────────────────

    public String getDeviceSystemInfo(String deviceId) {
        return new SystemModule(clientFor(deviceId)).getDeviceInfo();
    }

    public String getDeviceStatus(String deviceId) {
        return new SystemModule(clientFor(deviceId)).getSystemStatus();
    }

    public String getNetworkConfig(String deviceId) {
        return new SystemModule(clientFor(deviceId)).getNetworkInterfaces();
    }

    public void rebootDevice(String deviceId) {
        if (isupExecute(deviceId, "/ISAPI/System/reboot", "POST", null)) return;
        new SystemModule(clientFor(deviceId)).reboot();
    }

    public String getStorageInfo(String deviceId) {
        return new SystemModule(clientFor(deviceId)).getStorageInfo();
    }

    // ─── Alarm ───────────────────────────────────────────────────────────────

    public String getAlarmInputs(String deviceId) {
        return new AlarmModule(clientFor(deviceId)).getAlarmInputs();
    }

    public void triggerAlarmOutput(String deviceId, int outputId) {
        new AlarmModule(clientFor(deviceId)).triggerAlarmOutput(outputId);
    }

    public String getMotionDetection(String deviceId, int channelId) {
        return new AlarmModule(clientFor(deviceId)).getMotionDetection(channelId);
    }

    // ─── Recording ───────────────────────────────────────────────────────────

    public String searchRecordings(String deviceId, String channelId, String start, String end) {
        return new RecordingModule(clientFor(deviceId)).searchRecordings(channelId, start, end);
    }

    public void startRecording(String deviceId, int channelId) {
        new RecordingModule(clientFor(deviceId)).startRecording(channelId);
    }

    public void stopRecording(String deviceId, int channelId) {
        new RecordingModule(clientFor(deviceId)).stopRecording(channelId);
    }

    // ─── User Management ─────────────────────────────────────────────────────

    public String searchUsers(String deviceId, int maxResults) {
        return new UserModule(clientFor(deviceId)).searchUsers(maxResults, 0);
    }

    public String addUserToDevice(String deviceId, String employeeNo, String name, String cardNo) {
        String xml = String.format("<UserInfo><employeeNo>%s</employeeNo><name>%s</name><Valid><beginTime>2020-01-01T00:00:00</beginTime><endTime>2037-12-31T23:59:59</endTime></Valid></UserInfo>", employeeNo, name);
        if (isupExecute(deviceId, "/ISAPI/AccessControl/UserInfo/Record", "POST", xml)) return "Success (via ISUP)";
        return new UserModule(clientFor(deviceId)).addUser(employeeNo, name, cardNo, "normal");
    }

    public String deleteUserFromDevice(String deviceId, String employeeNo) {
        if (isupExecute(deviceId, "/ISAPI/AccessControl/UserInfo/Record?employeeNo=" + employeeNo, "DELETE", null)) return "Deleted (via ISUP)";
        return new UserModule(clientFor(deviceId)).deleteUser(employeeNo);
    }

    /** 
     * Tries to execute an ISAPI command.
     */
    private boolean isupExecute(String deviceId, String path, String method, String body) {
        IsupCommandIntent intent = new IsupCommandIntent(path, method, body);
        
        Optional<com.isup.session.DeviceSession> sessionOpt = sessions.findByDeviceId(deviceId);
        if (sessionOpt.isPresent() && sessionOpt.get().isActive()) {
            log.info("ISUP_COMMAND: Dispatching {} to {} with SID {}", method, deviceId, sessionOpt.get().getSessionId());
            io.netty.buffer.ByteBuf packet = com.isup.protocol.IsupProtocol.buildV1IsapiTransparent(sessionOpt.get().getSessionId(), path, method, body);
            sessionOpt.get().getChannel().writeAndFlush(packet);
            return true;
        } else {
            log.info("ISUP_COMMAND: Queuing intent {} for {} (SID will be bound on next login)", method, deviceId);
            commandQueue.computeIfAbsent(deviceId, k -> new java.util.concurrent.ConcurrentLinkedQueue<>()).add(intent);
            return true;
        }
    }

    /** Called by IsupMessageHandler on successful login */
    public static void drainQueue(String deviceId, int sid, io.netty.channel.Channel channel) {
        java.util.Queue<IsupCommandIntent> queue = commandQueue.get(deviceId);
        if (queue != null && !queue.isEmpty()) {
            log.info("ISUP_QUEUE: Draining {} intents for {} with live SID {}", queue.size(), deviceId, sid);
            while (!queue.isEmpty()) {
                IsupCommandIntent intent = queue.poll();
                if (intent != null) {
                    io.netty.buffer.ByteBuf packet = com.isup.protocol.IsupProtocol.buildV1IsapiTransparent(sid, intent.path(), intent.method(), intent.body());
                    channel.write(packet);
                }
            }
            channel.flush();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private IsapiClient clientFor(String deviceId) {
        return clientFor(findDevice(deviceId));
    }

    private IsapiClient clientFor(Device device) {
        String  ip  = device.getDeviceIp();
        String  usr = device.getDeviceUsername() != null ? device.getDeviceUsername() : "admin";
        String  pwd = device.getDevicePassword() != null ? device.getDevicePassword() : "";
        int     port = device.getDevicePort() != null ? device.getDevicePort() : 80;
        boolean ssl  = device.getUseHttps() != null && device.getUseHttps();
        
        if (ip == null || ip.isBlank()) {
            throw new IsapiException("Device has no IP address: " + device.getDeviceId(), 0);
        }
        return new IsapiClient(ip, port, ssl, usr, pwd);
    }

    private Device findDevice(String deviceId) {
        return deviceRepo.findByDeviceId(deviceId)
                .orElseThrow(() -> new IsapiException("Device not found: " + deviceId, 404));
    }

    private boolean hasCap(Device d, String cap) {
        return d.getCapabilities() != null && d.getCapabilities().contains(cap);
    }

    /** Stream URL info record. */
    @lombok.Data @lombok.Builder
    public static class StreamInfo {
        private String rtspUrl;
        private String rtspSubUrl;
    }
}
