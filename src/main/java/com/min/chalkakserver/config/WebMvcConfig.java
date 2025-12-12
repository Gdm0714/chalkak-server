package com.min.chalkakserver.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 * Interceptor 등록 등 웹 관련 설정을 담당합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")  // API 경로에만 적용
                .excludePathPatterns(
                        "/actuator/**",      // 헬스체크 제외
                        "/swagger-ui/**",    // Swagger 제외
                        "/v3/api-docs/**"    // API 문서 제외
                );
    }
}
