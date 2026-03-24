package com.isup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "discovered_devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiscoveredDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String ip;

    @Column(unique = true, length = 20)
    private String mac;

    @Column(length = 100)
    private String model;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "serial_no", length = 100)
    private String serialNo;

    @Column(length = 100)
    private String firmware;

    @Column(nullable = false)
    @Builder.Default
    private boolean activated = false;

    @Column(name = "first_seen", nullable = false, updatable = false)
    @Builder.Default
    private Instant firstSeen = Instant.now();

    @Column(name = "last_seen", nullable = false)
    @Builder.Default
    private Instant lastSeen = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean claimed = false;
}
