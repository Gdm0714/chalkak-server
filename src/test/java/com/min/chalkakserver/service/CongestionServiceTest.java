package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.congestion.CongestionReportRequestDto;
import com.min.chalkakserver.dto.congestion.CongestionReportResponseDto;
import com.min.chalkakserver.dto.congestion.CongestionResponseDto;
import com.min.chalkakserver.entity.CongestionReport;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.DuplicateCongestionReportException;
import com.min.chalkakserver.repository.CongestionReportRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CongestionService 테스트")
class CongestionServiceTest {

    @Mock
    private CongestionReportRepository congestionReportRepository;
    @Mock
    private PhotoBoothRepository photoBoothRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CongestionService congestionService;

    private PhotoBooth photoBooth;
    private User user;

    @BeforeEach
    void setUp() {
        photoBooth = PhotoBooth.builder()
                .name("테스트 부스")
                .address("서울시")
                .latitude(37.5)
                .longitude(127.0)
                .build();
        user = User.builder()
                .email("user@test.com")
                .provider(User.AuthProvider.EMAIL)
                .providerId("user-1")
                .role(User.Role.USER)
                .build();
    }

    @Test
    @DisplayName("최근 제보가 없으면 UNKNOWN 혼잡도를 반환한다")
    void getCurrentCongestion_Empty() {
        given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
        given(congestionReportRepository.findByPhotoBoothAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                .willReturn(List.of());

        CongestionResponseDto result = congestionService.getCurrentCongestion(1L);

        assertThat(result.getCongestionLevel()).isEqualTo(CongestionReport.CongestionLevel.UNKNOWN);
        assertThat(result.getSampleSize()).isEqualTo(0);
        assertThat(result.getConfidenceLevel()).isEqualTo(CongestionResponseDto.ConfidenceLevel.LOW);
    }

    @Test
    @DisplayName("최근 제보를 가중 집계해 혼잡도를 반환한다")
    void getCurrentCongestion_Aggregated() {
        given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));

        CongestionReport r1 = CongestionReport.builder()
                .user(user)
                .photoBooth(photoBooth)
                .congestionLevel(CongestionReport.CongestionLevel.BUSY)
                .build();
        setCreatedAt(r1, LocalDateTime.now().minusMinutes(5));

        CongestionReport r2 = CongestionReport.builder()
                .user(user)
                .photoBooth(photoBooth)
                .congestionLevel(CongestionReport.CongestionLevel.VERY_BUSY)
                .build();
        setCreatedAt(r2, LocalDateTime.now().minusMinutes(10));

        CongestionReport r3 = CongestionReport.builder()
                .user(user)
                .photoBooth(photoBooth)
                .congestionLevel(CongestionReport.CongestionLevel.NORMAL)
                .build();
        setCreatedAt(r3, LocalDateTime.now().minusMinutes(20));

        given(congestionReportRepository.findByPhotoBoothAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                .willReturn(List.of(r1, r2, r3));

        CongestionResponseDto result = congestionService.getCurrentCongestion(1L);

        assertThat(result.getCongestionLevel()).isIn(
                CongestionReport.CongestionLevel.BUSY,
                CongestionReport.CongestionLevel.VERY_BUSY
        );
        assertThat(result.getSampleSize()).isEqualTo(3);
        assertThat(result.getEstimatedWaitMinutesMin()).isNotNull();
        assertThat(result.getEstimatedWaitMinutesMax()).isNotNull();
    }

    @Test
    @DisplayName("최근 1시간 내 중복 제보 시 예외가 발생한다")
    void submitReport_Duplicate() {
        CongestionReportRequestDto request = buildRequest(CongestionReport.CongestionLevel.NORMAL);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(photoBoothRepository.findById(2L)).willReturn(Optional.of(photoBooth));
        given(congestionReportRepository.existsByUserAndPhotoBoothAndCreatedAtAfter(any(), any(), any()))
                .willReturn(true);

        assertThatThrownBy(() -> congestionService.submitReport(1L, 2L, request))
                .isInstanceOf(DuplicateCongestionReportException.class);
    }

