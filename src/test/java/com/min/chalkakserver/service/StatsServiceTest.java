package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.stats.AppStatsSummaryDto;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.ReviewRepository;
import com.min.chalkakserver.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService 테스트")
class StatsServiceTest {

    @Mock
    private PhotoBoothRepository photoBoothRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    @DisplayName("앱 공개 통계 요약은 사진관, 사용자, 리뷰 수를 앱 계약 필드로 반환한다")
    void getSummary_returnsAppStatsContract() {
        given(photoBoothRepository.count()).willReturn(123L);
        given(userRepository.count()).willReturn(45L);
        given(reviewRepository.count()).willReturn(678L);

        AppStatsSummaryDto result = statsService.getSummary();

        assertThat(result.getPhotoBoothCount()).isEqualTo(123L);
        assertThat(result.getActiveUserCount()).isEqualTo(45L);
        assertThat(result.getReviewCount()).isEqualTo(678L);
    }
}
