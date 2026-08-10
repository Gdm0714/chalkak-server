package com.min.chalkakserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.chalkakserver.dto.ErrorResponse;
import com.min.chalkakserver.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${cors.allowed-origins:http://localhost:*,https://localhost:*}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig filterChain Bean is being created");
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Swagger UI 및 API 문서 허용
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // Actuator 헬스체크 엔드포인트 허용
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 인증 관련 엔드포인트 허용
                .requestMatchers("/api/auth/login", "/api/auth/login/email", "/api/auth/register", "/api/auth/refresh", "/api/auth/logout").permitAll()
                // 비밀번호 재설정 및 가입 수단 찾기 (인증 불필요)
                .requestMatchers("/api/auth/password/**", "/api/auth/find-provider").permitAll()
                // 사진관 제보 현황은 인증 필요 (wildcard보다 먼저 선언)
                .requestMatchers(HttpMethod.GET, "/api/photo-booths/reports/**").authenticated()
                // 포토부스 조회 API는 인증 없이 허용 (GET만)
                .requestMatchers(HttpMethod.GET, "/api/photo-booths/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/congestion/**").permitAll()
                // 최근 리뷰 피드는 인증 없이 허용
                .requestMatchers(HttpMethod.GET, "/api/reviews/recent").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/stats/**").permitAll()
                // 피드 조회는 인증 없이 허용 (GET만)
                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/photo-booths/report").permitAll()
                // 헬스체크 허용
                .requestMatchers("/api/health").permitAll()
                // 캐시 관리는 ADMIN만
                .requestMatchers("/api/cache/**").hasRole("ADMIN")
                // 관리자 API는 ADMIN만
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 나머지 API는 인증 필요
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                // 인증 실패(토큰 없음/만료/서명 불일치)는 401로 응답한다.
                // 스프링 시큐리티 기본값은 403이라, 클라이언트가 "권한 없음"과 "토큰 갱신 필요"를
                // 구분할 수 없었다.
                .authenticationEntryPoint((request, response, authException) ->
                    writeErrorResponse(request, response, HttpStatus.UNAUTHORIZED,
                        "Unauthorized", "인증이 필요합니다. 토큰이 없거나 유효하지 않습니다."))
                // 인증은 되었으나 권한이 부족한 경우만 403으로 응답한다.
                .accessDeniedHandler((request, response, deniedException) ->
                    writeErrorResponse(request, response, HttpStatus.FORBIDDEN,
                        "Forbidden", "이 리소스에 접근할 권한이 없습니다."))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 시큐리티 필터 단계의 에러도 컨트롤러 예외와 동일한 ErrorResponse 형태로 응답한다.
     */
    private void writeErrorResponse(HttpServletRequest request, HttpServletResponse response,
                                    HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
            response.getWriter(),
            new ErrorResponse(status.value(), error, message, request.getRequestURI())
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 환경 변수로 허용된 origin 설정
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOriginPatterns(origins);

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
