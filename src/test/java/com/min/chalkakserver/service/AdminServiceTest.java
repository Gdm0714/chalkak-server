package com.min.chalkakserver.service;

import com.min.chalkakserver.repository.FavoriteRepository;
import com.min.chalkakserver.dto.PhotoBoothRequestDto;
import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.ReviewRepository;
import com.min.chalkakserver.repository.RefreshTokenRepository;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.repository.UserRepository;
import com.min.chalkakserver.dto.admin.AdminStatsDto;
import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.admin.UserListResponseDto;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.exception.ReviewNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 테스트")
class AdminServiceTest {

    @Mock
    private PhotoBoothRepository photoBoothRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AdminService adminService;

    private void setEntityId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== getStats ====================

    @Test
    @DisplayName("전체 통계를 조회하면 AdminStatsDto를 반환한다")
    void getStats_success() {
        // given
        given(photoBoothRepository.count()).willReturn(10L);
        given(userRepository.count()).willReturn(50L);
        given(reviewRepository.count()).willReturn(200L);
        given(favoriteRepository.count()).willReturn(150L);
        given(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(5L);
        given(reviewRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(20L);

        // when
        AdminStatsDto result = adminService.getStats();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalPhotoBooths()).isEqualTo(10L);
        assertThat(result.getTotalUsers()).isEqualTo(50L);
        assertThat(result.getTotalReviews()).isEqualTo(200L);
        assertThat(result.getTotalFavorites()).isEqualTo(150L);
        assertThat(result.getNewUsersToday()).isEqualTo(5L);
        assertThat(result.getNewReviewsToday()).isEqualTo(20L);
    }

    // ==================== createPhotoBooth ====================

    @Test
    @DisplayName("포토부스를 생성하면 저장된 엔티티 기반의 응답 DTO를 반환한다")
    void createPhotoBooth_success() {
        // given
        PhotoBoothRequestDto request = PhotoBoothRequestDto.builder()
                .name("테스트")
                .brand("인생네컷")
                .address("서울")
                .latitude(37.5)
                .longitude(127.0)
                .build();

        PhotoBooth saved = PhotoBooth.builder()
                .name("테스트")
                .brand("인생네컷")
                .address("서울")
                .latitude(37.5)
                .longitude(127.0)
                .build();
        setEntityId(saved, 1L);

        given(photoBoothRepository.save(any(PhotoBooth.class))).willReturn(saved);

        // when
        PhotoBoothResponseDto result = adminService.createPhotoBooth(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("테스트");
        assertThat(result.getBrand()).isEqualTo("인생네컷");
        then(photoBoothRepository).should().save(any(PhotoBooth.class));
    }

    // ==================== updatePhotoBooth ====================

    @Test
    @DisplayName("존재하는 포토부스를 수정하면 수정된 응답 DTO를 반환한다")
    void updatePhotoBooth_success() {
        // given
        PhotoBooth photoBooth = PhotoBooth.builder()
                .name("기존이름")
                .brand("인생네컷")
                .address("서울")
                .latitude(37.5)
                .longitude(127.0)
                .build();
        setEntityId(photoBooth, 1L);

        PhotoBoothRequestDto request = PhotoBoothRequestDto.builder()
                .name("수정이름")
                .brand("포토이즘")
                .address("부산")
                .latitude(35.1)
                .longitude(129.0)
                .build();

        given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));

        // when
        PhotoBoothResponseDto result = adminService.updatePhotoBooth(1L, request);

        // then
        assertThat(result).isNotNull();
        then(photoBoothRepository).should().findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 포토부스를 수정하면 PhotoBoothNotFoundException이 발생한다")
    void updatePhotoBooth_notFound() {
        // given
        PhotoBoothRequestDto request = PhotoBoothRequestDto.builder()
                .name("테스트")
                .brand("인생네컷")
                .address("서울")
                .latitude(37.5)
                .longitude(127.0)
                .build();

        given(photoBoothRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.updatePhotoBooth(999L, request))
                .isInstanceOf(PhotoBoothNotFoundException.class);
    }

    // ==================== deletePhotoBooth ====================

    @Test
    @DisplayName("존재하는 포토부스를 삭제하면 정상적으로 삭제된다")
    void deletePhotoBooth_success() {
        // given
        PhotoBooth photoBooth = PhotoBooth.builder()
                .name("테스트")
                .brand("인생네컷")
                .address("서울")
                .latitude(37.5)
                .longitude(127.0)
                .build();
        setEntityId(photoBooth, 1L);

        given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));

