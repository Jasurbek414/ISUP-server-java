package com.isup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "face_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FaceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private FaceLibrary library;

    @Column(name = "employee_no", nullable = false, length = 100)
    private String employeeNo;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 10)
    @Builder.Default
    private String gender = "male";

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String position;

    @Column(name = "photo_base64", columnDefinition = "TEXT")
    private String photoBase64;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "upload_status", length = 20)
    @Builder.Default
    private String uploadStatus = "pending";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
