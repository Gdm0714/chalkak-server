package com.min.chalkakserver.controller;

import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.Review;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.ReviewRepository;
import com.min.chalkakserver.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Stats API 통합 테스트")
class StatsIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PhotoBoothRepository photoBoothRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setUp() {
        deleteStatsFixtures();
    }

    @AfterEach
    void tearDown() {
        deleteStatsFixtures();
    }

    private void deleteStatsFixtures() {
        reviewRepository.deleteAll();
        photoBoothRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/stats/summary는 인증 없이 앱 통계 JSON을 반환한다")
    void getSummary_returnsPublicStatsWithoutAuthentication() {
        User user = userRepository.save(User.builder()
                .email("stats@test.com")
                .nickname("stats")
                .provider(User.AuthProvider.EMAIL)
                .providerId("stats@test.com")
                .role(User.Role.USER)
                .build());
        PhotoBooth photoBooth = photoBoothRepository.save(PhotoBooth.builder()
                .name("통계 테스트 사진관")
                .brand("인생네컷")
                .address("서울")
                .latitude(37.5)
                .longitude(127.0)
                .build());
        reviewRepository.save(Review.builder()
                .user(user)
                .photoBooth(photoBooth)
                .rating(5)
                .content("좋아요")
                .build());

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/stats/summary",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("photoBoothCount", 1);
        assertThat(response.getBody()).containsEntry("activeUserCount", 1);
        assertThat(response.getBody()).containsEntry("reviewCount", 1);
    }
}
