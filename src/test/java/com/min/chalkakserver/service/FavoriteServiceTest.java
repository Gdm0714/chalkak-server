package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.favorite.FavoriteResponseDto;
import com.min.chalkakserver.entity.Favorite;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.FavoriteRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService 테스트")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PhotoBoothRepository photoBoothRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private PhotoBooth photoBooth;
    private Favorite favorite;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();
        setEntityId(user, 1L);

        photoBooth = PhotoBooth.builder()
                .name("테스트 사진관")
                .brand("인생네컷")
                .address("서울시 강남구")
                .latitude(37.5)
                .longitude(127.0)
                .build();
        setEntityId(photoBooth, 1L);

        favorite = Favorite.builder()
                .user(user)
                .photoBooth(photoBooth)
                .build();
        setEntityId(favorite, 1L);
    }

    @Nested
    @DisplayName("즐겨찾기 추가 테스트")
    class AddFavoriteTest {

        @Test
        @DisplayName("정상적으로 즐겨찾기를 추가한다")
        void addFavorite_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(favoriteRepository.existsByUserAndPhotoBooth(user, photoBooth)).willReturn(false);
            given(favoriteRepository.save(any(Favorite.class))).willReturn(favorite);

            // when
            FavoriteResponseDto result = favoriteService.addFavorite(1L, 1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPhotoBooth().getId()).isEqualTo(1L);
            verify(favoriteRepository).save(any(Favorite.class));
        }

        @Test
        @DisplayName("이미 즐겨찾기에 있으면 기존 항목을 반환한다")
        void addFavorite_AlreadyExists() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(favoriteRepository.existsByUserAndPhotoBooth(user, photoBooth)).willReturn(true);
            given(favoriteRepository.findByUserAndPhotoBooth(user, photoBooth))
                    .willReturn(Optional.of(favorite));

            // when
            FavoriteResponseDto result = favoriteService.addFavorite(1L, 1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 AuthException 발생")
        void addFavorite_UserNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.addFavorite(1L, 1L))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("포토부스를 찾을 수 없는 경우 PhotoBoothNotFoundException 발생")
        void addFavorite_PhotoBoothNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.addFavorite(1L, 1L))
                    .isInstanceOf(PhotoBoothNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("즐겨찾기 삭제 테스트")
    class RemoveFavoriteTest {

        @Test
        @DisplayName("정상적으로 즐겨찾기를 삭제한다")
        void removeFavorite_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));

            // when
            favoriteService.removeFavorite(1L, 1L);

            // then
            verify(favoriteRepository).deleteByUserAndPhotoBooth(user, photoBooth);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 AuthException 발생")
        void removeFavorite_UserNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.removeFavorite(1L, 1L))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("포토부스를 찾을 수 없는 경우 PhotoBoothNotFoundException 발생")
        void removeFavorite_PhotoBoothNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.removeFavorite(1L, 1L))
                    .isInstanceOf(PhotoBoothNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("내 즐겨찾기 목록 조회 테스트")
    class GetMyFavoritesTest {

        @Test
        @DisplayName("내 즐겨찾기 목록을 반환한다")
        void getMyFavorites_Success() {
            // given
            PhotoBooth photoBooth2 = PhotoBooth.builder()
                    .name("두번째 사진관")
                    .brand("하루필름")
                    .address("서울시 서초구")
                    .latitude(37.49)
                    .longitude(127.01)
                    .build();
            setEntityId(photoBooth2, 2L);

            Favorite favorite2 = Favorite.builder()
                    .user(user)
                    .photoBooth(photoBooth2)
                    .build();
            setEntityId(favorite2, 2L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(favoriteRepository.findByUserWithPhotoBooth(user))
                    .willReturn(Arrays.asList(favorite, favorite2));

            // when
            List<FavoriteResponseDto> result = favoriteService.getMyFavorites(1L);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(1).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("즐겨찾기가 없으면 빈 목록을 반환한다")
        void getMyFavorites_Empty() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(favoriteRepository.findByUserWithPhotoBooth(user))
                    .willReturn(Collections.emptyList());

            // when
            List<FavoriteResponseDto> result = favoriteService.getMyFavorites(1L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 AuthException 발생")
        void getMyFavorites_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.getMyFavorites(999L))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    @DisplayName("즐겨찾기 여부 확인 테스트")
    class IsFavoriteTest {

        @Test
        @DisplayName("즐겨찾기에 있으면 true를 반환한다")
        void isFavorite_True() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(favoriteRepository.existsByUserAndPhotoBooth(user, photoBooth)).willReturn(true);

            // when
            boolean result = favoriteService.isFavorite(1L, 1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("즐겨찾기에 없으면 false를 반환한다")
        void isFavorite_False() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(favoriteRepository.existsByUserAndPhotoBooth(user, photoBooth)).willReturn(false);

            // when
            boolean result = favoriteService.isFavorite(1L, 1L);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("즐겨찾기 수 조회 테스트")
    class GetFavoriteCountTest {

        @Test
        @DisplayName("포토부스의 즐겨찾기 수를 반환한다")
        void getFavoriteCount_Success() {
            // given
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(favoriteRepository.countByPhotoBooth(photoBooth)).willReturn(42L);

            // when
            long result = favoriteService.getFavoriteCount(1L);

            // then
            assertThat(result).isEqualTo(42L);
        }

        @Test
        @DisplayName("포토부스를 찾을 수 없는 경우 PhotoBoothNotFoundException 발생")
        void getFavoriteCount_PhotoBoothNotFound() {
            // given
            given(photoBoothRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.getFavoriteCount(999L))
                    .isInstanceOf(PhotoBoothNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("내 즐겨찾기 목록 페이징 조회 테스트")
    class GetMyFavoritesPagedTest {

        @Test
        @DisplayName("내 즐겨찾기를 페이징으로 조회한다")
        void getMyFavoritesPaged_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            org.springframework.data.domain.Page<Favorite> favoritePage = new org.springframework.data.domain.PageImpl<>(
                    java.util.Collections.singletonList(favorite),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    1
            );
            given(favoriteRepository.findByUserWithPhotoBoothPaged(any(), any())).willReturn(favoritePage);

            // when
            com.min.chalkakserver.dto.PagedResponseDto<com.min.chalkakserver.dto.favorite.FavoriteResponseDto> result =
                    favoriteService.getMyFavoritesPaged(1L, 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 AuthException 발생")
        void getMyFavoritesPaged_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.getMyFavoritesPaged(999L, 0, 10))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    @DisplayName("즐겨찾기 여부 확인 - 엣지 케이스")
    class IsFavoriteEdgeCaseTest {

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 AuthException 발생")
        void isFavorite_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.isFavorite(999L, 1L))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("포토부스를 찾을 수 없는 경우 PhotoBoothNotFoundException 발생")
        void isFavorite_PhotoBoothNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> favoriteService.isFavorite(1L, 999L))
                    .isInstanceOf(PhotoBoothNotFoundException.class);
        }
    }

    private void setEntityId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
