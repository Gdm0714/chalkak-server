package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabDetailResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabRequestDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabResponseDto;
import com.min.chalkakserver.entity.FrameCollab;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.exception.FrameCollabNotFoundException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.FrameCollabRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FrameCollabService 테스트")
class FrameCollabServiceTest {

    @Mock
    private FrameCollabRepository frameCollabRepository;
    @Mock
    private PhotoBoothRepository photoBoothRepository;

    @InjectMocks
    private FrameCollabService frameCollabService;

    private FrameCollab activeCollab;
    private PhotoBooth booth;

    @BeforeEach
    void setUp() {
        activeCollab = FrameCollab.builder()
                .title("뉴진스 X 인생네컷")
                .brand("인생네컷")
                .artistName("뉴진스")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(activeCollab, "id", 1L);

        booth = PhotoBooth.builder()
                .name("인생네컷 홍대점")
                .brand("인생네컷")
                .latitude(37.55)
                .longitude(126.92)
                .build();
        ReflectionTestUtils.setField(booth, "id", 10L);
    }

    @Test
    @DisplayName("진행 중 콜라보 목록을 페이지로 반환한다")
    void getCollabs_Active() {
        given(frameCollabRepository.findActive(any(LocalDate.class), any()))
                .willReturn(new PageImpl<>(List.of(activeCollab), PageRequest.of(0, 20), 1));

        PagedResponseDto<FrameCollabResponseDto> result = frameCollabService.getCollabs("active", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("뉴진스 X 인생네컷");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(FrameCollab.CollabStatus.ACTIVE);
    }

    @Test
    @DisplayName("예정 콜라보 조회 시 upcoming 쿼리를 사용한다")
    void getCollabs_Upcoming() {
        given(frameCollabRepository.findUpcoming(any(LocalDate.class), any()))
                .willReturn(new PageImpl<>(List.of()));

        frameCollabService.getCollabs("upcoming", 0, 20);

        verify(frameCollabRepository).findUpcoming(any(LocalDate.class), any());
        verify(frameCollabRepository, never()).findActive(any(LocalDate.class), any());
    }

    @Test
    @DisplayName("콜라보 상세 조회 시 입고 매장 목록을 포함한다")
    void getCollab_WithBooths() {
        activeCollab.updatePhotoBooths(java.util.Set.of(booth));
        given(frameCollabRepository.findByIdWithBooths(1L)).willReturn(Optional.of(activeCollab));

        FrameCollabDetailResponseDto result = frameCollabService.getCollab(1L);

        assertThat(result.getPhotoBooths()).hasSize(1);
        assertThat(result.getPhotoBooths().get(0).getName()).isEqualTo("인생네컷 홍대점");
    }

    @Test
    @DisplayName("존재하지 않는 콜라보 조회 시 예외가 발생한다")
    void getCollab_NotFound() {
        given(frameCollabRepository.findByIdWithBooths(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> frameCollabService.getCollab(99L))
                .isInstanceOf(FrameCollabNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 매장의 콜라보 조회 시 예외가 발생한다")
    void getCollabsByPhotoBooth_BoothNotFound() {
        given(photoBoothRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> frameCollabService.getCollabsByPhotoBooth(99L))
                .isInstanceOf(PhotoBoothNotFoundException.class);
    }

    @Test
    @DisplayName("매장별 진행/예정 콜라보 목록을 반환한다")
    void getCollabsByPhotoBooth_Success() {
        given(photoBoothRepository.existsById(10L)).willReturn(true);
        given(frameCollabRepository.findCurrentByPhotoBoothId(any(), any(LocalDate.class)))
                .willReturn(List.of(activeCollab));

        List<FrameCollabResponseDto> result = frameCollabService.getCollabsByPhotoBooth(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("입고 매장과 함께 콜라보를 생성한다")
    void createCollab_WithBooths() {
        FrameCollabRequestDto request = FrameCollabRequestDto.builder()
                .title("에스파 X 포토이즘")
                .brand("포토이즘")
                .artistName("에스파")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .photoBoothIds(List.of(10L))
                .build();
        given(photoBoothRepository.findAllById(List.of(10L))).willReturn(List.of(booth));
        given(frameCollabRepository.save(any(FrameCollab.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        FrameCollabDetailResponseDto result = frameCollabService.createCollab(request);

        assertThat(result.getTitle()).isEqualTo("에스파 X 포토이즘");
        assertThat(result.getPhotoBooths()).hasSize(1);
        verify(frameCollabRepository).save(any(FrameCollab.class));
    }

    @Test
    @DisplayName("존재하지 않는 매장 ID로 생성 시 예외가 발생한다")
    void createCollab_MissingBooth() {
        FrameCollabRequestDto request = FrameCollabRequestDto.builder()
                .title("에스파 X 포토이즘")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(14))
                .photoBoothIds(List.of(10L, 99L))
                .build();
        given(photoBoothRepository.findAllById(List.of(10L, 99L))).willReturn(List.of(booth));

        assertThatThrownBy(() -> frameCollabService.createCollab(request))
                .isInstanceOf(PhotoBoothNotFoundException.class)
                .hasMessageContaining("99");
        verify(frameCollabRepository, never()).save(any());
    }

    @Test
    @DisplayName("콜라보 정보를 수정한다")
    void updateCollab_Success() {
        given(frameCollabRepository.findByIdWithBooths(1L)).willReturn(Optional.of(activeCollab));
        FrameCollabRequestDto request = FrameCollabRequestDto.builder()
                .title("수정된 제목")
                .brand("인생네컷")
                .artistName("뉴진스")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(10))
                .build();

        FrameCollabDetailResponseDto result = frameCollabService.updateCollab(1L, request);

        assertThat(result.getTitle()).isEqualTo("수정된 제목");
        assertThat(result.getEndDate()).isEqualTo(LocalDate.now().plusDays(10));
    }

    @Test
    @DisplayName("존재하지 않는 콜라보 삭제 시 예외가 발생한다")
    void deleteCollab_NotFound() {
        given(frameCollabRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> frameCollabService.deleteCollab(99L))
                .isInstanceOf(FrameCollabNotFoundException.class);
        verify(frameCollabRepository, never()).delete(any());
    }

    @Test
    @DisplayName("입고 매장을 일괄 설정한다")
    void setPhotoBooths_Success() {
        given(frameCollabRepository.findByIdWithBooths(1L)).willReturn(Optional.of(activeCollab));
        given(photoBoothRepository.findAllById(List.of(10L))).willReturn(List.of(booth));

        FrameCollabDetailResponseDto result = frameCollabService.setPhotoBooths(1L, List.of(10L));

        assertThat(result.getPhotoBooths()).hasSize(1);
    }

    @Test
    @DisplayName("날짜 기준으로 콜라보 상태를 판별한다")
    void getStatus_ByDate() {
        LocalDate today = LocalDate.of(2026, 6, 12);
        FrameCollab collab = FrameCollab.builder()
                .title("t")
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 20))
                .build();

        assertThat(collab.getStatus(today)).isEqualTo(FrameCollab.CollabStatus.ACTIVE);
        assertThat(collab.getStatus(LocalDate.of(2026, 6, 9))).isEqualTo(FrameCollab.CollabStatus.UPCOMING);
        assertThat(collab.getStatus(LocalDate.of(2026, 6, 21))).isEqualTo(FrameCollab.CollabStatus.ENDED);
        assertThat(collab.getStatus(LocalDate.of(2026, 6, 10))).isEqualTo(FrameCollab.CollabStatus.ACTIVE);
        assertThat(collab.getStatus(LocalDate.of(2026, 6, 20))).isEqualTo(FrameCollab.CollabStatus.ACTIVE);
    }
}
