package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.PhotoBoothReportDto;
import com.min.chalkakserver.dto.report.ReportResponseDto;
import com.min.chalkakserver.entity.PhotoBoothReport;
import com.min.chalkakserver.entity.ReportStatus;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.repository.PhotoBoothReportRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoBoothReportService {

    private final PhotoBoothReportRepository reportRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final double NEARBY_VERIFICATION_RADIUS_KM = 1.0; // 1km 이내면 근처 인증
    private static final double DUPLICATE_CHECK_RADIUS_KM = 0.05; // 50m 이내면 중복

    /**
     * 제보 접수 (DB 저장 + 이메일 전송)
     */
    @Transactional
    public ReportResponseDto submitReport(PhotoBoothReportDto reportDto, Long userId) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // GPS 거리 계산
        Double distance = null;
        Boolean isNearbyVerified = null;
        if (reportDto.getUserLatitude() != null && reportDto.getUserLongitude() != null) {
            distance = calculateDistance(
                reportDto.getUserLatitude(), reportDto.getUserLongitude(),
                reportDto.getLatitude(), reportDto.getLongitude()
            );
            isNearbyVerified = distance <= NEARBY_VERIFICATION_RADIUS_KM;
            log.info("제보 GPS 검증 - 거리: {}km, 근처 인증: {}",
                String.format("%.3f", distance), isNearbyVerified);
        }

        // 중복 제보 확인
        long nearbyReports = reportRepository.countNearbyReports(
            reportDto.getLatitude(), reportDto.getLongitude());
        long nearbyBooths = reportRepository.countNearbyPhotoBooths(
            reportDto.getLatitude(), reportDto.getLongitude());

        ReportStatus initialStatus = ReportStatus.PENDING;
        if (nearbyBooths > 0) {
            initialStatus = ReportStatus.DUPLICATE;
            log.info("중복 제보 감지 - 반경 50m 이내 기존 포토부스 존재: {}", reportDto.getName());
        } else if (nearbyReports > 0) {
            log.info("유사 제보 감지 - 반경 50m 이내 기존 제보 {}건: {}", nearbyReports, reportDto.getName());
        }

        // DB 저장
        PhotoBoothReport report = PhotoBoothReport.builder()
            .user(user)
            .name(reportDto.getName())
            .brand(reportDto.getBrand())
            .series(reportDto.getSeries())
            .address(reportDto.getAddress())
            .roadAddress(reportDto.getRoadAddress())
            .latitude(reportDto.getLatitude())
            .longitude(reportDto.getLongitude())
            .description(reportDto.getDescription())
            .priceInfo(reportDto.getPriceInfo())
            .reporterName(reportDto.getReporterName())
            .reporterEmail(reportDto.getReporterEmail())
            .userLatitude(reportDto.getUserLatitude())
            .userLongitude(reportDto.getUserLongitude())
            .distanceFromReport(distance)
            .isNearbyVerified(isNearbyVerified)
            .status(initialStatus)
            .build();

        PhotoBoothReport savedReport = reportRepository.save(report);
        log.info("제보 접수 완료 - ID: {}, 이름: {}, 상태: {}, GPS검증: {}",
            savedReport.getId(), savedReport.getName(), initialStatus, isNearbyVerified);

        // 이메일도 비동기로 전송 (기존 기능 유지)
        emailService.sendPhotoBoothReport(reportDto);

        return ReportResponseDto.from(savedReport);
    }

    /**
     * 내 제보 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReportResponseDto> getMyReports(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        return reportRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(ReportResponseDto::from)
            .collect(Collectors.toList());
    }

    /**
     * 내 제보 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ReportResponseDto> getMyReportsPaged(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<PhotoBoothReport> reportPage = reportRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        List<ReportResponseDto> content = reportPage.getContent().stream()
            .map(ReportResponseDto::from)
            .collect(Collectors.toList());

        return new PagedResponseDto<>(
            content,
            reportPage.getNumber(),
            reportPage.getSize(),
            reportPage.getTotalElements(),
            reportPage.getTotalPages(),
            reportPage.isFirst(),
            reportPage.isLast(),
            reportPage.hasNext(),
            reportPage.hasPrevious()
        );
    }

    /**
     * 제보 통계 (사용자용)
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getMyReportStats(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        long total = reportRepository.countByUser(user);
        long approved = reportRepository.countByUserAndStatus(user, ReportStatus.APPROVED);

        return Map.of(
            "totalReports", total,
            "approvedReports", approved
        );
    }

    /**
     * Haversine 공식으로 두 좌표 간 거리 계산 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // 지구 반경 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
