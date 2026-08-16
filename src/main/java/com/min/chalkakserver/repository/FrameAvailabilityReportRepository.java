package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.FrameAvailabilityReport;
import com.min.chalkakserver.entity.FrameCollab;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FrameAvailabilityReportRepository extends JpaRepository<FrameAvailabilityReport, Long> {

    List<FrameAvailabilityReport> findByFrameCollabIdAndPhotoBoothIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long frameCollabId, Long photoBoothId, LocalDateTime cutoff);

    List<FrameAvailabilityReport> findByFrameCollabIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long frameCollabId, LocalDateTime cutoff);

    boolean existsByUserAndFrameCollabAndPhotoBoothAndCreatedAtAfter(
            User user, FrameCollab frameCollab, PhotoBooth photoBooth, LocalDateTime cutoff);
}
