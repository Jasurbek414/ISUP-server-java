package com.isup.api.controller;

import com.isup.isapi.IsapiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/camera")
public class CameraController {

    private final IsapiService isapiService;

    public CameraController(IsapiService isapiService) {
        this.isapiService = isapiService;
    }

    /** GET /api/camera/{deviceId}/image/{channelId} - image settings */
    @GetMapping("/{deviceId}/image/{channelId}")
    public ResponseEntity<?> getImageSettings(@PathVariable String deviceId, @PathVariable int channelId) {
        return ResponseEntity.ok(isapiService.getCameraImageSettings(deviceId, channelId));
    }

    /** PUT /api/camera/{deviceId}/image/{channelId} */
    @PutMapping("/{deviceId}/image/{channelId}")
    public ResponseEntity<?> updateImageSettings(@PathVariable String deviceId, @PathVariable int channelId,
                                                  @RequestBody String body) {
        isapiService.updateCameraImageSettings(deviceId, channelId, body);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /** GET /api/camera/{deviceId}/streams - streaming channels */
    @GetMapping("/{deviceId}/streams")
    public ResponseEntity<?> getStreamingChannels(@PathVariable String deviceId) {
        return ResponseEntity.ok(isapiService.getStreamingChannels(deviceId));
    }

    /** POST /api/camera/{deviceId}/ptz/{channelId}/move */
    @PostMapping("/{deviceId}/ptz/{channelId}/move")
    public ResponseEntity<?> ptzMove(@PathVariable String deviceId, @PathVariable int channelId,
                                      @RequestBody PtzMoveRequest req) {
        isapiService.ptzMove(deviceId, channelId, req.direction(), req.speed());
        return ResponseEntity.ok(Map.of("status", "moving", "direction", req.direction()));
    }

    /** POST /api/camera/{deviceId}/ptz/{channelId}/stop */
    @PostMapping("/{deviceId}/ptz/{channelId}/stop")
    public ResponseEntity<?> ptzStop(@PathVariable String deviceId, @PathVariable int channelId) {
        isapiService.ptzStop(deviceId, channelId);
        return ResponseEntity.ok(Map.of("status", "stopped"));
    }

    /** POST /api/camera/{deviceId}/ptz/{channelId}/preset/{presetId}/goto */
    @PostMapping("/{deviceId}/ptz/{channelId}/preset/{presetId}/goto")
    public ResponseEntity<?> ptzPresetGoto(@PathVariable String deviceId, @PathVariable int channelId,
                                            @PathVariable int presetId) {
        isapiService.ptzPresetGoto(deviceId, channelId, presetId);
        return ResponseEntity.ok(Map.of("status", "success", "preset", presetId));
    }

    /** GET /api/camera/{deviceId}/ptz/{channelId}/presets */
    @GetMapping("/{deviceId}/ptz/{channelId}/presets")
    public ResponseEntity<?> ptzGetPresets(@PathVariable String deviceId, @PathVariable int channelId) {
        return ResponseEntity.ok(isapiService.ptzGetPresets(deviceId, channelId));
    }

    /** GET /api/camera/{deviceId}/ptz/{channelId}/status */
    @GetMapping("/{deviceId}/ptz/{channelId}/status")
    public ResponseEntity<?> ptzStatus(@PathVariable String deviceId, @PathVariable int channelId) {
        return ResponseEntity.ok(isapiService.ptzGetStatus(deviceId, channelId));
    }

    record PtzMoveRequest(String direction, int speed) {}
}
