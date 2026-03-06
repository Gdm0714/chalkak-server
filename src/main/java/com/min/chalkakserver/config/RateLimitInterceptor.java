package com.min.chalkakserver.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate Limiting Interceptor
 * API 요청에 대한 Rate Limiting을 적용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddress = getClientIP(request);
        String requestUri = request.getRequestURI();

        // API 종류에 따른 Rate Limit 적용
        Bucket bucket;
        if (requestUri.contains("/auth/login") || requestUri.contains("/auth/refresh")) {
            // 인증 API: 브루트포스 공격 방지를 위한 엄격한 제한
            bucket = rateLimitConfig.resolveAuthBucket(ipAddress);
        } else if (requestUri.contains("/report")) {
            // 제보 API: 스팸 방지
            bucket = rateLimitConfig.resolveReportBucket(ipAddress);
        } else {
            // 일반 API
            bucket = rateLimitConfig.resolveBucket(ipAddress);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Rate Limit 헤더 추가
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            // Rate Limit 초과
            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            
            log.warn("Rate limit exceeded for IP: {}, URI: {}, wait time: {}s", ipAddress, requestUri, waitTimeSeconds);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitTimeSeconds));
            response.getWriter().write("{\"error\": \"요청이 너무 많습니다. " + waitTimeSeconds + "초 후에 다시 시도해주세요.\"}");
            
            return false;
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     * 프록시/로드밸런서 뒤에 있는 경우 X-Forwarded-For 헤더 확인
     */
    private String getClientIP(HttpServletRequest request) {
        // remoteAddr를 기본으로 사용 (X-Forwarded-For 스푸핑 방지)
        // 프록시/로드밸런서 뒤에서는 Spring의 server.tomcat.remoteip.internal-proxies 설정으로
        // 신뢰할 수 있는 프록시에서만 X-Forwarded-For를 반영하도록 설정해야 함
        return request.getRemoteAddr();
    }
}
