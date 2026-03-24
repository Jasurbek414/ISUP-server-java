package com.isup.repository;

import com.isup.entity.FaceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface FaceRecordRepository extends JpaRepository<FaceRecord, Long> {
    List<FaceRecord> findByLibraryIdOrderByCreatedAtDesc(Long libraryId);
    long countByLibraryId(Long libraryId);
    long countByUploadStatus(String uploadStatus);

    @Transactional
    void deleteByLibraryId(Long libraryId);
}
