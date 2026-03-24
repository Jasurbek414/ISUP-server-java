package com.isup.api.controller;

import com.isup.isapi.IsapiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final IsapiService isapiService;

    public StreamController(IsapiService isapiService) {
        this.isapiService = isapiService;
    }

    /**
     * GET /api/stream/{deviceId}
     * Returns RTSP URLs for the device.
     */
    @GetMapping("/{deviceId}")
    public ResponseEntity<?> getStreamUrls(@PathVariable String deviceId) {
        try {
            IsapiService.StreamInfo info = isapiService.getStreamInfo(deviceId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