        // when
        adminService.deletePhotoBooth(1L);

        // then
        then(photoBoothRepository).should().delete(photoBooth);
    }

    @Test
    @DisplayName("존재하지 않는 포토부스를 삭제하면 PhotoBoothNotFoundException이 발생한다")
    void deletePhotoBooth_notFound() {
        // given
        given(photoBoothRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.deletePhotoBooth(999L))
                .isInstanceOf(PhotoBoothNotFoundException.class);
    }

    // ==================== getUsers ====================

    @Test
    @DisplayName("유저 목록을 페이지로 조회하면 PagedResponseDto를 반환한다")
    void getUsers_success() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();
        setEntityId(user, 1L);

        Page<User> userPage = new PageImpl<>(List.of(user));
        given(userRepository.findAll(any(Pageable.class))).willReturn(userPage);
        given(reviewRepository.countByUser(user)).willReturn(3L);
        given(favoriteRepository.countByUser(user)).willReturn(2L);

        // when
        PagedResponseDto<UserListResponseDto> result = adminService.getUsers(0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        then(userRepository).should().findAll(any(Pageable.class));
    }

    // ==================== getUser ====================

    @Test
    @DisplayName("존재하는 유저를 조회하면 UserListResponseDto를 반환한다")
    void getUser_success() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();
        setEntityId(user, 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(reviewRepository.countByUser(user)).willReturn(0L);
        given(favoriteRepository.countByUser(user)).willReturn(0L);

        // when
        UserListResponseDto result = adminService.getUser(1L);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 유저를 조회하면 AuthException이 발생한다")
    void getUser_notFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.getUser(999L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("User not found");
    }

    // ==================== updateUserRole ====================

    @Test
    @DisplayName("유효한 역할로 유저 권한을 변경하면 변경된 UserListResponseDto를 반환한다")
    void updateUserRole_success() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();
        setEntityId(user, 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(reviewRepository.countByUser(user)).willReturn(0L);
        given(favoriteRepository.countByUser(user)).willReturn(0L);

        // when
        UserListResponseDto result = adminService.updateUserRole(1L, "ADMIN");

        // then
        assertThat(result).isNotNull();
        then(userRepository).should().findById(1L);
    }

    @Test
    @DisplayName("유효하지 않은 역할로 유저 권한을 변경하면 IllegalArgumentException이 발생한다")
    void updateUserRole_invalidRole() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();
        setEntityId(user, 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> adminService.updateUserRole(1L, "SUPERADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== deleteUser ====================

    @Test
    @DisplayName("존재하는 유저를 삭제하면 관련 데이터도 함께 삭제된다")
    void deleteUser_success() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();
        setEntityId(user, 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        adminService.deleteUser(1L);

        // then
        then(refreshTokenRepository).should().deleteAllByUser(user);
        then(reviewRepository).should().deleteAllByUser(user);
        then(favoriteRepository).should().deleteAllByUser(user);
        then(userRepository).should().delete(user);
    }

    @Test
    @DisplayName("존재하지 않는 유저를 삭제하면 AuthException이 발생한다")
    void deleteUser_notFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.deleteUser(999L))
                .isInstanceOf(AuthException.class);
    }

    // ==================== deleteReview ====================

    @Test
    @DisplayName("존재하는 리뷰를 삭제하면 정상적으로 삭제된다")
    void deleteReview_success() {
        // given
        given(reviewRepository.existsById(1L)).willReturn(true);

        // when
        adminService.deleteReview(1L);

        // then
        then(reviewRepository).should().deleteById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰를 삭제하면 ReviewNotFoundException이 발생한다")
    void deleteReview_notFound() {
        // given
        given(reviewRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.deleteReview(999L))
                .isInstanceOf(ReviewNotFoundException.class);
    }
}
