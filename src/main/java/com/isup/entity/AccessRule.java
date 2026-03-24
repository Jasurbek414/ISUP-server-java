package com.isup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "access_rules")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AccessRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_no", nullable = false, length = 100)
    private String employeeNo;

    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType; // "blacklist" or "whitelist"

    @Column(name = "device_ids", length = 2000)
    private String deviceIds; // JSON array, null = all devices

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
