package com.isup.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "face_libraries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FaceLibrary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String type = "whitelist";

    @Column(name = "device_ids", columnDefinition = "TEXT")
    private String deviceIds; // JSON array string

    @Column(name = "face_count", nullable = false)
    @Builder.Default
    private int faceCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
