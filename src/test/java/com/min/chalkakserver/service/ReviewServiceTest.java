package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.review.ReviewRequestDto;
import com.min.chalkakserver.dto.review.ReviewResponseDto;
import com.min.chalkakserver.dto.review.ReviewStatsDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.Review;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.DuplicateReviewException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.exception.ReviewNotFoundException;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.ReviewRepository;
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
@DisplayName("ReviewService 테스트")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PhotoBoothRepository photoBoothRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private PhotoBooth photoBooth;
    private Review review;

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

        review = Review.builder()
                .user(user)
                .photoBooth(photoBooth)
                .rating(4)
                .content("좋아요!")
                .build();
        setEntityId(review, 1L);
    }

    @Nested
    @DisplayName("리뷰 작성 테스트")
    class CreateReviewTest {

        @Test
        @DisplayName("정상적으로 리뷰를 작성한다")
        void createReview_Success() {
            // given
            ReviewRequestDto request = ReviewRequestDto.builder()
                    .rating(4)
                    .content("좋아요!")
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(reviewRepository.existsByUserAndPhotoBooth(user, photoBooth)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(review);

            // when
            ReviewResponseDto result = reviewService.createReview(1L, 1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getRating()).isEqualTo(4);
            assertThat(result.getContent()).isEqualTo("좋아요!");
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("이미 리뷰를 작성한 경우 DuplicateReviewException 발생")
        void createReview_Duplicate() {
            // given
            ReviewRequestDto request = ReviewRequestDto.builder()
                    .rating(4)
                    .content("좋아요!")
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(reviewRepository.existsByUserAndPhotoBooth(user, photoBooth)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
                    .isInstanceOf(DuplicateReviewException.class)
                    .satisfies(ex -> {
                        DuplicateReviewException e = (DuplicateReviewException) ex;
                        assertThat(e.getPhotoBoothId()).isEqualTo(1L);
                    });
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 AuthException 발생")
        void createReview_UserNotFound() {
            // given
            ReviewRequestDto request = ReviewRequestDto.builder()
                    .rating(4)
                    .content("좋아요!")
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("포토부스를 찾을 수 없는 경우 PhotoBoothNotFoundException 발생")
        void createReview_PhotoBoothNotFound() {
            // given
            ReviewRequestDto request = ReviewRequestDto.builder()
                    .rating(4)
                    .content("좋아요!")
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
                    .isInstanceOf(PhotoBoothNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("리뷰 수정 테스트")
    class UpdateReviewTest {

        @Test
        @DisplayName("정상적으로 리뷰를 수정한다")
        void updateReview_Success() {
            // given
            ReviewRequestDto request = ReviewRequestDto.builder()
                    .rating(5)
                    .content("아주 좋아요!")
                    .build();
            given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

            // when
            ReviewResponseDto result = reviewService.updateReview(1L, 1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getContent()).isEqualTo("아주 좋아요!");
        }

        @Test
        @DisplayName("작성자가 아닌 경우 AuthException 발생")
        void updateReview_NotOwner() {
            // given
            ReviewRequestDto request = ReviewRequestDto.builder()
                    .rating(5)
                    .content("아주 좋아요!")
                    .build();
            given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.updateReview(99L, 1L, request))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("리뷰를 수정할 권한이 없습니다.");
        }

        @Test
        @DisplayName("리뷰를 찾을 수 없는 경우 ReviewNotFoundException 발생")
        void updateReview_NotFound() {
            // given
            ReviewRequestDto request = ReviewRequestDto.builder()
                    .rating(5)
                    .content("아주 좋아요!")
                    .build();
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.updateReview(1L, 999L, request))
                    .isInstanceOf(ReviewNotFoundException.class)
                    .satisfies(ex -> {
                        ReviewNotFoundException e = (ReviewNotFoundException) ex;
                        assertThat(e.getReviewId()).isEqualTo(999L);
                    });
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 테스트")
    class DeleteReviewTest {

        @Test
        @DisplayName("정상적으로 리뷰를 삭제한다")
        void deleteReview_Success() {
            // given
            given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

            // when
            reviewService.deleteReview(1L, 1L);

            // then
            verify(reviewRepository).delete(review);
        }

        @Test
        @DisplayName("작성자가 아닌 경우 AuthException 발생")
        void deleteReview_NotOwner() {
            // given
            given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.deleteReview(99L, 1L))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("리뷰를 삭제할 권한이 없습니다.");
        }

        @Test
        @DisplayName("리뷰를 찾을 수 없는 경우 ReviewNotFoundException 발생")
        void deleteReview_NotFound() {
            // given
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.deleteReview(1L, 999L))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("포토부스 리뷰 목록 조회 테스트")
    class GetPhotoBoothReviewsTest {

        @Test
        @DisplayName("포토부스 리뷰 목록을 반환한다")
        void getPhotoBoothReviews_Success() {
            // given
            Review review2 = Review.builder()
                    .user(user)
                    .photoBooth(photoBooth)
                    .rating(3)
                    .content("보통이에요")
                    .build();
            setEntityId(review2, 2L);

            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(reviewRepository.findByPhotoBoothWithUser(photoBooth))
                    .willReturn(Arrays.asList(review, review2));

            // when
            List<ReviewResponseDto> result = reviewService.getPhotoBoothReviews(1L);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("포토부스를 찾을 수 없는 경우 PhotoBoothNotFoundException 발생")
        void getPhotoBoothReviews_PhotoBoothNotFound() {
            // given
            given(photoBoothRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getPhotoBoothReviews(999L))
                    .isInstanceOf(PhotoBoothNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("내 리뷰 목록 조회 테스트")
    class GetMyReviewsTest {

        @Test
        @DisplayName("내가 작성한 리뷰 목록을 반환한다")
        void getMyReviews_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(reviewRepository.findByUserWithPhotoBooth(user))
                    .willReturn(Collections.singletonList(review));

            // when
            List<ReviewResponseDto> result = reviewService.getMyReviews(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 AuthException 발생")
        void getMyReviews_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getMyReviews(999L))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    @DisplayName("리뷰 통계 조회 테스트")
    class GetReviewStatsTest {

        @Test
        @DisplayName("리뷰가 있을 때 평균 평점과 분포를 반환한다")
        void getReviewStats_WithReviews() {
            // given
            List<Object[]> distribution = Arrays.asList(
                    new Object[]{5, 3L},
                    new Object[]{4, 2L},
                    new Object[]{3, 1L}
            );
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(reviewRepository.getAverageRatingByPhotoBooth(photoBooth)).willReturn(4.333);
            given(reviewRepository.countByPhotoBooth(photoBooth)).willReturn(6L);
            given(reviewRepository.getRatingDistribution(photoBooth)).willReturn(distribution);

            // when
            ReviewStatsDto result = reviewService.getReviewStats(1L);

            // then
            assertThat(result.getPhotoBoothId()).isEqualTo(1L);
            assertThat(result.getTotalCount()).isEqualTo(6L);
            // averageRating should be rounded to 1 decimal place: Math.round(4.333 * 10) / 10.0 = 4.3
            assertThat(result.getAverageRating()).isEqualTo(4.3);
            assertThat(result.getRatingDistribution().get(5)).isEqualTo(3L);
            assertThat(result.getRatingDistribution().get(4)).isEqualTo(2L);
            assertThat(result.getRatingDistribution().get(3)).isEqualTo(1L);
            assertThat(result.getRatingDistribution().get(2)).isEqualTo(0L);
            assertThat(result.getRatingDistribution().get(1)).isEqualTo(0L);
        }

        @Test
        @DisplayName("리뷰가 없을 때 averageRating=0.0, 분포 전체 0을 반환한다")
        void getReviewStats_NoReviews() {
            // given
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(reviewRepository.getAverageRatingByPhotoBooth(photoBooth)).willReturn(null);
            given(reviewRepository.countByPhotoBooth(photoBooth)).willReturn(0L);
            given(reviewRepository.getRatingDistribution(photoBooth)).willReturn(Collections.emptyList());

            // when
            ReviewStatsDto result = reviewService.getReviewStats(1L);

            // then
            assertThat(result.getAverageRating()).isEqualTo(0.0);
            assertThat(result.getTotalCount()).isEqualTo(0L);
            for (int i = 1; i <= 5; i++) {
                assertThat(result.getRatingDistribution().get(i)).isEqualTo(0L);
            }
        }
    }

    @Nested
    @DisplayName("특정 리뷰 조회 테스트")
    class GetReviewTest {

        @Test
        @DisplayName("리뷰를 정상적으로 조회한다")
        void getReview_Success() {
            // given
            given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

            // when
            ReviewResponseDto result = reviewService.getReview(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("리뷰를 찾을 수 없는 경우 ReviewNotFoundException 발생")
        void getReview_NotFound() {
            // given
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getReview(999L))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("특정 포토부스 내 리뷰 조회 테스트")
    class GetMyReviewForPhotoBoothTest {

        @Test
        @DisplayName("내가 작성한 리뷰가 있으면 반환한다")
        void getMyReviewForPhotoBooth_Found() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(reviewRepository.findByUserAndPhotoBooth(user, photoBooth))
                    .willReturn(Optional.of(review));

            // when
            ReviewResponseDto result = reviewService.getMyReviewForPhotoBooth(1L, 1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("내가 작성한 리뷰가 없으면 null을 반환한다")
        void getMyReviewForPhotoBooth_NotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(photoBoothRepository.findById(1L)).willReturn(Optional.of(photoBooth));
            given(reviewRepository.findByUserAndPhotoBooth(user, photoBooth))
                    .willReturn(Optional.empty());

            // when
            ReviewResponseDto result = reviewService.getMyReviewForPhotoBooth(1L, 1L);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("포토부스 리뷰 목록 페이징 조회 테스트")
    class GetPhotoBoothReviewsPagedTest {

        @Test
        @DisplayName("포토부스 리뷰를 페이징으로 조회한다")
        void getPhotoBoothReviewsPaged_Success() {
            // given
            given(photoBoothRepository.findById(1L)).willReturn(java.util.Optional.of(photoBooth));
            org.springframework.data.domain.Page<Review> reviewPage = new org.springframework.data.domain.PageImpl<>(
                    java.util.Collections.singletonList(review),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    1
            );
            given(reviewRepository.findByPhotoBoothWithUserPaged(any(), any())).willReturn(reviewPage);

            // when
            com.min.chalkakserver.dto.PagedResponseDto<ReviewResponseDto> result =
                    reviewService.getPhotoBoothReviewsPaged(1L, 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("포토부스를 찾을 수 없으면 예외가 발생한다")
        void getPhotoBoothReviewsPaged_NotFound() {
            // given
            given(photoBoothRepository.findById(999L)).willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getPhotoBoothReviewsPaged(999L, 0, 10))
                    .isInstanceOf(com.min.chalkakserver.exception.PhotoBoothNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("내 리뷰 목록 페이징 조회 테스트")
    class GetMyReviewsPagedTest {

        @Test
        @DisplayName("내 리뷰를 페이징으로 조회한다")
        void getMyReviewsPaged_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(java.util.Optional.of(user));
            org.springframework.data.domain.Page<Review> reviewPage = new org.springframework.data.domain.PageImpl<>(
                    java.util.Collections.singletonList(review),
                    org.springframework.data.domain.PageRequest.of(0, 10),
                    1
            );
            given(reviewRepository.findByUserWithPhotoBoothPaged(any(), any())).willReturn(reviewPage);

            // when
            com.min.chalkakserver.dto.PagedResponseDto<ReviewResponseDto> result =
                    reviewService.getMyReviewsPaged(1L, 0, 10);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외가 발생한다")
        void getMyReviewsPaged_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getMyReviewsPaged(999L, 0, 10))
                    .isInstanceOf(com.min.chalkakserver.exception.AuthException.class);
        }
    }

    @Nested
    @DisplayName("리뷰 통계 - 포토부스 못찾을때")
    class GetReviewStatsEdgeCaseTest {

        @Test
        @DisplayName("포토부스를 찾을 수 없으면 예외가 발생한다")
        void getReviewStats_PhotoBoothNotFound() {
            // given
            given(photoBoothRepository.findById(999L)).willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getReviewStats(999L))
                    .isInstanceOf(com.min.chalkakserver.exception.PhotoBoothNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("내 리뷰 포토부스 조회 - 엣지 케이스")
    class GetMyReviewForPhotoBoothEdgeCaseTest {

        @Test
        @DisplayName("사용자를 찾을 수 없으면 예외가 발생한다")
        void getMyReviewForPhotoBooth_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getMyReviewForPhotoBooth(999L, 1L))
                    .isInstanceOf(com.min.chalkakserver.exception.AuthException.class);
        }

        @Test
        @DisplayName("포토부스를 찾을 수 없으면 예외가 발생한다")
        void getMyReviewForPhotoBooth_PhotoBoothNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(java.util.Optional.of(user));
            given(photoBoothRepository.findById(999L)).willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getMyReviewForPhotoBooth(1L, 999L))
                    .isInstanceOf(com.min.chalkakserver.exception.PhotoBoothNotFoundException.class);
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
