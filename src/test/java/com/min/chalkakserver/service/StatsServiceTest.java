package com.min.chalkakserver.service;

import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.ReviewRepository;
import com.min.chalkakserver.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

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
    @DisplayName("앱 통계 요약을 조회한다")
    void getSummary_success() {
        given(photoBoothRepository.count()).willReturn(12L);
        given(userRepository.count()).willReturn(34L);
        given(reviewRepository.count()).willReturn(56L);

        Map<String, Long> result = statsService.getSummary();

        assertThat(result)
                .containsEntry("photoBoothCount", 12L)
                .containsEntry("activeUserCount", 34L)
                .containsEntry("reviewCount", 56L);
    }
}