    @Test
    @DisplayName("혼잡도 제보를 저장하고 응답을 반환한다")
    void submitReport_Success() {
        CongestionReportRequestDto request = buildRequest(CongestionReport.CongestionLevel.RELAXED);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(photoBoothRepository.findById(2L)).willReturn(Optional.of(photoBooth));
        given(congestionReportRepository.existsByUserAndPhotoBoothAndCreatedAtAfter(any(), any(), any()))
                .willReturn(false);
        given(congestionReportRepository.save(any(CongestionReport.class)))
                .willAnswer(invocation -> {
                    CongestionReport report = invocation.getArgument(0);
                    setCreatedAt(report, LocalDateTime.now());
                    return report;
                });

        CongestionReportResponseDto response = congestionService.submitReport(1L, 2L, request);

        ArgumentCaptor<CongestionReport> captor = ArgumentCaptor.forClass(CongestionReport.class);
        verify(congestionReportRepository).save(captor.capture());
        assertThat(captor.getValue().getCongestionLevel()).isEqualTo(CongestionReport.CongestionLevel.RELAXED);
        assertThat(response.getPhotoBoothId()).isEqualTo(2L);
        assertThat(response.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("제보가 6건 이상이면 HIGH 신뢰도를 반환한다")
    void getCurrentCongestion_HighConfidence() {
        given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));

        // 6건의 RELAXED 제보 생성
        java.util.List<CongestionReport> reports = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            CongestionReport r = CongestionReport.builder()
                    .user(user)
                    .photoBooth(photoBooth)
                    .congestionLevel(CongestionReport.CongestionLevel.RELAXED)
                    .build();
            setCreatedAt(r, LocalDateTime.now().minusMinutes(i * 5));
            reports.add(r);
        }

        given(congestionReportRepository.findByPhotoBoothAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                .willReturn(reports);

        CongestionResponseDto result = congestionService.getCurrentCongestion(1L);

        assertThat(result.getConfidenceLevel()).isEqualTo(CongestionResponseDto.ConfidenceLevel.HIGH);
        assertThat(result.getSampleSize()).isEqualTo(6);
    }

    @Test
    @DisplayName("제보가 3-5건이면 MEDIUM 신뢰도를 반환한다")
    void getCurrentCongestion_MediumConfidence() {
        given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));

        java.util.List<CongestionReport> reports = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            CongestionReport r = CongestionReport.builder()
                    .user(user)
                    .photoBooth(photoBooth)
                    .congestionLevel(CongestionReport.CongestionLevel.NORMAL)
                    .build();
            setCreatedAt(r, LocalDateTime.now().minusMinutes(i * 5));
            reports.add(r);
        }

        given(congestionReportRepository.findByPhotoBoothAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                .willReturn(reports);

        CongestionResponseDto result = congestionService.getCurrentCongestion(1L);

        assertThat(result.getConfidenceLevel()).isEqualTo(CongestionResponseDto.ConfidenceLevel.MEDIUM);
    }

    @Test
    @DisplayName("존재하지 않는 포토부스의 혼잡도를 조회하면 예외가 발생한다")
    void getCurrentCongestion_PhotoBoothNotFound() {
        given(photoBoothRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> congestionService.getCurrentCongestion(999L))
                .isInstanceOf(com.min.chalkakserver.exception.PhotoBoothNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 제보하면 예외가 발생한다")
    void submitReport_UserNotFound() {
        CongestionReportRequestDto request = buildRequest(CongestionReport.CongestionLevel.NORMAL);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> congestionService.submitReport(999L, 1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("존재하지 않는 포토부스에 제보하면 예외가 발생한다")
    void submitReport_PhotoBoothNotFound() {
        CongestionReportRequestDto request = buildRequest(CongestionReport.CongestionLevel.NORMAL);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(photoBoothRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> congestionService.submitReport(1L, 999L, request))
                .isInstanceOf(com.min.chalkakserver.exception.PhotoBoothNotFoundException.class);
    }

    private CongestionReportRequestDto buildRequest(CongestionReport.CongestionLevel level) {
        CongestionReportRequestDto request = new CongestionReportRequestDto();
        try {
            Field field = CongestionReportRequestDto.class.getDeclaredField("congestionLevel");
            field.setAccessible(true);
            field.set(request, level);
            return request;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setCreatedAt(CongestionReport report, LocalDateTime createdAt) {
        try {
            Field field = CongestionReport.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(report, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
