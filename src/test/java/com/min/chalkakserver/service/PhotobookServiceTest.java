package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.photobook.PhotobookCollectionResponseDto;
import com.min.chalkakserver.dto.photobook.PhotobookEntryRequestDto;
import com.min.chalkakserver.dto.photobook.PhotobookEntryResponseDto;
import com.min.chalkakserver.entity.FrameCollab;
import com.min.chalkakserver.entity.PhotobookEntry;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.FrameCollabNotFoundException;
import com.min.chalkakserver.exception.PhotobookEntryNotFoundException;
import com.min.chalkakserver.repository.FrameCollabRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.PhotobookEntryRepository;
import com.min.chalkakserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@DisplayName("PhotobookService 테스트")
class PhotobookServiceTest {

    @Mock
    private PhotobookEntryRepository photobookEntryRepository;
    @Mock
    private FrameCollabRepository frameCollabRepository;
    @Mock
    private PhotoBoothRepository photoBoothRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PhotobookService photobookService;

    private User user;
    private User otherUser;
    private FrameCollab collab;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("user@test.com")
                .provider(User.AuthProvider.EMAIL)
                .providerId("user-1")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 100L);

        otherUser = User.builder()
                .email("other@test.com")
                .provider(User.AuthProvider.EMAIL)
                .providerId("user-2")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(otherUser, "id", 200L);

        collab = FrameCollab.builder()
                .title("뉴진스 X 인생네컷")
                .artistName("뉴진스")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(collab, "id", 1L);
    }

    private PhotobookEntry entry(User owner) {
        PhotobookEntry e = PhotobookEntry.builder()
                .user(owner)
                .frameCollab(collab)
                .imageUrl("https://cdn.test/photo.jpg")
                .build();
        ReflectionTestUtils.setField(e, "id", 10L);
        return e;
    }

    @Test
    @DisplayName("콜라보 태깅과 함께 포토북 사진을 등록한다")
    void createEntry_WithCollab() {
        given(userRepository.findById(100L)).willReturn(Optional.of(user));
        given(frameCollabRepository.findById(1L)).willReturn(Optional.of(collab));
        given(photobookEntryRepository.save(any(PhotobookEntry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        PhotobookEntryResponseDto result = photobookService.createEntry(100L,
                PhotobookEntryRequestDto.builder()
                        .imageUrl("https://cdn.test/photo.jpg")
                        .frameCollabId(1L)
                        .build());

        assertThat(result.getImageUrl()).isEqualTo("https://cdn.test/photo.jpg");
        assertThat(result.getFrameCollabId()).isEqualTo(1L);
        assertThat(result.getFrameCollabTitle()).isEqualTo("뉴진스 X 인생네컷");
        verify(photobookEntryRepository).save(any(PhotobookEntry.class));
    }

    @Test
    @DisplayName("존재하지 않는 콜라보 태깅 시 예외가 발생한다")
    void createEntry_CollabNotFound() {
        given(userRepository.findById(100L)).willReturn(Optional.of(user));
        given(frameCollabRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> photobookService.createEntry(100L,
                PhotobookEntryRequestDto.builder()
                        .imageUrl("https://cdn.test/photo.jpg")
                        .frameCollabId(99L)
                        .build()))
                .isInstanceOf(FrameCollabNotFoundException.class);
        verify(photobookEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("태깅 없이도 사진을 등록할 수 있다")
    void createEntry_WithoutTags() {
        given(userRepository.findById(100L)).willReturn(Optional.of(user));
        given(photobookEntryRepository.save(any(PhotobookEntry.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        PhotobookEntryResponseDto result = photobookService.createEntry(100L,
                PhotobookEntryRequestDto.builder()
                        .imageUrl("https://cdn.test/photo.jpg")
                        .build());

        assertThat(result.getFrameCollabId()).isNull();
        assertThat(result.getPhotoBoothId()).isNull();
    }

    @Test
    @DisplayName("내 포토북을 최신순 페이지로 조회한다")
    void getMyEntries() {
        given(photobookEntryRepository.findByUserIdOrderByCreatedAtDesc(any(), any()))
                .willReturn(new PageImpl<>(List.of(entry(user)), PageRequest.of(0, 20), 1));

        PagedResponseDto<PhotobookEntryResponseDto> result = photobookService.getMyEntries(100L, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("본인 소유가 아닌 사진 삭제 시 예외가 발생한다")
    void deleteEntry_NotOwner() {
        given(photobookEntryRepository.findById(10L)).willReturn(Optional.of(entry(otherUser)));

        assertThatThrownBy(() -> photobookService.deleteEntry(100L, 10L))
                .isInstanceOf(AuthException.class);
        verify(photobookEntryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 사진 삭제 시 예외가 발생한다")
    void deleteEntry_NotFound() {
        given(photobookEntryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> photobookService.deleteEntry(100L, 99L))
                .isInstanceOf(PhotobookEntryNotFoundException.class);
    }

    @Test
    @DisplayName("본인 사진은 삭제된다")
    void deleteEntry_Success() {
        PhotobookEntry owned = entry(user);
        given(photobookEntryRepository.findById(10L)).willReturn(Optional.of(owned));

        photobookService.deleteEntry(100L, 10L);

        verify(photobookEntryRepository).delete(owned);
    }

    @Test
    @DisplayName("도감은 콜라보별 사진 수와 수집 여부를 집계한다")
    void getMyCollection() {
        FrameCollab uncollected = FrameCollab.builder()
                .title("에스파 X 포토이즘")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(2))
                .build();
        ReflectionTestUtils.setField(uncollected, "id", 2L);

        PhotobookEntryRepository.CollabPhotoCount count = new PhotobookEntryRepository.CollabPhotoCount() {
            @Override
            public Long getCollabId() {
                return 1L;
            }

            @Override
            public Long getPhotoCount() {
                return 3L;
            }
        };

        given(photobookEntryRepository.countByUserGroupedByCollab(100L)).willReturn(List.of(count));
        given(frameCollabRepository.findAll(any(Sort.class))).willReturn(List.of(collab, uncollected));
        given(photobookEntryRepository.countByUserId(100L)).willReturn(4L);

        PhotobookCollectionResponseDto result = photobookService.getMyCollection(100L);

        assertThat(result.getTotalCollabs()).isEqualTo(2);
        assertThat(result.getCollectedCollabs()).isEqualTo(1);
        assertThat(result.getTotalPhotos()).isEqualTo(4L);
        assertThat(result.getItems().get(0).isCollected()).isTrue();
        assertThat(result.getItems().get(0).getPhotoCount()).isEqualTo(3L);
        assertThat(result.getItems().get(1).isCollected()).isFalse();
        assertThat(result.getItems().get(1).getPhotoCount()).isZero();
    }
}
