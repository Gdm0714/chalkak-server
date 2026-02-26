package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.PhotoBoothReport;
import com.min.chalkakserver.entity.ReportStatus;
import com.min.chalkakserver.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoBoothReportRepository extends JpaRepository<PhotoBoothReport, Long> {

    // 내 제보 목록
    List<PhotoBoothReport> findByUserOrderByCreatedAtDesc(User user);

    Page<PhotoBoothReport> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 상태별 제보 목록 (관리자용)
    Page<PhotoBoothReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    // 중복 제보 감지 - 반경 50m 이내 같은 이름의 기존 제보 확인
    @Query(value = """
        SELECT COUNT(*) FROM photo_booth_reports pbr
        WHERE pbr.status NOT IN ('REJECTED', 'DUPLICATE')
        AND (6371 * acos(cos(radians(:latitude)) * cos(radians(pbr.latitude)) *
            cos(radians(pbr.longitude) - radians(:longitude)) +
            sin(radians(:latitude)) * sin(radians(pbr.latitude)))) <= 0.05
        """, nativeQuery = true)
    long countNearbyReports(@Param("latitude") double latitude, @Param("longitude") double longitude);

    // 반경 내 기존 등록된 포토부스 확인 (중복 등록 방지)
    @Query(value = """
        SELECT COUNT(*) FROM photo_booths pb
        WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(pb.latitude)) *
            cos(radians(pb.longitude) - radians(:longitude)) +
            sin(radians(:latitude)) * sin(radians(pb.latitude)))) <= 0.05
        """, nativeQuery = true)
    long countNearbyPhotoBooths(@Param("latitude") double latitude, @Param("longitude") double longitude);

    // 사용자 제보 수
    long countByUser(User user);

    // 사용자의 승인된 제보 수
    long countByUserAndStatus(User user, ReportStatus status);
}
