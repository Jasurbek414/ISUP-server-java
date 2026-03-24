package com.isup.api.controller;

import com.isup.isapi.IsapiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/device-config")
public class DeviceConfigController {

    private final IsapiService isapiService;

    public DeviceConfigController(IsapiService isapiService) {
        this.isapiService = isapiService;
    }

    /** GET /api/device-config/{deviceId}/info - full device system info */
    @GetMapping("/{deviceId}/info")
    public ResponseEntity<?> getDeviceInfo(@PathVariable String deviceId) {
        return ResponseEntity.ok(isapiService.getDeviceSystemInfo(deviceId));
    }

    /** GET /api/device-config/{deviceId}/status - CPU, memory, temperature */
    @GetMapping("/{deviceId}/status")
    public ResponseEntity<?> getDeviceStatus(@PathVariable String deviceId) {
        return ResponseEntity.ok(isapiService.getDeviceStatus(deviceId));
    }

    /** GET /api/device-config/{deviceId}/network - network settings */
    @GetMapping("/{deviceId}/network")
    public ResponseEntity<?> getNetworkConfig(@PathVariable String deviceId) {
        return ResponseEntity.ok(isapiService.getNetworkConfig(deviceId));
    }

    /** GET /api/device-config/{deviceId}/storage - HDD/storage info */
    @GetMapping("/{deviceId}/storage")
    public ResponseEntity<?> getStorageInfo(@PathVariable String deviceId) {
        return ResponseEntity.ok(isapiService.getStorageInfo(deviceId));
    }

    /** POST /api/device-config/{deviceId}/reboot - reboot device */
    @PostMapping("/{deviceId}/reboot")
    public ResponseEntity<?> reboot(@PathVariable String deviceId) {
        isapiService.rebootDevice(deviceId);
        return ResponseEntity.ok(Map.of("status", "rebooting"));
    }

    /** GET /api/device-config/{deviceId}/alarm/inputs - alarm inputs */
    @GetMapping("/{deviceId}/alarm/inputs")
    public ResponseEntity<?> getAlarmInputs(@PathVariable String deviceId) {
        return ResponseEntity.ok(isapiService.getAlarmInputs(deviceId));
    }

    /** POST /api/device-config/{deviceId}/alarm/outputs/{outputId}/trigger */
    @PostMapping("/{deviceId}/alarm/outputs/{outputId}/trigger")
    public ResponseEntity<?> triggerAlarm(@PathVariable String deviceId, @PathVariable int outputId) {
        isapiService.triggerAlarmOutput(deviceId, outputId);
        return ResponseEntity.ok(Map.of("status", "triggered"));
    }

    /** GET /api/device-config/{deviceId}/motion/{channelId} - motion detection */
    @GetMapping("/{deviceId}/motion/{channelId}")
    public ResponseEntity<?> getMotionDetection(@PathVariable String deviceId, @PathVariable int channelId) {
        return ResponseEntity.ok(isapiService.getMotionDetection(deviceId, channelId));
    }

    /** GET /api/device-config/{deviceId}/recordings - search recordings */
    @GetMapping("/{deviceId}/recordings")
    public ResponseEntity<?> searchRecordings(@PathVariable String deviceId,
                                               @RequestParam(defaultValue = "101") String channelId,
                                               @RequestParam String startTime,
                                               @RequestParam String endTime) {
        return ResponseEntity.ok(isapiService.searchRecordings(deviceId, channelId, startTime, endTime));
    }

    /** POST /api/device-config/{deviceId}/recordings/{channelId}/start */
    @PostMapping("/{deviceId}/recordings/{channelId}/start")
    public ResponseEntity<?> startRecording(@PathVariable String deviceId, @PathVariable int channelId) {
        isapiService.startRecording(deviceId, channelId);
        return ResponseEntity.ok(Map.of("status", "recording"));
    }

    /** POST /api/device-config/{deviceId}/recordings/{channelId}/stop */
    @PostMapping("/{deviceId}/recordings/{channelId}/stop")
    public ResponseEntity<?> stopRecording(@PathVariable String deviceId, @PathVariable int channelId) {
        isapiService.stopRecording(deviceId, channelId);
        return ResponseEntity.ok(Map.of("status", "stopped"));
    }

    /** GET /api/device-config/{deviceId}/users - list users on device */
    @GetMapping("/{deviceId}/users")
    public ResponseEntity<?> listUsers(@PathVariable String deviceId,
                                        @RequestParam(defaultValue = "50") int maxResults) {
        return ResponseEntity.ok(isapiService.searchUsers(deviceId, maxResults));
    }

    /** POST /api/device-config/{deviceId}/users - add user to device */
    @PostMapping("/{deviceId}/users")
    public ResponseEntity<?> addUser(@PathVariable String deviceId, @RequestBody AddUserRequest req) {
        return ResponseEntity.ok(isapiService.addUserToDevice(deviceId, req.employeeNo(), req.name(), req.cardNo()));
    }

    /** DELETE /api/device-config/{deviceId}/users/{employeeNo} */
    @DeleteMapping("/{deviceId}/users/{employeeNo}")
    public ResponseEntity<?> deleteUser(@PathVariable String deviceId, @PathVariable String employeeNo) {
        return ResponseEntity.ok(isapiService.deleteUserFromDevice(deviceId, employeeNo));
    }

    record AddUserRequest(String employeeNo, String name, String cardNo) {}
}
