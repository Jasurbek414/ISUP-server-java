package com.isup.repository;

import com.isup.entity.FaceLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface FaceLibraryRepository extends JpaRepository<FaceLibrary, Long> {
    List<FaceLibrary> findAllByOrderByCreatedAtDesc();
    List<FaceLibrary> findByProjectId(Long projectId);

    @Modifying @Transactional
    @Query("UPDATE FaceLibrary l SET l.faceCount = (SELECT COUNT(r) FROM FaceRecord r WHERE r.library.id = l.id), l.updatedAt = CURRENT_TIMESTAMP WHERE l.id = :id")
    void refreshFaceCount(Long id);
}
