package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityReportRequestDto;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityReportResponseDto;
import com.min.chalkakserver.dto.frameavailability.FrameAvailabilityResponseDto;
import com.min.chalkakserver.entity.FrameAvailabilityReport;
import com.min.chalkakserver.entity.FrameCollab;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.DuplicateAvailabilityReportException;
import com.min.chalkakserver.exception.FrameCollabNotFoundException;
import com.min.chalkakserver.repository.FrameAvailabilityReportRepository;
import com.min.chalkakserver.repository.FrameCollabRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FrameAvailabilityService 테스트")
class FrameAvailabilityServiceTest {

    @Mock
    private FrameAvailabilityReportRepository availabilityReportRepository;
    @Mock
    private FrameCollabRepository frameCollabRepository;
    @Mock
    private PhotoBoothRepository photoBoothRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FrameAvailabilityService frameAvailabilityService;

    private FrameCollab activeCollab;
    private PhotoBooth booth;
    private User user;

    @BeforeEach
    void setUp() {
        activeCollab = FrameCollab.builder()
                .title("뉴진스 X 인생네컷")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(activeCollab, "id", 1L);

        booth = PhotoBooth.builder()
                .name("인생네컷 홍대점")
                .latitude(37.55)
                .longitude(126.92)
                .build();
        ReflectionTestUtils.setField(booth, "id", 10L);

        user = User.builder()
                .email("user@test.com")
                .provider(User.AuthProvider.EMAIL)
                .providerId("user-1")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 100L);
    }

    private FrameAvailabilityReport report(FrameAvailabilityReport.AvailabilityStatus status, int minutesAgo) {
        FrameAvailabilityReport r = FrameAvailabilityReport.builder()
                .user(user)
                .frameCollab(activeCollab)
                .photoBooth(booth)
                .availabilityStatus(status)
                .build();
        ReflectionTestUtils.setField(r, "createdAt", LocalDateTime.now().minusMinutes(minutesAgo));
        return r;
    }

    @Test
    @DisplayName("제보가 없으면 UNKNOWN 상태를 반환한다")
    void getAvailability_Empty() {
        given(frameCollabRepository.existsById(1L)).willReturn(true);
        given(frameCollabRepository.existsByIdAndPhotoBoothsId(1L, 10L)).willReturn(true);
        given(availabilityReportRepository
                .findByFrameCollabIdAndPhotoBoothIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any(), any()))
                .willReturn(List.of());

        FrameAvailabilityResponseDto result = frameAvailabilityService.getAvailability(1L, 10L);

        assertThat(result.getStatus()).isEqualTo(FrameAvailabilityResponseDto.ResultStatus.UNKNOWN);
        assertThat(result.getSampleSize()).isZero();
    }

    @Test
    @DisplayName("최근 품절 제보가 오래된 있음 제보보다 큰 가중치를 가진다")
    void getAvailability_RecentSoldOutWins() {
        given(frameCollabRepository.existsById(1L)).willReturn(true);
        given(frameCollabRepository.existsByIdAndPhotoBoothsId(1L, 10L)).willReturn(true);
        given(availabilityReportRepository
                .findByFrameCollabIdAndPhotoBoothIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any(), any()))
                .willReturn(List.of(
                        report(FrameAvailabilityReport.AvailabilityStatus.SOLD_OUT, 10),
                        report(FrameAvailabilityReport.AvailabilityStatus.SOLD_OUT, 30),
                        report(FrameAvailabilityReport.AvailabilityStatus.AVAILABLE, 1200)
                ));

        FrameAvailabilityResponseDto result = frameAvailabilityService.getAvailability(1L, 10L);

        assertThat(result.getStatus()).isEqualTo(FrameAvailabilityResponseDto.ResultStatus.SOLD_OUT);
        assertThat(result.getSampleSize()).isEqualTo(3);
        assertThat(result.getConfidenceLevel()).isEqualTo(FrameAvailabilityResponseDto.ConfidenceLevel.MEDIUM);
    }

    @Test
    @DisplayName("콜라보 입고 매장이 아니면 예외가 발생한다")
    void getAvailability_NotCollabBooth() {
        given(frameCollabRepository.existsById(1L)).willReturn(true);
        given(frameCollabRepository.existsByIdAndPhotoBoothsId(1L, 99L)).willReturn(false);

        assertThatThrownBy(() -> frameAvailabilityService.getAvailability(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("입고 매장이 아닙니다");
    }

    @Test
    @DisplayName("존재하지 않는 콜라보 조회 시 예외가 발생한다")
    void getAvailability_CollabNotFound() {
        given(frameCollabRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> frameAvailabilityService.getAvailability(99L, 10L))
                .isInstanceOf(FrameCollabNotFoundException.class);
    }

    @Test
    @DisplayName("콜라보 전체 매장 조회 시 제보 없는 매장은 UNKNOWN으로 반환한다")
    void getCollabAvailability_MixesUnknown() {
        PhotoBooth booth2 = PhotoBooth.builder()
                .name("인생네컷 강남점")
                .latitude(37.49)
                .longitude(127.02)
                .build();
        ReflectionTestUtils.setField(booth2, "id", 20L);
        activeCollab.updatePhotoBooths(Set.of(booth, booth2));

        given(frameCollabRepository.findByIdWithBooths(1L)).willReturn(Optional.of(activeCollab));
        given(availabilityReportRepository
                .findByFrameCollabIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                .willReturn(List.of(report(FrameAvailabilityReport.AvailabilityStatus.AVAILABLE, 5)));

        List<FrameAvailabilityResponseDto> result = frameAvailabilityService.getCollabAvailability(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPhotoBoothId()).isEqualTo(10L);
        assertThat(result.get(0).getStatus()).isEqualTo(FrameAvailabilityResponseDto.ResultStatus.AVAILABLE);
        assertThat(result.get(1).getPhotoBoothId()).isEqualTo(20L);
        assertThat(result.get(1).getStatus()).isEqualTo(FrameAvailabilityResponseDto.ResultStatus.UNKNOWN);
    }

    @Test
    @DisplayName("재고를 제보하면 저장되고 응답을 반환한다")
    void submitReport_Success() {
        given(userRepository.findById(100L)).willReturn(Optional.of(user));
        given(frameCollabRepository.findById(1L)).willReturn(Optional.of(activeCollab));
        given(photoBoothRepository.findById(10L)).willReturn(Optional.of(booth));
        given(frameCollabRepository.existsById(1L)).willReturn(true);
        given(frameCollabRepository.existsByIdAndPhotoBoothsId(1L, 10L)).willReturn(true);
        given(availabilityReportRepository
                .existsByUserAndFrameCollabAndPhotoBoothAndCreatedAtAfter(any(), any(), any(), any()))
                .willReturn(false);
        given(availabilityReportRepository.save(any(FrameAvailabilityReport.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        FrameAvailabilityReportResponseDto result = frameAvailabilityService.submitReport(
                100L, 1L, 10L,
                FrameAvailabilityReportRequestDto.builder()
                        .availabilityStatus(FrameAvailabilityReport.AvailabilityStatus.SOLD_OUT)
                        .build());

        assertThat(result.getFrameCollabId()).isEqualTo(1L);
        assertThat(result.getPhotoBoothId()).isEqualTo(10L);
        verify(availabilityReportRepository).save(any(FrameAvailabilityReport.class));
    }

    @Test
    @DisplayName("쿨다운 내 중복 제보 시 예외가 발생한다")
    void submitReport_Cooldown() {
        given(userRepository.findById(100L)).willReturn(Optional.of(user));
        given(frameCollabRepository.findById(1L)).willReturn(Optional.of(activeCollab));
        given(photoBoothRepository.findById(10L)).willReturn(Optional.of(booth));
        given(frameCollabRepository.existsById(1L)).willReturn(true);
        given(frameCollabRepository.existsByIdAndPhotoBoothsId(1L, 10L)).willReturn(true);
        given(availabilityReportRepository
                .existsByUserAndFrameCollabAndPhotoBoothAndCreatedAtAfter(any(), any(), any(), any()))
                .willReturn(true);

        assertThatThrownBy(() -> frameAvailabilityService.submitReport(
                100L, 1L, 10L,
                FrameAvailabilityReportRequestDto.builder()
                        .availabilityStatus(FrameAvailabilityReport.AvailabilityStatus.AVAILABLE)
                        .build()))
                .isInstanceOf(DuplicateAvailabilityReportException.class);
        verify(availabilityReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("진행 중이 아닌 콜라보에는 제보할 수 없다")
    void submitReport_NotActiveCollab() {
        FrameCollab endedCollab = FrameCollab.builder()
                .title("종료된 콜라보")
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(1))
                .build();
        ReflectionTestUtils.setField(endedCollab, "id", 2L);

        given(userRepository.findById(100L)).willReturn(Optional.of(user));
        given(frameCollabRepository.findById(2L)).willReturn(Optional.of(endedCollab));
        given(photoBoothRepository.findById(10L)).willReturn(Optional.of(booth));
        given(frameCollabRepository.existsById(2L)).willReturn(true);
        given(frameCollabRepository.existsByIdAndPhotoBoothsId(2L, 10L)).willReturn(true);

        assertThatThrownBy(() -> frameAvailabilityService.submitReport(
                100L, 2L, 10L,
                FrameAvailabilityReportRequestDto.builder()
                        .availabilityStatus(FrameAvailabilityReport.AvailabilityStatus.AVAILABLE)
                        .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("진행 중인 콜라보");
        verify(availabilityReportRepository, never()).save(any());
    }
}
