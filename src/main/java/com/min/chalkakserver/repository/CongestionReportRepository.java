package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.CongestionReport;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CongestionReportRepository extends JpaRepository<CongestionReport, Long> {

    List<CongestionReport> findByPhotoBoothAndCreatedAtAfterOrderByCreatedAtDesc(
            PhotoBooth photoBooth,
            LocalDateTime createdAt
    );

    boolean existsByUserAndPhotoBoothAndCreatedAtAfter(
            User user,
            PhotoBooth photoBooth,
            LocalDateTime createdAt
    );
}
