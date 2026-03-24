package com.isup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 64)
    private String deviceId;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;  // plain password stored (or empty for no-auth)

    @Column(length = 200)
    private String name;

    @Column(length = 300)
    private String location;

    @Column(length = 100)
    private String model;

    @Column(name = "device_type", nullable = false, length = 50)
    @Builder.Default
    private String deviceType = "face_terminal";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Project project;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "offline";

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 100)
    private String firmware;

    @Column(name = "device_ip", length = 50)
    private String deviceIp;

    @Column(name = "device_username", length = 50)
    private String deviceUsername;

    @Column(name = "device_password", length = 255)
    private String devicePassword;

    @Column(length = 2000)
    private String capabilities; // JSON array: ["face","door","rtsp"]

    // ─── Extended device metadata (added in V4 migration) ────────────────────

    @Column(name = "serial_no", length = 100)
    private String serialNo;

    @Column(name = "mac_address", length = 20)
    private String macAddress;

    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;

    @Column(name = "device_port")
    @Builder.Default
    private Integer devicePort = 80;

    @Column(name = "use_https")
    @Builder.Default
    private Boolean useHttps = false;

    @Column(name = "device_channels")
    @Builder.Default
    private Integer deviceChannels = 1;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
