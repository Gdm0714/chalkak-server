package com.min.chalkakserver.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RateLimitConfig rateLimitConfig;

    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRemoteAddr("192.168.1.1");
    }

    private Bucket buildAllowingBucket(long remaining) {
        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        given(probe.isConsumed()).willReturn(true);
        given(probe.getRemainingTokens()).willReturn(remaining);
        given(bucket.tryConsumeAndReturnRemaining(1)).willReturn(probe);
        return bucket;
    }

    private Bucket buildBlockingBucket(long nanosToWait) {
        Bucket bucket = mock(Bucket.class);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        given(probe.isConsumed()).willReturn(false);
        given(probe.getNanosToWaitForRefill()).willReturn(nanosToWait);
        given(bucket.tryConsumeAndReturnRemaining(1)).willReturn(probe);
        return bucket;
    }

    @Test
    @DisplayName("일반 API - 제한 내 요청 → true 반환, X-Rate-Limit-Remaining 헤더 설정")
    void preHandle_generalApi_withinLimit_returnsTrue() throws Exception {
        // given
        request.setRequestURI("/api/photo-booths");
        Bucket bucket = buildAllowingBucket(59L);
        given(rateLimitConfig.resolveBucket(anyString())).willReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("59");
        verify(rateLimitConfig).resolveBucket("192.168.1.1");
    }

    @Test
    @DisplayName("인증 API /auth/login - 제한 내 요청 → resolveAuthBucket 사용")
    void preHandle_authLoginApi_withinLimit_usesAuthBucket() throws Exception {
        // given
        request.setRequestURI("/api/auth/login");
        Bucket bucket = buildAllowingBucket(9L);
        given(rateLimitConfig.resolveAuthBucket(anyString())).willReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("9");
        verify(rateLimitConfig).resolveAuthBucket("192.168.1.1");
    }

    @Test
    @DisplayName("인증 API /auth/refresh - 제한 내 요청 → resolveAuthBucket 사용")
    void preHandle_authRefreshApi_withinLimit_usesAuthBucket() throws Exception {
        // given
        request.setRequestURI("/api/auth/refresh");
        Bucket bucket = buildAllowingBucket(8L);
        given(rateLimitConfig.resolveAuthBucket(anyString())).willReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        verify(rateLimitConfig).resolveAuthBucket("192.168.1.1");
    }

    @Test
    @DisplayName("제보 API /report - 제한 내 요청 → resolveReportBucket 사용")
    void preHandle_reportApi_withinLimit_usesReportBucket() throws Exception {
        // given
        request.setRequestURI("/api/photo-booths/report");
        Bucket bucket = buildAllowingBucket(4L);
        given(rateLimitConfig.resolveReportBucket(anyString())).willReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("4");
        verify(rateLimitConfig).resolveReportBucket("192.168.1.1");
    }

    @Test
    @DisplayName("Rate limit 초과 → false 반환, 429 상태, Retry-After 헤더 설정")
    void preHandle_rateLimitExceeded_returnsFalseWith429() throws Exception {
        // given
        request.setRequestURI("/api/photo-booths");
        long nanosToWait = 30_000_000_000L; // 30 seconds
        Bucket bucket = buildBlockingBucket(nanosToWait);
        given(rateLimitConfig.resolveBucket(anyString())).willReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-Rate-Limit-Retry-After-Seconds")).isEqualTo("30");
    }

    @Test
    @DisplayName("IP 추출 - getRemoteAddr() 사용")
    void preHandle_ipExtraction_usesRemoteAddr() throws Exception {
        // given
        request.setRequestURI("/api/photo-booths");
        request.setRemoteAddr("10.0.0.1");
        Bucket bucket = buildAllowingBucket(50L);
        given(rateLimitConfig.resolveBucket("10.0.0.1")).willReturn(bucket);

        // when
        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        verify(rateLimitConfig).resolveBucket("10.0.0.1");
    }

    @Test
    @DisplayName("Rate limit 초과 시 응답 본문에 에러 메시지 포함")
    void preHandle_rateLimitExceeded_includesErrorMessageInBody() throws Exception {
        // given
        request.setRequestURI("/api/reviews");
        long nanosToWait = 10_000_000_000L; // 10 seconds
        Bucket bucket = buildBlockingBucket(nanosToWait);
        given(rateLimitConfig.resolveBucket(anyString())).willReturn(bucket);

        // when
        rateLimitInterceptor.preHandle(request, response, new Object());

        // then
        assertThat(response.getContentType()).contains("application/json");
        String body = response.getContentAsString();
        assertThat(body).contains("error");
    }
}
