package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.collabcandidate.CollabCandidateApproveRequestDto;
import com.min.chalkakserver.dto.collabcandidate.CollabCandidateRequestDto;
import com.min.chalkakserver.dto.collabcandidate.CollabCandidateResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabDetailResponseDto;
import com.min.chalkakserver.dto.framecollab.FrameCollabRequestDto;
import com.min.chalkakserver.entity.CollabCandidate;
import com.min.chalkakserver.exception.CollabCandidateNotFoundException;
import com.min.chalkakserver.repository.CollabCandidateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollabCandidateService 테스트")
class CollabCandidateServiceTest {

    @Mock
    private CollabCandidateRepository collabCandidateRepository;
    @Mock
    private FrameCollabService frameCollabService;

    @InjectMocks
    private CollabCandidateService collabCandidateService;

    private CollabCandidate pendingCandidate() {
        CollabCandidate candidate = CollabCandidate.builder()
                .rawText("뉴진스 X 인생네컷\n2026.06.01 ~ 2026.06.30")
                .title("뉴진스 X 인생네컷")
                .brand("인생네컷")
                .artistName("뉴진스")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();
        ReflectionTestUtils.setField(candidate, "id", 1L);
        return candidate;
    }

    @Test
    @DisplayName("공지 텍스트를 파싱해 후보 초안을 생성한다")
    void createCandidate_ParsesDraft() {
        given(collabCandidateRepository.save(any(CollabCandidate.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        CollabCandidateResponseDto result = collabCandidateService.createCandidate(
                CollabCandidateRequestDto.builder()
                        .sourceUrl("https://instagram.com/p/abc")
                        .rawText("뉴진스 X 인생네컷 한정판 프레임\n2026.06.01 ~ 2026.06.30")
                        .build());

        assertThat(result.getTitle()).isEqualTo("뉴진스 X 인생네컷 한정판 프레임");
        assertThat(result.getBrand()).isEqualTo("인생네컷");
        assertThat(result.getArtistName()).isEqualTo("뉴진스");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.getStatus()).isEqualTo(CollabCandidate.CandidateStatus.PENDING);
    }

    @Test
    @DisplayName("후보를 승인하면 프레임 콜라보가 생성되고 후보가 연결된다")
    void approveCandidate_CreatesCollab() {
        CollabCandidate candidate = pendingCandidate();
        given(collabCandidateRepository.findById(1L)).willReturn(Optional.of(candidate));
        given(frameCollabService.createCollab(any(FrameCollabRequestDto.class)))
                .willReturn(FrameCollabDetailResponseDto.builder().id(77L).title("뉴진스 X 인생네컷").build());

        FrameCollabDetailResponseDto result = collabCandidateService.approveCandidate(1L, null);

        assertThat(result.getId()).isEqualTo(77L);
        assertThat(candidate.getStatus()).isEqualTo(CollabCandidate.CandidateStatus.APPROVED);
        assertThat(candidate.getCreatedFrameCollabId()).isEqualTo(77L);

        ArgumentCaptor<FrameCollabRequestDto> captor = ArgumentCaptor.forClass(FrameCollabRequestDto.class);
        verify(frameCollabService).createCollab(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("뉴진스 X 인생네컷");
        assertThat(captor.getValue().getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    @DisplayName("승인 시 덮어쓰기 필드가 파싱 값보다 우선한다")
    void approveCandidate_OverridesWin() {
        CollabCandidate candidate = pendingCandidate();
        given(collabCandidateRepository.findById(1L)).willReturn(Optional.of(candidate));
        given(frameCollabService.createCollab(any(FrameCollabRequestDto.class)))
                .willReturn(FrameCollabDetailResponseDto.builder().id(77L).build());

        collabCandidateService.approveCandidate(1L,
                CollabCandidateApproveRequestDto.builder()
                        .title("수정된 콜라보명")
                        .endDate(LocalDate.of(2026, 7, 15))
                        .build());

        ArgumentCaptor<FrameCollabRequestDto> captor = ArgumentCaptor.forClass(FrameCollabRequestDto.class);
        verify(frameCollabService).createCollab(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("수정된 콜라보명");
        assertThat(captor.getValue().getEndDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(captor.getValue().getBrand()).isEqualTo("인생네컷");
    }

    @Test
    @DisplayName("필수 필드가 비어 있으면 승인할 수 없다")
    void approveCandidate_MissingFields() {
        CollabCandidate incomplete = CollabCandidate.builder()
                .rawText("기간 미정 공지")
                .title("기간 미정 공지")
                .build();
        ReflectionTestUtils.setField(incomplete, "id", 2L);
        given(collabCandidateRepository.findById(2L)).willReturn(Optional.of(incomplete));

        assertThatThrownBy(() -> collabCandidateService.approveCandidate(2L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("제목/시작일/종료일");
        verify(frameCollabService, never()).createCollab(any());
    }

    @Test
    @DisplayName("대기 상태가 아닌 후보는 승인할 수 없다")
    void approveCandidate_NotPending() {
        CollabCandidate candidate = pendingCandidate();
        candidate.reject();
        given(collabCandidateRepository.findById(1L)).willReturn(Optional.of(candidate));

        assertThatThrownBy(() -> collabCandidateService.approveCandidate(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("대기 중인 후보");
    }

    @Test
    @DisplayName("후보를 거절 처리한다")
    void rejectCandidate() {
        CollabCandidate candidate = pendingCandidate();
        given(collabCandidateRepository.findById(1L)).willReturn(Optional.of(candidate));

        CollabCandidateResponseDto result = collabCandidateService.rejectCandidate(1L);

        assertThat(result.getStatus()).isEqualTo(CollabCandidate.CandidateStatus.REJECTED);
    }

    @Test
    @DisplayName("존재하지 않는 후보 승인 시 예외가 발생한다")
    void approveCandidate_NotFound() {
        given(collabCandidateRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> collabCandidateService.approveCandidate(99L, null))
                .isInstanceOf(CollabCandidateNotFoundException.class);
    }

    @Test
    @DisplayName("잘못된 status 필터는 예외가 발생한다")
    void getCandidates_InvalidStatus() {
        assertThatThrownBy(() -> collabCandidateService.getCandidates("unknown", 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
