package com.isup.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of(
            "version", "v1.1.4-STABLE",
            "timestamp", String.valueOf(System.currentTimeMillis()),
            "status", "online",
            "ui_hint", "Amber logo, Professional forms"
        );
    }
}
