package com.isup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "event_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "employee_no", length = 100)
    private String employeeNo;

    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @Column(name = "card_no", length = 100)
    private String cardNo;

    @Column(length = 20)
    private String direction;

    @Column(name = "door_no")
    private Integer doorNo;

    @Column(name = "verify_mode", length = 50)
    private String verifyMode;

    @Column(name = "event_time")
    private Instant eventTime;

    @Column(name = "photo_base64", columnDefinition = "TEXT")
    private String photoBase64;

    @Column(name = "photo_path", length = 512)
    private String photoPath;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "webhook_status", nullable = false, length = 20)
    @Builder.Default
    private String webhookStatus = "pending";

    @Column(name = "webhook_attempts", nullable = false)
    @Builder.Default
    private int webhookAttempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
