package com.min.chalkakserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인증 실패 시 응답 상태코드 통합 테스트
 *
 * 클라이언트는 401을 받으면 토큰 갱신을 시도하고, 403은 "권한 없음"으로 처리한다.
 * 만료/무효 토큰이 403으로 내려가면 이 둘을 구분할 수 없으므로 401을 보장한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Security 응답 상태코드 통합 테스트")
class SecurityResponseStatusIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("토큰 없이 보호된 API 호출 시 401을 반환한다")
    void protectedEndpointWithoutToken_returns401() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, null,
                new org.springframework.core.ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody()).containsEntry("status", 401);
        assertThat(response.getBody()).containsEntry("error", "Unauthorized");
        assertThat(response.getBody()).containsEntry("path", "/api/auth/me");
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 보호된 API 호출 시 401을 반환한다")
    void protectedEndpointWithInvalidToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this.is.not.a.valid.jwt");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("잘못된 서명의 refresh token으로 갱신 요청 시 500이 아니라 401을 반환한다")
    void refreshWithForeignSignedToken_returns401NotServerError() {
        // 다른 시크릿으로 서명된 토큰 (구 서버/로컬 환경에서 발급된 토큰)
        String foreignToken = io.jsonwebtoken.Jwts.builder()
                .subject("1")
                .claim("type", "refresh")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "totally-different-secret-key-at-least-32-chars".getBytes()))
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(Map.of("refreshToken", foreignToken), headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
